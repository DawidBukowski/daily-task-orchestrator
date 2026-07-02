### 1. Stałe i Pola Klasy (Zmienne)

```java
private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
```

- **`Pattern EMAIL_PATTERN`**: To wyrażenie regularne (Regex) służące do sprawdzania, czy adres email ma poprawny format. Wyrażenie to w uproszczeniu oznacza: _ciąg znaków bez spacji i znaku '@' + znak '@' + ciąg znaków bez spacji i '@' + kropka + ciąg znaków bez spacji i '@'_.
- `static final`: Oznacza to, że jest to stała, przypisana do samej klasy (nie do konkretnego obiektu) i nigdy nie zmieni swojej wartości. Zostaje skompilowana raz, co oszczędza pamięć.

**Pola konfiguracyjne:**

```java
private final String smtpHost;
private final int smtpPort;
// ... pozostałe pola
```

- Wszystkie pola przechowujące dane (host, port, hasło, emaile itd.) są oznaczone jako `private final`.
- `private` chroni je przed zmianą z zewnątrz.
- `final` oznacza, że **po utworzeniu obiektu, jego stan nie może ulec zmianie**. Obiekt jest "niemutowalny" (immutable). To ogromna zaleta w aplikacjach wielowątkowych, ponieważ masz pewność, że żadna inna część programu nagle nie zmieni np. portu SMTP w trakcie wysyłania maila.

### 2. Konstruktor i Zasada "Fail-Fast"

```java
private EmailConfiguration(Builder builder) {
    this.smtpHost = Objects.requireNonNull(builder.smtpHost, "SMTP host cannot be null");
    // ...
    validate();
}
```

- **Prywatny konstruktor (`private`)**: Zauważ, że konstruktor jest prywatny! Oznacza to, że nikt z zewnątrz nie może zrobić `new EmailConfiguration(...)`. Obiekt można stworzyć **tylko** za pomocą klasy `Builder` (o której za chwilę) lub metody `fromEnv()`.
- **`Objects.requireNonNull(...)`**: Kod od razu sprawdza, czy kluczowe dane nie są puste (`null`). Jeśli ktoś spróbuje przekazać `null`, program natychmiast wyrzuci błąd `NullPointerException`. To tzw. zasada **Fail-Fast** – lepiej, żeby program "wybuchnął" od razu przy tworzeniu konfiguracji, niż żeby wyrzucił trudny do zdiagnozowania błąd dopiero podczas próby wysłania maila.
- Na koniec konstruktor wywołuje metodę `validate()`.

### 3. Metoda `validate()`

```java
private void validate() {
    if (smtpHost.isBlank()) {
        throw new IllegalStateException("EMAIL_SMTP_HOST cannot be blank");
    }
    if (smtpPort < 1 || smtpPort > 65535) { ... }
    if (!EMAIL_PATTERN.matcher(fromEmail).matches()) { ... }
    // ...
}
```

Tutaj odbywa się właściwa walidacja logiki biznesowej:

- Sprawdza, czy teksty nie są puste lub nie składają się z samych spacji (`isBlank()`).
- Sprawdza, czy port SMTP mieści się w standardowym zakresie portów sieciowych (od 1 do 65535).
- Sprawdza, czy emaile nadawcy i odbiorcy pasują do naszego wyrażenia regularnego `EMAIL_PATTERN` (korzystając z metody `matcher().matches()`).
- Sprawdza, czy czas oczekiwania (timeout) jest większy od zera. Jeśli cokolwiek jest nie tak, rzuca błąd `IllegalStateException`, przerywając działanie aplikacji i wyraźnie mówiąc, co zostało źle skonfigurowane.

### 4. Konstruowanie z Zmiennych Środowiskowych (`fromEnv`)

```java
public static EmailConfiguration fromEnv() {
    String smtpHost = System.getenv("EMAIL_SMTP_HOST");
    // ... sprawdzanie czy nie jest null lub puste
```

Jest to tzw. **Statyczna Metoda Fabrykująca** (Static Factory Method). Służy do automatycznego zbudowania obiektu `EmailConfiguration` na podstawie tzw. zmiennych środowiskowych systemu (Environment Variables).

- Używa `System.getenv("NAZWA_ZMIENNEJ")` do odczytania konfiguracji (często używane w aplikacjach uruchamianych w Dockerze, na serwerach chmurowych czy w systemach CI/CD).
- Metoda pobiera wszystkie wymagane zmienne, a jeśli jakiejś brakuje – od razu rzuca błąd.
- Na samym końcu używa `Buildera` by złożyć pobrane wartości w gotowy obiekt i zwraca go: `return new Builder().smtpHost(...).build();`

**Metody pomocnicze (parsowanie):**

```java
private static int getEnvInt(String key, int defaultValue) { ... }
private static int getEnvInt(String key, String value) { ... }
private static boolean getEnvBoolean(String key, boolean defaultValue) { ... }
```

Zmienne środowiskowe zawsze są tekstami (`String`). Jeśli potrzebujemy liczby całkowitej (port) lub wartości logicznej (prawda/fałsz dla włączenia TLS), musimy ten tekst przekonwertować.

- Metody te próbują zmienić tekst na liczbę (`Integer.parseInt`) lub boolean (`Boolean.parseBoolean`).
- Dodatkowo obsługują **wartości domyślne** (np. jeśli użytkownik nie zdefiniuje timeoutu w systemie, program ustawi domyślnie 30000 milisekund).
- Mamy dwie wersje `getEnvInt` (zjawisko tzw. **przeciążania metod**). Jedna pobiera zmienną sama z systemu, a druga przyjmuje już odczytany `String` jako argument. Obie dbają o to, by w przypadku błędu (np. port to "XYZ" zamiast "587") wyrzucić zrozumiały komunikat (`NumberFormatException`).
### Metoda do zmiennych OPCJONALNYCH (z wartością domyślną)

