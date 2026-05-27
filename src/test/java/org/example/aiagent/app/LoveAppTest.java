package org.example.aiagent.app;

import jakarta.annotation.Resource;
import org.jetbrains.annotations.TestOnly;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

//@SpringBootTest
class LoveAppTest {

    @Autowired
    private LoveApp loveApp;

    @Test
    void testDoChat() {
        String chatId = UUID.randomUUID().toString();
        //第一轮
        String message="你好";
        String answer= loveApp.doChat(chatId,message);
        //第2轮
        message="我想让另一半（张爱玲）更爱我";
        answer= loveApp.doChat(chatId,message);
        Assertions.assertNotNull(answer);//检验answer不为空
        //第3轮
        message="我刚刚跟你说的我的另一半叫什么来着?你帮我回忆一下";
        answer= loveApp.doChat(chatId,message);
        Assertions.assertNotNull(answer);


    }
}