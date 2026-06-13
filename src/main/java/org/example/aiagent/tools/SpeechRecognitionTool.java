
package org.example.aiagent.tools;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;

/**
 *  因为 @Value 只能在 Spring 管理的 Bean 里生效，
 *  所以它必须是 @Component，然后通过方法参数注入到 allTools() 里。
 */
@Slf4j
@Component
public class SpeechRecognitionTool {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    /**
     * 识别音频文件（非实时方式）
     */
    @Tool(description = "Recognize speech from an audio file and convert to text")
    public String recognizeAudioFile(
            @ToolParam(description = "Path to the audio file (wav/mp3/m4a)") String filePath) {
        try {
            // 1. 读取音频文件为字节数组
            File audioFile = new File(filePath);
            byte[] audioData;
            try (FileInputStream fis = new FileInputStream(audioFile);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                audioData = baos.toByteArray();
            }

            // 2. 调用 Paraformer 识别
            RecognitionParam param = RecognitionParam.builder()
                    .model("paraformer-realtime-v2")
                    .apiKey(apiKey)
                    .format("wav")
                    .sampleRate(16000)
                    .build();

            Recognition recognizer = new Recognition();
            StringBuilder result = new StringBuilder();
            CountDownLatch latch = new CountDownLatch(1);

            ResultCallback<RecognitionResult> callback = new ResultCallback<>() {
                @Override
                public void onEvent(RecognitionResult recognitionResult) {
                    if (recognitionResult.isSentenceEnd()) {
                        result.append(recognitionResult.getSentence().getText());
                    }
                }
                @Override
                public void onComplete() { latch.countDown(); }
                @Override
                public void onError(Exception e) {
                    log.error("ASR error", e);
                    result.append("识别错误: ").append(e.getMessage());
                    latch.countDown();
                }
            };

            recognizer.call(param, callback);
            // 发送音频数据
            recognizer.sendAudioFrame(ByteBuffer.wrap(audioData));
            recognizer.stop();
            latch.await();

            return result.toString();
        } catch (Exception e) {
            log.error("音频识别失败", e);
            return "音频识别失败: " + e.getMessage();
        }
    }

    /**
     * 录音并识别（从麦克风实时录音）
     * 使用 Java Sound API 采集麦克风 PCM 数据，通过 WebSocket 实时发送给 Paraformer 识别
     */
    @Tool(description = "Record audio from microphone and recognize speech")
    public String recordAndRecognize(
            @ToolParam(description = "Recording duration in seconds (1-60)") int durationSeconds) {
        // 参数校验
        if (durationSeconds < 1 || durationSeconds > 60) {
            return "录音时长必须在 1-60 秒之间";
        }

        // 音频格式：16kHz、16bit、单声道、PCM 有符号
        float sampleRate = 16000;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        javax.sound.sampled.AudioFormat format =
                new javax.sound.sampled.AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!javax.sound.sampled.AudioSystem.isLineSupported(info)) {
            return "错误：系统不支持 16kHz 录音，请检查麦克风驱动";
        }

        try (TargetDataLine microphone = (TargetDataLine) javax.sound.sampled.AudioSystem.getLine(info)) {
            microphone.open(format);
            microphone.start();

            // 1. 启动 Paraformer WebSocket 识别
            RecognitionParam param = RecognitionParam.builder()
                    .model("paraformer-realtime-v2")
                    .apiKey(apiKey)
                    .format("pcm")
                    .sampleRate(16000)
                    .build();

            Recognition recognizer = new Recognition();
            StringBuilder fullResult = new StringBuilder();
            CountDownLatch finishLatch = new CountDownLatch(1);

            ResultCallback<RecognitionResult> callback = new ResultCallback<>() {
                @Override
                public void onEvent(RecognitionResult recognitionResult) {
                    if (recognitionResult.isSentenceEnd()) {
                        String text = recognitionResult.getSentence().getText();
                        fullResult.append(text);
                    }
                }
                @Override
                public void onComplete() {
                    finishLatch.countDown();
                }
                @Override
                public void onError(Exception e) {
                    log.error("实时语音识别错误", e);
                    fullResult.append("识别错误: ").append(e.getMessage());
                    finishLatch.countDown();
                }
            };

            // 启动识别（建立 WebSocket 连接）
            recognizer.call(param, callback);

            // 2. 从麦克风读取 PCM 数据，逐块发送给识别服务
            byte[] buffer = new byte[3200];  // 每块 100ms 的音频数据 (16000*2*0.1=3200)
            long startTime = System.currentTimeMillis();
            long endTime = startTime + durationSeconds * 1000L;
            int bytesRead;

            while (System.currentTimeMillis() < endTime
                    && (bytesRead = microphone.read(buffer, 0, buffer.length)) > 0) {
                recognizer.sendAudioFrame(ByteBuffer.wrap(buffer, 0, bytesRead));
            }

            // 3. 停止录音和识别
            microphone.stop();
            recognizer.stop();
            // 等待识别结果返回（最多等 5 秒）
            finishLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);
            recognizer.getDuplexApi().close(1000, "bye");

            String result = fullResult.toString().trim();
            if (result.isEmpty()) {
                return "未检测到语音内容";
            }
            return result;

        } catch (javax.sound.sampled.LineUnavailableException e) {
            log.error("麦克风不可用", e);
            return "错误：无法访问麦克风，请检查麦克风权限和驱动";
        } catch (Exception e) {
            log.error("实时录音识别失败", e);
            return "录音识别失败: " + e.getMessage();
        }
    }
}