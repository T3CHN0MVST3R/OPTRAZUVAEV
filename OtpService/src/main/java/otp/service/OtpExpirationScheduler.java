package otp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Планировщик, который по расписанию помечает просроченные OTP-коды как EXPIRED.
 */
public class OtpExpirationScheduler {
    private static final Logger logger = LoggerFactory.getLogger(OtpExpirationScheduler.class);

    private final OtpService otpService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** Интервал в минутах между запусками */
    private final long intervalMinutes;

    public OtpExpirationScheduler(OtpService otpService, long intervalMinutes) {
        this.otpService = otpService;
        this.intervalMinutes = intervalMinutes;
    }

    /**
     * Запускает планировщик.
     * По расписанию будет вызываться метод run().
     */
    public void start() {
        logger.info("Запуск планировщика истечения OTP, интервал={} мин", intervalMinutes);
        scheduler.scheduleAtFixedRate(
                this::run,           // явно вызываем наш метод run()
                0,                   // запускаем сразу, без начальной задержки
                intervalMinutes,     // период
                TimeUnit.MINUTES
        );
    }

    /**
     * Однократный прогон: помечает все просроченные OTP как EXPIRED.
     */
    public void run() {
        try {
            otpService.markExpiredOtps();
            logger.debug("OtpExpirationScheduler run(): просроченные коды обработаны");
        } catch (Exception e) {
            logger.error("Ошибка в задаче проверки истечения OTP", e);
        }
    }

    /** Останавливает планировщик */
    public void stop() {
        logger.info("Остановка планировщика истечения OTP");
        scheduler.shutdownNow();
    }
}
