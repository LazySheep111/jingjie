package com.novelgeneration.novel.service.impl;

import com.novelgeneration.novel.dto.AssetMergeRequest;
import com.novelgeneration.novel.mapper.VisualAssetMapper;
import com.novelgeneration.novel.service.VisualAssetService;
import com.novelgeneration.novel.vo.VisualAssetVO;
import com.novelgeneration.novel.vo.NovelLibraryItemVO;
import com.novelgeneration.novel.vo.VisualAssetPageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

@Service
public class VisualAssetServiceImpl implements VisualAssetService {

    @Resource
    private VisualAssetMapper visualAssetMapper;

    @Override
    public List<VisualAssetVO> list(Long novelId, String assetType) {
        requireNovelId(novelId);
        return visualAssetMapper.selectAssets(novelId, blankToNull(assetType));
    }

    @Override
    public VisualAssetVO get(Long novelId, Long assetId) {
        requireNovelId(novelId);
        requireAssetId(assetId);
        VisualAssetVO asset = visualAssetMapper.selectAssetVersion(assetId, null);
        if (asset == null || !novelId.equals(asset.getNovelId())) {
            throw new IllegalArgumentException("资产不存在");
        }
        return asset;
    }

    @Override
    @Transactional
    public VisualAssetVO update(Long novelId, Long assetId, VisualAssetVO request) {
        requireNovelId(novelId);
        requireAssetId(assetId);
        if (request == null) {
            throw new IllegalArgumentException("资产内容不能为空");
        }
        VisualAssetVO current = get(novelId, assetId);
        boolean promptsChanged = !safeEquals(current.getFrontPrompt(), request.getFrontPrompt())
                || !safeEquals(current.getSidePrompt(), request.getSidePrompt())
                || !safeEquals(current.getBackPrompt(), request.getBackPrompt());
        current.setAssetName(firstNonBlank(request.getAssetName(), current.getAssetName()));
        current.setNormalizedName(normalizeName(current.getAssetName()));
        current.setCoreFeatures(request.getCoreFeatures());
        current.setFrontPrompt(request.getFrontPrompt());
        current.setSidePrompt(request.getSidePrompt());
        current.setBackPrompt(request.getBackPrompt());
        current.setStatus(firstNonBlank(request.getStatus(), current.getStatus()));
        if (promptsChanged) {
            current.setCompositeImagePath(null);
            current.setStatus("PROMPT_READY");
        }
        visualAssetMapper.updateAsset(current);
        visualAssetMapper.updateAssetVersion(current);
        return get(novelId, assetId);
    }

    @Override
    @Transactional
    public VisualAssetVO merge(Long novelId, Long assetId, AssetMergeRequest request) {
        requireNovelId(novelId);
        requireAssetId(assetId);
        if (request == null || request.getTargetAssetId() == null) {
            throw new IllegalArgumentException("目标资产不能为空");
        }
        if (assetId.equals(request.getTargetAssetId())) {
            throw new IllegalArgumentException("不能将资产合并到自身");
        }
        VisualAssetVO source = get(novelId, assetId);
        VisualAssetVO target = get(novelId, request.getTargetAssetId());
        int targetVersion = request.getTargetVersion() == null
                ? target.getVersion() : request.getTargetVersion();
        if (visualAssetMapper.selectAssetVersion(target.getAssetId(), targetVersion) == null) {
            throw new IllegalArgumentException("目标资产版本不存在");
        }
        visualAssetMapper.replaceAssetReferences(source.getAssetId(), target.getAssetId(), targetVersion);
        visualAssetMapper.markMerged(novelId, source.getAssetId());
        return target;
    }

    @Override
    public List<VisualAssetVO> listByChapter(Long novelId, Long chapterNum) {
        requireNovelId(novelId);
        if (chapterNum == null || chapterNum < 1) {
            throw new IllegalArgumentException("chapterNum 必须大于 0");
        }
        return visualAssetMapper.selectAssetsByChapter(novelId, chapterNum);
    }

