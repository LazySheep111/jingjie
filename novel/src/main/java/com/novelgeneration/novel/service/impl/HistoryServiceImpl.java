package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto.KeysDTO;
import com.novelgeneration.novel.entity.NovelInfo;
import com.novelgeneration.novel.mapper.HistoryMapper;
import com.novelgeneration.novel.service.HistoryService;
import com.novelgeneration.novel.vo.HistoryOutlineRecordVO;
import com.novelgeneration.novel.vo.NovelHistoryDetailVO;
import com.novelgeneration.novel.vo.NovelHistoryItemVO;
import com.novelgeneration.novel.vo.NovelOutlineVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class HistoryServiceImpl implements HistoryService {

    @Resource
    private HistoryMapper historyMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public NovelHistoryDetailVO.PageResult<NovelHistoryItemVO> listHistory(Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);
        int offset = (safePage - 1) * safePageSize;
        List<NovelHistoryItemVO> records = historyMapper.selectHistoryList(offset, safePageSize);
        Long total = historyMapper.countHistory();
        return new NovelHistoryDetailVO.PageResult<>(records, total == null ? 0L : total);
    }

    @Override
    public NovelHistoryDetailVO getOutline(Long novelId) {
        NovelInfo novelInfo = historyMapper.selectNovelInfo(novelId);
        if (novelInfo == null) {
            throw new IllegalArgumentException("Novel not found: " + novelId);
        }

        HistoryOutlineRecordVO outlineRecord = historyMapper.selectOutlineRecord(novelId);
        int chapterCount = historyMapper.countChapters(novelId);
        boolean fullGenerated = chapterCount > 0;

        NovelHistoryDetailVO detail = new NovelHistoryDetailVO();
        detail.setNovelId(novelId);
        detail.setStatus(fullGenerated ? "FULL_GENERATED" : "OUTLINE_ONLY");
        detail.setFullGenerated(fullGenerated);
        detail.setFormData(toFormData(novelInfo));
        detail.setOutline(toOutline(novelInfo, outlineRecord));
        return detail;
    }

    @Override
    public void deleteNovel(Long novelId) {
        int affected = historyMapper.softDeleteNovel(novelId);
        if (affected == 0) {
            throw new IllegalArgumentException("Novel not found: " + novelId);
        }
    }

    private KeysDTO toFormData(NovelInfo novelInfo) {
        KeysDTO keysDTO = new KeysDTO();
        keysDTO.setNovelTitle(novelInfo.getNovelTitle());
        keysDTO.setCategory(novelInfo.getCategory());
        keysDTO.setNovelLength(novelInfo.getNovelLength());
        keysDTO.setEndingType(novelInfo.getEndingType());
        keysDTO.setWritingStyle(novelInfo.getWritingStyle());
        keysDTO.setTargetAudience(novelInfo.getTargetAudience());
        keysDTO.setProtagonist(novelInfo.getProtagonist());
        keysDTO.setRoleList(parseList(novelInfo.getRoleList(), new TypeReference<List<Object>>() {
        }));
        keysDTO.setBackground(novelInfo.getBackground());
        keysDTO.setWorldRule(novelInfo.getWorldRule());
        keysDTO.setTheme(novelInfo.getTheme());
        keysDTO.setTriggerEvent(novelInfo.getTriggerEvent());
        keysDTO.setForeshadowCount(novelInfo.getForeshadowCount() == null ? null : String.valueOf(novelInfo.getForeshadowCount()));
        keysDTO.setNarrativeView(novelInfo.getNarrativeView());
        keysDTO.setAvoidContent(novelInfo.getAvoidContent());
        return keysDTO;
    }

    private NovelOutlineVO toOutline(NovelInfo novelInfo, HistoryOutlineRecordVO outlineRecord) {
        NovelOutlineVO outline = new NovelOutlineVO();
        outline.setNovelId(novelInfo.getId());

        if (outlineRecord == null) {
            outline.setNovelTitle(novelInfo.getNovelTitle());
            outline.setTotalChapter(0);
            outline.setOverallPlot("");
            outline.setForeshadowList(new ArrayList<>());
            outline.setChapterList(new ArrayList<>());
            return outline;
        }

        List<NovelOutlineVO.ChapterOutlineVO> chapterList = parseList(
                outlineRecord.getChapterListJson(),
                new TypeReference<List<NovelOutlineVO.ChapterOutlineVO>>() {
                }
        );
        outline.setNovelTitle(firstNonBlank(outlineRecord.getNovelTitle(), novelInfo.getNovelTitle()));
        outline.setOverallPlot(outlineRecord.getOverallPlot());
        outline.setForeshadowList(parseList(outlineRecord.getForeshadowListJson(), new TypeReference<List<String>>() {
        }));
        outline.setChapterList(chapterList);
        outline.setTotalChapter(chapterList.size());
        return outline;
    }

    private <T> List<T> parseList(String json, TypeReference<List<T>> typeReference) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception e) {
            throw new RuntimeException("Parse history json failed", e);
        }
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
