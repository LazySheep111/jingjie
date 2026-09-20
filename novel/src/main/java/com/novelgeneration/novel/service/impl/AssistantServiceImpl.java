package com.novelgeneration.novel.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelgeneration.novel.dto.AssistantChatRequest;
import com.novelgeneration.novel.dto.AssistantChatResponse;
import com.novelgeneration.novel.service.AssistantReadOnlyToolRegistry;
import com.novelgeneration.novel.service.AssistantConversationService;
import com.novelgeneration.novel.service.AssistantService;
import com.novelgeneration.novel.utils.AiUtil;
import com.novelgeneration.novel.vo.AssistantConversationVO;
import com.novelgeneration.novel.vo.AssistantMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    private static final List<String> TOOLS = List.of(
            "getCurrentNovel", "getNovelList", "getNovelChapters", "getChapterContent",
            "getLatestStoryboard", "getVisualAssets", "getVideoTaskStatus", "getAssetTaskStatus"
    );

    private final AiUtil aiUtil;
    private final AssistantReadOnlyToolRegistry toolRegistry;
    private final AssistantConversationService conversationService;
    private final ObjectMapper objectMapper;

    @Override
    public AssistantChatResponse chat(AssistantChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("请输入想了解的内容");
        }
        AssistantConversationVO conversation = conversationService.getOrCreate(request.getNovelId(),
                request.getNovelId() == null ? "通用助手" : "作品 " + request.getNovelId());
        List<AssistantMessageVO> history = conversationService.recentMessages(conversation, 20);
        AssistantMessageVO userMessage = conversationService.record(conversation.getConversationId(),
                conversation.getNovelId(), "user", request.getMessage(), "COMPLETED", null, null, null, null, 1);
        String planResponse;
        try {
            planResponse = aiUtil.chatJsonObject(buildPlanPrompt(request, history));
        } catch (RuntimeException e) {
            AssistantMessageVO failure = conversationService.record(conversation.getConversationId(), conversation.getNovelId(),
                    "assistant", "助手请求失败：" + safeMessage(e), "FAILED", null, null, null, userMessage.getMessageId(), 1);
            AssistantChatResponse failed = new AssistantChatResponse(failure.getContent());
            failed.setConversationId(conversation.getConversationId());
            failed.setMessageId(failure.getMessageId());
            return failed;
        }
        Plan plan;
        try {
            plan = parsePlan(planResponse);
        } catch (RuntimeException e) {
            return failedResponse(conversation, userMessage, e);
        }
        AssistantChatResponse response = new AssistantChatResponse();
        response.setConversationId(conversation.getConversationId());
        if ("none".equals(plan.tool)) {
            String answer;
            try { answer = cleanAnswer(aiUtil.chat(buildDirectAnswerPrompt(request, history))); }
            catch (RuntimeException e) { return failedResponse(conversation, userMessage, e); }
            AssistantMessageVO saved = conversationService.record(conversation.getConversationId(), conversation.getNovelId(),
                    "assistant", answer, "COMPLETED", null, null, null, userMessage.getMessageId(), 1);
            response.setMessageId(saved.getMessageId());
            response.setAnswer(answer);
            return response;
        }

        Map<String, Object> toolResult;
        try { toolResult = toolRegistry.execute(plan.tool, plan.arguments, request); }
        catch (RuntimeException e) { return failedResponse(conversation, userMessage, e); }
        Map<String, Object> call = new LinkedHashMap<>();
        call.put("tool", plan.tool);
        call.put("arguments", plan.arguments);
        call.put("success", toolResult.get("success"));
        response.setToolCalls(List.of(call));
        Object source = toolResult.get("source");
        if (source instanceof Map<?, ?> sourceMap) {
            response.setSources(List.of((Map<String, Object>) sourceMap));
        }
        String toolJson = toJson(toolResult);
        conversationService.record(conversation.getConversationId(), conversation.getNovelId(), "tool",
                "已查询工具：" + plan.tool, "COMPLETED", plan.tool,
                toJson(plan.arguments), toolJson, userMessage.getMessageId(), 1);
        String answer;
        try { answer = cleanAnswer(aiUtil.chat(buildFinalAnswerPrompt(request, history, toolResult))); }
        catch (RuntimeException e) { return failedResponse(conversation, userMessage, e); }
        AssistantMessageVO saved = conversationService.record(conversation.getConversationId(), conversation.getNovelId(),
                "assistant", answer, "COMPLETED", null, null, null, userMessage.getMessageId(), 1);
        response.setMessageId(saved.getMessageId());
        response.setAnswer(answer);
        return response;
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank() ? "未知错误" : e.getMessage();
    }

    private AssistantChatResponse failedResponse(AssistantConversationVO conversation,
                                                  AssistantMessageVO userMessage, RuntimeException error) {
        String text = "助手请求失败：" + safeMessage(error);
        AssistantMessageVO failure = conversationService.record(conversation.getConversationId(), conversation.getNovelId(),
                "assistant", text, "FAILED", null, null, null, userMessage.getMessageId(), 1);
        AssistantChatResponse response = new AssistantChatResponse(text);
        response.setConversationId(conversation.getConversationId());
        response.setMessageId(failure.getMessageId());
        return response;
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException("序列化助手工具结果失败", e); }
    }

    private Plan parsePlan(String raw) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(raw));
            String tool = root.path("tool").asText("none");
            if (!"none".equals(tool) && !TOOLS.contains(tool)) {
                throw new IllegalArgumentException("AI 选择了未注册的只读工具");
            }
            Map<String, Object> arguments = root.has("arguments")
                    ? objectMapper.convertValue(root.get("arguments"), new TypeReference<Map<String, Object>>() {})
                    : new LinkedHashMap<>();
            return new Plan(tool, arguments);
        } catch (Exception e) {
            log.warn("Parse assistant tool plan failed: {}", raw, e);
            throw new IllegalArgumentException("AI 助手返回的工具计划格式不正确", e);
        }
    }

    private String buildPlanPrompt(AssistantChatRequest request, List<AssistantMessageVO> history) {
        return "你是小说与视频创作工作台的只读助手。请根据用户问题选择一个工具，或在无需查询时返回 none。"
                + "只能选择以下工具：" + String.join(", ", TOOLS) + "。"
                + "严禁选择生成、修改、删除、重试、上传、执行 SQL 的工具。"
                + "只返回 JSON：{\"tool\":\"工具名或 none\",\"arguments\":{\"novelId\":数字,\"chapterNum\":数字,\"taskId\":数字,\"assetType\":字符串}}。"
                + "当前页面：" + value(request.getPage()) + "；当前作品 ID：" + value(request.getNovelId())
                + "；当前章节：" + value(request.getChapterNum()) + "；最近对话：" + historyText(history)
                + "；用户问题：" + request.getMessage();
    }

    private String buildDirectAnswerPrompt(AssistantChatRequest request, List<AssistantMessageVO> history) {
        return "请用简洁中文回答用户问题。你是只读助手，不能声称已经执行生成、修改、删除或上传操作。用户问题："
                + request.getMessage() + "。最近对话：" + historyText(history);
    }

    private String buildFinalAnswerPrompt(AssistantChatRequest request, List<AssistantMessageVO> history,
                                          Map<String, Object> toolResult) {
        try {
            return "请根据只读查询结果，用简洁、准确的中文回答用户问题。不要编造查询结果，不要执行任何写操作。"
                    + "如果数据为空，请明确说明当前没有找到。用户问题：" + request.getMessage()
                    + "。最近对话：" + historyText(history)
                    + "。只读查询结果：" + objectMapper.writeValueAsString(toolResult);
        } catch (Exception e) {
            throw new IllegalStateException("构造助手回答失败", e);
        }
    }

    private String extractJson(String raw) {
        if (raw == null) throw new IllegalArgumentException("AI 助手未返回内容");
        String text = raw.trim().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) throw new IllegalArgumentException("不是 JSON 对象");
        return text.substring(start, end + 1);
    }

    private String cleanAnswer(String answer) {
        if (answer == null || answer.isBlank()) return "暂时没有可用回答。";
        return answer.replaceFirst("^```(?:text)?\\s*", "").replaceFirst("\\s*```$", "").trim();
    }

    private String value(Object value) {
        return value == null ? "未提供" : String.valueOf(value);
    }

    private String historyText(List<AssistantMessageVO> history) {
        if (history == null || history.isEmpty()) return "无";
        StringBuilder text = new StringBuilder();
        int total = 0;
        for (AssistantMessageVO message : history) {
            if (message == null || message.getContent() == null || message.getContent().isBlank()) continue;
            String content = message.getContent().replaceAll("\\s+", " ").trim();
            if (content.length() > 1200) content = content.substring(0, 1200) + "…";
            String line = "[" + value(message.getRole()) + "] " + content + "\\n";
            if (total + line.length() > 12000) break;
            text.append(line);
            total += line.length();
        }
        return text.length() == 0 ? "无" : text.toString();
    }

    private record Plan(String tool, Map<String, Object> arguments) { }
}