    @Override
    @Transactional
    public VisualAssetVO findOrCreate(Long novelId, String assetType, String displayName,
                                      String normalizedName, String coreFeatures) {
        requireNovelId(novelId);
        if (assetType == null || assetType.isBlank()) {
            throw new IllegalArgumentException("assetType 不能为空");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("assetName 不能为空");
        }
        String identity = normalizeName(normalizedName == null ? displayName : normalizedName);
        VisualAssetVO existing = visualAssetMapper.selectAsset(novelId, assetType, identity);
        if (existing != null) {
            return existing;
        }
        VisualAssetVO asset = new VisualAssetVO();
        asset.setNovelId(novelId);
        asset.setAssetType(assetType);
        asset.setAssetName(displayName.trim());
        asset.setNormalizedName(identity);
        asset.setVersion(1);
        asset.setStatus("PROMPT_PENDING");
        visualAssetMapper.insertAsset(asset);
        asset.setCoreFeatures(coreFeatures);
        visualAssetMapper.insertAssetVersion(asset);
        return asset;
    }

    @Override
    @Transactional
    public VisualAssetVO createNextVersion(Long novelId, Long assetId, String coreFeatures,
                                            String frontPrompt, String sidePrompt, String backPrompt) {
        VisualAssetVO current = get(novelId, assetId);
        VisualAssetVO next = new VisualAssetVO();
        next.setNovelId(novelId);
        next.setAssetId(assetId);
        next.setAssetType(current.getAssetType());
        next.setAssetName(current.getAssetName());
        next.setNormalizedName(current.getNormalizedName());
        next.setVersion(current.getVersion() + 1);
        next.setCoreFeatures(coreFeatures);
        next.setFrontPrompt(frontPrompt);
        next.setSidePrompt(sidePrompt);
        next.setBackPrompt(backPrompt);
        next.setStatus("PROMPT_READY");
        visualAssetMapper.insertAssetVersion(next);
        VisualAssetVO assetUpdate = new VisualAssetVO();
        assetUpdate.setNovelId(novelId);
        assetUpdate.setAssetId(assetId);
        assetUpdate.setAssetName(current.getAssetName());
        assetUpdate.setNormalizedName(current.getNormalizedName());
        assetUpdate.setVersion(next.getVersion());
        assetUpdate.setStatus(next.getStatus());
        visualAssetMapper.updateAsset(assetUpdate);
        return next;
    }

    @Override
    public List<NovelLibraryItemVO> listNovelsForLibrary() {
        return visualAssetMapper.selectNovelsForLibrary();
    }

    @Override
    public VisualAssetPageVO listLibrary(Long novelId, String assetType, String keyword,
                                         Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        String safeType = blankToNull(assetType);
        String safeKeyword = blankToNull(keyword);
        int offset = (safePage - 1) * safePageSize;
        List<VisualAssetVO> records = visualAssetMapper.selectLibrary(
                novelId, safeType, safeKeyword, offset, safePageSize);
        records.forEach(this::populateChapterList);
        Long total = visualAssetMapper.countLibrary(novelId, safeType, safeKeyword);
        return new VisualAssetPageVO(records, total == null ? 0L : total, safePage, safePageSize);
    }

    @Override
    public VisualAssetVO getGlobal(Long assetId) {
        requireAssetId(assetId);
        VisualAssetVO asset = visualAssetMapper.selectGlobalAsset(assetId);
        if (asset == null) {
            throw new IllegalArgumentException("资产不存在");
        }
        populateChapterList(asset);
        return asset;
    }

