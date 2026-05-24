package com.admin.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class AiModelConfig {

    @Value("${ai.model.base-url}")
    private String baseUrl;

    @Value("${ai.model.api-key}")
    private String apiKey;

    @Value("${ai.model.model-name}")
    private String modelName;

    @Value("${ai.model.temperature}")
    private Double temperature;

    @Value("${ai.model.max-tokens}")
    private Integer maxTokens;

    @Value("${ai.embedding.type:local}")
    private String embeddingType;

    @Value("${ai.embedding.base-url:}")
    private String embeddingBaseUrl;

    @Value("${ai.embedding.api-key:}")
    private String embeddingApiKey;

    @Value("${ai.embedding.model-name:text-embedding-v1}")
    private String embeddingModelName;

    @Value("${ai.vector-store.type:milvus}")
    private String vectorStoreType;

    @Value("${ai.milvus.host:localhost}")
    private String milvusHost;

    @Value("${ai.milvus.port:19530}")
    private Integer milvusPort;

    @Value("${ai.milvus.collection-name:kb_chunks}")
    private String milvusCollectionName;

    @Value("${ai.milvus.dimension:1024}")
    private Integer milvusDimension;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        if ("api".equalsIgnoreCase(embeddingType)) {
            String actualBaseUrl = (embeddingBaseUrl == null || embeddingBaseUrl.isBlank()) ? baseUrl : embeddingBaseUrl;
            String actualApiKey = (embeddingApiKey == null || embeddingApiKey.isBlank()) ? apiKey : embeddingApiKey;
            log.info("使用远程 Embedding 模型: {} ({})", embeddingModelName, actualBaseUrl);
            return OpenAiEmbeddingModel.builder()
                    .baseUrl(actualBaseUrl)
                    .apiKey(actualApiKey)
                    .modelName(embeddingModelName)
                    .timeout(Duration.ofSeconds(120))
                    .build();
        }
        log.info("使用本地 Embedding 模型: AllMiniLmL6V2");
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        if("milvus".equalsIgnoreCase(vectorStoreType)){
            log.info("使用 Milvus 向量库: {}:{},collection={},dimension={}",
                    milvusHost,milvusPort,milvusCollectionName,milvusDimension);

            return MilvusEmbeddingStore.builder()
                    .host(milvusHost)
                    .port(milvusPort)
                    .collectionName(milvusCollectionName)
                    .dimension(milvusDimension)
                    .consistencyLevel(ConsistencyLevelEnum.BOUNDED)
                    .autoFlushOnInsert(false)
                    .build();
        }

        log.info("使用内存向量库 InMemoryEmbeddingStore");
        return new InMemoryEmbeddingStore<>();
    }
}
