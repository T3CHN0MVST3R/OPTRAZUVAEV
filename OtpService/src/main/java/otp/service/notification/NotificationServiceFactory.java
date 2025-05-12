package otp.service.notification;

/**
 * Фабрика для получения нужной реализации NotificationService по каналу.
 */
public class NotificationServiceFactory {

    /**
     * Возвращает реализацию NotificationService под указанный канал.
     */
    public NotificationService getService(NotificationChannel channel) {
        switch (channel) {
            case EMAIL:
                return new EmailNotificationService();
            case SMS:
                return new SmsNotificationService();
            case TELEGRAM:
                return new TelegramNotificationService();
            case FILE:
                return new FileNotificationService();
            default:
                throw new IllegalArgumentException("Unsupported channel: " + channel);
        }
    }
}
