package com.cjack.novaagent.controller;

import com.cjack.novaagent.model.IntentResult;
import com.cjack.novaagent.tool.WeatherTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.cjack.novaagent.service.SimpleAgent;
import com.cjack.novaagent.service.SimpleRagService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;
    private final WeatherTool weatherTool;
    private final SimpleAgent simpleAgent;
    private final SimpleRagService simpleRagService;

    // 构造函数里加参数
    public ChatController(ChatClient.Builder builder,
                          WeatherTool weatherTool,
                          SimpleAgent simpleAgent,
                          SimpleRagService simpleRagService) {
        this.chatClient = builder.build();
        this.weatherTool = weatherTool;
        this.simpleAgent = simpleAgent;
        this.simpleRagService = simpleRagService;
    }


    /** 第 1 课：普通聊天 */
    @GetMapping
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .system("你是一个 Java 后端助手，回答要简洁、准确。")
                .user(message)
                .call()
                .content();
    }

    /** 第 2 课：结构化输出 */
    @GetMapping("/json")
    public IntentResult json(@RequestParam String message) {
        String systemPrompt = """
                你是一个意图识别助手。用户会输入一句话，你需要判断他想做什么。

                请严格按以下 JSON 格式返回，不要输出任何其他内容：
                {
                  "action": "动作名称，只能是 get_weather 或 unknown",
                  "city": "城市名，没有则填空字符串",
                  "reason": "你判断的理由，一句话"
                }
                """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .call()
                .entity(IntentResult.class);
    }

    /** 第 3 课：Agent 工具调用 */
    @GetMapping("/agent")
    public String agent(@RequestParam String message) {
        return chatClient.prompt()
                .system("你是一个智能助手。你可以调用工具来帮助用户解决问题。")
                .user(message)
                .tools(weatherTool)   // 👈 关键：注册工具
                .call()
                .content();
    }

    @GetMapping("/manual-agent")
    public String manualAgent(@RequestParam String message) {
        return simpleAgent.run(message);
    }

    @GetMapping("/rag")
    public String rag(@RequestParam String message) {
        return simpleRagService.ask(message);
    }
}