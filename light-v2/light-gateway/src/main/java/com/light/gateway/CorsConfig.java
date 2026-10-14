package com.light.gateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
@Configuration
public class CorsConfig {
 @Bean CorsWebFilter corsWebFilter(@Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}") String origins){CorsConfiguration c=new CorsConfiguration();c.setAllowedOriginPatterns(java.util.List.of(origins.split(",")));c.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(java.util.List.of("*"));c.setAllowCredentials(true);UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/**",c);return new CorsWebFilter(s);}
}
