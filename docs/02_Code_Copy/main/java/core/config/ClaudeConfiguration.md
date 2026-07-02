## Class [[ClaudeConfiguration]] - Konfiguracja integracji z Claude AI

Klasa odpowiedzialna za przechowywanie i walidację parametrów konfiguracyjnych wymaganych do połączenia z modelami językowymi Claude. Obsługuje dwa tryby dostępu (Direct Anthropic API oraz AWS Bedrock).

```java
public class ClaudeConfiguration {

    public enum Provider {
        ANTHROPIC,
        AWS_BEDROCK
    }

    private final Provider provider;
    private final String modelId;
    private final int maxTokens;
    private final double temperature;
    private final int timeoutSeconds;

    // Anthropic-specific configuration
    private final String anthropicApiKey;
    private final String anthropicApiUrl;

    // AWS-specific configuration
    private final String awsRegion;

    private ClaudeConfiguration(Builder builder) {
        this.provider = Objects.requireNonNull(builder.provider, "Provider cannot be null");
        this.modelId = Objects.requireNonNull(builder.modelId, "Model ID cannot be null");
        this.maxTokens = builder.maxTokens;
        this.temperature = builder.temperature;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.anthropicApiKey = builder.anthropicApiKey;
        this.anthropicApiUrl = builder.anthropicApiUrl;
        this.awsRegion = builder.awsRegion;

        validate();
    }

    // ... metoda fromEnv() i validate() ...
    
    public static class Builder {
        // ... pola i metody buildera ...
    }
}
```

### Funkcje i działanie klasy

* **`Provider` (Dostawca)**: Typ wyliczeniowy określający, w jaki sposób program łączy się z Claude:
  - `ANTHROPIC` - Bezpośrednie połączenie HTTP z API Anthropic.
  - `AWS_BEDROCK` - Korzystanie z usługi chmurowej Amazon Web Services Bedrock.
* **`fromEnv()`**: Statyczna metoda ładująca konfigurację ze zmiennych środowiskowych systemu. Odczytuje klucze takie jak `CLAUDE_PROVIDER`, `CLAUDE_MODEL_ID`, `ANTHROPIC_API_KEY` czy `AWS_REGION`.
* **`validate()`**: Metoda wywoływana w konstruktorze, która upewnia się, że podane parametry są logiczne i poprawne (np. temperatura mieści się w przedziale 0.0 - 1.0, a wymagane zmienne środowiskowe dla wybranego dostawcy nie są puste). Jeśli walidacja nie przejdzie, rzuca błąd `IllegalStateException`.

### Pojęcia dla nowicjuszy

* **Zmienne środowiskowe (Environment Variables)**: Specjalne zmienne przechowywane w systemie operacyjnym (np. Windows/Linux), a nie bezpośrednio w kodzie programu. Jest to standardowa i bezpieczna metoda przekazywania haseł i kluczy API (`System.getenv(...)`), aby nie trafiły one przez przypadek na np. publicznego GitHub-a.
* **Wzorzec Projektowy Builder (Budowniczy)**: Konstruktory klas z wieloma parametrami bywają nieczytelne i podatne na pomyłki (łatwo pomylić kolejność argumentów). Klasa `Builder` pozwala tworzyć obiekt krok po kroku, w czytelny sposób (np. `new Builder().provider(Provider.ANTHROPIC).modelId("claude-3").build()`).
* **Enkapsulacja (Ukrywanie informacji)**: Wszystkie pola w tej klasie są oznaczone jako `private final`, co oznacza, że po utworzeniu obiektu nie można ich zmodyfikować z zewnątrz. Klasa udostępnia jedynie metody pobierające wartości (getters), gwarantując pełne bezpieczeństwo danych.
### 1. Konstruktor

```java
    private ClaudeConfiguration(Builder builder) {
        this.provider = Objects.requireNonNull(builder.provider, "Provider cannot be null");
        this.modelId = Objects.requireNonNull(builder.modelId, "Model ID cannot be null");
        this.maxTokens = builder.maxTokens;
        // ... (przypisanie reszty zmiennych)
        this.awsRegion = builder.awsRegion;

        validate();
    }
```

- **`private`**: Konstruktor jest prywatny! Oznacza to, że nikt z zewnątrz nie może zrobić `new ClaudeConfiguration(...)`. Aby stworzyć tę klasę, trzeba użyć wewnętrznej klasy `Builder` (o czym później).
    
- Jako argument przyjmuje obiekt `Builder` i z niego przepisuje wartości do swoich własnych pól `final`.
    
- **`Objects.requireNonNull(...)`**: To wbudowane narzędzie Javy, które rzuca błąd (wyjątek `NullPointerException`), jeśli podana wartość nie istnieje (jest `null`). Zabezpiecza to przed stworzeniem wybrakowanej konfiguracji.
    
- **`validate()`**: Na samym końcu konstruktora wywoływana jest metoda sprawdzająca poprawność. Jeśli coś jest nie tak, proces tworzenia obiektu zostaje przerwany.

### 2. Metoda `validate()` (Zasada Fail-fast)

```java
    private void validate() {
        if (modelId.isBlank()) {
            throw new IllegalStateException("Model ID cannot be blank");
        }
        // ... (reszta sprawdzeń)
```

