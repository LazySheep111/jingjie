package com.novelgeneration.novel.dto;

import com.novelgeneration.novel.vo.StoryboardVO;
import com.novelgeneration.novel.vo.StoryboardVideoVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StoryboardGenerationContext {
    private StoryboardVO.Scene scene;
    private StoryboardFrameRequest frameRequest;
    private VideoProviderRequest videoRequest;
    private List<StoryboardVideoVO.AssetSnapshot> assetSnapshots = new ArrayList<>();
    private String stylePrompt;
}
