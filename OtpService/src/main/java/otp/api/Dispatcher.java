package otp.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import otp.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatcher отвечает за регистрацию HTTP-контекстов (маршрутов) и их привязку к методам контроллеров.
 * <p>
 * Список маршрутов:
 * <ul>
 *   <li>POST   /register           → AuthController.handleRegister()  (публичный)</li>
 *   <li>POST   /login              → AuthController.handleLogin()     (публичный)</li>
 *   <li>POST   /otp/generate       → UserController.generateOtp()     (роль USER)</li>
 *   <li>POST   /otp/validate       → UserController.validateOtp()     (роль USER)</li>
 *   <li>PATCH  /admin/config       → AdminController.updateOtpConfig() (роль ADMIN)</li>
 *   <li>GET    /admin/users        → AdminController.listUsers()       (роль ADMIN)</li>
 *   <li>DELETE /admin/users/{id}   → AdminController.deleteUser()      (роль ADMIN)</li>
 * </ul>
 * </p>
 */
public class Dispatcher {
    private static final Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private final AuthController authController = new AuthController();
    private final UserController userController = new UserController();
    private final AdminController adminController = new AdminController();

    /**
     * Регистрация всех маршрутов и подключение фильтров аутентификации.
     *
     * @param server экземпляр HttpServer
     */
    public void registerRoutes(HttpServer server) {
        logger.info("Регистрация маршрутов приложения");

        // Публичные маршруты
        HttpContext registerCtx = server.createContext("/register", authController::handleRegister);
        logger.info("Зарегистрирован публичный маршрут: POST /register");

        HttpContext loginCtx = server.createContext("/login", authController::handleLogin);
        logger.info("Зарегистрирован публичный маршрут: POST /login");

        // Маршруты для пользователей (роль USER)
        HttpContext genCtx = server.createContext("/otp/generate", userController::generateOtp);
        genCtx.getFilters().add(new AuthFilter(UserRole.USER));
        logger.info("Зарегистрирован защищенный маршрут: POST /otp/generate (роль: USER)");

        HttpContext valCtx = server.createContext("/otp/validate", userController::validateOtp);
        valCtx.getFilters().add(new AuthFilter(UserRole.USER));
        logger.info("Зарегистрирован защищенный маршрут: POST /otp/validate (роль: USER)");

        // Маршруты для администратора (роль ADMIN)
        HttpContext configCtx = server.createContext("/admin/config", adminController::updateOtpConfig);
        configCtx.getFilters().add(new AuthFilter(UserRole.ADMIN));
        logger.info("Зарегистрирован защищенный маршрут: PATCH /admin/config (роль: ADMIN)");

        HttpContext usersCtx = server.createContext("/admin/users", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            // Проверяем путь - если это /admin/users/{id}, вызываем deleteUser
            if (path.matches("/admin/users/\\d+") && "DELETE".equalsIgnoreCase(method)) {
                adminController.deleteUser(exchange);
            }
            // Если просто /admin/users и метод GET, вызываем listUsers
            else if ("/admin/users".equals(path) && "GET".equalsIgnoreCase(method)) {
                adminController.listUsers(exchange);
            }
            else {
                logger.warn("Метод не разрешен: {} для /admin/users", method);
                exchange.sendResponseHeaders(405, -1);
            }
        });
        usersCtx.getFilters().add(new AuthFilter(UserRole.ADMIN));
        logger.info("Зарегистрированы защищенные маршруты: GET/DELETE /admin/users (роль: ADMIN)");

        logger.info("Все маршруты успешно зарегистрированы");
    }
}
