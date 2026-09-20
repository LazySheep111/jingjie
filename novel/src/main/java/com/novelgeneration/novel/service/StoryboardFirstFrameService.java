package com.novelgeneration.novel.service;

import com.novelgeneration.novel.vo.StoryboardFirstFrameVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StoryboardFirstFrameService {
    StoryboardFirstFrameVO createTask(Long novelId, Long chapterNum, Long sceneId);

    StoryboardFirstFrameVO upload(Long novelId, Long chapterNum, Long sceneId, MultipartFile file);

    StoryboardFirstFrameVO getTask(Long firstFrameId);

    List<StoryboardFirstFrameVO> listByScene(Long novelId, Long chapterNum, Long sceneId);

    void delete(Long firstFrameId);
}
