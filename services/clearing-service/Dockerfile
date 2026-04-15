# Stage 1: Build the application safely inside the container
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the lightweight runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the fresh JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port you defined in application.yml
EXPOSE 8083

# IMPORTANT: runs clearing service
ENTRYPOINT ["java", "-jar", "app.jar"]