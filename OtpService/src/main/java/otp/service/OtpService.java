package otp.service;

import otp.dao.OtpCodeDao;
import otp.dao.OtpConfigDao;
import otp.dao.UserDao;
import otp.model.OtpCode;
import otp.model.OtpConfig;
import otp.model.OtpStatus;
import otp.model.User;
import otp.service.notification.NotificationChannel;
import otp.service.notification.NotificationService;
import otp.service.notification.NotificationServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom random = new SecureRandom();

    private final OtpCodeDao otpCodeDao;
    private final OtpConfigDao otpConfigDao;
    private final UserDao userDao;
    private final NotificationServiceFactory notificationFactory;

    public OtpService(OtpCodeDao otpCodeDao,
                      OtpConfigDao otpConfigDao,
                      UserDao userDao,
                      NotificationServiceFactory notificationFactory) {
        this.otpCodeDao = otpCodeDao;
        this.otpConfigDao = otpConfigDao;
        this.userDao = userDao;
        this.notificationFactory = notificationFactory;
    }

    /**
     * Генерирует новый OTP-код, сохраняет его в БД и возвращает строку.
     */
    public String generateOtp(Long userId, String operationId) {
        OtpConfig config = otpConfigDao.getConfig();
        int length = config.getLength();

        // Генерация случайного цифрового кода нужной длины
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        String code = sb.toString();

        // Сохраняем в БД
        OtpCode otp = new OtpCode(
                null,
                userId,
                operationId,
                code,
                OtpStatus.ACTIVE,
                LocalDateTime.now()
        );
        otpCodeDao.save(otp);
        logger.info("Сгенерирован OTP {} для userId={}, operationId={}", code, userId, operationId);
        return code;
    }

    /**
     * Возвращает текущую конфигурацию длины и TTL для кодов.
     */
    public OtpConfig getConfig() {
        return otpConfigDao.getConfig();
    }

    /**
     * Сгенерировать и отправить код указанным каналом.
     */
    public void sendOtpToUser(Long userId, String operationId, NotificationChannel channel) {
        String code = generateOtp(userId, operationId);
        User user = userDao.findById(userId);
        if (user == null) {
            logger.error("sendOtpToUser: пользователь не найден, id={}", userId);
            throw new IllegalArgumentException("Пользователь не найден");
        }

        // Для простоты используем username как адресат (email, sms, chatId)
        String recipient = user.getUsername();
        NotificationService svc = notificationFactory.getService(channel);
        svc.sendCode(recipient, code);
        logger.info("Отправлен OTP-код для userId={} через {}", userId, channel);
    }

    /**
     * Проверяет введённый код: активность и срок жизни, и переключает статус на USED.
     */
    public boolean validateOtp(String inputCode) {
        OtpCode otp = otpCodeDao.findByCode(inputCode);
        if (otp == null) {
            logger.warn("validateOtp: код не найден {}", inputCode);
            return false;
        }
        // Проверка статуса
        if (otp.getStatus() != OtpStatus.ACTIVE) {
            logger.warn("validateOtp: код {} не активен (статус={})", inputCode, otp.getStatus());
            return false;
        }
        // Проверка истечения по времени
        OtpConfig config = otpConfigDao.getConfig();
        LocalDateTime expiry = otp.getCreatedAt().plusSeconds(config.getTtlSeconds());
        if (LocalDateTime.now().isAfter(expiry)) {
            otpCodeDao.markAsExpiredOlderThan(Duration.ofSeconds(config.getTtlSeconds()));
            logger.warn("validateOtp: код {} истек {}", inputCode, expiry);
            return false;
        }

        // Всё ок — помечаем как USED
        otpCodeDao.markAsUsed(otp.getId());
        logger.info("validateOtp: код {} подтвержден и помечен как USED", inputCode);
        return true;
    }

    /**
     * Меняет статус всех просроченных кодов на EXPIRED.
     */
    public void markExpiredOtps() {
        OtpConfig config = otpConfigDao.getConfig();
        Duration ttl = Duration.ofSeconds(config.getTtlSeconds());
        otpCodeDao.markAsExpiredOlderThan(ttl);
        logger.info("markExpiredOtps: просроченные коды старше {} секунд помечены", config.getTtlSeconds());
    }
}
