package com.star.ai.llm.server;

import com.star.ai.llm.client.ModelServiceManager;
import com.star.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 模型控制接口
 * 提供大模型相关的API接口。
 */
@RestController
@RequestMapping("/api/ai/model")
@Slf4j
public class ModelController {

    @Autowired
    private ModelServiceManager modelServiceManager;

    /**
     * 获取所有可用的模型列表
     */
    @GetMapping("/list")
    public Result<List<ModelInfo>> getModelList() {
        try {
            List<String> modelIds = modelServiceManager.getAllModelIds();
            List<ModelInfo> modelInfos = new ArrayList<>();
            
            for (String modelId : modelIds) {
                ModelInfo info = ModelInfo.builder()
                        .modelId(modelId)
                        .modelName(getModelDisplayName(modelId))
                        .status("active")
                        .description(getModelDescription(modelId))
                        .build();
                modelInfos.add(info);
            }
            
            log.info("获取模型列表完成，共{}个模型", modelInfos.size());
            return Result.success(modelInfos);
            
        } catch (Exception e) {
            log.error("获取模型列表失败", e);
            return Result.error("获取模型列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取特定模型的详细信息
     */
    @GetMapping("/{modelId}")
    public Result<ModelInfo> getModelInfo(@PathVariable String modelId) {
        try {
            if (!StringUtils.hasText(modelId)) {
                return Result.error("模型ID不能为空");
            }
            
            ModelServiceManager.LLMService service = modelServiceManager.getModel(modelId);
            if (service == null) {
                return Result.error("模型不存在：" + modelId);
            }
            
            ModelInfo info = ModelInfo.builder()
                    .modelId(modelId)
                    .modelName(getModelDisplayName(modelId))
                    .status("active")
                    .description(getModelDescription(modelId))
                    .capabilities(getModelCapabilities(modelId))
                    .build();
            
            log.info("获取模型详情完成，模型ID：{}", modelId);
            return Result.success(info);
            
        } catch (Exception e) {
            log.error("获取模型详情失败，模型ID：{}", modelId, e);
            return Result.error("获取模型详情失败：" + e.getMessage());
        }
    }

    /**
     * 调用单个模型
     */
    @PostMapping("/{modelId}/call")
    public Result<ModelResponse> callModel(@PathVariable String modelId, 
                                         @RequestBody ModelRequest request) {
        try {
            if (!StringUtils.hasText(modelId)) {
                return Result.error("模型ID不能为空");
            }
            
            if (request == null || !StringUtils.hasText(request.getPrompt())) {
                return Result.error("请求内容不能为空");
            }
            
            ModelServiceManager.LLMService service = modelServiceManager.getModel(modelId);
            if (service == null) {
                return Result.error("模型不存在：" + modelId);
            }
            
            long startTime = System.currentTimeMillis();
            String response = service.call(request.getPrompt());
            long endTime = System.currentTimeMillis();
            
            ModelResponse modelResponse = ModelResponse.builder()
                    .modelId(modelId)
                    .response(response)
                    .responseTime(endTime - startTime)
                    .tokens(estimateTokens(request.getPrompt(), response))
                    .build();
            
            log.info("模型调用完成，模型ID：{}，响应时间：{}ms", modelId, endTime - startTime);
            return Result.success(modelResponse);
            
        } catch (Exception e) {
            log.error("模型调用失败，模型ID：{}", modelId, e);
            return Result.error("模型调用失败：" + e.getMessage());
        }
    }

    /**
     * 并行调用多个模型
     */
    @PostMapping("/parallel-call")
    public Result<Map<String, ModelResponse>> parallelCall(@RequestBody ParallelCallRequest request) {
        try {
            if (request == null || !StringUtils.hasText(request.getPrompt())) {
                return Result.error("请求内容不能为空");
            }
            
            if (request.getModelIds() == null || request.getModelIds().isEmpty()) {
                return Result.error("模型ID列表不能为空");
            }
            
            long startTime = System.currentTimeMillis();
            Map<String, String> rawResponses = modelServiceManager.parallelCall(
                request.getPrompt(), request.getModelIds());
            long totalTime = System.currentTimeMillis() - startTime;
            
            Map<String, ModelResponse> responses = new HashMap<>();
            for (Map.Entry<String, String> entry : rawResponses.entrySet()) {
                ModelResponse modelResponse = ModelResponse.builder()
                        .modelId(entry.getKey())
                        .response(entry.getValue())
                        .responseTime(totalTime) // 并行调用，时间为总时间
                        .tokens(estimateTokens(request.getPrompt(), entry.getValue()))
                        .build();
                responses.put(entry.getKey(), modelResponse);
            }
            
            log.info("并行模型调用完成，模型数量：{}，总响应时间：{}ms", 
                request.getModelIds().size(), totalTime);
            return Result.success(responses);
            
        } catch (Exception e) {
            log.error("并行模型调用失败", e);
            return Result.error("并行模型调用失败：" + e.getMessage());
        }
    }

    /**
     * 注册新的模型服务
     */
    @PostMapping("/register")
    public Result<String> registerModel(@RequestBody RegisterModelRequest request) {
        try {
            if (request == null || !StringUtils.hasText(request.getModelId())) {
                return Result.error("模型ID不能为空");
            }
            
            if (!StringUtils.hasText(request.getServiceType())) {
                return Result.error("服务类型不能为空");
            }
            
            // 创建一个简单的模型服务实现（实际项目中应该根据serviceType创建对应的服务）
            ModelServiceManager.LLMService service = createModelService(request);
            
            modelServiceManager.registerModel(request.getModelId(), service);
            
            log.info("模型注册成功，模型ID：{}，服务类型：{}", 
                request.getModelId(), request.getServiceType());
            return Result.success("模型注册成功");
            
        } catch (Exception e) {
            log.error("模型注册失败", e);
            return Result.error("模型注册失败：" + e.getMessage());
        }
    }

    /**
     * 模型健康检查
     */
    @GetMapping("/{modelId}/health")
    public Result<Map<String, Object>> healthCheck(@PathVariable String modelId) {
        try {
            if (!StringUtils.hasText(modelId)) {
                return Result.error("模型ID不能为空");
            }
            
            ModelServiceManager.LLMService service = modelServiceManager.getModel(modelId);
            if (service == null) {
                return Result.error("模型不存在：" + modelId);
            }
            
            // 执行简单的健康检查
            long startTime = System.currentTimeMillis();
            String testResponse = service.call("Hello");
            long responseTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("modelId", modelId);
            healthInfo.put("status", "healthy");
            healthInfo.put("responseTime", responseTime + "ms");
            healthInfo.put("testResponse", testResponse.length() > 0 ? "正常" : "异常");
            healthInfo.put("checkTime", new Date());
            
            log.info("模型健康检查完成，模型ID：{}，状态：健康", modelId);
            return Result.success(healthInfo);
            
        } catch (Exception e) {
            log.error("模型健康检查失败，模型ID：{}", modelId, e);
            
            Map<String, Object> healthInfo = new HashMap<>();
            healthInfo.put("modelId", modelId);
            healthInfo.put("status", "unhealthy");
            healthInfo.put("error", e.getMessage());
            healthInfo.put("checkTime", new Date());
            
            return Result.success(healthInfo);
        }
    }

    /**
     * 获取模型显示名称
     */
    private String getModelDisplayName(String modelId) {
        switch (modelId.toLowerCase()) {
            case "openai": return "OpenAI GPT";
            case "deepseek": return "DeepSeek";
            case "qwen": return "通义千问";
            case "chatglm": return "ChatGLM";
            default: return modelId.toUpperCase();
        }
    }

    /**
     * 获取模型描述
     */
    private String getModelDescription(String modelId) {
        switch (modelId.toLowerCase()) {
            case "openai": return "OpenAI官方大语言模型，支持对话和文本生成";
            case "deepseek": return "DeepSeek开源大语言模型，专注于推理能力";
            case "qwen": return "阿里云通义千问大语言模型，中文理解能力强";
            case "chatglm": return "智谱AI开源大语言模型，支持多轮对话";
            default: return "第三方大语言模型服务";
        }
    }

    /**
     * 获取模型能力列表
     */
    private List<String> getModelCapabilities(String modelId) {
        List<String> capabilities = new ArrayList<>();
        capabilities.add("文本生成");
        capabilities.add("对话聊天");
        capabilities.add("问答回复");
        
        switch (modelId.toLowerCase()) {
            case "openai":
                capabilities.add("代码生成");
                capabilities.add("创意写作");
                break;
            case "deepseek":
                capabilities.add("数学推理");
                capabilities.add("逻辑分析");
                break;
            case "qwen":
                capabilities.add("中文理解");
                capabilities.add("知识问答");
                break;
        }
        
        return capabilities;
    }

    /**
     * 估算Token数量（简单实现）
     */
    private int estimateTokens(String prompt, String response) {
        // 简单估算：中文按字符数，英文按单词数*1.3
        int promptTokens = prompt.length();
        int responseTokens = response.length();
        return (int) ((promptTokens + responseTokens) * 1.2);
    }

    /**
     * 创建模型服务（简单实现，实际应该根据serviceType创建具体服务）
     */
    private ModelServiceManager.LLMService createModelService(RegisterModelRequest request) {
        return new ModelServiceManager.LLMService() {
            @Override
            public String call(String prompt) {
                return "这是来自 " + request.getModelId() + " 的模拟回复：" + prompt;
            }
        };
    }

    // 内部数据类
    public static class ModelInfo {
        private String modelId;
        private String modelName;
        private String status;
        private String description;
        private List<String> capabilities;
        
        public static ModelInfoBuilder builder() {
            return new ModelInfoBuilder();
        }
        
        // Getters and Setters
        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getCapabilities() { return capabilities; }
        public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }
        
        public static class ModelInfoBuilder {
            private ModelInfo info = new ModelInfo();
            public ModelInfoBuilder modelId(String modelId) { info.modelId = modelId; return this; }
            public ModelInfoBuilder modelName(String modelName) { info.modelName = modelName; return this; }
            public ModelInfoBuilder status(String status) { info.status = status; return this; }
            public ModelInfoBuilder description(String description) { info.description = description; return this; }
            public ModelInfoBuilder capabilities(List<String> capabilities) { info.capabilities = capabilities; return this; }
            public ModelInfo build() { return info; }
        }
    }

    public static class ModelRequest {
        private String prompt;
        private Map<String, Object> parameters;
        
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }

    public static class ModelResponse {
        private String modelId;
        private String response;
        private long responseTime;
        private int tokens;
        
        public static ModelResponseBuilder builder() {
            return new ModelResponseBuilder();
        }
        
        // Getters and Setters
        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
        public int getTokens() { return tokens; }
        public void setTokens(int tokens) { this.tokens = tokens; }
        
        public static class ModelResponseBuilder {
            private ModelResponse response = new ModelResponse();
            public ModelResponseBuilder modelId(String modelId) { response.modelId = modelId; return this; }
            public ModelResponseBuilder response(String resp) { response.response = resp; return this; }
            public ModelResponseBuilder responseTime(long time) { response.responseTime = time; return this; }
            public ModelResponseBuilder tokens(int tokens) { response.tokens = tokens; return this; }
            public ModelResponse build() { return response; }
        }
    }

    public static class ParallelCallRequest {
        private String prompt;
        private List<String> modelIds;
        private Map<String, Object> parameters;
        
        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public List<String> getModelIds() { return modelIds; }
        public void setModelIds(List<String> modelIds) { this.modelIds = modelIds; }
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    }

    public static class RegisterModelRequest {
        private String modelId;
        private String serviceType;
        private String endpoint;
        private String apiKey;
        private Map<String, Object> config;
        
        public String getModelId() { return modelId; }
        public void setModelId(String modelId) { this.modelId = modelId; }
        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public Map<String, Object> getConfig() { return config; }
        public void setConfig(Map<String, Object> config) { this.config = config; }
    }
} 