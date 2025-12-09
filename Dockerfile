FROM gcr.io/distroless/java17-debian12:nonroot
WORKDIR /app
# Copy your locally built jar
COPY target/*.jar /app/app.jar

EXPOSE 8080
USER nonroot
ENTRYPOINT ["java","-jar","/app/app.jar"]
