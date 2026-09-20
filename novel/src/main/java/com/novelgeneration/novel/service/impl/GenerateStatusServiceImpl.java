package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.mapper.GenerateOutlineMapper;
import com.novelgeneration.novel.mapper.NovelTextMapper;
import com.novelgeneration.novel.service.GenerateStatusService;
import com.novelgeneration.novel.vo.NovelVO;
import com.novelgeneration.novel.vo.StatusVo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

@Service
public class GenerateStatusServiceImpl implements GenerateStatusService {


    @Resource
    private NovelTextMapper novelTextMapper;

    @Resource
    private GenerateOutlineMapper generateOutlineMapper;

    @Override
    public Result generatestaus(Long novelId) {
        List<NovelVO.NovelText> novelText = novelTextMapper.selectNovelText(novelId);
        if (novelText == null) {
            novelText = Collections.emptyList();
        }
        String novelTitle = generateOutlineMapper.selectNovelTitle(novelId);
        Long currentChapter= (long) novelText.size();
        Long totalChapter = generateOutlineMapper.selectTotalChapter(novelId);
        if (totalChapter == null || totalChapter <= 0) {
            // Imported novels have chapter text but no outline row. They are already complete
            // for the full-text generation workflow and must not be treated as 20-chapter jobs.
            totalChapter = currentChapter > 0 ? currentChapter : 20L;
        }
        GenerateFullServiceImpl.GenerationState generationState = GenerateFullServiceImpl.getGenerationState(novelId);
        StatusVo statusVo = new StatusVo();
        statusVo.setNovelId(novelId);
        statusVo.setNovelTitle(novelTitle);
        statusVo.setTotalChapter(totalChapter);

        if ("FAILED".equals(generationState.getStatus())) {
            statusVo.setStatus("FAILED");
            statusVo.setCurrentChapter(generationState.getCurrentChapter() == null ? currentChapter : generationState.getCurrentChapter());
            statusVo.setErrorMessage(generationState.getErrorMessage());
            return Result.ok(statusVo);
        }
        if(currentChapter < totalChapter){
            statusVo.setStatus("GENERATING_FULL");
        }else {
            statusVo.setStatus("FULL_GENERATED");
        }
        statusVo.setCurrentChapter(currentChapter);
        return Result.ok(statusVo);
    }
}