    @Override
    @Transactional
    public VisualAssetVO createNextVersionGlobal(Long assetId, String coreFeatures,
                                                  String frontPrompt, String sidePrompt, String backPrompt) {
        VisualAssetVO current = getGlobal(assetId);
        VisualAssetVO next = new VisualAssetVO();
        next.setNovelId(current.getNovelId());
        next.setAssetId(assetId);
        next.setAssetType(current.getAssetType());
        next.setAssetName(current.getAssetName());
        next.setNormalizedName(current.getNormalizedName());
        next.setVersion(current.getVersion() + 1);
        next.setCoreFeatures(coreFeatures);
        next.setFrontPrompt(frontPrompt);
        next.setSidePrompt(sidePrompt);
        next.setBackPrompt(backPrompt);
        next.setStatus("PROMPT_READY");
        visualAssetMapper.insertAssetVersion(next);

        VisualAssetVO assetUpdate = new VisualAssetVO();
        assetUpdate.setNovelId(current.getNovelId());
        assetUpdate.setAssetId(assetId);
        assetUpdate.setAssetName(current.getAssetName());
        assetUpdate.setNormalizedName(current.getNormalizedName());
        assetUpdate.setVersion(next.getVersion());
        assetUpdate.setStatus(next.getStatus());
        visualAssetMapper.updateAsset(assetUpdate);
        return getGlobal(assetId);
    }

    @Override
    @Transactional
    public void reuse(Long assetId, Long targetNovelId, Long targetChapterNum,
                      String assetRole, Long targetAssetId) {
        VisualAssetVO source = getGlobal(assetId);
        requireNovelId(targetNovelId);
        requireAssetId(targetAssetId);
        VisualAssetVO target = get(targetNovelId, targetAssetId);
        String role = blankToNull(assetRole);
        if (role == null) {
            role = source.getAssetType();
        }
        if (!"CHARACTER".equals(role) && !"LOCATION".equals(role)) {
            throw new IllegalArgumentException("assetRole 只能是 CHARACTER 或 LOCATION");
        }
        if (!role.equals(source.getAssetType()) || !role.equals(target.getAssetType())) {
            throw new IllegalArgumentException("源资产、目标资产和资产角色类型必须一致");
        }
        if (source.getAssetId().equals(target.getAssetId())) {
            return;
        }
        VisualAssetVO next = new VisualAssetVO();
        next.setNovelId(target.getNovelId());
        next.setAssetId(target.getAssetId());
        next.setAssetType(target.getAssetType());
        next.setAssetName(target.getAssetName());
        next.setNormalizedName(target.getNormalizedName());
        next.setVersion(target.getVersion() + 1);
        next.setCoreFeatures(source.getCoreFeatures());
        next.setFrontPrompt(source.getFrontPrompt());
        next.setSidePrompt(source.getSidePrompt());
        next.setBackPrompt(source.getBackPrompt());
        next.setCompositeImagePath(source.getCompositeImagePath());
        next.setStatus(source.getStatus());
        visualAssetMapper.insertAssetVersion(next);

        VisualAssetVO assetUpdate = new VisualAssetVO();
        assetUpdate.setNovelId(target.getNovelId());
        assetUpdate.setAssetId(target.getAssetId());
        assetUpdate.setAssetName(target.getAssetName());
        assetUpdate.setNormalizedName(target.getNormalizedName());
        assetUpdate.setVersion(next.getVersion());
        assetUpdate.setStatus(next.getStatus());
        visualAssetMapper.updateAsset(assetUpdate);
        visualAssetMapper.updateAssetReferenceVersions(target.getAssetId(), next.getVersion());
    }

    private void populateChapterList(VisualAssetVO asset) {
        List<Long> chapters = new ArrayList<>();
        if (asset.getChapterNumbers() != null && !asset.getChapterNumbers().isBlank()) {
            for (String value : asset.getChapterNumbers().split(",")) {
                try {
                    chapters.add(Long.valueOf(value));
                } catch (NumberFormatException ignored) {
                    // Ignore malformed aggregation values and keep valid chapter numbers.
                }
            }
        }
        asset.setChapters(chapters);
    }

    public String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    private void requireNovelId(Long novelId) {
        if (novelId == null || novelId < 1) {
            throw new IllegalArgumentException("novelId 必须大于 0");
        }
    }

    private void requireAssetId(Long assetId) {
        if (assetId == null || assetId < 1) {
            throw new IllegalArgumentException("assetId 必须大于 0");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private boolean safeEquals(String first, String second) {
        return first == null ? second == null : first.equals(second);
    }
}
