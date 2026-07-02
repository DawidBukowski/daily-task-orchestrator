## Class [[DirectAnthropicClient]] - Bezpośredni klient API Anthropic

Implementacja interfejsu [[ClaudeApiClient]] łącząca się bezpośrednio z oficjalnym interfejsem HTTP (Messages API) firmy Anthropic przy użyciu nowoczesnego, wbudowanego w Javę 21 klienta HTTP.

```java
public class DirectAnthropicClient implements ClaudeApiClient {
    private static final Logger logger = LoggerFactory.getLogger(DirectAnthropicClient.class);
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final ClaudeConfiguration config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DirectAnthropicClient(ClaudeConfiguration config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String sendMessage(String systemPrompt, String userPrompt) throws ClaudeApiClient.ClaudeApiException {
        try {
            String requestBody = buildRequestBody(systemPrompt, userPrompt);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getAnthropicApiUrl()))
                .header("x-api-key", config.getAnthropicApiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);
        } catch (Exception e) {
            // ... obsługa wyjątków sieciowych i timeoutów ...
        }
    }
}
```

### Działanie i struktura

* **`HttpClient`**: Standardowe narzędzie Javy do wykonywania połączeń sieciowych. Konfigurujemy je z czasem oczekiwania (timeout) ustawionym w [[ClaudeConfiguration]].
* **Budowanie żądania (`buildRequestBody`)**: Używamy biblioteki Jackson (`objectMapper`), aby utworzyć dokument JSON. Zgodnie z wymaganiami Anthropic, żądanie musi zawierać:
  - `"model"` (np. `claude-3-5-sonnet`),
  - `"messages"` (lista wiadomości z rolą `user` i treścią),
  - opcjonalny parametr `"system"` dla instrukcji systemowych.
* **Obsługa odpowiedzi (`handleResponse`)**:
  - Kod `200` oznacza sukces, wyciągamy tekst odpowiedzi z JSON.
  - Kod `401` / `403` oznacza błąd autoryzacji (zły klucz API).
  - Kod `429` to przekroczenie limitu liczby zapytań (Rate Limit).
  - Kody `5xx` to awarie po stronie serwerów Anthropic.
  Wszystkie te sytuacje są tłumaczone na odpowiednie typy wyjątków `ClaudeApiException`.

### Pojęcia dla nowicjuszy

* **HTTP Client**: Programistyczny odpowiednik przeglądarki internetowej. Zamiast klikać myszką, piszemy kod, który wysyła zapytanie pod dany adres URL (tzw. endpoint) i odbiera odpowiedź w formie tekstowej.
* **JSON (JavaScript Object Notation)**: Najpopularniejszy format przesyłania danych w sieci. Dane zapisywane są jako pary "klucz": "wartość" w nawiasach klamrowych (np. `{"name": "Jan"}`).
* **Jackson (`ObjectMapper`)**: Popularna biblioteka w Javie służąca do konwersji (tłumaczenia) obiektów Javy na tekst JSON (serializacja) oraz tekstu JSON z powrotem na obiekty Javy (deserializacja).
* **Nagłówki HTTP (Headers)**: Dodatkowe informacje przesyłane razem z żądaniem HTTP. W nagłówkach przekazujemy m.in. klucz uwierzytelniający (`x-api-key`), aby serwer wiedział, kto wysyła zapytanie.
### 1. Wstrzykiwanie (Dependency Injection)

```java
this.config = config;
```

**Dlaczego z zewnątrz?** Obiekt `config` zawiera dane, które zmieniają się w zależności od środowiska (np. klucze API, URL, timeouty dla środowiska deweloperskiego i produkcyjnego). Konstruktor nie powinien sam z siebie wiedzieć, skąd wziąć te dane (np. czytać z pliku `.properties`), więc żąda ich z zewnątrz. Dzięki temu klasa jest uniwersalna i łatwa do testowania (możesz wstrzyknąć mocka konfiguracji).

### 2. Wzorzec Budowniczego (Builder) + Lokalna Inicjalizacja

```java
this.httpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
    .build();
```

**O co tu chodzi?** Obiekty takie jak `HttpClient` są skomplikowane i mają mnóstwo parametrów opcjonalnych (proxy, certyfikaty, timeouty, wersje protokołu). Tradycyjny konstruktor miałby tu z 10 argumentów, co byłoby nieczytelne. Dlatego twórcy Javy użyli wzorca **Builder** (metody łączone łańcuchowo i kończone `.build()`).

