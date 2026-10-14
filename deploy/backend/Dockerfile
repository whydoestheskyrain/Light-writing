FROM openjdk:8-jre-slim

WORKDIR /app

COPY knowledge-service.jar /app/knowledge-service.jar

RUN mkdir -p /app/uploads

EXPOSE 9001

ENTRYPOINT ["java", "-jar", "/app/knowledge-service.jar"]
