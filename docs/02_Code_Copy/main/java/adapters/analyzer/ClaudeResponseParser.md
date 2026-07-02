## Class [[ClaudeResponseParser]] - Parser odpowiedzi Claude AI

Klasa odpowiedzialna za przetwarzanie surowego tekstu JSON otrzymanego z API Claude na ustrukturyzowany obiekt Javy — [[ClaudeTaskSummaryResponse]].

```java
public class ClaudeResponseParser {
    private static final Logger logger = LoggerFactory.getLogger(ClaudeResponseParser.class);
    private final ObjectMapper objectMapper;

    public ClaudeResponseParser() {
        this.objectMapper = new ObjectMapper();
    }

    public ClaudeTaskSummaryResponse parse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.isBlank()) {
            logger.warn("Received empty JSON response, returning fallback");
            return ClaudeTaskSummaryResponse.createFallback();
        }

        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            String summary = extractTextField(rootNode, "summary", "No summary");
            String schedule = extractTextField(rootNode, "schedule", "No schedule");
            List<String> recommendations = extractRecommendations(rootNode);
            List<ClaudeTaskSummaryResponse.TaskUpdate> taskUpdates = extractTaskUpdates(rootNode);

            return ClaudeTaskSummaryResponse.createWithDefaults(
                    summary, schedule, recommendations, taskUpdates
            );
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse JSON response: {}", e.getMessage());
            return ClaudeTaskSummaryResponse.createFallback();
        } catch (Exception e) {
            logger.error("Unexpected error parsing response: {}", e.getMessage(), e);
            return ClaudeTaskSummaryResponse.createFallback();
        }
    }
    
    // ... metody pomocnicze extractTextField, extractTaskUpdates ...
}
```

### Kluczowe cechy i działanie parsera

* **Odporność na błędy (Defensive Programming)**: Parser został zaprojektowany tak, aby aplikacja nigdy nie wyłączyła się z powodu nieprzewidzianej struktury odpowiedzi z AI. Jeśli Claude zwróci uszkodzony JSON, metoda `parse` nie rzuci wyjątku na zewnątrz, lecz zwróci bezpieczny obiekt zastępczy (fallback) poprzez `ClaudeTaskSummaryResponse.createFallback()`.
* **Praca z drzewem JSON (`JsonNode`)**: Zamiast mapować cały tekst na sztywną klasę wprost, parser ładuje go jako drzewo obiektów (`readTree`). Umożliwia to elastyczną, ręczną weryfikację każdego pola i chroni przed błędami w przypadku, gdy AI doda do odpowiedzi jakieś dodatkowe, nieznane nam klucze.
* **Selektywna walidacja elementów**: Podczas analizowania listy aktualizacji zadań (`taskUpdates`), każdy element tablicy jest sprawdzany pod kątem poprawności:
  - Czy posiada unikalne `taskId`?
  - Czy podany priorytet jest zgodny z naszym typem [[Priority]]?
  - Czy szacowany czas (`estimatedHours`) jest liczbą dodatnią?
  Jeśli któryś wpis jest niepoprawny, zostaje zignorowany, a reszta poprawnych zadań jest przetwarzana dalej.

### Pojęcia dla nowicjuszy

* **Parser**: Program lub moduł, który analizuje ciąg znaków (tekst) i przekształca go w strukturę zrozumiałą dla kodu aplikacji (np. obiekty w pamięci).
* **Fallback (Mechanizm awaryjny)**: Domyślne zachowanie lub zestaw danych używany w sytuacji awaryjnej (gdy główny mechanizm zawiedzie), pozwalający na kontynuowanie działania programu.
* **Null-Safety**: Technika pisania kodu w taki sposób, aby unikać słynnego błędu `NullPointerException` (próby odwołania się do nieistniejącego obiektu w pamięci). Parser dba o to, podmieniając wartości `null` na bezpieczne teksty (np. `"No summary"`) lub puste listy.
## 1. Serce programu: Metoda `parse`

To główna metoda, która wykonuje całą pracę. Przyjmuje surowy tekst (`jsonResponse`) i zwraca gotowy obiekt `ClaudeTaskSummaryResponse`.

### Faza 1: Wczesne odrzucanie błędów (Early Return)
```java
if (jsonResponse == null) {
    logger.warn("Received null JSON response, returning fallback");
    return ClaudeTaskSummaryResponse.createFallback();
}
if (jsonResponse.isBlank()) { ... }
```

Zanim kod w ogóle spróbuje cokolwiek analizować, sprawdza, czy w ogóle dostał jakikolwiek tekst. Jeśli odpowiedź jest pusta, funkcja natychmiast się kończy i zwraca tzw. **fallback** (obiekt awaryjny z bezpiecznymi, pustymi danymi).

