## Test [[EmailFilterTest]] - Testy filtrowania e-maili

Klasa testowa weryfikująca poprawność działania filtru wiadomości [[EmailFilter]]. Upewnia się, że wiadomości e-mail są poprawnie kwalifikowane jako zawierające zadania na podstawie słów kluczowych oraz że zapytania wyszukiwania są generowane prawidłowo.

```java
class EmailFilterTest {
    private EmailFilter filter;

    @BeforeEach
    void setUp() {
        filter = new EmailFilter(List.of("assignment", "deadline"));
    }

    @Test
    void isTaskEmail_KeywordInSubject_ReturnsTrue() {
        GmailMessage msg = new GmailMessage();
        msg.setSubject("Upcoming ASSIGNMENT");
        assertTrue(filter.isTaskEmail(msg));
    }

    @Test
    void isTaskEmail_NoKeywords_ReturnsFalse() {
        GmailMessage msg = new GmailMessage();
        msg.setSubject("Hello there");
        msg.setBody("How are you?");
        assertFalse(filter.isTaskEmail(msg));
    }

    @Test
    void getTaskQuery_WithInstant_GeneratesCorrectly() {
        Instant now = Instant.now();
        String query = filter.getTaskQuery(now);
        assertEquals("after:" + now.getEpochSecond(), query);
    }
}
```

### Wyjaśnienie scenariuszy testowych

Każda metoda oznaczona adnotacją `@Test` to osobny, niezależny scenariusz testowy:

* **`setUp()`** (z adnotacją `@BeforeEach`): Uruchamia się przed **każdym** pojedynczym testem. Tworzy nową instancję [[EmailFilter]] ze słowami kluczowymi: `"assignment"` oraz `"deadline"`, dzięki czemu każdy test zaczyna z czystą i identyczną konfiguracją.
* **`isTaskEmail_KeywordInSubject_ReturnsTrue`**: Sprawdza, czy filtr poprawnie wykrywa słowo kluczowe w temacie wiadomości. Zwróć uwagę, że słowo w temacie ("ASSIGNMENT") jest pisane wielkimi literami, a filtr powinien działać bez względu na wielkość liter (case-insensitive).
* **`isTaskEmail_NoKeywords_ReturnsFalse`**: Sprawdza scenariusz negatywny — upewnia się, że zwykły mail niezawierający słów kluczowych zostanie zignorowany (zwróci `false`).
* **`getTaskQuery_WithInstant_GeneratesCorrectly`**: Testuje metodę generującą zapytanie filtrujące do serwera Gmail. Zapytanie powinno mieć format `after:<znacznik_czasu>`.

### Kluczowe pojęcia dla nowicjuszy

* **`JUnit` / JUnit 5**: Najpopularniejsza biblioteka (framework) w świecie Javy służąca do automatycznego uruchamiania testów.
* **Adnotacja `@Test`**: Informacja dla JUnit, że ta metoda jest testem i należy ją automatycznie uruchomić.
* **Adnotacja `@BeforeEach`**: Służy do przygotowania środowiska (danych testowych, obiektów) przed wykonaniem każdego testu. Pomaga to zachować tzw. **izolację testów** (żaden test nie wpływa na wynik innego).
* **Asercje (`assertTrue`, `assertFalse`, `assertEquals`)**: Metody sprawdzające, czy rzeczywisty wynik działania programu zgadza się z naszymi oczekiwaniami. Jeśli warunek w asercji nie jest spełniony, test zgłasza błąd ("wybucha na czerwono").
