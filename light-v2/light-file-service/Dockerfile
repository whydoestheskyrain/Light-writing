# 运行阶段基础镜像：只带 JRE（不含 JDK），体积更小
# 必须是 25，因为本模块 jar 由 Java 25 编译，用低版本 JRE 会报 Unsupported class file major version
FROM eclipse-temurin:25-jre

WORKDIR /app

# 拷贝本模块已构建的可执行 fat jar
# target/*.jar 只会匹配 fat jar；spring-boot-maven-plugin 生成的 *.jar.original 不会被匹配
COPY target/*.jar app.jar

# 预留上传文件落盘目录，便于 compose 挂载数据卷
RUN mkdir -p /app/data/uploads

EXPOSE 8103

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
