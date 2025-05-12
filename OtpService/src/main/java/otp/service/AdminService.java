package otp.service;

import otp.dao.OtpConfigDao;
import otp.dao.OtpCodeDao;
import otp.dao.UserDao;
import otp.model.OtpConfig;
import otp.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminService {
    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private final OtpConfigDao configDao;
    private final UserDao userDao;
    private final OtpCodeDao codeDao;

    public AdminService(OtpConfigDao configDao, UserDao userDao, OtpCodeDao codeDao) {
        this.configDao = configDao;
        this.userDao = userDao;
        this.codeDao = codeDao;
    }

    /**
     * Обновляет конфигурацию OTP-кодов с новыми значениями длины и времени жизни
     *
     * @param length     новая длина кода
     * @param ttlSeconds новое время жизни в секундах
     * @throws IllegalArgumentException если параметры некорректны
     */
    public void updateOtpConfig(int length, int ttlSeconds) {
        // Валидация параметров
        if (length < 4 || length > 10) {
            logger.warn("Некорректная длина OTP-кода: {}", length);
            throw new IllegalArgumentException("Длина OTP-кода должна быть от 4 до 10 символов");
        }

        if (ttlSeconds < 30 || ttlSeconds > 3600) {
            logger.warn("Некорректное время жизни OTP-кода: {}", ttlSeconds);
            throw new IllegalArgumentException("Время жизни OTP-кода должно быть от 30 секунд до 1 часа");
        }

        // Получаем текущую конфигурацию
        OtpConfig config = configDao.getConfig();
        if (config == null) {
            config = new OtpConfig(1L, length, ttlSeconds);
            logger.info("Конфигурация OTP не найдена, инициализируем новую");
            configDao.initDefaultConfigIfEmpty();
            config = configDao.getConfig(); // Получаем снова после инициализации
        }

        // Обновляем значения
        config.setLength(length);
        config.setTtlSeconds(ttlSeconds);
        configDao.updateConfig(config);

        logger.info("Конфигурация OTP обновлена: длина={}, ttlSeconds={}", length, ttlSeconds);
    }

    /**
     * Возвращает список всех пользователей без администраторов
     */
    public List<User> getAllUsersWithoutAdmins() {
        List<User> users = userDao.findAllUsersWithoutAdmins();
        logger.info("Получен список пользователей без администраторов, количество: {}", users.size());
        return users;
    }

    /**
     * Удаляет пользователя и связанные с ним OTP-коды
     *
     * @param userId ID пользователя для удаления
     * @throws IllegalArgumentException если пользователь не найден
     */
    public void deleteUserAndCodes(Long userId) {
        User user = userDao.findById(userId);
        if (user == null) {
            logger.warn("Пользователь с ID {} не найден для удаления", userId);
            throw new IllegalArgumentException("Пользователь не найден");
        }

        // Сначала удаляем коды
        codeDao.deleteAllByUserId(userId);
        // Затем удаляем пользователя
        userDao.delete(userId);

        logger.info("Удален пользователь {} и его OTP-коды", userId);
    }
}
