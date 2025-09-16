package com.star.ai.agent.tool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Tool注册中心，支持Tool的注册、发现和统一调用
 */
@Component
public class ToolRegistry {
    @Autowired(required = false)
    private List<Tool> toolList = new ArrayList<>();

    private final Map<String, Tool> toolMap = new HashMap<>();

    @PostConstruct
    public void init() {
        if (toolList != null) {
            for (Tool tool : toolList) {
                toolMap.put(tool.getName(), tool);
            }
        }
    }

    /**
     * 获取所有已注册的Tool
     */
    public List<Tool> getAllTools() {
        return new ArrayList<>(toolMap.values());
    }

    /**
     * 根据名称获取Tool
     */
    public Tool getTool(String name) {
        return toolMap.get(name);
    }

    /**
     * 统一调用Tool
     */
    public Object callTool(String name, Map<String, Object> params) {
        Tool tool = getTool(name);
        if (tool == null) throw new IllegalArgumentException("Tool not found: " + name);
        return tool.call(params);
    }
} 