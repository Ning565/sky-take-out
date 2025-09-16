package com.star.ai.llm.client;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;

/**
 * 模型服务管理类
 * 负责管理和调度不同的大模型服务。
 */
@Component
public class ModelServiceManager {
    private final Map<String, LLMService> modelMap = new HashMap<>();

    /**
     * 注册模型服务
     */
    public void registerModel(String modelId, LLMService service) {
        modelMap.put(modelId, service);
    }

    /**
     * 获取所有模型ID
     */
    public List<String> getAllModelIds() {
        return new ArrayList<>(modelMap.keySet());
    }

    /**
     * 获取模型服务
     */
    public LLMService getModel(String modelId) {
        return modelMap.get(modelId);
    }

    /**
     * 并行调用多个模型
     */
    public Map<String, String> parallelCall(String prompt, List<String> modelIds) {
        Map<String, String> result = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(modelIds.size());
        List<Future<?>> futures = new ArrayList<>();
        for (String modelId : modelIds) {
            LLMService service = modelMap.get(modelId);
            if (service != null) {
                futures.add(executor.submit(() -> {
                    String resp = service.call(prompt);
                    result.put(modelId, resp);
                }));
            }
        }
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception ignored) {}
        }
        executor.shutdown();
        return result;
    }

    /**
     * LLMService接口，需由具体模型实现
     */
    public interface LLMService {
        String call(String prompt);
    }
} 