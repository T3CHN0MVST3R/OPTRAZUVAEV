package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.dao.impl.UserDaoImpl;
import otp.model.UserRole;
import otp.service.UserService;
import otp.util.JsonUtil;
import otp.util.HttpUtils;
import otp.util.JwtUtils;

import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Контроллер аутентификации и регистрации пользователей.
 * Обрабатывает публичные запросы:
 * <ul>
 *   <li>POST /register — регистрация нового пользователя (username, password, role)</li>
 *   <li>POST /login    — аутентификация и выдача токена (username, password)</li>
 * </ul>
 */
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserService userService = new UserService(new UserDaoImpl());

    /**
     * Обрабатывает HTTP POST запрос на регистрацию пользователя.
     * Проверяет метод, Content-Type и формат JSON, затем вызывает UserService.register().
     * Возвращает:
     * <ul>
     *   <li>201 Created — при успешной регистрации</li>
     *   <li>409 Conflict — если имя занято или администратор уже существует</li>
     *   <li>415 Unsupported Media Type — если Content-Type некорректен</li>
     *   <li>405 Method Not Allowed — если метод не POST</li>
     *   <li>500 Internal Server Error — при других ошибках</li>
     * </ul>
     *
     * @param exchange объект HttpExchange для текущего запроса
     * @throws IOException при ошибках чтения/записи
     */
    public void handleRegister(HttpExchange exchange) throws IOException {
        logger.info("Получен запрос на регистрацию: {}", exchange.getRequestURI());

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Метод не разрешен: {} для регистрации", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Метод не разрешен");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            logger.warn("Неподдерживаемый Content-Type: {} для регистрации", contentType);
            HttpUtils.sendError(exchange, 415, "Content-Type должен быть application/json");
            return;
        }

        try {
            RegisterRequest req = JsonUtil.fromJson(exchange.getRequestBody(), RegisterRequest.class);
            logger.info("Обработка регистрации для логина: {}, роль: {}", req.username, req.role);

            // Проверка, не существует ли уже администратор
            if ("ADMIN".equals(req.role) && userService.adminExists()) {
                logger.warn("Регистрация отклонена: Администратор уже существует");
                HttpUtils.sendError(exchange, 409, "Администратор уже существует");
                return;
            }

            userService.register(req.username, req.password, UserRole.valueOf(req.role));
            logger.info("Пользователь успешно зарегистрирован: {}", req.username);
            HttpUtils.sendEmptyResponse(exchange, 201);
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Ошибка регистрации: {}", e.getMessage());
            HttpUtils.sendError(exchange, 409, e.getMessage());
        } catch (Exception e) {
            logger.error("Внутренняя ошибка сервера при регистрации", e);
            HttpUtils.sendError(exchange, 500, "Внутренняя ошибка сервера");
        }
    }

    /**
     * Обрабатывает HTTP POST запрос на аутентификацию пользователя.
     * Проверяет метод, Content-Type и формат JSON, затем вызывает UserService.login().
     * Возвращает:
     * <ul>
     *   <li>200 OK — возвращает JSON {"token":"..."}</li>
     *   <li>401 Unauthorized — если логин или пароль неверны</li>
     *   <li>415 Unsupported Media Type — если Content-Type некорректен</li>
     *   <li>405 Method Not Allowed — если метод не POST</li>
     *   <li>500 Internal Server Error — при других ошибках</li>
     * </ul>
     *
     * @param exchange объект HttpExchange для текущего запроса
     * @throws IOException при ошибках чтения/записи
     */
    public void handleLogin(HttpExchange exchange) throws IOException {
        logger.info("Получен запрос на вход: {}", exchange.getRequestURI());

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Метод не разрешен: {} для входа", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Метод не разрешен");
            return;
        }

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.contains("application/json")) {
            logger.warn("Неподдерживаемый Content-Type: {} для входа", contentType);
            HttpUtils.sendError(exchange, 415, "Content-Type должен быть application/json");
            return;
        }

        try {
            LoginRequest req = JsonUtil.fromJson(exchange.getRequestBody(), LoginRequest.class);
            logger.info("Обработка входа для логина: {}", req.username);

            String token = userService.login(req.username, req.password);
            if (token == null) {
                logger.warn("Вход не выполнен для логина: {}", req.username);
                HttpUtils.sendError(exchange, 401, "Неавторизован");
                return;
            }

            logger.info("Вход успешно выполнен для логина: {}", req.username);
            String json = JsonUtil.toJson(Map.of("token", token));
            HttpUtils.sendJsonResponse(exchange, 200, json);
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка входа: {}", e.getMessage());
            HttpUtils.sendError(exchange, 401, e.getMessage());
        } catch (Exception e) {
            logger.error("Внутренняя ошибка сервера при входе", e);
            HttpUtils.sendError(exchange, 500, "Внутренняя ошибка сервера");
        }
    }

    /**
     * DTO для разбора JSON тела запроса регистрации.
     */
    private static class RegisterRequest {
        public String username;
        public String password;
        public String role;
    }

    /**
     * DTO для разбора JSON тела запроса логина.
     */
    private static class LoginRequest {
        public String username;
        public String password;
    }
}
