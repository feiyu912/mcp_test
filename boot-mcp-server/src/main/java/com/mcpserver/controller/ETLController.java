package com.mcpserver.controller;

import com.mcpserver.service.IngestionService;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/etl")
public class ETLController {
    private final IngestionService ingestionService;
    private final Tika tika = new Tika();

    @Autowired
    public ETLController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 用Tika自动识别并提取文本内容
            String content = tika.parseToString(file.getInputStream());
            ingestionService.ingestDataFromString(content);
            return "文件已成功入库！";
        } catch (Exception e) {
            e.printStackTrace();
            return "文件处理失败：" + e.getMessage();
        }
    }
} 