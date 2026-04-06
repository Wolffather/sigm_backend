# sigm_backend

Бэкенд мессенджера sigm на Kotlin + Ktor.

## Стек

- **Kotlin** — язык разработки
- **Ktor** — веб-фреймворк
- **Exposed** — ORM для работы с БД
- **PostgreSQL** — база данных
- **JWT** — авторизация
- **BCrypt** — хеширование паролей
- **WebSocket** — real-time сообщения
- **Docker** — контейнеризация БД

## Быстрый старт

### Требования
- JDK 21
- Docker

### Запуск PostgreSQL
```bash
docker-compose up -d
```

### Запуск сервера
```bash
JWT_SECRET=your_secret_key ./gradlew run
```

Сервер запустится на `http://localhost:8080`

## Структура проекта

```
src/main/kotlin/
├── Application.kt          — точка входа
├── Routing.kt              — регистрация роутов
├── Database.kt             — подключение к БД
├── Extensions.kt           — extension функции для Ktor
├── model/                  — модели данных
│   ├── User.kt
│   ├── Room.kt
│   ├── Message.kt
│   ├── UserProfile.kt
│   ├── ProfileUpdate.kt
│   └── ChangeRequest.kt
├── tables/                 — таблицы Exposed
│   ├── UsersTable.kt
│   ├── RoomsTable.kt
│   ├── MessagesTable.kt
│   └── RoomMembersTable.kt
├── routes/                 — роуты по группам
│   ├── AuthRoutes.kt
│   ├── UserRoutes.kt
│   ├── RoomRoutes.kt
│   ├── MessageRoutes.kt
│   ├── ChatRoutes.kt
│   └── WebSocketRoutes.kt
├── services/               — бизнес-логика
│   ├── UserService.kt
│   ├── RoomService.kt
│   └── MessageService.kt
└── utils/                  — утилиты
    ├── PasswordUtils.kt
    └── BroadcastUtils.kt
```

## API

### Авторизация

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/register` | Регистрация |
| POST | `/login` | Вход, возвращает JWT токен |

### Пользователи
> Требуют заголовок `Authorization: Bearer <token>`

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/users/me` | Получить профиль |
| PUT | `/users/me` | Обновить профиль |
| PUT | `/users/me/password` | Сменить пароль |
| PUT | `/users/me/username` | Сменить юзернейм |

### Комнаты

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/rooms` | Мои комнаты |
| GET | `/rooms/public` | Все публичные комнаты |
| POST | `/rooms` | Создать комнату |
| DELETE | `/rooms/{id}` | Удалить комнату (только OWNER) |
| POST | `/rooms/{id}/join` | Вступить в комнату |
| POST | `/rooms/{id}/leave` | Покинуть комнату |
| GET | `/rooms/{id}/members` | Список участников |
| POST | `/chats/private/{username}` | Создать приватный чат |

### Сообщения

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/messages/{roomId}` | История сообщений комнаты |

### WebSocket

```
ws://host/chat/{roomId}?token=<jwt>
```

Формат сообщения:
```json
{"author": "username", "text": "текст сообщения"}
```

## Конфигурация

`application.yaml`:
```yaml
ktor:
  deployment:
    port: 8080
jwt:
  secret: ${JWT_SECRET}
  issuer: "sigm"
  audience: "sigm-users"
```

## Типы комнат

| Тип | Описание |
|-----|----------|
| `GROUP` | Групповой чат |
| `CHANNEL` | Канал |
| `CHAT` | Приватный чат между двумя пользователями |

## Роли участников

| Роль | Описание |
|------|----------|
| `OWNER` | Создатель комнаты |
| `ADMIN` | Администратор группы |
| `MEMBER` | Участник |
| `SUBSCRIBER` | Подписчик канала |