**Dlaczego tworzymy to wewnątrz, a nie wstrzykujemy?** Autor tego kodu uznał, że ten konkretny klient HTTP jest ściśle powiązany z tą klasą i potrzebuje specyficznego timeoutu z `config`. Jednak z punktu widzenia czystej architektury to **podejście dyskusyjne**. Trudniej będzie przetestować tę klasę, bo nie da się łatwo podmienić (zmockować) tego `HttpClienta` – jest on na twardo tworzony w pamięci podczas inicjalizacji `DirectAnthropicClient`.

### 3. Bezpośrednie utworzenie (`new`)

```java
this.objectMapper = new ObjectMapper();
```

**Dlaczego po prostu `new`?** `ObjectMapper` (biblioteka Jackson do obsługi JSON) to potężne narzędzie, ale można je utworzyć jako zwykły, pusty obiekt, jeśli nie potrzebujemy skomplikowanej konfiguracji.

**Gdzie jest haczyk?** W ekosystemie Spring Boota to bardzo często **anty-wzorzec**. Spring ma własnego, globalnie skonfigurowanego `ObjectMapper`a (z obsługą dat z Javy 8, strategiami nazywania zmiennych camelCase -> snake_case, itp.). Robiąc tu `new ObjectMapper()`, tworzysz zupełnie "gołą" instancję, która zignoruje wszystkie globalne ustawienia Twojej aplikacji.

### Jak to powinno wyglądać (Złota zasada)

W nowoczesnym programowaniu (zwłaszcza w architekturze heksagonalnej i przy użyciu frameworków takich jak Spring), dążymy do tego, aby klasy posiadające logikę biznesową lub integracyjną **dostawały wszystko z zewnątrz**.

Gdybyśmy chcieli to zrefaktoryzować na kod łatwiejszy w utrzymaniu i testowaniu (np. w TDD), konstruktor wyglądałby po prostu tak:
```java
public DirectAnthropicClient(ClaudeConfiguration config, HttpClient httpClient, ObjectMapper objectMapper) {
    this.config = config;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
}
```

A cała skomplikowana "budowa" `HttpClient` (to wielolinijkowe cudo z Builderem) znalazłaby się w klasie konfiguracyjnej (np. w metodzie z adnotacją `@Bean` w Springu), która wyprodukowałaby gotowego klienta i wstrzyknęła go tutaj.

## 3. Główna metoda: `sendMessage`

To "serce" całej klasy. Służy do wysyłania promptów do AI.
```java
public String sendMessage(String systemPrompt, String userPrompt) throws ClaudeApiClient.ClaudeApiException {
```

Przyjmuje instrukcje systemowe (`systemPrompt` – np. "Jesteś pomocnym asystentem") i zapytanie użytkownika (`userPrompt`). Może wyrzucić specjalny, niestandardowy wyjątek `ClaudeApiException`.

**Co dzieje się w środku?**

1. **Budowanie zapytania:** Wywołuje metodę `buildRequestBody`, która pakuje teksty w odpowiedniego JSON-a.
    
2. **Konfiguracja żądania HTTP (`HttpRequest`):**
    
    - Ustawia adres URL docelowy (`uri`).
        
    - Dodaje klucz API (`x-api-key`). Bez tego Anthropic odrzuci zapytanie.
        
    - Dodaje nagłówek z wersją API (`anthropic-version`).
        
    - Ustawia `Content-Type: application/json`, informując serwer, że wysyła dane w formacie JSON.
        
    - Ustawia timeout na wypadek, gdyby model Claude przetwarzał odpowiedź zbyt długo.
        
    - Wykonuje metodę **POST**, wysyłając wcześniej zbudowany JSON.
        
3. **Wysyłanie i odbieranie:**
    
    - `httpClient.send(...)` wysyła żądanie do sieci i czeka na odpowiedź.
        
    - Wymusza traktowanie odpowiedzi (body) jako zwykłego tekstu (`BodyHandlers.ofString()`).
        
4. **Bloki `catch` (Obsługa błędów):**
    
    - **`HttpTimeoutException`**: Wyłapuje sytuację, gdy żądanie trwało za długo.
        
    - **`InterruptedException`**: Ktoś (inny wątek) przerwał proces oczekiwania na odpowiedź. Zauważ bardzo ważną i dobrą praktykę: `Thread.currentThread().interrupt()` – kod przywraca flagę przerwania wątku.
        
    - Pozostałe błędy są logowane i pakowane w czytelny wyjątek `ClaudeApiException`.
        

