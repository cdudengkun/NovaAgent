package com.cjack.novaagent.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool {

    @Tool(description = "查询指定城市的当前天气。当用户询问天气时调用此工具。")
    public String getWeather(String city) {
        // 这里是假的，真实项目里应该调天气 API
        System.out.println(">>> 工具被调用：getWeather(city=" + city + ")");
        return switch (city) {
            case "北京" -> "北京今天晴，气温 25°C，微风";
            case "上海" -> "上海今天多云，气温 28°C，东南风 3 级";
            case "广州" -> "广州今天有雨，气温 30°C，注意带伞";
            default -> city + "今天天气晴朗，气温 22°C";
        };
    }
}