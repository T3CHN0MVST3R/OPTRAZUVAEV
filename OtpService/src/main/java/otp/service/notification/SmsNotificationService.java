package otp.service.notification;

import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.MessageClass;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

/**
 * Реализация NotificationService для отправки OTP-кодов по SMS
 * через SMPP протокол.
 */
public class SmsNotificationService implements NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SmsNotificationService.class);
    private static final TimeFormatter TIME_FORMATTER = new AbsoluteTimeFormatter();

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddr;

    public SmsNotificationService() {
        Properties props = loadConfig();
        this.host = props.getProperty("smpp.host");
        this.port = Integer.parseInt(props.getProperty("smpp.port"));
        this.systemId = props.getProperty("smpp.system_id");
        this.password = props.getProperty("smpp.password");
        this.systemType = props.getProperty("smpp.system_type");
        this.sourceAddr = props.getProperty("smpp.source_addr");
    }

    private Properties loadConfig() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sms.properties")) {
            if (is == null) throw new IllegalStateException("sms.properties не найден");
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (IOException e) {
            logger.error("Не удалось загрузить sms.properties", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendCode(String recipientPhone, String code) {
        SMPPSession session = null;
        try {
            // Создаем сессию SMPP
            session = new SMPPSession();

            // Подключаемся к SMPP серверу
            session.connectAndBind(
                host,
                port,
                new BindParameter(
                    BindType.BIND_TX,
                    systemId,
                    password,
                    systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    null
                )
            );

            // Формируем сообщение
            String message = "Ваш OTP-код: " + code;

            // Отправляем сообщение
            session.submitShortMessage(
                "CMT",
                TypeOfNumber.INTERNATIONAL,
                NumberingPlanIndicator.UNKNOWN,
                sourceAddr,
                TypeOfNumber.INTERNATIONAL,
                NumberingPlanIndicator.UNKNOWN,
                recipientPhone,
                new ESMClass(),
                (byte)0,
                (byte)1,
                TIME_FORMATTER.format(new Date()),
                null,
                new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                (byte)0,
                new GeneralDataCoding(Alphabet.ALPHA_DEFAULT, MessageClass.CLASS1, false),
                (byte)0,
                message.getBytes()
            );

            logger.info("OTP-код отправлен по SMS на {}", recipientPhone);
        } catch (Exception e) {
            logger.error("Ошибка при отправке SMS на {}", recipientPhone, e);
            throw new RuntimeException("Ошибка отправки SMS", e);
        } finally {
            if (session != null) {
                session.unbindAndClose();
            }
        }
    }
}
