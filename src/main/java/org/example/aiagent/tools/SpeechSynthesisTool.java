// src/main/java/org/example/aiagent/tools/SpeechSynthesisTool.java

package org.example.aiagent.tools;

import com.alibaba.dashscope.audio.tts.SpeechSynthesisResult;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import com.alibaba.dashscope.common.ResultCallback;
import com.alibaba.dashscope.utils.Constants;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.aiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;

@Slf4j
@Component
public class SpeechSynthesisTool {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        // 设置 WebSocket 服务端地址（北京地区）
        Constants.baseWebsocketApiUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";
    }

    @Tool(description = "Convert text to speech and save as audio file")
    public String textToSpeech(
            @ToolParam(description = "Text to convert to speech") String text,
            @ToolParam(description = "Output file name (e.g. output.wav)") String fileName) {
        try {
            if (text.length() > 20000) {
                return "错误：文本超过 20000 字限制";
            }
            // 1. 构建合成参数
            SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                    .apiKey(apiKey)
                    .model("cosyvoice-v3-flash")       // 最新模型
                    .voice("longanyang")                // v3 音色（注意：v3 模型用 v3 音色）
                    .build();

            // 2. 调用合成（同步方式）
            SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, null);
            ByteBuffer audioBuffer = synthesizer.call(text);

            // 3. 保存为文件
            String outputPath = FileConstant.FILE_SAVE_DIR + "/audio/" + fileName;
            java.io.File outputFile = new java.io.File(outputPath);
            outputFile.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(audioBuffer.array());
            }

            synthesizer.getDuplexApi().close(1000, "bye");
            return "语音合成成功，文件: " + outputPath;
        } catch (Exception e) {
            log.error("语音合成失败", e);
            return "语音合成失败: " + e.getMessage();
        }
    }

    /**
     * TTS流式回调
     * @param text
     * @param callback
     */
    @Tool(description = "Stream text-to-speech with callback for real-time playback")
    public void streamTextToSpeech(
            @ToolParam(description = "Text to convert to speech") String text,
            @ToolParam(description = "Callback for receiving audio chunks") ResultCallback<SpeechSynthesisResult> callback) {
        try {
            SpeechSynthesisParam param = SpeechSynthesisParam.builder()
                    .apiKey(apiKey)
                    .model("cosyvoice-v3-flash")
                    .voice("longanyang")
                    .build();

            // 传入 callback，call() 为异步方式，音频分片通过 onEvent 回调返回
            SpeechSynthesizer synthesizer = new SpeechSynthesizer(param, callback);
            synthesizer.call(text);
        } catch (Exception e) {
            log.error("流式语音合成失败", e);
        }
    }
}