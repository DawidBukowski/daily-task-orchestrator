Testy jednostkowe weryfikujące logikę wyciągania i analizowania terminów (`deadlines`) z treści wiadomości e-mail.

```java
class DeadlineParserTest {

    private DeadlineParser parser;
    private LocalDateTime referenceTime;

    @BeforeEach
    void setUp() {
        parser = new DeadlineParser();
        referenceTime = LocalDateTime.of(2026, 6, 29, 10, 0);
    }

    @Test
    void testParseDueFriday() {
        String text = "Due Friday";
        LocalDateTime result = parser.extractDeadline(text, referenceTime);

        assertNotNull(result);
        assertEquals(DayOfWeek.FRIDAY, result.getDayOfWeek());
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
        assertTrue(result.isAfter(referenceTime));
    }

    @Test
    void testParseDueNumericDate() {
        String text = "Due 7/5";
        LocalDateTime result = parser.extractDeadline(text, referenceTime);

        assertNotNull(result);
        assertEquals(7, result.getMonthValue());
        assertEquals(5, result.getDayOfMonth());
        assertEquals(2026, result.getYear());
    }
}
```

* `setUp()`: Inicjalizuje [[DeadlineParser]] oraz ustawia punkt odniesienia (`referenceTime`), który w testach symuluje czas odebrania wiadomości. Jest on kluczowy do prawidłowego obliczania względnych dat, np. „Due Friday” czy „Due in 3 days”.
* `testParseDueFriday()`: Sprawdza parsowanie terminów z nazwami dni tygodnia. Parser ustawia godzinę na koniec dnia (`23:59`) i upewnia się, że zwrócona data przypada w najbliższy nadchodzący piątek względem czasu odniesienia.
* `testParseDueNumericDate()`: Testuje wyciąganie dat w formacie numerycznym `M/D` (np. „7/5” jako 5 lipca) z automatycznym uzupełnieniem bieżącego roku.
* Klasa testowa pokrywa także inne scenariusze, m.in. formaty z podanym rokiem (np. `5/25/2026`), formaty opisowe (np. `May 25, 2026`), formy względne typu `Due in 3 days`, a także poprawne ignorowanie tekstu bez informacji o terminach oraz odporność na wielkość liter (`DUE FRIDAY`).
