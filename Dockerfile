FROM node:22-alpine AS frontend-build

WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-21-alpine AS backend-build

WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src/ src/
COPY --from=frontend-build /workspace/frontend/dist/ src/main/resources/static/
RUN mvn -B package

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app
COPY --from=backend-build --chown=10001:10001 /workspace/target/vehicle-assistant-0.0.1-SNAPSHOT.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60.0", "-jar", "/app/app.jar"]
