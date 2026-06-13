package org.example.aiagent.controller;


import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.example.aiagent.agent.Manus;
import org.example.aiagent.app.LoveApp;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步调用 AI 恋爱大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return loveApp.doChat(message, chatId);
    }

    /**
     * SSE 流式调用 AI 恋爱大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId);
    }


    /**
     * SSE 流式调用 AI 恋爱大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/love_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithLoveAppServerSentEvent(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式调用 AI 恋爱大师应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/love_app/chat/sse_emitter")
    public SseEmitter doChatWithLoveAppServerSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 SseEmitter
        loveApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        // 返回
        return sseEmitter;
    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        Manus yuManus = new Manus(allTools, dashscopeChatModel);
        return yuManus.runStream(message);
    }


    @Resource
    private ChatClient visionChatClient;

    /**
     * 多模态对话（通过图片 URL）
     * GET /api/ai/vision/chat?message=描述一下&imageUrl=https://...
     */
    @GetMapping("/vision/chat")
    public SseEmitter doChatWithVision(
            @RequestParam String message,
            @RequestParam(required = false) String imageUrl) {
        SseEmitter emitter = new SseEmitter(180000L);
        CompletableFuture.runAsync(() -> {
            try {
                var spec = visionChatClient.prompt().user(u -> {
                    u.text(message);
                    if (StrUtil.isNotBlank(imageUrl)) {
                        try {
                            u.media(MediaType.IMAGE_JPEG, URI.create(imageUrl).toURL());
                        } catch (java.net.MalformedURLException e) {
                            u.text("\n[图片URL格式错误]");
                        }
                    }
                });
                spec.stream().content().subscribe(
                        chunk -> {
                            try {
                                emitter.send(chunk);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        emitter::completeWithError,
                        emitter::complete
                );
            } catch (Exception e) {
                try {
                    emitter.send("处理失败: " + e.getMessage());
                    emitter.complete();
                } catch (IOException ignored) {
                }
            }
        });
        return emitter;
    }

    /**
     * 多模态对话（前端上传图片文件）
     * POST /api/ai/vision/chat/upload  multipart/form-data
     * 字段: message, file
     */
    @PostMapping(value = "/vision/chat/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SseEmitter doChatWithVisionUpload(
            @RequestParam String message,
            @RequestParam(required = false) MultipartFile file) {
        SseEmitter emitter = new SseEmitter(180000L);
        CompletableFuture.runAsync(() -> {
            try {
                visionChatClient.prompt().user(u -> {
                    u.text(message);
                    if (file != null && !file.isEmpty()) {
                        try {
                            u.media(MediaType.IMAGE_JPEG, new ByteArrayResource(file.getBytes()));
                        } catch (IOException e) {
                            u.text("\n[图片读取失败: " + e.getMessage() + "]");
                        }
                    }
                }).stream().content().subscribe(
                        chunk -> { try { emitter.send(chunk); } catch (IOException e) { emitter.completeWithError(e); } },
                        emitter::completeWithError,
                        emitter::complete
                );
            } catch (Exception e) {
                try { emitter.send("处理失败: " + e.getMessage()); emitter.complete(); } catch (IOException ignored) {}
            }
        });
        return emitter;
    }
}
