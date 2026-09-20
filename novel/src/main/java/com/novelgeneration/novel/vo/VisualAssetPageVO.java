package com.novelgeneration.novel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisualAssetPageVO {
    private List<VisualAssetVO> records;
    private Long total;
    private Integer page;
    private Integer pageSize;
}
