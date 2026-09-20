package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.AssistantChatRequest;
import com.novelgeneration.novel.entity.NovelInfo;
import com.novelgeneration.novel.mapper.ChapterMapper;
import com.novelgeneration.novel.mapper.HistoryMapper;
import com.novelgeneration.novel.mapper.StoryboardMapper;
import com.novelgeneration.novel.mapper.VideoGenerationTaskMapper;
import com.novelgeneration.novel.mapper.VisualAssetMapper;
import com.novelgeneration.novel.vo.ChapterVO;
import com.novelgeneration.novel.vo.StoryboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AssistantReadOnlyToolRegistry {

    private final HistoryMapper historyMapper;
    private final ChapterMapper chapterMapper;
    private final StoryboardMapper storyboardMapper;
    private final VisualAssetMapper visualAssetMapper;
    private final VideoGenerationTaskMapper videoGenerationTaskMapper;

    public Map<String, Object> execute(String toolName, Map<String, Object> arguments,
                                       AssistantChatRequest context) {
        if (toolName == null || !List.of(
                "getCurrentNovel", "getNovelList", "getNovelChapters", "getChapterContent",
                "getLatestStoryboard", "getVisualAssets", "getVideoTaskStatus", "getAssetTaskStatus"
        ).contains(toolName)) {
            throw new IllegalArgumentException("只读助手不支持该工具: " + toolName);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tool", toolName);
        result.put("success", true);
        result.put("data", switch (toolName) {
            case "getCurrentNovel" -> currentNovel(id(arguments, "novelId", context == null ? null : context.getNovelId()));
            case "getNovelList" -> historyMapper.selectHistoryList(0, 50);
            case "getNovelChapters" -> chapterMapper.selectChapter(id(arguments, "novelId", context == null ? null : context.getNovelId()));
            case "getChapterContent" -> chapterMapper.selectChapterByNum(
                    id(arguments, "novelId", context == null ? null : context.getNovelId()),
                    id(arguments, "chapterNum", context == null ? null : context.getChapterNum()));
            case "getLatestStoryboard" -> latestStoryboard(
                    id(arguments, "novelId", context == null ? null : context.getNovelId()),
                    id(arguments, "chapterNum", context == null ? null : context.getChapterNum()));
            case "getVisualAssets" -> visualAssetMapper.selectAssets(
                    id(arguments, "novelId", context == null ? null : context.getNovelId()),
                    string(arguments, "assetType"));
            case "getVideoTaskStatus" -> videoGenerationTaskMapper.selectById(
                    id(arguments, "taskId", context == null ? null : context.getTaskId()));
            case "getAssetTaskStatus" -> visualAssetMapper.selectTask(string(arguments, "taskId"));
            default -> throw new IllegalArgumentException("未知只读工具");
        });
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("novelId", id(arguments, "novelId", context == null ? null : context.getNovelId()));
        source.put("chapterNum", id(arguments, "chapterNum", context == null ? null : context.getChapterNum()));
        result.put("source", source);
        return result;
    }

    private Map<String, Object> currentNovel(Long novelId) {
        NovelInfo info = requireNovel(novelId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("novelId", info.getId());
        data.put("novelTitle", info.getNovelTitle());
        data.put("category", info.getCategory());
        data.put("outline", historyMapper.selectOutlineRecord(novelId));
        data.put("chapterCount", historyMapper.countChapters(novelId));
        return data;
    }

    private StoryboardVO latestStoryboard(Long novelId, Long chapterNum) {
        StoryboardVO storyboard = storyboardMapper.selectLatest(novelId, chapterNum);
        if (storyboard != null) {
            storyboard.setScenes(storyboardMapper.selectScenes(storyboard.getId()));
        }
        return storyboard;
    }

    private NovelInfo requireNovel(Long novelId) {
        if (novelId == null || novelId < 1) {
            throw new IllegalArgumentException("novelId 必须大于 0");
        }
        NovelInfo info = historyMapper.selectNovelInfo(novelId);
        if (info == null) {
            throw new IllegalArgumentException("作品不存在: " + novelId);
        }
        return info;
    }

    private Long id(Map<String, Object> arguments, String name, Long fallback) {
        Object value = arguments == null ? null : arguments.get(name);
        if (value == null) value = fallback;
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text && !text.isBlank()) {
            try { return Long.valueOf(text); } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private String string(Map<String, Object> arguments, String name) {
        Object value = arguments == null ? null : arguments.get(name);
        return value == null || String.valueOf(value).isBlank() ? null : String.valueOf(value);
    }
}
