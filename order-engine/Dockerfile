# Stage 1 — build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY proto proto
COPY order-engine/pom.xml order-engine/pom.xml
WORKDIR /app/order-engine
RUN mvn dependency:go-offline -q
COPY order-engine/src src
RUN mvn package -DskipTests -q

# Stage 2 — runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/order-engine/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
