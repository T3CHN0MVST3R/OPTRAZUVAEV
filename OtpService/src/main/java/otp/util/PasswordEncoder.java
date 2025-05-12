package otp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Утиль для хеширования паролей и проверки совпадения raw-пароля с сохранённым хешем.
 */
public final class PasswordEncoder {
    private static final Logger logger = LoggerFactory.getLogger(PasswordEncoder.class);

    // Закрытый конструктор, чтобы нельзя было создать экземпляр
    private PasswordEncoder() {}

    /**
     * Хеширует переданный пароль алгоритмом SHA-256 и возвращает HEX-представление.
     *
     * @param rawPassword исходный пароль
     * @return hex-строка хеша
     */
    public static String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Не удалось получить алгоритм SHA-256 для хеширования пароля", e);
            throw new IllegalStateException("Алгоритм SHA-256 недоступен", e);
        }
    }

    /**
     * Проверяет, что rawPassword после хеширования совпадает с уже сохранённым хешем.
     *
     * @param rawPassword  сырой пароль
     * @param storedHash   сохранённый хеш (hex-строка)
     * @return true, если совпадают
     */
    public static boolean matches(String rawPassword, String storedHash) {
        if (storedHash == null || rawPassword == null) {
            return false;
        }
        return hash(rawPassword).equalsIgnoreCase(storedHash);
    }

    // Помощник: перевод массива байт в hex
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
