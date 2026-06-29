Testy jednostkowe weryfikujące logikę walidacji i inicjalizacji konfiguracji połączenia z Gmail API w klasie [[GmailConfiguration]].

```java
class GmailConfigurationTest {

    @Test
    void constructor_ThrowsException_WhenCredentialsMissing() {
        assertThrows(IllegalStateException.class, () ->
                new GmailConfiguration("", "", "http://localhost", "~/.tokens", List.of())
        );
    }

    @Test
    void constructor_Succeeds_WithValidData() {
        GmailConfiguration config = new GmailConfiguration("id", "secret", "http://local", "~/.tokens", List.of("scope1"));
        assertEquals("id", config.getClientId());

        String expectedPath = System.getProperty("user.home") + "/.tokens";
        assertEquals(expectedPath, config.getTokenDirectory());
    }
}
```

* `constructor_ThrowsException_WhenCredentialsMissing()`: Sprawdza, czy konstruktor chroni przed błędną konfiguracją i rzuca wyjątek `IllegalStateException`, jeśli identyfikator (`clientId`) lub klucz klienta (`clientSecret`) są puste.
* `constructor_Succeeds_WithValidData()`: Weryfikuje poprawne działanie przy poprawnych danych. Szczególną uwagę zwraca się na automatyczne rozwiązywanie ścieżki zapisu tokenów — jeśli ścieżka zaczyna się od tyldy (`~`), system powinien zastąpić ją bezwzględną ścieżką do katalogu domowego użytkownika (`user.home`).
