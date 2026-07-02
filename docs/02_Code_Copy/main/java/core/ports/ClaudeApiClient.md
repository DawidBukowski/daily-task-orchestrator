## Interface [[ClaudeApiClient]] - Port klienta Claude AI

Interfejs pełniący rolę portu wyjściowego (outbound port) w architekturze heksagonalnej (Ports and Adapters). Definiuje on abstrakcyjny kontrakt dla komunikacji z modelami językowymi Claude AI od firmy Anthropic.

```java
public interface ClaudeApiClient {

    String sendMessage(String systemPrompt, String userPrompt) throws ClaudeApiException;

    class ClaudeApiException extends Exception {

        public enum ErrorType {
            AUTHENTICATION_FAILED,
            RATE_LIMIT_EXCEEDED,
            SERVER_ERROR,
            TIMEOUT,
            MALFORMED_RESPONSE,
            NETWORK_ERROR,
            INVALID_REQUEST
        }

        private final ErrorType errorType;
        private final int statusCode;

        // ... konstruktory ...

        public ErrorType getErrorType() {
            return errorType;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
```

### Dlaczego ta klasa istnieje i jak działa?

W architekturze aplikacji nie chcemy, aby rdzeń biznesowy (use cases) bezpośrednio zależał od konkretnego sposobu łączenia się z Claude AI (np. przez AWS Bedrock czy bezpośrednio przez HTTP). 
Dlatego tworzymy **port** (interfejs) `ClaudeApiClient`, a szczegóły implementacji przenosimy do **adapterów** (np. [[DirectAnthropicClient]] i [[AwsBedrockClaudeClient]]).

* **`sendMessage(String, String)`**: Główna i jedyna metoda interfejsu. Przyjmuje dwa parametry:
  - `systemPrompt` (instrukcje systemowe definiujące zachowanie sztucznej inteligencji),
  - `userPrompt` (dane użytkownika, które mają zostać poddane analizie).
  Zwraca odpowiedź tekstową z modelu AI lub rzuca dedykowany wyjątek `ClaudeApiException`.

* **`ClaudeApiException`**: Dedykowana klasa wyjątku (checked exception). Każdy błąd komunikacji (brak prądu, zły klucz API, przekroczenie limitów zapytań) jest tłumaczony na ten wyjątek, co ułatwia jego obsługę na wyższych warstwach aplikacji.
  - Wyjątek przechowuje `ErrorType` (typ błędu) oraz `statusCode` (kod statusu HTTP), co pozwala na precyzyjną reakcję (np. ponowienie próby przy timeouts).

### Pojęcia dla nowicjuszy

* **Interfejs (Interface)**: Nazwijmy to "umową prawną" dla klas. Interfejs nie zawiera gotowego kodu, a jedynie spis metod, jakie klasa go implementująca musi posiadać. Dzięki temu reszta aplikacji wie, że może wywołać `sendMessage()`, nie interesując się, czy pod spodem działa AWS, czy Anthropic.
* **Checked Exception (Wyjątek sprawdzany)**: Wyjątek dziedziczący bezpośrednio po klasie `Exception`. Kompilator Javy zmusza nas do obsłużenia go w bloku `try-catch` lub przekazania wyżej za pomocą `throws`. Chroni to aplikację przed nagłym wyłączeniem się w razie awarii sieci.
* **Nested Class (Klasa wewnętrzna/zagnieżdżona)**: Klasa zdefiniowana wewnątrz innej klasy lub interfejsu. Tutaj `ClaudeApiException` jest zagnieżdżona w `ClaudeApiClient`, ponieważ ten wyjątek jest ściśle związany i sensowny tylko w kontekście tego klienta API.
