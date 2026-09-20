package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.AssistantChatRequest;
import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.service.AssistantService;
import com.novelgeneration.novel.service.AssistantConversationService;
import com.novelgeneration.novel.vo.AssistantConversationVO;
import com.novelgeneration.novel.vo.AssistantMessageVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    @Resource
    private AssistantService assistantService;

    @Resource
    private AssistantConversationService conversationService;

    @PostMapping("/chat")
    public Result chat(@RequestBody AssistantChatRequest request) {
        return Result.ok(assistantService.chat(request));
    }

    @GetMapping("/conversations/current")
    public Result current(@RequestParam(required = false) Long novelId) {
        return Result.ok(conversationService.getOrCreate(novelId, novelId == null ? "通用助手" : "作品 " + novelId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Result messages(@PathVariable String conversationId, @RequestParam(defaultValue = "20") int rounds) {
        AssistantConversationVO conversation = new AssistantConversationVO();
        conversation.setConversationId(conversationId);
        return Result.ok(conversationService.recentMessages(conversation, Math.min(Math.max(rounds, 1), 20)));
    }

    @GetMapping("/conversations")
    public Result list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(conversationService.list(page, size), conversationService.count());
    }

    @DeleteMapping("/conversations/{conversationId}")
    public Result delete(@PathVariable String conversationId) {
        conversationService.delete(conversationId);
        return Result.ok();
    }
}
