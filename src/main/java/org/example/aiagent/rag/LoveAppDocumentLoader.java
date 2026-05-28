package org.example.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 恋爱大师应用文档加载器
 */
@Component
@Slf4j
public class LoveAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;
    //构造函数
    public LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载多篇md文档
     * @return
     */
    public List<Document> loadDocuments() {
        List<org.springframework.ai.document.Document> allDocuments = new ArrayList<>();
        //加载多篇md
       try{
           Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
           for (Resource resource : resources) {
               String filename = resource.getFilename();
               String status = filename.substring(filename.length() - 6, filename.length() - 4);
               MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                       .withHorizontalRuleCreateDocument(true)//切割
                       .withIncludeCodeBlock(false)
                       .withIncludeBlockquote(false)
                       .withAdditionalMetadata("filename", filename)//添加源信息
                       .withAdditionalMetadata("status", status)
                       .build();
               MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
               allDocuments.addAll(markdownDocumentReader.get());
           }

       }catch(Exception e){
           log.error("markdown文档加载失败！！");
       }
        return allDocuments;
    }
}
