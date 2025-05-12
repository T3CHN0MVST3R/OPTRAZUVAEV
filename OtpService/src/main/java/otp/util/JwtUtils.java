package otp.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import otp.model.User;
import otp.model.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

/**
 * Утилитный класс для работы с JWT-токенами.
 * Обрабатывает генерацию токенов, валидацию и извлечение информации о пользователе.
 */
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private static final String SECRET_KEY;
    private static final long TOKEN_EXPIRATION_MS;

    static {
        Properties props = new Properties();
        try (InputStream is = JwtUtils.class.getClassLoader().getResourceAsStream("application.properties")) {
            props.load(is);
            SECRET_KEY = props.getProperty("jwt.secret", "denzomaster_default_jwt_secret_key");
            TOKEN_EXPIRATION_MS = Long.parseLong(props.getProperty("jwt.expiration.ms", "1800000")); // По умолчанию: 30 минут
        } catch (Exception e) {
            logger.error("Ошибка загрузки свойств JWT", e);
            throw new RuntimeException("Не удалось инициализировать JWT-утилиту", e);
        }
    }

    private static final Algorithm ALGORITHM = Algorithm.HMAC256(SECRET_KEY);
    private static final JWTVerifier VERIFIER = JWT.require(ALGORITHM).build();

    /**
     * Генерирует JWT-токен для пользователя.
     * @param user Пользователь, для которого генерируется токен
     * @return JWT-токен в виде строки
     */
    public static String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + TOKEN_EXPIRATION_MS);

        String token = JWT.create()
                .withSubject(user.getId().toString())
                .withClaim("username", user.getUsername())
                .withClaim("role", user.getRole().name())
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(ALGORITHM);

        logger.info("Сгенерирован JWT-токен для пользователя {}, истекает в {}", user.getUsername(), expiryDate);
        return token;
    }

    /**
     * Проверяет JWT-токен и возвращает связанного пользователя.
     * @param token JWT-токен для проверки
     * @return Объект User, извлеченный из токена, или null, если токен недействителен
     */
    public static User validateTokenAndGetUser(String token) {
        try {
            DecodedJWT jwt = VERIFIER.verify(token);

            Long userId = Long.parseLong(jwt.getSubject());
            String username = jwt.getClaim("username").asString();
            String roleStr = jwt.getClaim("role").asString();

            User user = new User();
            user.setId(userId);
            user.setUsername(username);
            user.setRole(UserRole.valueOf(roleStr));

            return user;
        } catch (JWTVerificationException e) {
            logger.warn("Ошибка валидации JWT: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Ошибка обработки JWT-токена", e);
            return null;
        }
    }
}
