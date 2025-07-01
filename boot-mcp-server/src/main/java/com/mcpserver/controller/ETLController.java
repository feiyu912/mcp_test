package com.mcpserver.controller;

import com.mcpserver.service.IngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/etl")
public class ETLController {
    private final IngestionService ingestionService;

    @Autowired
    public ETLController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            ingestionService.ingestDataFromString(content);
            return "文件已成功入库！";
        } catch (Exception e) {
            e.printStackTrace();
            return "文件处理失败：" + e.getMessage();
        }
    }
} 