package de.uol.pgdoener.civicsage.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

/**
 * This post processor handles table names for vector stores.
 * <p>
 * If the {@code spring.ai.vectorstore.pgvector.table-name} is already set, this does nothing.
 * <p>
 * If the property is not set, it sets it to {@code vector_store_${spring.ai.openai.embedding.options.model}} where
 * slashes and dashes in the model name are replaced with underscores.
 */
public class VectorStoreTableNameEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String TABLE_NAME_PROPERTY_NAME = "spring.ai.vectorstore.pgvector.table-name";
    private static final String MODEL_PROPERTY_NAME = "spring.ai.openai.embedding.options.model";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.containsProperty(TABLE_NAME_PROPERTY_NAME)) {
            return; // Property has been overwritten externally
        }

        Object o = environment.getProperty(MODEL_PROPERTY_NAME);
        if (o instanceof String model) {
            // build table name from model name
            String tableName = "vector_store_" + model.replace("/", "_");
            tableName = tableName.replace("-", "_");

            // create new property map
            Map<String, Object> override = new HashMap<>();
            override.put(TABLE_NAME_PROPERTY_NAME, tableName);

            // add as new property source
            environment.getPropertySources().addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    new MapPropertySource("table-name-override-with-model-name", override));
        }
    }

}