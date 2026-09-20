package com.novelgeneration.novel.service;

import com.novelgeneration.novel.dto.NovelImportResult;
import org.springframework.web.multipart.MultipartFile;

public interface NovelImportService {
    NovelImportResult importNovel(MultipartFile file, String requestedTitle);
}
