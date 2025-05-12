# OTP Service

## Описание проекта

OTP Service - это сервис генерации и проверки одноразовых паролей (OTP). Система поддерживает отправку OTP-кодов через различные каналы (Email, SMS, Telegram, File) и предоставляет REST API для управления пользователями, кодами и настройками системы.

## Особенности

- JWT-аутентификация и авторизация
- Два уровня доступа: USER и ADMIN
- Отправка OTP-кодов по нескольким каналам
- Автоматическая пометка просроченных кодов
- Полное логирование операций

## Требования

- Java 17 или выше
- PostgreSQL 17
- Gradle 8.5+

## Установка и запуск

1. **Клонировать репозиторий**
```bash
git clone https://github.com/denzomaster/OtpService.git
cd OtpService
```

2. **Настройка базы данных PostgreSQL**
```bash
# Создание базы данных
sudo -u postgres psql -c "CREATE DATABASE otp_db;"

# Настройка пароля (замените 'your_password' на надежный пароль)
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'your_password';"

# Создание структуры базы данных
sudo -u postgres psql -d otp_db -f src/main/resources/schema.sql
```

3. **Настройка конфигурации**

Отредактируйте файл `src/main/resources/application.properties`:
```properties
# Настройки сервера
server.port=8080

# Настройки базы данных (замените 'your_password' на ваш пароль PostgreSQL)
db.url=jdbc:postgresql://localhost:5432/otp_db
db.user=postgres
db.password=your_password

# JWT настройки
jwt.secret=denzomaster_otp_service_secret_key
jwt.expiration.ms=1800000
```

4. **Сборка и запуск**
```bash
./gradlew build
./gradlew run
```

Сервис запустится на адресе: http://localhost:8080

## Конфигурация каналов отправки OTP-кодов

### Email
Для настройки отправки OTP-кодов по email отредактируйте файл `src/main/resources/email.properties`:

```properties
email.smtp.host=smtp.example.com          # SMTP сервер
email.smtp.port=587                       # Порт SMTP
email.smtp.auth=true                      # Аутентификация SMTP
email.smtp.starttls.enable=true           # Использование STARTTLS
email.username=your-email@example.com     # Ваш email логин
email.password=your-password              # Ваш email пароль
email.from=noreply@example.com            # Адрес отправителя
```

### Telegram
Для настройки отправки через Telegram отредактируйте файл `src/main/resources/telegram.properties`:

```properties
telegram.apiUrl=https://api.telegram.org/bot # URL Telegram API
telegram.token=your-telegram-bot-token       # Токен бота (получите у @BotFather)
telegram.chatId=your-default-chat-id         # ID чата для отправки уведомлений
```

### SMS
Настройки для SMS эмулятора в файле `src/main/resources/sms.properties`.

### File
OTP-коды можно сохранять в файл, указав путь к файлу при отправке.

## API документация

### Публичные эндпоинты

#### Регистрация пользователя
`POST /register`

Запрос:
```json
{
  "username": "user1",
  "password": "password123",
  "role": "USER"
}
```

Ответ (успех): 201 Created

#### Аутентификация
`POST /login`

Запрос:
```json
{
  "username": "user1",
  "password": "password123"
}
```

Ответ (успех):
```json
{
  "token": "your.jwt.token"
}
```

### Пользовательские эндпоинты (требуют роли USER)

#### Генерация OTP-кода
`POST /otp/generate`

Заголовки:
```
Authorization: Bearer your.jwt.token
Content-Type: application/json
```

Запрос:
```json
{
  "userId": 1,
  "operationId": "transfer-12345",
  "channel": "EMAIL"  // EMAIL, SMS, TELEGRAM, FILE
}
```

Ответ (успех): 202 Accepted

#### Валидация OTP-кода
`POST /otp/validate`

Заголовки:
```
Authorization: Bearer your.jwt.token
Content-Type: application/json
```

Запрос:
```json
{
  "code": "123456"
}
```

Ответ (успех): 200 OK

### Администраторские эндпоинты (требуют роли ADMIN)

#### Изменение конфигурации OTP
`PATCH /admin/config`

Заголовки:
```
Authorization: Bearer your.jwt.token
Content-Type: application/json
```

Запрос:
```json
{
  "length": 6,
  "ttlSeconds": 300
}
```

Ответ (успех): 204 No Content

#### Получение списка пользователей
`GET /admin/users`

Заголовки:
```
Authorization: Bearer your.jwt.token
```

Ответ:
```json
[
  {
    "id": 1,
    "username": "user1",
    "role": "USER"
  }
]
```

#### Удаление пользователя
`DELETE /admin/users/{id}`

Заголовки:
```
Authorization: Bearer your.jwt.token
```

Ответ (успех): 204 No Content

## Первые шаги

1. Зарегистрируйте администратора:
```bash
curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin_password","role":"ADMIN"}'
```

2. Получите JWT-токен:
```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin_password"}'
```

3. Используйте полученный токен для доступа к защищенным эндпоинтам.

## Планировщик задач

Приложение включает планировщик, который автоматически помечает просроченные OTP-коды как "EXPIRED" каждые 5 минут.

## Поддержка и развитие

Проект разработан denzomaster. Если у вас есть вопросы или предложения по улучшению - обращайтесь по email или создавайте issues в репозитории.

**Примечание:** Перед использованием в продакшене рекомендуется:
1. Настроить HTTPS для защиты API
2. Сменить все дефолтные пароли и ключи
3. Настроить реальные параметры для отправки уведомлений (Email, SMS, Telegram)
4. Усилить политики безопасности паролей