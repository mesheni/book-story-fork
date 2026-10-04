<div align="center">
  <img src="fastlane/metadata/android/en-US/images/icon_2.png" width="120" alt="Иконка Book's Story" />
  <h1>Book's Story (fork)</h1>
  <h3>Офлайн-читалка электронных книг для Android</h3>
  <p>Неофициальный форк Book's Story с выбором режима чтения PDF и встроенным просмотрщиком страниц.</p>
</div>

<p align="center">
  <a href="https://github.com/mesheni/book-story-fork/releases"><img src="https://img.shields.io/github/v/release/mesheni/book-story-fork?label=Релиз&labelColor=27303D&color=3f719b" alt="Последний релиз" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/Лицензия-GPL--3.0--only-9b3f57?labelColor=27303D" alt="Лицензия GPL-3.0-only" /></a>
</p>

> Это **неофициальный форк** проекта [Book's Story](https://github.com/Acclorite/book-story). Релизы, обсуждения и изменения этой версии относятся к данному репозиторию и не являются официальными релизами upstream-проекта.

## Чем форк отличается от оригинала

- **Русский интерфейс:** добавлен перевод приложения на русский язык и возможность выбрать его в настройках.
- **Два режима чтения PDF:** можно извлечь текст и читать его в обычном ридере либо открыть исходные страницы документа во встроенном просмотрщике на базе Android `PdfRenderer`.
- **Сохранение позиции в PDF:** приложение запоминает выбранный режим и последнюю страницу для каждой книги.
- **Открытие PDF извне:** документ можно открыть в читалке из файлового менеджера или другого Android-приложения через системное действие «Открыть с помощью».
- **Экран «О приложении»:** убрана лента значков и ссылок на сторонние площадки.

![Промо-изображение Book's Story](fastlane/metadata/android/en-US/images/featureGraphic.png)

Book's Story — бесплатная читалка с открытым исходным кодом, созданная на Jetpack Compose. Приложение работает с локальными файлами, не содержит рекламы и позволяет настроить внешний вид и чтение под себя.

## Скриншоты

<div>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="30%" alt="Скриншот 1" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="30%" alt="Скриншот 2" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="30%" alt="Скриншот 3" />
  <hr width="91%">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="30%" alt="Скриншот 4" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" width="30%" alt="Скриншот 5" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" width="30%" alt="Скриншот 6" />
</div>

## Возможности

- Импорт книг и доступ к папкам через системное средство выбора файлов Android (Storage Access Framework).
- Поддержка форматов `.pdf`, `.txt`, `.epub`, `.fb2`, `.html`, `.htm` и `.md`.
- Для PDF доступны два режима: извлечение текста для чтения в обычном ридере и просмотр страниц во встроенном PDF-просмотрщике.
- При чтении PDF можно переключать режим; позиция страницы сохраняется.
- PDF можно открыть в приложении из файлового менеджера или другого приложения Android.
- Библиотека с категориями, историей чтения и настройками сортировки.
- Оформление в стиле Material You, темы и цветовые пресеты.
- Настройка отображения текста и параметров чтения.

## Скачать

Сборки форка публикуются в разделе [Releases](https://github.com/mesheni/book-story-fork/releases). Если APK ещё не опубликован, приложение можно собрать самостоятельно — инструкции ниже.

Приложение требует **Android 8.0 (API 26) или новее**.

> **Важно при переходе с оригинального приложения:** форк использует тот же идентификатор пакета — `ua.acclorite.book_story`, поэтому установить его рядом с оригинальной версией не получится. Если APK форка подписан другим ключом, Android не установит его поверх оригинала: перед заменой может потребоваться удалить прежнюю версию. Учитывайте риск потери локальных данных и заранее сохраните важное.

## Сборка из исходников

Нужны JDK 17 и Android SDK с платформой API 36. Откройте проект в Android Studio либо выполните из корня репозитория:

```bash
git clone https://github.com/mesheni/book-story-fork.git
cd book-story-fork
./gradlew assembleDebug
```

Готовый debug-APK появится в `app/build/outputs/apk/debug/`. Для Windows используйте `gradlew.bat assembleDebug`.

## Как помочь проекту

- Сообщить об ошибке или предложить улучшение можно через [Issues](https://github.com/mesheni/book-story-fork/issues).
- Для существенных изменений сначала создайте Issue, чтобы обсудить решение.
- Pull Request с исправлением или улучшением можно отправить в этот репозиторий.
- Строки интерфейса и переводы находятся в `app/src/main/res/values-*`.

## Оригинальный проект и благодарности

Этот форк основан на [Book's Story от Acclorite](https://github.com/Acclorite/book-story). Благодарность автору оригинального приложения и проектам, материалы и идеи которых используются в нём:

- [Mihon](https://github.com/mihonapp/mihon)
- [Kitsune](https://github.com/Drumber/Kitsune)
- [Voyager](https://voyager.adriel.cafe/)
- [Material Design Icons](https://fonts.google.com/icons) и [шрифты Google](https://fonts.google.com/)
- [Переводчики оригинального проекта на Weblate](https://hosted.weblate.org/projects/book-story)
- [GitHub Badge](https://github.com/Kunzisoft/Github-badge)
- [Codeberg Badge](https://codeberg.org/Codeberg/GetItOnCodeberg)

## Лицензия

Проект распространяется на условиях **GNU General Public License v3.0 only**. Полный текст лицензии находится в файле [`LICENSE`](LICENSE). Авторские права на исходный проект и сторонние материалы принадлежат их правообладателям; этот репозиторий — неофициальный форк.
