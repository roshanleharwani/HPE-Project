FROM openjdk:17

WORKDIR /app

COPY target/clearing-service.jar app.jar

# IMPORTANT: runs clearing service
ENTRYPOINT ["java", "-jar", "app.jar"]
