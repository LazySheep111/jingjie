package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.AssetMergeRequest;
import com.novelgeneration.novel.vo.VisualAssetVO;
import com.novelgeneration.novel.vo.NovelLibraryItemVO;
import com.novelgeneration.novel.vo.VisualAssetPageVO;

import java.util.List;

public interface VisualAssetService {
    List<VisualAssetVO> list(Long novelId, String assetType);

    VisualAssetVO get(Long novelId, Long assetId);

    VisualAssetVO update(Long novelId, Long assetId, VisualAssetVO request);

    VisualAssetVO merge(Long novelId, Long assetId, AssetMergeRequest request);

    List<VisualAssetVO> listByChapter(Long novelId, Long chapterNum);

    VisualAssetVO findOrCreate(Long novelId, String assetType, String displayName,
                               String normalizedName, String coreFeatures);

    VisualAssetVO createNextVersion(Long novelId, Long assetId, String coreFeatures,
                                    String frontPrompt, String sidePrompt, String backPrompt);

    List<NovelLibraryItemVO> listNovelsForLibrary();

    VisualAssetPageVO listLibrary(Long novelId, String assetType, String keyword,
                                  Integer page, Integer pageSize);

    VisualAssetVO getGlobal(Long assetId);

    VisualAssetVO createNextVersionGlobal(Long assetId, String coreFeatures,
                                          String frontPrompt, String sidePrompt, String backPrompt);

    void reuse(Long assetId, Long targetNovelId, Long targetChapterNum,
               String assetRole, Long targetAssetId);
}