## 4. Metoda budująca JSON-a: `buildRequestBody`

Zamiast sklejać tekst (tzw. string concatenation), programista użył `ObjectNode` z biblioteki Jackson. To bardzo bezpieczne podejście, które gwarantuje, że powstały JSON będzie miał prawidłową strukturę i uniknie problemów ze znakami specjalnymi.

Wygenerowany przez tę metodę dokument JSON wygląda mniej więcej tak:

```json
{
  "model": "claude-3-opus-20240229", 
  "max_tokens": 1024,
  "temperature": 0.7,
  "system": "Twój system prompt (opcjonalny, dodawany tylko jeśli nie jest pusty)",
  "messages": [
    {
      "role": "user",
      "content": "Tutaj jest to, o co zapytał użytkownik"
    }
  ]
}
```

Kod dynamicznie podmienia te wartości, pobierając np. model z obiektu `config`.

## 5. Metoda zarządzająca odpowiedzią: `handleResponse`

Metoda sprawdza **kod statusu HTTP** zwrócony przez serwer Anthropic.

- `statusCode == 200`: Sukces! Serwer poprawnie wygenerował odpowiedź. Wywołuje się wtedy metoda do wyciągania tekstu z tej odpowiedzi.
    
- W przeciwnym razie wyciągany jest szczegółowy komunikat o błędzie z body żądania i następuje sprawdzanie statusów HTTP:
    
    - **401 lub 403**: Brak dostępu. Zły lub nieaktywny klucz API.
        
    - **429**: Zbyt wiele zapytań (Rate Limit). Wyczerpano przydział w danym czasie.
        
    - **500 i więcej**: Błędy krytyczne po stronie serwerów Anthropic (My zrobiliśmy wszystko dobrze, to oni mają awarię).
        
    - **Inne (4xx)**: Błędne zapytanie – np. przekazano złą strukturę JSON (Bad Request).
        

Zamiast rzucać generyczne błędy, programista używa `ErrorType` w klasie `ClaudeApiException`, co potem ułatwia innym częściom programu odpowiednią reakcję (np. próbę wysłania zapytania ponownie w przypadku 429).

## 6. Wyciąganie tekstu z odpowiedzi: `extractTextFromResponse`

Gdy wszystko się uda (status 200), API Anthropic zwraca potężnego JSON-a zawierającego mnóstwo metadanych. Nas jednak interesuje tylko wygenerowany tekst.

Struktura zwracana przez Claude wygląda tak:

```json
{
  "id": "msg_01...",
  "type": "message",
  "content": [
    {
      "type": "text",
      "text": "Odpowiedź sztucznej inteligencji, na którą czekamy!"
    }
  ]
}
```

Metoda ta korzysta z Jacksona:

1. Odczytuje całego JSON-a (`objectMapper.readTree`).
    
2. Pobiera tablicę `"content"`.
    
3. Zabezpiecza się przed błędami (sprawdza, czy tablica nie jest pusta i czy w ogóle istnieje).
    
4. Pobiera z tablicy pierwszy element (indeks `0`).
    
5. Pobiera z tego elementu wartość klucza `"text"` i zwraca ją jako String.
    

## 7. Metoda wyciągania błędu: `extractErrorMessage`

Jeśli coś poszło nie tak i otrzymaliśmy np. błąd 400, Anthropic zwraca JSON o takiej strukturze:

```json
{
  "type": "error",
  "error": {
    "type": "authentication_error",
    "message": "invalid x-api-key"
  }
}
```

Ta metoda "nurkuje" do pola `"error"`, potem do `"message"` i wyciąga dla nas ludzkim językiem napisane: _"invalid x-api-key"_. Jeśli coś by nie zadziałało (np. serwer Anthropic oddał w ogóle zepsutego JSONa), metoda po prostu zwróci całą, surową odpowiedź serwera (z klauzuli `catch`), by nie zgubić kontekstu błędu.

Podsumowując: jest to napisany książkowo, produkcyjny kawałek kodu do komunikacji po REST API. Posiada świetną obsługę wyjątków, prawidłowo interpretuje kody HTTP i bezpiecznie generuje oraz parsuje dokumenty JSON.