package otp.api;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import otp.model.User;
import otp.model.UserRole;
import otp.util.HttpUtils;
import otp.util.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Фильтр аутентификации и авторизации для HTTP-контроллеров.
 * <p>
 * Проверяет наличие заголовка Authorization: Bearer &lt;token&gt;,
 * валидирует JWT-токен и проверяет требуемую роль.
 * Если проверка проходит, сохраняет объект User в
 * exchange.setAttribute("user", user) и передаёт управление дальше.
 * Иначе возвращает соответствующий HTTP-статус:
 * <ul>
 *   <li>401 Unauthorized — при отсутствии или недействительном токене</li>
 *   <li>403 Forbidden — при недостаточности прав</li>
 * </ul>
 * </p>
 */
public class AuthFilter extends Filter {
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);
    private final UserRole requiredRole;

    /**
     * @param requiredRole минимальная роль пользователя для доступа к ресурсу
     */
    public AuthFilter(UserRole requiredRole) {
        this.requiredRole = requiredRole;
    }

    @Override
    public String description() {
        return "Фильтр аутентификации и проверки роли (ROLE >= " + requiredRole + ")";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        logger.info("Аутентификация запроса: {} {}", method, path);

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Ошибка аутентификации: Отсутствует или неверный заголовок Authorization для {} {}", method, path);
            HttpUtils.sendError(exchange, 401, "Отсутствует или неверный заголовок Authorization");
            return;
        }

        String token = authHeader.substring(7);
        // Получаем пользователя по токену
        User user = JwtUtils.validateTokenAndGetUser(token);
        if (user == null) {
            logger.warn("Ошибка аутентификации: Недействительный или просроченный токен для {} {}", method, path);
            HttpUtils.sendError(exchange, 401, "Недействительный или просроченный токен");
            return;
        }

        if (user.getRole().ordinal() < requiredRole.ordinal()) {
            logger.warn("Ошибка авторизации: Пользователь {} с ролью {} попытался получить доступ к {} {} (требуется роль {})",
                    user.getUsername(), user.getRole(), method, path, requiredRole);
            HttpUtils.sendError(exchange, 403, "Доступ запрещен");
            return;
        }

        logger.info("Пользователь {} с ролью {} успешно аутентифицирован для {} {}",
                user.getUsername(), user.getRole(), method, path);
        exchange.setAttribute("user", user);
        chain.doFilter(exchange);
    }
}
