package com.novelgeneration.novel.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class StructuredOutputSchemas {

    private StructuredOutputSchemas() {
    }

    public static ObjectNode outline(ObjectMapper mapper) {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "object");
        root.put("additionalProperties", false);
        ObjectNode properties = root.putObject("properties");
        properties.putObject("novelTitle").put("type", "string");
        properties.putObject("totalChapter").put("type", "integer");
        properties.putObject("overallPlot").put("type", "string");
        ObjectNode foreshadowList = properties.putObject("foreshadowList");
        foreshadowList.put("type", "array");
        foreshadowList.set("items", mapper.getNodeFactory().objectNode().put("type", "string"));

        ObjectNode chapterList = properties.putObject("chapterList");
        chapterList.put("type", "array");
        ObjectNode chapterItem = chapterList.putObject("items");
        chapterItem.put("type", "object");
        chapterItem.put("additionalProperties", false);
        ObjectNode chapterProperties = chapterItem.putObject("properties");
        chapterProperties.putObject("chapterNum").put("type", "integer");
        chapterProperties.putObject("chapterTitle").put("type", "string");
        chapterProperties.putObject("chapterSummary").put("type", "string");
        chapterProperties.putObject("wordCount").put("type", "integer");
        addRequired(chapterItem, "chapterNum", "chapterTitle", "chapterSummary", "wordCount");
        addRequired(root, "novelTitle", "totalChapter", "overallPlot", "foreshadowList", "chapterList");
        return root;
    }

    public static ObjectNode chapter(ObjectMapper mapper) {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "object");
        root.put("additionalProperties", false);
        ObjectNode properties = root.putObject("properties");
        properties.putObject("chapterNum").put("type", "integer");
        properties.putObject("chapterTitle").put("type", "string");
        properties.putObject("chapterSummary").put("type", "string");
        properties.putObject("chapterText").put("type", "string");
        addRequired(root, "chapterNum", "chapterTitle", "chapterSummary", "chapterText");
        return root;
    }

    public static ObjectNode storyboard(ObjectMapper mapper) {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "object");
        root.put("additionalProperties", false);
        ObjectNode properties = root.putObject("properties");
        properties.putObject("totalDurationSec").put("type", "integer");

        ObjectNode scenes = properties.putObject("scenes");
        scenes.put("type", "array");
        ObjectNode scene = scenes.putObject("items");
        scene.put("type", "object");
        scene.put("additionalProperties", false);
        ObjectNode sceneProperties = scene.putObject("properties");
        sceneProperties.putObject("sequence").put("type", "integer");
        sceneProperties.putObject("durationSec").put("type", "integer");
        sceneProperties.putObject("location").put("type", "string");
        sceneProperties.putObject("timeOfDay").put("type", "string");
        sceneProperties.putObject("weather").put("type", "string");
        sceneProperties.putObject("characters").put("type", "string");
        sceneProperties.putObject("shotType").put("type", "string");
        sceneProperties.putObject("cameraMovement").put("type", "string");
        sceneProperties.putObject("shotPlan").put("type", "string");
        sceneProperties.putObject("characterEmotion").put("type", "string");
        sceneProperties.putObject("voiceOver").put("type", "string");
        sceneProperties.putObject("transition").put("type", "string");
        sceneProperties.putObject("imagePrompt").put("type", "string");
        addRequired(scene, "sequence", "shotPlan");
        addRequired(root, "scenes");
        return root;
    }

    public static ObjectNode assetEntities(ObjectMapper mapper) {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "object");
        root.put("additionalProperties", false);
        ObjectNode properties = root.putObject("properties");
        ObjectNode entities = properties.putObject("entities");
        entities.put("type", "array");
        ObjectNode entity = entities.putObject("items");
        entity.put("type", "object");
        entity.put("additionalProperties", false);
        ObjectNode entityProperties = entity.putObject("properties");
        entityProperties.putObject("assetType").put("type", "string");
        entityProperties.putObject("assetName").put("type", "string");
        entityProperties.putObject("coreFeatures").put("type", "string");
        addRequired(entity, "assetType", "assetName", "coreFeatures");
        addRequired(root, "entities");
        return root;
    }

    public static ObjectNode assetPrompts(ObjectMapper mapper) {
        ObjectNode root = mapper.createObjectNode();
        root.put("type", "object");
        root.put("additionalProperties", false);
        ObjectNode properties = root.putObject("properties");
        properties.putObject("frontPrompt").put("type", "string");
        properties.putObject("sidePrompt").put("type", "string");
        properties.putObject("backPrompt").put("type", "string");
        addRequired(root, "frontPrompt", "sidePrompt", "backPrompt");
        return root;
    }

    private static void addRequired(ObjectNode node, String... fields) {
        ArrayNode required = node.putArray("required");
        for (String field : fields) {
            required.add(field);
        }
    }
}
