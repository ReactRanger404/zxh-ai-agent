package org.example.aiagent.controller;


import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.aiagent.agent.Manus;
import org.example.aiagent.app.LoveApp;
import org.example.aiagent.constant.FileConstant;
import org.example.aiagent.tools.SpeechRecognitionTool;
import org.example.aiagent.tools.SpeechSynthesisTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
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
@Slf4j
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


    @Resource
    private SpeechRecognitionTool speechRecognitionTool;

    /**
     * 语音识别（上传音频文件，返回识别文字）
     * POST /api/ai/asr/recognize  multipart/form-data
     * 字段: file (音频文件)
     */
    @PostMapping(value = "/asr/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> recognizeSpeech(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("请上传音频文件");
        }

        try {
            // 1. 保存上传的音频到临时文件
            String tempDir = FileConstant.FILE_SAVE_DIR + "/temp_audio";
            java.io.File tempFile = new java.io.File(tempDir, file.getOriginalFilename());
            tempFile.getParentFile().mkdirs();
            file.transferTo(tempFile);

            // 2. 调用语音识别工具
            String result = speechRecognitionTool.recognizeAudioFile(tempFile.getAbsolutePath());

            // 3. 清理临时文件
            tempFile.delete();

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("语音识别失败", e);
            return ResponseEntity.internalServerError().body("语音识别失败: " + e.getMessage());
        }
    }

    @Resource
    private SpeechSynthesisTool speechSynthesisTool;

    /**
     * 语音合成（文字转语音，直接返回音频流供浏览器播放）
     * GET /api/ai/tts/synthesize?text=你好
     * 前端直接用 new Audio(url).play() 播放
     */
    @GetMapping("/tts/synthesize")
    public ResponseEntity<byte[]> synthesizeSpeech(@RequestParam String text) {
        try {
            // 1. 调用 TTS 合成音频
            String tempFileName = "tts_temp_" + System.currentTimeMillis() + ".wav";
            String result = speechSynthesisTool.textToSpeech(text, tempFileName);
            if (result.startsWith("错误")) {
                return ResponseEntity.badRequest().build();
            }

            // 2. 读取临时文件
            String filePath = FileConstant.FILE_SAVE_DIR + "/audio/" + tempFileName;
            java.io.File file = new java.io.File(filePath);
            byte[] audioBytes = java.nio.file.Files.readAllBytes(file.toPath());
            file.delete();  // 发送后删除临时文件

            // 3. 返回音频字节流（浏览器自动播放，不触发下载）
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/wav"))
                    .body(audioBytes);
        } catch (Exception e) {
            log.error("语音合成失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
