package otp.main;

import com.sun.net.httpserver.HttpServer;
import otp.api.Dispatcher;
import otp.dao.impl.OtpCodeDaoImpl;
import otp.dao.impl.OtpConfigDaoImpl;
import otp.dao.impl.UserDaoImpl;
import otp.service.OtpExpirationScheduler;
import otp.service.OtpService;
import otp.service.notification.NotificationServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

/**
 * Точка входа приложения. Поднимает HTTP-сервер на порту из application.properties
 * и регистрирует все маршруты через Dispatcher.
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        try {
            logger.info("Запуск приложения OTP-сервиса denzomaster...");

            // Загружаем конфигурацию
            Properties config = new Properties();
            try (InputStream is = Application.class.getClassLoader()
                    .getResourceAsStream("application.properties")) {
                if (is != null) {
                    config.load(is);
                }
            }
            int port = Integer.parseInt(config.getProperty("server.port", "8080"));

            // Инициализируем БД
            initDatabase();

            // Запускаем планировщик для проверки просроченных OTP
            startExpirationScheduler();

            // Создаём HTTP-сервер
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

            // Регистрируем маршруты
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.registerRoutes(server);

            // Запускаем сервер
            server.start();

            logger.info("Сервер запущен на http://localhost:{}", port);
            System.out.println("Сервер запущен на http://localhost:" + port);
        } catch (IOException e) {
            logger.error("Не удалось запустить сервер", e);
            System.err.println("Не удалось запустить сервер: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Инициализирует базу данных начальными данными
     */
    private static void initDatabase() {
        try {
            logger.info("Инициализация конфигурации OTP по умолчанию");
            OtpConfigDaoImpl configDao = new OtpConfigDaoImpl();
            configDao.initDefaultConfigIfEmpty();
            logger.info("Инициализация БД завершена успешно");
        } catch (Exception e) {
            logger.error("Ошибка инициализации БД", e);
            throw e; // повторно выбрасываем для остановки приложения
        }
    }

    /**
     * Запускает планировщик проверки просроченных OTP-кодов
     */
    private static void startExpirationScheduler() {
        OtpService otpService = new OtpService(
            new OtpCodeDaoImpl(),
            new OtpConfigDaoImpl(),
            new UserDaoImpl(),
            new NotificationServiceFactory()
        );

        // Проверка каждые 5 минут
        OtpExpirationScheduler scheduler = new OtpExpirationScheduler(otpService, 5);
        scheduler.start();

        logger.info("Планировщик проверки просроченных OTP-кодов запущен");

        // Добавляем hook для остановки планировщика при завершении приложения
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Остановка планировщика...");
            scheduler.stop();
        }));
    }
}
