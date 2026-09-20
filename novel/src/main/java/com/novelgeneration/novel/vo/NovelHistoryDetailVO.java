package com.novelgeneration.novel.vo;

import com.novelgeneration.novel.dto.KeysDTO;
import lombok.Data;

import java.util.List;

@Data
public class NovelHistoryDetailVO {
    private Long novelId;
    private String status;
    private Boolean fullGenerated;
    private KeysDTO formData;
    private NovelOutlineVO outline;

    @Data
    public static class PageResult<T> {
        private List<T> records;
        private Long total;

        public PageResult(List<T> records, Long total) {
            this.records = records;
            this.total = total;
        }
    }
}