### Faza 2: Drzewo JSON i wyciąganie danych
```java
try {
    JsonNode rootNode = objectMapper.readTree(jsonResponse);
```

Tutaj Jackson czyta tekst JSON i buduje z niego strukturę drzewa (`JsonNode`), po której można swobodnie nawigować (np. pobierz gałąź "summary").
```java
    String summary = extractTextField(rootNode, "summary", "No summary");
    String schedule = extractTextField(rootNode, "schedule", "No schedule");
    List<String> recommendations = extractRecommendations(rootNode);
    List<ClaudeTaskSummaryResponse.TaskUpdate> taskUpdates = extractTaskUpdates(rootNode);
```

Następnie kod używa specjalnie napisanych metod pomocniczych (omówię je za chwilę), aby bezpiecznie wyciągnąć 4 główne rzeczy. Jeśli w JSON-ie od AI nie będzie np. pola "summary", program nie wybuchnie, tylko wstawi tekst "No summary".

### Faza 3: Bezpieczeństwo i łapanie wyjątków
```java
} catch (JsonProcessingException e) {
    logger.error("Failed to parse JSON response: {}", e.getMessage()); ...
    return ClaudeTaskSummaryResponse.createFallback();
} catch (Exception e) { ... }
```

Całość jest zamknięta w bloku `try-catch`. Jeśli JSON jest uszkodzony (np. brakuje klamry zamykającej `}`), Jackson rzuci błędem `JsonProcessingException`. Klasa łapie ten błąd, zapisuje go w logach (zrzucając nawet pierwsze 200 znaków zepsutego JSON-a, żeby programista mógł to zbadać) i... ponownie zwraca bezpieczny **fallback**, pozwalając aplikacji działać dalej.

## 2. Metody pomocnicze (Pomocnicy od czarnej roboty)

Klasa dzieli skomplikowaną pracę na małe, wyspecjalizowane metody.

### `extractTextField`
```java
private String extractTextField(JsonNode node, String fieldName, String defaultValue)
```

Ta metoda szuka konkretnego pola tekstowego.

1. Sprawdza, czy pole w ogóle istnieje (`fieldNode == null`).
    
2. Sprawdza, czy to na pewno jest tekst, a nie np. liczba albo zagnieżdżony obiekt (`!fieldNode.isTextual()`).
    
3. Jeśli coś jest nie tak, zwraca podaną `defaultValue`. Jeśli jest ok, sprawdza jeszcze, czy tekst nie jest pustymi spacjami (`isBlank()`).
    

### `extractRecommendations`

Szuka w JSON-ie tablicy rekomendacji.

- Jeśli pole `recommendations` nie istnieje lub nie jest tablicą (`!recommendationsNode.isArray()`), zwraca pustą listę `List.of()`.
- Jeśli jest tablicą, iteruje po każdym elemencie. Dodaje go do wynikowej listy _tylko_, jeśli ten element jest tekstem i nie jest pusty. Dzięki temu, jeśli AI przypadkowo wygeneruje listę typu `["Zrób X", null, "", "Zrób Y"]`, program poprawnie odczyta tylko sensowne punkty.

### `extractTaskUpdates` oraz `parseTaskUpdate`

To najciekawsza część biznesowa, w której analizowana jest tablica zadań (Task Updates).

Zadaniem metody `extractTaskUpdates` jest przejście przez całą tablicę zadań z JSON-a i użycie na każdym elemencie metody `parseTaskUpdate`. Co ważne:
```java
try {
    ClaudeTaskSummaryResponse.TaskUpdate update = parseTaskUpdate(updateNode, i);
    if (update != null) { taskUpdates.add(update); }
} catch (Exception e) { ... }
```

Jeśli analiza _jednego_ konkretnego zadania się nie powiedzie, to zadanie jest ignorowane, ale pętla leci dalej! Program ocala te zadania, które są poprawne, odrzucając tylko "zepsute" (częściowy sukces).

**Reguły biznesowe wewnątrz `parseTaskUpdate`:**

1. **Identyfikator (taskId)**: Musi istnieć i nie może być pusty. Jeśli go nie ma, całe zadanie traci sens, więc funkcja zwraca `null` (odrzuca to zadanie).
    
2. **Priorytet (priority)**: Program próbuje go odczytać, a jeśli go brakuje, zakłada domyślnie `"MEDIUM"`. Następnie próbuje go dopasować do zdefiniowanego w systemie wyliczenia (`Priority.fromString`).
    
3. **Szacowany czas (estimatedHours)**: Kod sprawdza, czy pole jest liczbą (`isNumber()`). Narzuca również logiczny warunek: czas musi być większy od zera (`hours > 0`). Jeśli AI poda, że zadanie zajmie -5 godzin, system to zignoruje jako nonsens.
