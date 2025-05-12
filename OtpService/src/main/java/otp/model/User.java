package otp.model;

import java.util.Objects;

/**
 * Представляет пользователя сервиса OTP.
 * Пользователь имеет уникальный идентификатор, логин, хеш пароля и роль в системе.
 */
public class User {
    private Long id;
    private String username;
    private String passwordHash;
    private UserRole role;

    /**
     * Пустой конструктор для фреймворков и JDBC.
     */
    public User() {
    }

    /**
     * Полный конструктор.
     *
     * @param id           уникальный идентификатор пользователя
     * @param username     логин для аутентификации
     * @param passwordHash хеш пароля
     * @param role         роль пользователя (ADMIN или USER)
     */
    public User(Long id, String username, String passwordHash, UserRole role) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    /**
     * @return уникальный идентификатор пользователя
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id устанавливает уникальный идентификатор пользователя
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return логин пользователя
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username устанавливает логин пользователя
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return хеш пароля пользователя
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * @param passwordHash устанавливает хеш пароля пользователя
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * @return роль пользователя в системе
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * @param role устанавливает роль пользователя в системе
     */
    public void setRole(UserRole role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;
        return Objects.equals(id, user.id)
                && Objects.equals(username, user.username)
                && Objects.equals(passwordHash, user.passwordHash)
                && role == user.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, passwordHash, role);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", role=" + role +
                '}';
    }
}
