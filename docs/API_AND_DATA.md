# HTTP API та формат імпорту

Кореневий шлях: `/library-server/api`. Успішна відповідь має JSON-форму `{"data":...}`. Помилка: `{"error":"зрозумілий опис"}` з відповідним HTTP status.

| Метод | Шлях | Призначення |
|---|---|---|
| GET | `/publications` | список видань |
| GET | `/publications/{id}` | одне видання |
| POST | `/publications` | створення видання; JSON з полями видання |
| PUT | `/publications/{id}` | редагування видання |
| DELETE | `/publications/{id}` | видалення видання |
| GET/POST/PUT/DELETE | `/readers[/{id}]` | CRUD читачів |
| GET/POST/PUT/DELETE | `/bookings[/{id}]` | CRUD бронювань (редагування полів тільки у CREATED) |
| POST | `/bookings/{id}/transition` | перехід стану; JSON `{"state":"CONFIRMED"}` тощо |
| GET/POST/PUT/DELETE | `/loans[/{id}]` | перегляд, коригування терміну та видалення видач |
| POST | `/loans/{id}/return` | реєстрація повернення |

Заборонено пропускати, повторювати або повертати назад стани бронювання. Перехід у `COMPLETED` атомарно створює запис видачі, зменшує кількість доступних примірників і оновлює показник. Повернення видачі збільшує доступну кількість.

## CSV-каталог

UTF-8, перший рядок — заголовок. Порядок колонок: `isbn,title,author,publisher,publication_year,category,total_copies`. Обов'язкові: ISBN, назва, автор, рік, кількість примірників. Кількість при імпорті стає і загальною, і доступною кількістю. Дублікати ISBN або некоректні рядки пропускаються з переліком помилок; інші коректні рядки імпортуються. Поля тексту можуть містити коми, якщо взяті в подвійні лапки.

## Модель і переходи

`Publication 1—N Booking`, `Reader 1—N Booking`, `Booking 1—0..1 Loan`. У `booking` значення F1, F2 та F3 зберігаються як нормовані бали в межах 0–100 (`f1_wait_score`, `f2_demand`, `f3_shortage`).

`CREATED → CONFIRMED → READY_FOR_ISSUE → COMPLETED`.
