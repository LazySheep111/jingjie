package com.novelgeneration.novel.vo;

import lombok.Data;

@Data
public class NovelLibraryItemVO {
    private Long novelId;
    private String novelTitle;
    private String category;
    private Integer assetCount;
    private Integer characterCount;
    private Integer locationCount;
}
