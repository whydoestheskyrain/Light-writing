package com.light.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Long 类型统一按字符串序列化。
 *
 * 原因：主键使用 MyBatis-Plus 雪花算法生成的 19 位 Long（如 2110328914396680194），
 * 超出 JavaScript Number.MAX_SAFE_INTEGER（2^53-1，约 16 位）。
 * 如果以 JSON 数字返回，前端解析会丢失末位精度，再拿失真的 id 查询
 * 会导致详情 404、状态更新/文件下载指向错误记录等一系列问题。
 *
 * 入参方向不受影响：Spring 能把请求中的字符串数字自动绑定到 Long。
 */
@AutoConfiguration
public class JacksonLongToStringConfig {

    @Bean
    public JsonMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule("long-to-string");
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            builder.addModule(module);
        };
    }
}