Ta metoda to tzw. _Fail-fast_ (zepsuj się szybko). Chodzi o to, że jeśli konfiguracja jest zła (np. brakuje klucza API, temperatura jest poza skalą 0.0-1.0), to chcemy, aby program wyrzucił błąd **od razu** przy uruchamianiu, a nie dopiero po 10 minutach, gdy będzie próbował wysłać zapytanie do AI.

- Jeśli używasz `ANTHROPIC`, musisz mieć wpisany klucz API. Jeśli `AWS_BEDROCK`, musisz mieć wpisany region (np. "us-east-1").
    
- `throw new IllegalStateException(...)`: Jeśli zasada jest złamana, rzucamy wyjątek oznaczający "Ten obiekt jest w nielegalnym/nieprawidłowym stanie".

### 3. Metoda Fabrykująca `fromEnv()` (Główna logika pobierania)

```java
    public static ClaudeConfiguration fromEnv() {
        String providerStr = System.getenv("CLAUDE_PROVIDER");
        // ...
```

To tak zwana _metoda fabrykująca (factory method)_.

- **`public static`**: Można ją wywołać bezpośrednio na klasie: `ClaudeConfiguration.fromEnv()`.
    
- **`System.getenv("NAZWA")`**: Służy do pobierania _zmiennych środowiskowych_ z systemu operacyjnego lub kontenera (np. Docker). To standardowy i najbezpieczniejszy sposób przekazywania haseł i kluczy API do aplikacji.
    
- **Magia z Enumem**: `Provider.valueOf(providerStr.toUpperCase())` - bierze tekst z systemu (np. "anthropic"), zamienia na duże litery ("ANTHROPIC") i przypisuje do naszego bezpiecznego Enuma. Jak tekst będzie zły, aplikacja "wybuchnie" rzucając błąd.


```java
        Builder builder = new Builder()
            .provider(provider)
            .modelId(modelId)
            .maxTokens(getEnvInt("CLAUDE_MAX_TOKENS", 1000))
            // ...
```

Tutaj klasa zaczyna korzystać z naszego `Buildera`, ustawiając po kolei wartości. Zauważ użycie funkcji pomocniczych (np. `getEnvInt`), które odczytują wartość ze zmiennych środowiskowych, a jeśli jej nie ma, wstawiają wartość domyślną (np. `1000` dla tokenów).

### 4. Funkcje pomocnicze dla zmiennych środowiskowych

```java
    private static int getEnvInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) { return defaultValue; }
        try { return Integer.parseInt(value); } 
        catch (NumberFormatException e) { ... }
    }
```

- Te funkcje (`getEnvOrDefault`, `getEnvInt`, `getEnvDouble`) pomagają zachować czystość kodu (zasada DRY - Don't Repeat Yourself).
    
- Próbują pobrać tekstową zmienną środowiskową. Jeśli jest pusta, oddają `defaultValue`.
    
- Jeśli nie jest pusta, próbują zamienić tekst (np. "100") na liczbę (`Integer.parseInt`). Jeśli ktoś w środowisku wpisał "sto" zamiast "100", kod to złapie w bloku `catch` i rzuci czytelny błąd.

### 5. Gettery

```java
    public Provider getProvider() { return provider; }
    // ...
```

Skoro nasze zmienne w sekcji 2. były `private`, z zewnątrz nie można ich odczytać. _Gettery_ to proste publiczne metody, które służą **tylko do odczytu** tych wartości. Nie ma tu _Setterów_ (metod typu `setProvider(...)`), co podtrzymuje naszą zasadę niezmienności obiektu (Immutability).

### 6. Wzorzec Budowniczego (Builder Pattern) - Majstersztyk tego kodu

```java
    public static class Builder {
        private Provider provider;
        private String modelId;
        private int maxTokens = 1000; // domyślna wartość
        // ...
        
        public Builder provider(Provider provider) {
            this.provider = provider;
            return this;
        }
        
        // ... reszta metod buildera ...

        public ClaudeConfiguration build() {
            return new ClaudeConfiguration(this);
        }
    }
```

Dlaczego stosuje się klasę wewnątrz klasy (`public static class Builder`)? Wyobraź sobie, że nie ma Buildera. Mamy 8 parametrów. Jak wyglądałoby tworzenie obiektu? `new ClaudeConfiguration(Provider.ANTHROPIC, "claude-3", 1000, 0.5, 30, "moj_klucz", "url", null);` To jest koszmar. Łatwo pomylić kolejność argumentów (np. wpisać temperaturę w miejsce tokenów).

Builder rozwiązuje ten problem tworząc tzw. _Fluent API_ (płynny interfejs).

- Zauważ, że metody w Builderze robią: `this.provider = provider;` a potem **`return this;`**.
    
- Zwracanie samego siebie (`this`) pozwala na łączenie wywołań w taki piękny łańcuch:

```java
// Tak wyglądałoby ręczne tworzenie konfiguracji dzięki Builderowi:
ClaudeConfiguration config = new ClaudeConfiguration.Builder()
    .provider(Provider.ANTHROPIC)
    .modelId("claude-3-opus")
    .maxTokens(2000)
    .build(); // Ostatecznie wywołuje konstruktor i metodę validate()
```
