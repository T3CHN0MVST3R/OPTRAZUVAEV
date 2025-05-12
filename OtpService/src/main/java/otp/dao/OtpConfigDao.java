package otp.dao;

import otp.model.OtpConfig;

/**
 * Интерфейс для доступа к данным конфигурации OTP-кодов.
 */
public interface OtpConfigDao {

    /**
     * Получает текущую конфигурацию OTP.
     * Предполагается, что в таблице ровно одна запись.
     * @return объект OtpConfig или null, если запись отсутствует
     */
    OtpConfig getConfig();

    /**
     * Обновляет существующую конфигурацию OTP (length, ttlSeconds).
     * @param config объект OtpConfig с новыми значениями
     */
    void updateConfig(OtpConfig config);

    /**
     * Инициализирует дефолтную конфигурацию, если в таблице нет записей.
     * Обычно вызывается при старте приложения.
     */
    void initDefaultConfigIfEmpty();
}