```java
private static int getEnvInt(String key, int defaultValue) { ... }
```

Ta metoda w argumentach przyjmuje nazwę klucza oraz `int defaultValue` (wartość domyślną). W kodzie jest ona używana dla zmiennej `EMAIL_TIMEOUT_MS`:

```java
.timeoutMs(getEnvInt("EMAIL_TIMEOUT_MS", 30000))
```

**Jak działa?** Idzie do systemu, szuka zmiennej. Jeśli zmiennej tam nie ma lub jest pusta, **nie wyrzuca błędu**, tylko po cichu zwraca bezpieczną wartość domyślną (np. `30000`). Jeśli zmienna jest, próbuje zamienić jej tekst na liczbę.

### Metoda do zmiennych WYMAGANYCH (bez wartości domyślnej)

```java
private static int getEnvInt(String key, String value) { ... }
```

Ta metoda w argumentach przyjmuje nazwę klucza oraz **już wcześniej pobraną wartość** w postaci tekstu (`String value`). Jest ona używana dla zmiennej `EMAIL_SMTP_PORT`.

Zauważ, co dzieje się na samym początku metody `fromEnv()`:

```java
String smtpPortStr = System.getenv("EMAIL_SMTP_PORT");
if (smtpPortStr == null || smtpPortStr.isBlank()) {
    throw new IllegalStateException("EMAIL_SMTP_PORT environment variable is required");
}
```

Zmienna portu jest **wymagana**, więc program od razu ją pobiera i sprawdza, czy istnieje. Jeśli jej nie ma – przerywa działanie z komunikatem, że port jest wymagany.

Skoro tekst został już pobrany z systemu (zapisany w `smtpPortStr`) i wiemy, że nie jest pusty, używamy tej drugiej metody do zrobienia z nim porządku podczas budowania obiektu:

```java
.smtpPort(getEnvInt("EMAIL_SMTP_PORT", smtpPortStr))
```

**Jak działa?** Nie odpytuje ponownie systemu (`System.getenv`), bo wartość została już pobrana. Nie ma tu też żadnej wartości domyślnej. Jej jedynym zadaniem jest próba zamiany tekstu (np. `"587"`) na liczbę typu `int` (`587`) oraz ładne opakowanie ewentualnego błędu w `IllegalStateException`, jeśli ktoś zamiast portu wpisał w konfiguracji np. słowo `"port_pięćset"`.

### Podsumowując: Dlaczego nie jedna metoda?

Gdybyśmy mieli **tylko pierwszą metodę** (z wartością domyślną), trudno byłoby nam wymusić podanie wymaganego portu. Musielibyśmy podawać fałszywą wartość domyślną (np. `-1`), a potem pisać dodatkowe linijki sprawdzające, czy port nie jest przypadkiem równy `-1`.

Gdybyśmy mieli **tylko drugą metodę**, nie moglibyśmy łatwo obsługiwać zmiennych opcjonalnych. Przy każdym parametrze opcjonalnym (jak timeout) musielibyśmy ręcznie pisać instrukcje `if` sprawdzające, czy tekst jest `null`, zanim w ogóle spróbowalibyśmy zamienić go na liczbę.

### 5. Gettery

```java
public String getSmtpHost() { return smtpHost; }
public boolean isEnableTls() { return enableTls; }
```

Klasa nie posiada Setterów (metod typu `setSmtpHost`), bo – jak ustaliliśmy wcześniej – jest niemutowalna. Zamiast tego ma same Gettery, które pozwalają **tylko na odczyt** danych. Zwróć uwagę, że dla zmiennych typu `boolean` (prawda/fałsz), w Javie tradycyjnie getter zaczyna się od słowa `is` (np. `isEnableTls()`), a nie `get`.

### 6. Wzorzec Projektowy Builder (Klasa zagnieżdżona `Builder`)

```java
public static class Builder {
    private String smtpHost;
    private int smtpPort = 587; // Wartość domyślna
    // ...
    
    public Builder smtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
        return this;
    }
    // ...
    public EmailConfiguration build() {
        return new EmailConfiguration(this);
    }
}
```

Na samym dole znajduje się tzw. statyczna klasa zagnieżdżona (Static Nested Class). To serce wzorca **Builder**. Dlaczego się go używa?

Gdy klasa ma dużo pól konfiguracyjnych (tutaj aż 9!), tworzenie obiektu zwykłym konstruktorem byłoby koszmarem: `new EmailConfiguration("smtp.wp.pl", 465, "user", "pass", "a@a.pl", "b@b.pl", true, true, 30000)` – łatwo pomylić kolejność parametrów!

**Builder rozwiązuje ten problem:**

1. Klasa `Builder` ma te same pola co klasa docelowa, ale nie są one `final` (można je zmieniać podczas budowania). Posiada też od razu przypisane sensowne wartości domyślne (np. port 587).
2. Metody do ustawiania wartości (np. `smtpHost()`) przypisują wartość i **zwracają sam obiekt Buildera (`return this;`)**. Dzięki temu można łączyć metody w czytelny łańcuch (tzw. _fluent interface_).
3. Na koniec wywołujemy metodę `build()`, która wywołuje ten ukryty, prywatny konstruktor klasy `EmailConfiguration`, przekazując mu gotowego Buildera (`new EmailConfiguration(this)`).

**Przykład, jak używa się tego Buildera w praktyce:**

```java
EmailConfiguration config = new EmailConfiguration.Builder()
    .smtpHost("smtp.gmail.com")
    .username("moj.email@gmail.com")
    .password("mojetajnehaslo")
    .fromEmail("moj.email@gmail.com")
    .toEmail("odbiorca@test.pl")
    .build();
```