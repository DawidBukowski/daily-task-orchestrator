Testy jednostkowe sprawdzające reguły wyodrębniania tytułu zadania za pomocą [[TitleExtractor]].

```java
class TitleExtractorTest {

    private TitleExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new TitleExtractor();
    }

    @Test
    void testExtractTitle_fromRawDataTitle() {
        RawData rawData = new RawData(
            "Gmail",
            "Assignment 5: Data Structures",
            "Body content",
            LocalDateTime.now(),
            "sender@example.com",
            "msg-123",
            "HIGH",
            Collections.emptyMap()
        );

        String title = extractor.extractTitle(rawData);
        assertEquals("Assignment 5: Data Structures", title);
    }

    @Test
    void testExtractTitle_fallbackToFirst50Characters() {
        String longContent = "This is a very long piece of text that does not match any specific pattern but should be truncated";
        RawData rawData = new RawData(
            "Gmail",
            "",
            longContent,
            LocalDateTime.now(),
            "sender@example.com",
            "msg-123",
            "LOW",
            Collections.emptyMap()
        );

        String title = extractor.extractTitle(rawData);
        assertTrue(title.length() <= 53); // 50 + "..."
        assertTrue(title.endsWith("..."));
    }
}
```

* `testExtractTitle_fromRawDataTitle()`: Sprawdza standardową logikę: jeśli obiekt [[RawData]] posiada już określony tytuł (pochodzący np. z tematu wiadomości e-mail), ekstraktor powinien go bezpośrednio zwrócić.
* `testExtractTitle_fallbackToFirst50Characters()`: Testuje mechanizm rezerwowy (fallback). Gdy brak jest tematu wiadomości oraz żadne wzorce tekstowe nie pasują, ekstraktor obcina treść wiadomości do 50 znaków i dodaje wielokropek na końcu.
* Klasa testuje również dopasowywanie specyficznych wzorców w treści wiadomości (wielkość liter ignorowana):
    - Wzorce zadań domowych typu `Assignment X` (`testExtractTitle_assignmentPattern()`).
    - Wzorce projektowe typu `Project: ...` (`testExtractTitle_projectPattern()`).
    - Wzorce kartkówek/testów typu `Quiz ...` (`testExtractTitle_quizPattern()`).
* Testy weryfikują zachowanie dla przypadków brzegowych, w tym zachowanie przy przekazaniu obiektu `null` lub pustego tematu/treści (oczekiwany wynik to „Untitled Task”), a także ignorowanie bezużytecznych tytułów zastępczych, takich jak `(no title)`.
