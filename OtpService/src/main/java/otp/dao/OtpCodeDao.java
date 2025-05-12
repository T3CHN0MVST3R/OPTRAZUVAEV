package otp.dao;

import otp.model.OtpCode;
import java.time.Duration;
import java.util.List;

/**
 * Интерфейс для доступа к данным одноразовых кодов (OTP).
 */
public interface OtpCodeDao {

    /**
     * Сохраняет новый одноразовый код в БД.
     * @param code объект OtpCode (id и createdAt могут быть null — будут заполнены БД)
     */
    void save(OtpCode code);

    /**
     * Ищет запись по самому значению кода.
     * @param code строка кода
     * @return объект OtpCode или null, если не найден
     */
    OtpCode findByCode(String code);

    /**
     * Возвращает все коды, связанные с указанным пользователем.
     * @param userId идентификатор пользователя
     * @return список всех OtpCode для данного пользователя
     */
    List<OtpCode> findAllByUser(Long userId);

    /**
     * Помечает код с заданным id как использованный.
     * @param id идентификатор записи OtpCode
     */
    void markAsUsed(Long id);

    /**
     * Помечает все коды старше указанного TTL как просроченные.
     * @param ttl время жизни кода (Duration), все коды с createdAt + ttl &lt; now() станут EXPIRED
     */
    void markAsExpiredOlderThan(Duration ttl);

    /**
     * Удаляет все коды, принадлежащие указанному пользователю.
     * @param userId идентификатор пользователя
     */
    void deleteAllByUserId(Long userId);
}
