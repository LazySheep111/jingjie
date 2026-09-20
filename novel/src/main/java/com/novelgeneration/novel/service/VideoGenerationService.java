package com.novelgeneration.novel.service;

import com.novelgeneration.novel.vo.StoryboardVideoVO;
import com.novelgeneration.novel.vo.VideoGenerationTaskVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VideoGenerationService {
    VideoGenerationTaskVO createTask(Long novelId, Long chapterNum, Long sceneId, Long firstFrameId);

    VideoGenerationTaskVO createTask(Long novelId, Long chapterNum, Long sceneId, Long firstFrameId, String promptOverride);

    VideoGenerationTaskVO createTask(Long novelId, Long chapterNum, Long sceneId, Long firstFrameId,
                                     String promptOverride, String resolution);

    String rewritePromptForSafety(String prompt);

    VideoGenerationTaskVO getTask(Long taskId);

    List<StoryboardVideoVO> listVideos(Long novelId, Long chapterNum, Long sceneId);

    StoryboardVideoVO upload(Long novelId, Long chapterNum, Long sceneId, MultipartFile file);

    StoryboardVideoVO delete(Long videoId);

    VideoGenerationTaskVO retry(Long taskId);

    VideoGenerationTaskVO retry(Long taskId, String promptOverride);

    void setCurrent(Long videoId);
}
