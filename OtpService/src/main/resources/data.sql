-- Инициализация конфигурации OTP по умолчанию
INSERT INTO otp_config (length, ttl_seconds)
VALUES (6, 300)
ON CONFLICT DO NOTHING;
