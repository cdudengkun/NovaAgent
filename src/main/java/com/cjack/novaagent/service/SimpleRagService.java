package com.cjack.novaagent.service;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SimpleRagService {

    private final ChatClient chatClient;
    private final List<String> chunks = new ArrayList<>();

    public SimpleRagService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostConstruct
    public void loadDocument() throws IOException {
        ClassPathResource resource = new ClassPathResource("docs/company-rules.txt");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        String[] blocks = content.split("\\n\\s*\\n");
        for (String block : blocks) {
            String trimmed = block.trim();
            // 过滤：太短的块不要（标题、空行等）
            if (trimmed.length() > 20) {
                chunks.add(trimmed);
            }
        }
        System.out.println(">>> 文档加载完成，共 " + chunks.size() + " 块");
    }

    /**
     * 极简检索：按关键词命中数排序
     */
    private List<String> retrieve(String question, int topK) {
        return chunks.stream()
                .map(chunk -> new Object[]{chunk, score(chunk, question)})
                .sorted((a, b) -> Double.compare((double) b[1], (double) a[1]))
                .limit(topK)
                .map(arr -> (String) arr[0])
                .toList();
    }

    private double score(String chunk, String question) {
        // 简单分词：中文按 2-gram（相邻两个字一组），英文按空格
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < question.length() - 1; i++) {
            tokens.add(question.substring(i, i + 2));
        }
        return tokens.stream().filter(chunk::contains).count();
    }

    public String ask(String question) {
        List<String> relevant = retrieve(question, 3);
        String context = String.join("\n\n---\n\n", relevant);

        String prompt = """
                请根据下面的资料回答用户问题。
                如果资料中没有相关信息，请直接说“资料中没有相关内容”，不要编造。

                【资料】
                %s

                【用户问题】
                %s
                """.formatted(context, question);

        System.out.println(">>> 检索到 " + relevant.size() + " 段资料");
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}