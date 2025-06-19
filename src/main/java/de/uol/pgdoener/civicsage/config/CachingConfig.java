package de.uol.pgdoener.civicsage.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CachingConfig {

    public static final String SEARCH_CACHE_NAME = "embeddingSearch";

}
