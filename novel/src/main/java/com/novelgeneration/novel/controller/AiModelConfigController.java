package com.novelgeneration.novel.controller;

import com.novelgeneration.novel.dto.AiModelConfigRequest;
import com.novelgeneration.novel.dto.Result;
import com.novelgeneration.novel.service.AiModelConfigService;
import com.novelgeneration.novel.vo.AiModelConfigTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/model-configs")
public class AiModelConfigController {
    @Resource
    private AiModelConfigService service;

    @GetMapping
    public Result list() {
        try {
            return Result.ok(service.list());
        } catch (RuntimeException e) {
            return Result.fail(message(e));
        }
    }

    @PostMapping("/{capabilityType}/test")
    public Result test(@PathVariable String capabilityType, @RequestBody AiModelConfigRequest request) {
        try {
            AiModelConfigTestResult result = service.test(capabilityType, request);
            return result.isSuccess() ? Result.ok(result) : Result.fail(result.getMessage());
        } catch (RuntimeException e) {
            return Result.fail(message(e));
        }
    }

    @PutMapping("/{capabilityType}")
    public Result save(@PathVariable String capabilityType, @RequestBody AiModelConfigRequest request) {
        try {
            return Result.ok(service.save(capabilityType, request));
        } catch (RuntimeException e) {
            return Result.fail(message(e));
        }
    }

    @PostMapping("/{capabilityType}/disable")
    public Result disable(@PathVariable String capabilityType) {
        try {
            return Result.ok(service.disable(capabilityType));
        } catch (RuntimeException e) {
            return Result.fail(message(e));
        }
    }

    private String message(RuntimeException e) {
        return e.getMessage() == null || e.getMessage().isBlank() ? "模型配置操作失败" : e.getMessage();
    }
}
