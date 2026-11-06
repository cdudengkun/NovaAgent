package com.cjack.novaagent.service;

import com.cjack.novaagent.tool.WeatherTool;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatOptions; // 👈 新的 import
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Service
public class SimpleAgent {

    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final List<ToolCallback> toolCallbacks;

    public SimpleAgent(ChatModel chatModel,
                       ToolCallingManager toolCallingManager,
                       WeatherTool weatherTool) {
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
        this.toolCallbacks = List.of(
                buildToolCallback(weatherTool, "getWeather")
        );
    }

    private ToolCallback buildToolCallback(Object toolObject, String methodName) {
        Method method = ReflectionUtils.findMethod(toolObject.getClass(), methodName, String.class);
        if (method == null) {
            throw new IllegalArgumentException("找不到方法: " + methodName);
        }

        return MethodToolCallback.builder()
                .toolDefinition(ToolDefinitions.builder(method)
                        .description("查询指定城市的当前天气。当用户询问天气时调用此工具。")
                        .build())
                .toolMethod(method)
                .toolObject(toolObject)
                .build();
    }

    public String run(String userInput) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage("你是一个智能助手，可以调用工具帮助用户。"));
        messages.add(new UserMessage(userInput));

        // ✅ 关键修复：显式指定模型名称
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("glm-4-flash")
                .toolCallbacks(toolCallbacks)
                .build();

        int round = 0;
        while (round++ < 5) {
            System.out.println("===== 第 " + round + " 轮 =====");

            Prompt prompt = new Prompt(messages, options);
            ChatResponse response = chatModel.call(prompt);

            AssistantMessage assistantMessage = response.getResult().getOutput();
            messages.add(assistantMessage);

            if (assistantMessage.getToolCalls() == null || assistantMessage.getToolCalls().isEmpty()) {
                System.out.println("===== 模型给出最终回答 =====");
                return assistantMessage.getText();
            }

            System.out.println("模型决定调用 " + assistantMessage.getToolCalls().size() + " 个工具");
            for (var toolCall : assistantMessage.getToolCalls()) {
                System.out.println("  → " + toolCall.name() + "(" + toolCall.arguments() + ")");
            }

            ToolExecutionResult execResult = toolCallingManager.executeToolCalls(prompt, response);
            messages = execResult.conversationHistory();
        }

        return "达到最大循环次数，任务未完成";
    }
}