package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.AssetMergeRequest;
import com.novelgeneration.novel.mapper.VisualAssetMapper;
import com.novelgeneration.novel.vo.VisualAssetVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisualAssetServiceImplTest {

    @Mock
    private VisualAssetMapper visualAssetMapper;

    @InjectMocks
    private VisualAssetServiceImpl service;

    @Test
    void findOrCreate_reusesExistingAssetWithinSameNovel() {
        VisualAssetVO existing = asset(1L, 10L, 1);
        when(visualAssetMapper.selectAsset(1L, "CHARACTER", "凌程"))
                .thenReturn(existing);

        VisualAssetVO result = service.findOrCreate(1L, "CHARACTER", "凌程", "凌程", "工程师");

        assertEquals(10L, result.getAssetId());
        verify(visualAssetMapper, never()).insertAsset(any());
    }

    @Test
    void findOrCreate_doesNotReuseAssetFromAnotherNovel() {
        when(visualAssetMapper.selectAsset(2L, "CHARACTER", "凌程")).thenReturn(null);

        VisualAssetVO result = service.findOrCreate(2L, "CHARACTER", "凌程", "凌程", "工程师");

        verify(visualAssetMapper).insertAsset(any(VisualAssetVO.class));
        verify(visualAssetMapper).insertAssetVersion(any(VisualAssetVO.class));
        assertEquals(2L, result.getNovelId());
    }

    @Test
    void createNextVersion_keepsPreviousVersionUnchanged() {
        VisualAssetVO current = asset(1L, 10L, 1);
        when(visualAssetMapper.selectAssetVersion(10L, null)).thenReturn(current);

        VisualAssetVO next = service.createNextVersion(1L, 10L, "装备强化", "正面", "侧面", "背面");

        verify(visualAssetMapper).insertAssetVersion(any(VisualAssetVO.class));
        assertEquals(2, next.getVersion());
        assertEquals(1, current.getVersion());
    }

    @Test
    void merge_rejectsSelfMerge() {
        AssetMergeRequest request = new AssetMergeRequest();
        request.setTargetAssetId(10L);

        assertThrows(IllegalArgumentException.class, () -> service.merge(1L, 10L, request));
    }

    @Test
    void update_rejectsAssetFromAnotherNovel() {
        when(visualAssetMapper.selectAssetVersion(10L, null)).thenReturn(asset(2L, 10L, 1));

        assertThrows(IllegalArgumentException.class,
                () -> service.get(1L, 10L));
    }

    @Test
    void listLibrary_parsesChapterAggregation() {
        VisualAssetVO asset = asset(1L, 10L, 2);
        asset.setChapterNumbers("1,3,8");
        when(visualAssetMapper.selectLibrary(null, null, null, 0, 20))
                .thenReturn(java.util.List.of(asset));
        when(visualAssetMapper.countLibrary(null, null, null)).thenReturn(1L);

        var result = service.listLibrary(null, null, null, 1, 20);

        assertEquals(java.util.List.of(1L, 3L, 8L), result.getRecords().get(0).getChapters());
        assertEquals(1L, result.getTotal());
    }

    @Test
    void reuse_overwritesTargetAssetWithSourceVersion() {
        VisualAssetVO source = asset(1L, 10L, 2);
        source.setAssetType("CHARACTER");
        source.setCoreFeatures("source features");
        source.setFrontPrompt("source front");
        source.setSidePrompt("source side");
        source.setBackPrompt("source back");
        source.setCompositeImagePath("/uploads/source.png");
        source.setStatus("IMAGE_READY");
        VisualAssetVO target = asset(2L, 20L, 1);
        target.setAssetType("CHARACTER");
        when(visualAssetMapper.selectGlobalAsset(10L)).thenReturn(source);
        when(visualAssetMapper.selectAssetVersion(20L, null)).thenReturn(target);

        service.reuse(10L, 2L, 3L, "CHARACTER", 20L);

        verify(visualAssetMapper).insertAssetVersion(argThat(version ->
                version.getAssetId().equals(20L)
                        && version.getVersion().equals(2)
                        && version.getFrontPrompt().equals("source front")
                        && version.getCompositeImagePath().equals("/uploads/source.png")));
        verify(visualAssetMapper).updateAssetReferenceVersions(20L, 2);
    }

    @Test
    void reuse_doesNotRequireTargetChapterStoryboard() {
        VisualAssetVO source = asset(1L, 10L, 2);
        source.setAssetType("CHARACTER");
        VisualAssetVO target = asset(2L, 20L, 1);
        target.setAssetType("CHARACTER");
        when(visualAssetMapper.selectGlobalAsset(10L)).thenReturn(source);
        when(visualAssetMapper.selectAssetVersion(20L, null)).thenReturn(target);

        service.reuse(10L, 2L, 3L, "CHARACTER", 20L);

        verify(visualAssetMapper).insertAssetVersion(any(VisualAssetVO.class));
    }

    private VisualAssetVO asset(Long novelId, Long assetId, int version) {
        VisualAssetVO value = new VisualAssetVO();
        value.setNovelId(novelId);
        value.setAssetId(assetId);
        value.setVersion(version);
        value.setAssetName("凌程");
        value.setNormalizedName("凌程");
        value.setStatus("PROMPT_READY");
        return value;
    }
}
