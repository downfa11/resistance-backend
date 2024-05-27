FROM gradle:jdk17-alpine as builder
WORKDIR /workspace/app
COPY . /workspace/app/
RUN chmod +x gradlew
RUN ./gradlew build -p ${MODULE}
FROM openjdk:17-ea-17-slim

RUN groupadd -r appuser && useradd -r -g appuser appuser

COPY --from=builder /workspace/app/${MODULE}/build/libs/${MODULE}.jar ./${MODULE}.jar
EXPOSE 8080

USER appuser

ENTRYPOINT ["java","-jar","${MODULE}.jar"]