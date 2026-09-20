package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.NovelImportResult;
import com.novelgeneration.novel.entity.NovelInfo;
import com.novelgeneration.novel.mapper.GenerateOutlineMapper;
import com.novelgeneration.novel.mapper.NovelTextMapper;
import com.novelgeneration.novel.service.NovelImportService;
import com.novelgeneration.novel.vo.NovelVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Resource;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Service
public class NovelImportServiceImpl implements NovelImportService {
    private static final Pattern CHAPTER_HEADING = Pattern.compile(
            "(?im)^(第\\s*[0-9零〇一二两三四五六七八九十百千万]+\\s*[章节回].*|chapter\\s+\\d+.*)$");
    private static final List<String> SUPPORTED_EXTENSIONS = List.of("txt", "docx");

    @Resource
    private GenerateOutlineMapper generateOutlineMapper;

    @Resource
    private NovelTextMapper novelTextMapper;

    @Value("${novel.import-dir:uploads/novels}")
    private String importDir;

    @Value("${novel.import.max-bytes:52428800}")
    private long maxBytes;

    @Override
    @Transactional
    public NovelImportResult importNovel(MultipartFile file, String requestedTitle) {
        validateFile(file);
        String extension = extension(file.getOriginalFilename());
        String sourceText = "docx".equals(extension) ? parseDocx(file) : parseText(file);
        List<NovelVO.NovelText> chapters = splitChapters(sourceText);
        if (chapters.isEmpty()) {
            throw new IllegalArgumentException("文件中没有可导入的正文内容");
        }

        String title = normalizedTitle(requestedTitle, file.getOriginalFilename());
        NovelInfo novel = new NovelInfo();
        novel.setNovelTitle(title);
        novel.setCategory("上传导入");
        novel.setNovelLength("自定义");
        novel.setEndingType("未设置");
        novel.setWritingStyle("以原文为准");
        novel.setTargetAudience("未设置");
        novel.setProtagonist("以原文为准");
        // novel_info.role_list is a MySQL JSON column; imported novels have no structured roles yet.
        novel.setRoleList("[]");
        novel.setBackground("以原文为准");
        novel.setWorldRule("以原文为准");
        novel.setTheme("以原文为准");
        novel.setTriggerEvent("以原文为准");
        novel.setForeshadowCount(0);
        novel.setNarrativeView("以原文为准");
        novel.setAvoidContent("");
        generateOutlineMapper.add(novel);

        LocalDateTime now = LocalDateTime.now();
        for (NovelVO.NovelText chapter : chapters) {
            novelTextMapper.insertChapter(novel.getId(), chapter, now, now);
        }
        saveSource(file, novel.getId(), extension);

        NovelImportResult result = new NovelImportResult();
        result.setNovelId(novel.getId());
        result.setNovelTitle(title);
        result.setChapterCount(chapters.size());
        for (NovelVO.NovelText chapter : chapters) {
            NovelImportResult.ChapterPreview preview = new NovelImportResult.ChapterPreview();
            preview.setChapterNum(chapter.getChapterNum());
            preview.setChapterTitle(chapter.getChapterTitle());
            preview.setCharacterCount(chapter.getChapterText().length());
            result.getChapters().add(preview);
        }
        return result;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的小说文件");
        }
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("小说文件不能超过 " + (maxBytes / 1024 / 1024) + "MB");
        }
        if (!SUPPORTED_EXTENSIONS.contains(extension(file.getOriginalFilename()))) {
            throw new IllegalArgumentException("暂只支持 TXT 和 DOCX 文件");
        }
    }

    private String parseText(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            try {
                return decode(bytes, StandardCharsets.UTF_8);
            } catch (CharacterCodingException ignored) {
                return decode(bytes, Charset.forName("GB18030"));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("读取 TXT 文件失败", e);
        }
    }

    private String decode(byte[] bytes, Charset charset) throws CharacterCodingException {
        CharBuffer buffer = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes));
        return buffer.toString();
    }

    private String parseDocx(MultipartFile file) {
        Path temp = null;
        try {
            temp = Files.createTempFile("novel-import-", ".docx");
            file.transferTo(temp);
            try (ZipFile zip = new ZipFile(temp.toFile())) {
                ZipEntry entry = zip.getEntry("word/document.xml");
                if (entry == null) {
                    throw new IllegalArgumentException("DOCX 文件缺少正文内容");
                }
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                Document document = factory.newDocumentBuilder().parse(zip.getInputStream(entry));
                StringBuilder text = new StringBuilder();
                NodeList paragraphs = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "p");
                for (int i = 0; i < paragraphs.getLength(); i++) {
                    NodeList texts = ((org.w3c.dom.Element) paragraphs.item(i)).getElementsByTagNameNS(
                            "http://schemas.openxmlformats.org/wordprocessingml/2006/main", "t");
                    for (int j = 0; j < texts.getLength(); j++) {
                        text.append(texts.item(j).getTextContent());
                    }
                    text.append('\n');
                }
                return text.toString();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("读取 DOCX 文件失败", e);
        } catch (Exception e) {
            throw new IllegalArgumentException("解析 DOCX 文件失败", e);
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException e) {
                    log.warn("清理 DOCX 临时文件失败: {}", temp, e);
                }
            }
        }
    }

    private List<NovelVO.NovelText> splitChapters(String rawText) {
        String text = rawText == null ? "" : rawText.replace("\uFEFF", "").replace("\r\n", "\n").trim();
        if (text.isBlank()) {
            return List.of();
        }
        List<Heading> headings = new ArrayList<>();
        Matcher matcher = CHAPTER_HEADING.matcher(text);
        while (matcher.find()) {
            headings.add(new Heading(matcher.start(), matcher.end(), matcher.group().trim()));
        }
        if (headings.isEmpty()) {
            return List.of(chapter(1, "全文", text));
        }
        List<NovelVO.NovelText> chapters = new ArrayList<>();
        for (int i = 0; i < headings.size(); i++) {
            Heading heading = headings.get(i);
            int bodyStart = heading.end;
            int bodyEnd = i + 1 < headings.size() ? headings.get(i + 1).start : text.length();
            String body = text.substring(bodyStart, bodyEnd).trim();
            if (!body.isBlank()) {
                chapters.add(chapter(chapters.size() + 1, heading.title, body));
            }
        }
        return chapters;
    }

    private NovelVO.NovelText chapter(int number, String title, String text) {
        NovelVO.NovelText chapter = new NovelVO.NovelText();
        chapter.setChapterNum(number);
        chapter.setChapterTitle(title);
        chapter.setChapterText(text);
        chapter.setChapterSummary("");
        return chapter;
    }

    private void saveSource(MultipartFile file, Long novelId, String extension) {
        try {
            Path directory = Paths.get(importDir).resolve("novels").resolve(String.valueOf(novelId)).resolve("source");
            Files.createDirectories(directory);
            String filename = "original-" + UUID.randomUUID() + "." + extension;
            Files.copy(file.getInputStream(), directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException("保存原始小说文件失败", e);
        }
    }

    private String normalizedTitle(String requestedTitle, String filename) {
        if (requestedTitle != null && !requestedTitle.isBlank()) {
            return requestedTitle.trim();
        }
        String name = filename == null ? "未命名小说" : filename.replaceFirst("(?i)\\.(txt|docx)$", "");
        return name.isBlank() ? "未命名小说" : name;
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static class Heading {
        private final int start;
        private final int end;
        private final String title;

        private Heading(int start, int end, String title) {
            this.start = start;
            this.end = end;
            this.title = title;
        }
    }
}
