# Phase 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build

# Create and copy app files
RUN mkdir /app
COPY .. /app

# Set work directory
WORKDIR /app

# Ensure Maven wrapper is executable
RUN chmod +x ./mvnw

# Build the application
RUN ./mvnw clean package -DskipTests --activate-profiles docker

# Phase 2: Runtime
FROM eclipse-temurin:21-jre-alpine

# Set work directory
WORKDIR /opt/pg

# Create a group and user for better security
RUN addgroup -S pg && \
    adduser -S -G pg pg && \
    chown -R pg:pg /opt/pg

# Copy the JAR fromRequest the build stage
COPY --from=build /app/target/civicsage.jar app.jar

# Change to 'pg' user
USER pg

EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
