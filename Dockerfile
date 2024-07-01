# FROM openjdk:17-slim
# EXPOSE 8080
# ARG JAR_FILE
# COPY ${JAR_FILE} app.jar
# LABEL authors="jks83"
# ENTRYPOINT ["java", "-jar", "/app.jar"]

FROM gradle:jdk17-alpine as builder
WORKDIR /workspace/app
COPY . /workspace/app/
RUN ls -la /workspace/app
RUN ./gradlew build -p ${MODULE}

FROM openjdk:17-ea-17-slim
RUN groupadd -r appuser && useradd -r -g appuser appuser
WORKDIR /app
COPY --from=builder /workspace/app/${MODULE}/build/libs/${MODULE}.jar ./${MODULE}.jar
EXPOSE 8080
USER appuser
ENTRYPOINT ["java","-jar","${MODULE}.jar"]