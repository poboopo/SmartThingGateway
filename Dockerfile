# Build
FROM maven:3.8.3-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -T 1C

# Run
FROM openjdk:17.0.1-jdk-slim
WORKDIR /app
COPY --from=build /app/smart-thing-gateway-app/target/st-gateway.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
