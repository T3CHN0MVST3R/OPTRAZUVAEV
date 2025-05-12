package otp.util;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpUtils {

    // Отправить JSON-ответ с указанным статусом
    public static void sendJsonResponse(HttpExchange exch, int status, String json) throws IOException {
        exch.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exch.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exch.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Отправить ответ без тела (например, 204)
    public static void sendEmptyResponse(HttpExchange exch, int status) throws IOException {
        exch.sendResponseHeaders(status, -1);
    }

    // Отправить JSON-ошибку с сообщением
    public static void sendError(HttpExchange exch, int status, String message) throws IOException {
        String errorJson = String.format("{\"error\":\"%s\"}", message);
        sendJsonResponse(exch, status, errorJson);
    }
}
