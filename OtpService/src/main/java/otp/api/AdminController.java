package otp.api;

import com.sun.net.httpserver.HttpExchange;
import otp.dao.impl.OtpCodeDaoImpl;
import otp.dao.impl.OtpConfigDaoImpl;
import otp.dao.impl.UserDaoImpl;
import otp.model.User;
import otp.service.AdminService;
import otp.util.JsonUtil;
import otp.util.HttpUtils;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Контроллер для административных операций (роль ADMIN).
 * <p>
 * Доступные маршруты:
 * <ul>
 *   <li>PATCH  /admin/config     — изменить длину и время жизни OTP-кодов</li>
 *   <li>GET    /admin/users      — получить список всех пользователей без админов</li>
 *   <li>DELETE /admin/users/{id} — удалить пользователя и связанные OTP-коды</li>
 * </ul>
 * </p>
 */
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService = new AdminService(
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new OtpCodeDaoImpl()
    );

    /**
     * Обрабатывает HTTP PATCH запрос на изменение конфигурации OTP.
     * <p>
     * Ожидает JSON: {"length": 6, "ttlSeconds": 300}
     * </p>
     * <ul>
     *   <li>204 No Content — успешно обновлено</li>
     *   <li>400 Bad Request — если параметры некорректны</li>
     *   <li>415 Unsupported Media Type — если Content-Type не application/json</li>
     *   <li>405 Method Not Allowed — если метод не PATCH</li>
     *   <li>500 Internal Server Error — другие ошибки</li>
     * </ul>
     *
     * @param exchange HTTP-контекст текущего запроса
     * @throws IOException при ошибках ввода-вывода
     */
    public void updateOtpConfig(HttpExchange exchange) throws IOException {
        logger.info("Получен запрос на обновление конфигурации OTP: {} {}",
                exchange.getRequestMethod(), exchange.getRequestURI());

        if (!"PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Метод не разрешен: {} для обновления конфигурации", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Метод не разрешен");
            return;
        }

        String ct = exchange.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.contains("application/json")) {
            logger.warn("Неподдерживаемый Content-Type: {} для обновления конфигурации", ct);
            HttpUtils.sendError(exchange, 415, "Content-Type должен быть application/json");
            return;
        }

        try {
            ConfigRequest req = JsonUtil.fromJson(exchange.getRequestBody(), ConfigRequest.class);
            logger.info("Обработка обновления конфигурации OTP: length={}, ttlSeconds={}", req.length, req.ttlSeconds);

            adminService.updateOtpConfig(req.length, req.ttlSeconds);
            logger.info("Конфигурация OTP успешно обновлена");
            HttpUtils.sendEmptyResponse(exchange, 204);
        } catch (IllegalArgumentException e) {
            logger.warn("Ошибка обновления конфигурации OTP: {}", e.getMessage());
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            logger.error("Внутренняя ошибка сервера при обновлении конфигурации OTP", e);
            HttpUtils.sendError(exchange, 500, "Внутренняя ошибка сервера");
        }
    }

    /**
     * Обрабатывает HTTP GET запрос для получения списка пользователей без админов.
     * <ul>
     *   <li>200 OK — возвращает JSON-массив пользователей</li>
     *   <li>405 Method Not Allowed — если метод не GET</li>
     *   <li>500 Internal Server Error — другие ошибки</li>
     * </ul>
     *
     * @param exchange HTTP-контекст текущего запроса
     * @throws IOException при ошибках ввода-вывода
     */
    public void listUsers(HttpExchange exchange) throws IOException {
        logger.info("Получен запрос на список пользователей: {} {}",
                exchange.getRequestMethod(), exchange.getRequestURI());

        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Метод не разрешен: {} для получения списка пользователей", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Метод не разрешен");
            return;
        }

        try {
            List<User> users = adminService.getAllUsersWithoutAdmins();
            logger.info("Получен список пользователей, количество: {}", users.size());

            String json = JsonUtil.toJson(users);
            HttpUtils.sendJsonResponse(exchange, 200, json);
        } catch (Exception e) {
            logger.error("Внутренняя ошибка сервера при получении списка пользователей", e);
            HttpUtils.sendError(exchange, 500, "Внутренняя ошибка сервера");
        }
    }

    /**
     * Обрабатывает HTTP DELETE запрос на удаление пользователя по ID.
     * <ul>
     *   <li>204 No Content — успешно удалено</li>
     *   <li>400 Bad Request — если ID некорректен</li>
     *   <li>404 Not Found — если пользователь не найден</li>
     *   <li>405 Method Not Allowed — если метод не DELETE</li>
     *   <li>500 Internal Server Error — другие ошибки</li>
     * </ul>
     *
     * @param exchange HTTP-контекст текущего запроса
     * @throws IOException при ошибках ввода-вывода
     */
    public void deleteUser(HttpExchange exchange) throws IOException {
        logger.info("Получен запрос на удаление пользователя: {} {}",
                exchange.getRequestMethod(), exchange.getRequestURI());

        if (!"DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            logger.warn("Метод не разрешен: {} для удаления пользователя", exchange.getRequestMethod());
            HttpUtils.sendError(exchange, 405, "Метод не разрешен");
            return;
        }

        try {
            URI uri = exchange.getRequestURI();
            String[] segments = uri.getPath().split("/");
            Long id = Long.valueOf(segments[segments.length - 1]);

            logger.info("Обработка удаления пользователя с ID: {}", id);
            adminService.deleteUserAndCodes(id);

            logger.info("Пользователь успешно удален: ID={}", id);
            HttpUtils.sendEmptyResponse(exchange, 204);
        } catch (NumberFormatException e) {
            logger.warn("Неверный ID пользователя для удаления");
            HttpUtils.sendError(exchange, 400, "Неверный ID пользователя");
        } catch (IllegalArgumentException e) {
            logger.warn("Пользователь не найден: {}", e.getMessage());
            HttpUtils.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            logger.error("Внутренняя ошибка сервера при удалении пользователя", e);
            HttpUtils.sendError(exchange, 500, "Внутренняя ошибка сервера");
        }
    }

    /**
     * DTO для разбора JSON тела PATCH запроса /admin/config.
     */
    private static class ConfigRequest {
        public int length;
        public int ttlSeconds;
    }
}
