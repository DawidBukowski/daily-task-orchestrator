## Test [[ClaudeConfigurationTest]] - Testy konfiguracji Claude

Klasa testowa sprawdzająca poprawność wczytywania i walidacji konfiguracji integracji z Claude w klasie [[ClaudeConfiguration]]. Testuje zachowanie wzorca Builder (Budowniczy) oraz poprawność rzucania wyjątków walidacyjnych.

```java
class ClaudeConfigurationTest {

    @AfterEach
    void tearDown() {
        // Czyszczenie zmiennych środowiskowych po każdym teście
        clearEnvironmentVariable("CLAUDE_PROVIDER");
        clearEnvironmentVariable("CLAUDE_MODEL_ID");
        // ...
    }

    @Test
    void builder_shouldCreateValidAnthropicConfiguration() {
        ClaudeConfiguration config = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId("claude-3-5-sonnet-20241022")
            .maxTokens(2000)
            .temperature(0.7)
            .timeoutSeconds(60)
            .anthropicApiKey("test-key")
            .anthropicApiUrl("https://api.anthropic.com/v1/messages")
            .build();

        assertEquals(ClaudeConfiguration.Provider.ANTHROPIC, config.getProvider());
        assertEquals("claude-3-5-sonnet-20241022", config.getModelId());
        assertEquals(2000, config.getMaxTokens());
        assertEquals(0.7, config.getTemperature());
    }
}
```

### Struktura i scenariusze testowe

* **Sprzątanie stanu (`@AfterEach`)**: Ponieważ niektóre testy modyfikują globalny stan zmiennych środowiskowych systemu (przy użyciu refleksji), metoda `tearDown` oznaczona `@AfterEach` dba o ich wyczyszczenie po każdym teście. Dzięki temu testy nie wpływają na siebie nawzajem (zachowanie zasady **izolacji**).
* **Testowanie ograniczeń walidacyjnych**: Testy weryfikują restrykcyjne reguły biznesowe konfiguracji, upewniając się, że wywołanie budowania (`builder::build`) zgłosi błąd `IllegalStateException` w przypadku podania:
  - Ujemnej liczby tokenów wejściowych (`maxTokens(-1)`),
  - Temperatury spoza dozwolonego zakresu 0.0 - 1.0 (np. `1.1` lub `-0.1`),
  - Czasu oczekiwania mniejszego bądź równego zero (`timeoutSeconds(0)`),
  - Brakującego klucza API dla Anthropic lub regionu dla AWS.
* **Testowanie wartości domyślnych (Defaults)**: Test `builder_shouldUseDefaultValues` upewnia się, że niepodanie opcjonalnych parametrów zainicjalizuje je bezpiecznymi, domyślnymi wartościami (np. 1000 tokenów, temperatura 0.3, timeout 30s).

### Pojęcia dla nowicjuszy

* **`@AfterEach` / TearDown**: Adnotacja JUnit 5 określająca metodę uruchamianą automatycznie po wykonaniu każdego testu jednostkowego. Służy do zwalniania zasobów, zamykania połączeń lub czyszczenia tymczasowego stanu, co zapobiega zjawisku tzw. "test leaks" (przeciekania stanu między testami).
* **Negative Testing (Testowanie negatywne)**: Weryfikacja reakcji systemu na niepoprawne, zniekształcone dane wejściowe. Dobry program nie tylko działa poprawnie przy poprawnych danych, ale też w sposób przewidywalny i kontrolowany zgłasza błędy w przypadku niepoprawnych danych.
* **`@Disabled`**: Adnotacja pozwalająca wyłączyć (zignorować) dany test podczas uruchamiania całego zestawu testów. Stosowana m.in. dla testów wymagających specyficznej konfiguracji systemu operacyjnego lub środowiska zewnętrznego.
