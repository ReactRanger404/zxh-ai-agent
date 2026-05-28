package org.example.aiagent.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.DocumentEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 向量数据库配置（初始化基于内存的向量化数据库Bean）
 */
@Configuration
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore= SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        List<Document> documentList = loveAppDocumentLoader.loadDocuments();
        simpleVectorStore.add(documentList);
        return simpleVectorStore;
    }
}
