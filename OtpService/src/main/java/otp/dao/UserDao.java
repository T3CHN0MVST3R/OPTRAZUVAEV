package otp.dao;

import otp.model.User;
import java.util.List;

/**
 * Интерфейс для доступа к данным пользователей.
 */
public interface UserDao {

    /**
     * Сохраняет нового пользователя в БД.
     * @param user объект User для вставки (id генерируется БД)
     */
    void create(User user);

    /**
     * Ищет пользователя по логину.
     * @param username логин
     * @return найденный User или null, если не найден
     */
    User findByUsername(String username);

    /**
     * Ищет пользователя по его идентификатору.
     * @param id идентификатор
     * @return найденный User или null
     */
    User findById(Long id);

    /**
     * Возвращает список всех пользователей, у которых роль не ADMIN.
     * @return список пользователей без администраторов
     */
    List<User> findAllUsersWithoutAdmins();

    /**
     * Проверяет, существует ли в системе хотя бы один администратор.
     * @return true, если администратор уже есть
     */
    boolean adminExists();

    /**
     * Удаляет пользователя по идентификатору (с каскадным удалением OTP-кодов).
     * @param userId идентификатор удаляемого пользователя
     */
    void delete(Long userId);
}
