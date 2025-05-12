package otp.service.notification;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Реализация NotificationService для отправки OTP-кодов через Telegram Bot API.
 * Конфигурация берётся из файла telegram.properties.
 */
public class TelegramNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(TelegramNotificationService.class);

    private final String apiBaseUrl;   // например "https://api.telegram.org/bot"
    private final String token;        // токен бота
    private final String defaultChatId;// chatId по умолчанию

    public TelegramNotificationService() {
        Properties props = loadConfig();
        this.apiBaseUrl    = props.getProperty("telegram.apiUrl");
        this.token         = props.getProperty("telegram.token");
        this.defaultChatId = props.getProperty("telegram.chatId");
    }

    private Properties loadConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("telegram.properties")) {
            if (is == null) throw new IllegalStateException("telegram.properties не найден в classpath");
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (Exception e) {
            logger.error("Не удалось загрузить telegram.properties", e);
            throw new RuntimeException("Не удалось загрузить конфигурацию Telegram", e);
        }
    }

    /**
     * Отправляет OTP-код через Telegram Bot.
     *
     * @param recipientChatId chatId получателя (если null или пусто, используется defaultChatId)
     * @param code            само сообщение — OTP-код
     */
    @Override
    public void sendCode(String recipientChatId, String code) {
        String chatId = (recipientChatId == null || recipientChatId.isBlank())
                ? defaultChatId
                : recipientChatId;
        String text = "Ваш одноразовый код подтверждения: " + code;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Составляем URL вида: https://api.telegram.org/bot<TOKEN>/sendMessage?chat_id=<ID>&text=<TEXT>
            URI uri = new URIBuilder(apiBaseUrl + token + "/sendMessage")
                    .addParameter("chat_id", chatId)
                    .addParameter("text", text)
                    .build();

            HttpGet request = new HttpGet(uri);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                if (status != 200) {
                    logger.error("Ошибка Telegram API. Код состояния: {}", status);
                    throw new RuntimeException("Telegram API вернул " + status);
                }
                logger.info("OTP-код отправлен через Telegram в chatId {}", chatId);
            }
        } catch (URISyntaxException e) {
            logger.error("Неверный URI для Telegram API", e);
            throw new RuntimeException("Неверный Telegram API URI", e);
        } catch (Exception e) {
            logger.error("Не удалось отправить сообщение Telegram на {}", chatId, e);
            throw new RuntimeException("Ошибка отправки Telegram", e);
        }
    }
}
