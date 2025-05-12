package otp.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Реализация NotificationService для сохранения OTP-кодов в файл.
 * Путь к файлу передаётся в параметре recipient при вызове sendCode().
 */
public class FileNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(FileNotificationService.class);
    // Формат временной метки
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Сохраняет OTP-код в файл.
     *
     * @param recipientPath путь к файлу, где нужно сохранить код
     * @param code          OTP-код
     */
    @Override
    public void sendCode(String recipientPath, String code) {
        // Если путь не указан, используем дефолтный в корне проекта
        if (recipientPath == null || recipientPath.isBlank()) {
            recipientPath = "otp_codes.txt";
        }

        Path path = Paths.get(recipientPath);
        String entry = String.format("%s - OTP: %s%n",
                LocalDateTime.now().format(TIMESTAMP_FORMAT),
                code);
        try {
            // Убедимся, что директория существует
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            // Запишем код в файл (создаём, если нужно, и дописываем в конец)
            Files.write(path, entry.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            logger.info("OTP-код записан в файл {}", recipientPath);
        } catch (IOException e) {
            logger.error("Не удалось записать OTP в файл {}", recipientPath, e);
            throw new RuntimeException("Ошибка записи в файл", e);
        }
    }
}
