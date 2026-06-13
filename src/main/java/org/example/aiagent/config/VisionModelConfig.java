
package org.example.aiagent.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VisionModelConfig {

    @Bean("visionChatClient")
    public ChatClient visionChatClient(ChatModel dashscopeChatModel) {
        return ChatClient.builder(dashscopeChatModel)
                .defaultOptions(DashScopeChatOptions.builder()
                        .withModel("qwen-vl-max")       // ← 视觉模型
                        .withMultiModel(true)            // ← 必须开启多模态
                        .build())
                .build();
    }
}