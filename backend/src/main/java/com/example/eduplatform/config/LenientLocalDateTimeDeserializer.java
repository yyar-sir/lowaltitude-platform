package com.example.eduplatform.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 宽容的 {@link LocalDateTime} 反序列化器。
 *
 * <p>为什么需要它：{@code spring.jackson.date-format} 只作用于 {@code java.util.Date}，
 * 对 {@code LocalDateTime} 无效；而前后端流传的时间字符串有「带秒」与「不带秒」两种
 * （Element Plus 的 datetime 选择器给带秒值，部分手写表单给不带秒值）。
 * 只注册一种格式时，另一种会抛 {@code DateTimeParseException} 并被全局兜底成
 * 「系统繁忙」，现象很像服务故障，实际只是格式没配上。
 *
 * <p>⚠️ 必须是<b>具名的具体类</b>并显式声明泛型参数：匿名内部类会让 Jackson
 * 读不到 handledType（报 {@code Unknown handled type}），应用启动即失败。
 */
public class LenientLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter[] FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    /**
     * 显式声明处理类型。
     *
     * <p>Spring 的 {@code Jackson2ObjectMapperBuilder.deserializers()} 在注册时会读取
     * 该类型；某些编译/代理场景下光靠泛型签名读不到，会抛
     * {@code Unknown handled type} 导致应用启动失败，因此这里明确重写。
     */
    @Override
    public Class<?> handledType() {
        return LocalDateTime.class;
    }

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        String value = text.trim();
        for (DateTimeFormatter f : FORMATS) {
            try {
                return LocalDateTime.parse(value, f);
            } catch (DateTimeParseException ignored) {
                // 换下一种格式继续尝试
            }
        }
        throw new IOException("时间格式无法解析：" + value + "（支持 yyyy-MM-dd HH:mm[:ss]）");
    }
}
