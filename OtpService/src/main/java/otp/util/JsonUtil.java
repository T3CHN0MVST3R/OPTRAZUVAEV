package otp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;

public class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Парсит JSON-тело запроса в объект указанного класса
    public static <T> T fromJson(InputStream is, Class<T> clazz) throws IOException {
        return MAPPER.readValue(is, clazz);
    }

    // Сериализует объект в JSON-строку
    public static String toJson(Object obj) throws IOException {
        return MAPPER.writeValueAsString(obj);
    }
}
