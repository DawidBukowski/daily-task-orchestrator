#### Testy (Metody z `@Test`)

Każda metoda z adnotacją `@Test` to osobny scenariusz:

- **`isTaskEmail_KeywordInSubject_ReturnsTrue`**: Sprawdza pozytywny scenariusz. Jeśli w temacie maila jest słowo „assignment” (nawet wielkimi literami), metoda `isTaskEmail` powinna zwrócić `true`.
    
- **`isTaskEmail_NoKeywords_ReturnsFalse`**: Sprawdza scenariusz negatywny. Jeśli mail nie zawiera żadnych słów kluczowych, system powinien uznać, że to nie jest „mail z zadaniem” (`false`).
    
- **`getTaskQuery_WithInstant_GeneratesCorrectly`**: Sprawdza, czy filtr poprawnie tworzy zapytanie do Gmaila (z parametrem `after`), używając czasu `Instant`.
### Kluczowe pojęcia dla nowicjusza:

- **`assertTrue(...)` / `assertFalse(...)` / `assertEquals(...)`**: To tzw. **asercje**. To one decydują o tym, czy test przeszedł. Jeśli warunek w nawiasie nie zgadza się z oczekiwaniem, test „wybuchnie” na czerwono, informując Cię o błędzie.
    
- **`JUnit`**: To standardowa biblioteka w Javie do pisania takich testów.
    
- **Izolacja**: Każdy test jest niezależny. Dzięki temu, jeśli zepsujesz logikę szukania słów, dowiesz się o tym natychmiast z konkretnego testu, zamiast szukać błędu w całej aplikacji.