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
the configuration for openAI or any other platform that provides a openAI like API.

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

To embed text, this project uses the `EmbeddingModel` interface from Spring AI.
By default, it uses the `TransformersEmbeddingModel`
[(documentation)](https://docs.spring.io/spring-ai/reference/api/embeddings/onnx.html) implementation,
which requires a model URI and a tokenizer URI.
You can configure these properties in your `application.properties` file.

```properties
spring.ai.embedding.transformer.onnx.modelUri=https://example.com/model.onnx
spring.ai.embedding.transformer.tokenizer.uri=https://example.com/tokenizer.json
```

Spring AI has a default model URI and tokenizer URI that you can use for testing purposes.
The model is downloaded at startup and cached for subsequent runs.
