FROM openjdk:17-jdk-slim

# Create a volume for temporary files
VOLUME /tmp

# Build-time argument for the jar file location
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar

# Expose port 8080 (for documentation)
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "/app.jar"]
