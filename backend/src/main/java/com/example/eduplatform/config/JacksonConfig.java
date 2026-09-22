package com.example.eduplatform.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * 统一日期时间的 JSON 格式：输出 {@code yyyy-MM-dd HH:mm:ss}，
 * 输入通过 {@link LenientLocalDateTimeDeserializer} 兼容带秒与不带秒两种写法。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter OUT_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter OUT_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer dateTimeCustomizer() {
        return builder -> {
            builder.serializers(new LocalDateTimeSerializer(OUT_DATETIME));
            builder.serializers(new LocalDateSerializer(OUT_DATE));
            builder.deserializers(new LenientLocalDateTimeDeserializer());
        };
    }
}
