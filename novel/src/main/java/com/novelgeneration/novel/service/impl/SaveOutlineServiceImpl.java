package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto.OutlineDTO;
import com.novelgeneration.novel.mapper.SaveOutlineMapper;
import com.novelgeneration.novel.service.SaveOutlineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Slf4j
@Service
public class SaveOutlineServiceImpl implements SaveOutlineService {

    @Resource
    private SaveOutlineMapper saveOutlineMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Long saveOutline(OutlineDTO outlineDTO) {
        outlineDTO.setCreateTime(LocalDateTime.now());
        outlineDTO.setUpdateTime(LocalDateTime.now());
        outlineDTO.setRoleListJson(toJson(outlineDTO.getFormData() == null ? null : outlineDTO.getFormData().getRoleList()));
        outlineDTO.setForeshadowListJson(toJson(outlineDTO.getOutline() == null ? null : outlineDTO.getOutline().getForeshadowList()));
        outlineDTO.setChapterListJson(toJson(outlineDTO.getOutline() == null ? null : outlineDTO.getOutline().getChapterList()));
        int updated = saveOutlineMapper.updateOutline(outlineDTO);
        if (updated == 0) {
            saveOutlineMapper.insertOutline(outlineDTO);
        }
        return outlineDTO.getNovelId();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Serialize outline data failed", e);
        }
    }
}
