package com.mcpserver.service;

import java.util.List;

public class EmbeddingUtils {
    /**
     * 计算两个向量的余弦相似度
     */
    public static double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1.size() != v2.size()) return 0;
        double dot = 0, norm1 = 0, norm2 = 0;
        for (int i = 0; i < v1.size(); i++) {
            dot += v1.get(i) * v2.get(i);
            norm1 += v1.get(i) * v1.get(i);
            norm2 += v2.get(i) * v2.get(i);
        }
        if (norm1 == 0 || norm2 == 0) return 0;
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
} 