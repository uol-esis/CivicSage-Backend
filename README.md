# CivicSage

This is the backend for the CivicSage project.

## Building the project

This project is written in Java 21 and uses Maven as its build tool.
To build the project, you need to have Java 21 installed.
Clone this repository and run the following command in the root directory of the project:

```bash
./mvnw clean install package
```

This will build the project.
To run the project, run the following command:

```bash
java -jar target/CivicSage-0.0.1-SNAPSHOT.jar
```

## Running the project

In order to use the application you need to provide a configuration to access a LLM. Our recommended approach during
development is to use [Docker Model Runner](https://docs.docker.com/ai/model-runner/) (DMR). But you may also provide
the configuration for openAI or any other platform that provides a openAI like API. The default model to use is
currently `ai/mxbai-embed-large`. Please provide it via `docker model pull ai/mxbai-embed-large`.

## OpenAPI

This project uses OpenAPI to document the API and generate the server code.
The OpenAPI Document is located [here](https://github.com/uol-esis/CivicSage-OpenAPI).
You can read up on OpenAPI and the specification [here](https://spec.openapis.org/oas/v3.0.3) and use the
[Reference Guide](https://swagger.io/docs/specification/v3_0/about/) by Swagger.

To generate the server code we are using the OpenAPI Generator via the Maven plugin.
You can find the documentation for the Java generator [here](https://openapi-generator.tech/docs/generators/java/).

If you want to run the project for the first time or have made changes to the OpenAPI specification, you need to
generate the server code. You can do this by running the following command:

```bash
./mvnw clean compile
```

After generating the server code, you can run the project as described above.
While the project is running, you can access the Swagger UI at http://localhost:8080/swagger-ui/index.html

> **Note:** If you are using an IDE, you might have to set `target/generated-sources/openapi/src/main/java` as a
> generated sources root to avoid compilation errors.

## Configuring the EmbeddingModel

Prior versions did host the embedding model within this application. For better scaling the LLM has been separated into
a separate source. Setting up the model is not scope of this project. For development, we recommend the usage of Docker
Model Runner. For production, a separate service should be used, self-hosted or from a cloud provider. Configuring the
model is done using the following properties:

```properties
spring.ai.openai.embedding.options.model=ai/mxbai-embed-large
spring.ai.openai.base-url=http://localhost:12434/engines
spring.ai.openai.api-key=test
```

## Configuring the VectorStore

You can switch between different vector stores.
The default is the MariaDB VectorStore, which is configured via the following properties:

```properties
civicsage.ai.vectorstore.type=mariadb
civicsage.ai.vectorstore.initialize-schema=false
civicsage.ai.vectorstore.remove-existing-vector-store-table=false
civicsage.ai.vectorstore.schema-name=null
civicsage.ai.vectorstore.schema-validation=false
civicsage.ai.vectorstore.table-name-prefix=vector_store_
civicsage.ai.vectorstore.distance=cosine
```

You can also use the `postgresql` vector store by setting the type to `postgresql`:

```properties
civicsage.ai.vectorstore.type=postgresql
```

However, we are not currently testing this vector store, so it may not work as expected.
Spring AI does not support vectors with more than 2000 dimensions with PostgreSQL.

## Upload limits

Upload limits can be configured via the `application.properties` file. The default values are:

```properties
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
server.tomcat.max-part-count=10
server.tomcat.max-part-header-size=512B
```
