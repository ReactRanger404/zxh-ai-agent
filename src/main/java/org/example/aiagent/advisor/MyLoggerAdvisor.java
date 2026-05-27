package org.example.aiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 * 自定义日志 Advisor
 * 打印 info 级别日志、只输出单次用户提示词和 AI 回复的文本
 */
@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int getOrder() {
		return 0;
	}//代表最高优先级

	private ChatClientRequest before(ChatClientRequest request) {
		log.info("AI Request: {}", request.prompt());
		return request;
	}

	private void observeAfter(ChatClientResponse chatClientResponse) {
		log.info("AI Response: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
		// 1. 发车前：调用自定义的 before() 方法，打印出用户发出的 Prompt
		chatClientRequest = before(chatClientRequest);
		// 【步骤 2：核心交接】
		// chain.nextCall() 这句代码才是真正给阿里云发网络请求的地方！
		// 你的程序会在这里“卡住”等一会儿，直到阿里云把大模型的回答传回来。
		// 传回来的结果，被塞进了 chatClientResponse 这个变量里。
		ChatClientResponse chatClientResponse = chain.nextCall(chatClientRequest);
		observeAfter(chatClientResponse);
		// 检查站工作完毕，把最终结果交还给你的业务代码（比如你的 Test 类）。
		return chatClientResponse;
	}

	@Override
	public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
		chatClientRequest = before(chatClientRequest);
		Flux<ChatClientResponse> chatClientResponseFlux = chain.nextStream(chatClientRequest);
		return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chatClientResponseFlux, this::observeAfter);
	}
}
