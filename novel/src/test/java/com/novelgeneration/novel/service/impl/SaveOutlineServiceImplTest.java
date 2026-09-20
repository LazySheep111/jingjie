package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.OutlineDTO;
import com.novelgeneration.novel.mapper.SaveOutlineMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaveOutlineServiceImplTest {

    @Test
    void insertsOutlineWhenUpdateAffectsNoRows() {
        SaveOutlineMapper saveOutlineMapper = mock(SaveOutlineMapper.class);
        SaveOutlineServiceImpl service = new SaveOutlineServiceImpl();
        ReflectionTestUtils.setField(service, "saveOutlineMapper", saveOutlineMapper);

        OutlineDTO outlineDTO = buildOutlineDTO();
        when(saveOutlineMapper.updateOutline(outlineDTO)).thenReturn(0);

        service.saveOutline(outlineDTO);

        verify(saveOutlineMapper).insertOutline(outlineDTO);
    }

    private OutlineDTO buildOutlineDTO() {
        OutlineDTO dto = new OutlineDTO();
        dto.setNovelId(48L);
        OutlineDTO.FormData formData = new OutlineDTO.FormData();
        formData.setNovelTitle("星海归途");
        formData.setCategory("科幻冒险");
        formData.setProtagonist("凌程");
        formData.setRoleList(List.of("凌溪"));
        dto.setFormData(formData);

        OutlineDTO.Outline outline = new OutlineDTO.Outline();
        outline.setNovelTitle("星海归途");
        outline.setOverallPlot("整体梗概");
        outline.setForeshadowList(List.of("伏笔"));
        outline.setChapterList(List.of(chapter(1)));
        dto.setOutline(outline);
        return dto;
    }

    private OutlineDTO.ChapterItem chapter(Integer chapterNum) {
        OutlineDTO.ChapterItem item = new OutlineDTO.ChapterItem();
        item.setChapterNum(chapterNum);
        item.setChapterTitle("第" + chapterNum + "章");
        item.setChapterSummary("概要");
        item.setWordCount(2000);
        return item;
    }
}
