package de.uol.pgdoener.civicsage.config;

import de.uol.pgdoener.civicsage.business.completion.DocumentAdvisor;
import de.uol.pgdoener.civicsage.business.embedding.embedding.VectorStoreExtension;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ChatConfig {

    private final ChatModel chatModel;
    private final VectorStoreExtension vectorStoreExtension;

    @Bean
    public ChatClient documentChatClient() {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().build(),
                        DocumentAdvisor.builder().vectorStoreExtension(vectorStoreExtension).build()
                )
                .build();
    }

}
