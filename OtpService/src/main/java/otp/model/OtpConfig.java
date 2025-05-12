package otp.model;

import java.util.Objects;

/**
 * Сущность конфигурации OTP-кодов.
 * Содержит параметры длины кода и времени жизни (TTL) в секундах.
 */
public class OtpConfig {
    private Long id;
    private int length;      // количество символов в коде
    private int ttlSeconds;  // время жизни кода в секундах

    /**
     * Пустой конструктор для фреймворков и JDBC.
     */
    public OtpConfig() {
    }

    /**
     * Полный конструктор.
     *
     * @param id         уникальный идентификатор записи
     * @param length     длина OTP-кода
     * @param ttlSeconds время жизни кода в секундах
     */
    public OtpConfig(Long id, int length, int ttlSeconds) {
        this.id = id;
        this.length = length;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * @return уникальный идентификатор конфигурации
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id задаёт уникальный идентификатор конфигурации
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return длину OTP-кода
     */
    public int getLength() {
        return length;
    }

    /**
     * @param length задаёт длину OTP-кода
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * @return время жизни OTP-кода в секундах
     */
    public int getTtlSeconds() {
        return ttlSeconds;
    }

    /**
     * @param ttlSeconds задаёт время жизни OTP-кода в секундах
     */
    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OtpConfig that = (OtpConfig) o;
        return length == that.length
                && ttlSeconds == that.ttlSeconds
                && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, length, ttlSeconds);
    }

    @Override
    public String toString() {
        return "OtpConfig{" +
                "id=" + id +
                ", length=" + length +
                ", ttlSeconds=" + ttlSeconds +
                '}';
    }
}
