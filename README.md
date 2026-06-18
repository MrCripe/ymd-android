# YMD Android — Yandex Music Downloader

> **⚠️ ВНИМАНИЕ: Проект сгенерирован ИИ (OpenAI / Google / аналоги). Код может содержать ошибки. Используйте на свой страх и риск.**

Android-приложение для загрузки музыки из Яндекс Музыки. Форк проекта [yandex-music-downloader](https://github.com/llistochek/yandex-music-downloader) — переписан на Kotlin с Jetpack Compose.

## 🚀 Быстрый старт

### 1. Получение API ключа (GitHub Personal Access Token)

Для сборки проекта нужен GitHub Personal Access Token (PAT):

1. Зайдите на https://github.com/settings/tokens
2. Нажмите **Generate new token**
3. Выберите scope: **Contents (Read and write)**
4. Сгенерируйте токен
5. Скопируйте токен

### 2. Получение токена Яндекс Музыки

1. Зайдите на https://music.yandex.ru
2. Откройте DevTools (F12) → Application → Cookies
3. Скопируйте значение куки `music_token` (начинается с `y0_`)
4. Или используйте тестовый токен из документации Яндекс Музыки

### 3. Сборка

```bash
git clone https://github.com/MrCripe/ymd-android.git
cd ymd-android

# Создайте local.properties
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# Соберите
./gradlew :app:assembleDebug

# Установите на устройство
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 📋 Функционал

- ✅ Загрузка треков по ссылке или ID
- ✅ Поддержка качества: Low (AAC 64), Medium (AAC 192), Best (FLAC)
- ✅ Прогресс-бар загрузки в реальном времени
- ✅ Кнопка отмены загрузки
- ✅ Отображение статуса (Downloading, Completed, Failed, Cancelled)
- ✅ Библиотека загруженных треков
- ✅ Сохранение в публичную директорию Music/YMD
- ✅ Автоматическое вшивание обложки (всегда включено)
- ✅ Проверка на уже загруженные треки
- ✅ Поддержка форматов: MP3, AAC (M4A), FLAC

## 🏗️ Архитектура

```
com.mrcriper.ymd/
├── data/
│   ├── remote/          # API клиент (OkHttp + Ktor)
│   │   ├── api/         # YandexMusicApi, подпись запросов
│   │   ├── dto/         # DTO модели, мапперы
│   │   └── download/    # DownloadManager, Decryptor
│   ├── local/           # Room DB, DataStore
│   └── repository/      # Репозитории данных
├── domain/
│   ├── model/           # Доменные модели
│   ├── repository/      # Интерфейсы репозиториев
│   ├── usecase/         # Use cases
│   └── util/            # TagWriter, PathPatternParser
├── presentation/
│   ├── screens/         # Compose экраны
│   ├── components/      # UI компоненты
│   └── viewmodel/       # ViewModels
├── di/                  # Hilt модули
└── service/             # Foreground service
```

## 🔧 Технологии

- **Kotlin** 2.1.10+
- **Jetpack Compose** с Material 3
- **Hilt** для DI
- **OkHttp** для загрузки файлов
- **Ktor** для API запросов
- **Room** для локальной БД
- **DataStore** для настроек
- **jaudiotagger** для записи метаданных
- **Coil** для загрузки изображений
- **MediaStore** для сохранения в публичную директорию

## ⚠️ Известные баги

- **Обложки не вшиваются** в MP4/M4A файлы — URL обложки требует замены `%%` на `orig`, но вшивание через jaudiotagger работает нестабильно
- **Лирик файлы (LRC)** не загружаются и не вшиваются — функционал не реализован
- **FLAC внутри MP4** (flac-mp4) не поддерживается — jaudiotagger не может обработать FLAC в MP4 контейнере
- **Некоторые треки** могут не загружаться из-за региональных ограничений
- **Подпись API** требует корректного User-Agent (используется Android UA вместо API UA)

## 📝 Лицензия

MIT License. Не является официальным продуктом Яндекс Музыки.

## 🙏 Благодарности

- **Яндекс Музыка** — за API
- **jaudiotagger** — за библиотеку работы с аудио метаданными
- **Jetpack Compose** — за отличный UI фреймворк
- **Сообщество open-source** — за библиотеки и инструменты

---

**Дисклеймер:** Этот проект предназначен только для образовательных целей. Соблюдайте авторские права и условия использования Яндекс Музыки.
