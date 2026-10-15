# syntax=docker/dockerfile:1
# ============ 构建阶段：容器内用 Maven 编译，宿主机无需预装 Maven/JDK ============
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build
COPY . .
# -s 指定阿里云镜像 settings，避免容器内直连 Maven Central 握手中断
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -s /build/docker/maven-settings.xml -pl light-knowledge-service -am package -DskipTests

# ============ 运行阶段：只带 JRE，镜像更小 ============
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /build/light-knowledge-service/target/*.jar app.jar
EXPOSE 8102
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
