package org.example.aiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Springai框架调用ai
 */
@Component
public class SpringAiAiInvoke implements CommandLineRunner {


    @Resource
    private ChatModel dashscopeChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage = dashscopeChatModel.call(new Prompt("你好，介绍一下自己。"))
                .getResult()
                .getOutput();

        System.out.println(assistantMessage.getText());
    }


}
