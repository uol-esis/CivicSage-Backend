package de.uol.pgdoener.civicsage.test.support;

import org.testcontainers.containers.MariaDBContainer;

public class MariaDBContainerFactory {

    public static MariaDBContainer<?> create() {
        return new MariaDBContainer<>("mariadb:11.8.2-ubi9")
                .withDatabaseName("test")
                .withUsername("test")
                .withPassword("test");
    }

}
