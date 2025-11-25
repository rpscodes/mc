FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build/

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Build the application (this creates the runner JAR)
RUN mvn clean package -DskipTests

# Runtime stage
FROM registry.access.redhat.com/ubi8/openjdk-17:1.18

# Switch to root to create user
USER root

WORKDIR /work/

# Copy the built application from build stage
COPY --from=build /build/target/*-runner.jar app.jar

# Create a non-root user (use UID 1001 which is OpenShift-friendly)
RUN groupadd -r -g 1001 quarkus && \
    useradd -r -u 1001 -g quarkus quarkus && \
    chown -R quarkus:quarkus /work

# Switch back to non-root user
USER quarkus

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

