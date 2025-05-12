package otp.service;

import otp.dao.UserDao;
import otp.model.User;
import otp.model.UserRole;
import otp.util.PasswordEncoder;
import otp.util.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Регистрирует нового пользователя.
     * @throws IllegalArgumentException если логин уже занят или если пытаются создать второго администратора.
     */
    public void register(String username, String password, UserRole role) {
        if (username == null || username.trim().isEmpty() || password == null || password.length() < 6) {
            logger.warn("Регистрация не выполнена: Недопустимое имя пользователя или пароль");
            throw new IllegalArgumentException("Имя пользователя и пароль обязательны. Пароль должен содержать минимум 6 символов.");
        }

        if (userDao.findByUsername(username) != null) {
            logger.warn("Регистрация не выполнена: Имя пользователя {} уже существует", username);
            throw new IllegalArgumentException("Имя пользователя уже существует");
        }

        if (role == UserRole.ADMIN && userDao.adminExists()) {
            logger.warn("Регистрация не выполнена: Попытка зарегистрировать второго администратора: {}", username);
            throw new IllegalStateException("Администратор уже существует");
        }

        String hashed = PasswordEncoder.hash(password);
        User user = new User(null, username, hashed, role);
        userDao.create(user);
        logger.info("Зарегистрирован новый пользователь: {} с ролью {}", username, role);
    }

    /**
     * Проверяет, существует ли уже администратор.
     * @return true, если администратор существует, иначе false
     */
    public boolean adminExists() {
        return userDao.adminExists();
    }

    /**
     * Аутентифицирует пользователя и возвращает JWT-токен.
     * @throws IllegalArgumentException если пользователь не найден или пароль неверен.
     */
    public String login(String username, String password) {
        User user = userDao.findByUsername(username);
        if (user == null) {
            logger.warn("Вход не выполнен: Пользователь не найден {}", username);
            throw new IllegalArgumentException("Неверное имя пользователя или пароль");
        }

        if (!PasswordEncoder.matches(password, user.getPasswordHash())) {
            logger.warn("Вход не выполнен: Неверный пароль для {}", username);
            throw new IllegalArgumentException("Неверное имя пользователя или пароль");
        }

        String token = JwtUtils.generateToken(user);
        logger.info("Пользователь {} успешно вошел в систему, сгенерирован JWT-токен", username);
        return token;
    }

    public User findById(Long id) {
        return userDao.findById(id);
    }

    public List<User> findAllWithoutAdmins() {
        return userDao.findAllUsersWithoutAdmins();
    }

    public void deleteUser(Long id) {
        userDao.delete(id);
        logger.info("Удален пользователь с id {}", id);
    }
}
