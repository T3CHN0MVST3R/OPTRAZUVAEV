package otp.dao.impl;

import otp.config.DatabaseManager;
import otp.dao.UserDao;
import otp.model.User;
import otp.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC-реализация UserDao.
 * Использует DatabaseManager для получения соединения и SLF4J для логирования.
 */
public class UserDaoImpl implements UserDao {
    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);

    private static final String INSERT_SQL =
            "INSERT INTO users (username, password_hash, role) VALUES (?, ?, ?)";
    private static final String SELECT_BY_USERNAME_SQL =
            "SELECT id, username, password_hash, role FROM users WHERE username = ?";
    private static final String SELECT_BY_ID_SQL =
            "SELECT id, username, password_hash, role FROM users WHERE id = ?";
    private static final String SELECT_ALL_USERS_SQL =
            "SELECT id, username, password_hash, role FROM users WHERE role <> 'ADMIN'";
    private static final String SELECT_ADMIN_EXISTS_SQL =
            "SELECT 1 FROM users WHERE role = 'ADMIN' LIMIT 1";
    private static final String DELETE_USER_SQL =
            "DELETE FROM users WHERE id = ?";

    @Override
    public void create(User user) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            logger.info("Создан пользователь: {}", user);
        } catch (SQLException e) {
            logger.error("Ошибка создания пользователя [{}]: {}", user.getUsername(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public User findByUsername(String username) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_USERNAME_SQL)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapRow(rs);
                    logger.info("Найден пользователь по имени {}: {}", username, user);
                    return user;
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка поиска пользователя по имени [{}]: {}", username, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public User findById(Long id) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID_SQL)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = mapRow(rs);
                    logger.info("Найден пользователь по id {}: {}", id, user);
                    return user;
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка поиска пользователя по id [{}]: {}", id, e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public List<User> findAllUsersWithoutAdmins() {
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ALL_USERS_SQL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
            logger.info("Найдено {} пользователей без администраторов", users.size());
        } catch (SQLException e) {
            logger.error("Ошибка получения списка пользователей без администраторов: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return users;
    }

    @Override
    public boolean adminExists() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ADMIN_EXISTS_SQL);
             ResultSet rs = ps.executeQuery()) {
            boolean exists = rs.next();
            logger.info("Существует ли администратор: {}", exists);
            return exists;
        } catch (SQLException e) {
            logger.error("Ошибка проверки существования администратора: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(Long userId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_USER_SQL)) {
            ps.setLong(1, userId);
            int affected = ps.executeUpdate();
            logger.info("Удален пользователь id {}: затронуто {} строк", userId, affected);
        } catch (SQLException e) {
            logger.error("Ошибка удаления пользователя id [{}]: {}", userId, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Вспомогательный метод для маппинга строки ResultSet в объект User.
     */
    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        return user;
    }
}
