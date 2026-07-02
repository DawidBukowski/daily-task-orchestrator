## Record [[ClaudeTaskSummaryResponse]] - Struktura odpowiedzi analizy zadań

Niezmienny obiekt transferu danych (DTO), który reprezentuje pełne, ustrukturyzowane podsumowanie planu dnia wygenerowane przez Claude AI.

```java
public record ClaudeTaskSummaryResponse(
        String summary,
        String schedule,
        List<String> recommendations,
        List<TaskUpdate> taskUpdates
) {

    public record TaskUpdate(
            String taskId,
            String priority,
            Double estimatedHours,
            String notes
    ) {}

    public static ClaudeTaskSummaryResponse createFallback() {
        return new ClaudeTaskSummaryResponse(
                "No summary available",
                "No schedule available",
                List.of(),
                List.of()
        );
    }

    public static ClaudeTaskSummaryResponse createWithDefaults(
            String summary,
            String schedule,
            List<String> recommendations,
            List<TaskUpdate> taskUpdates
    ) {
        return new ClaudeTaskSummaryResponse(
                summary != null && !summary.isBlank() ? summary : "No summary",
                schedule != null && !schedule.isBlank() ? schedule : "No schedule",
                recommendations != null ? List.copyOf(recommendations) : List.of(),
                taskUpdates != null ? List.copyOf(taskUpdates) : List.of()
        );
    }
}
```

### Struktura i metody

* **`ClaudeTaskSummaryResponse`**: Główny rekord zawierający:
  - Ogólny tekst podsumowania zadań (`summary`),
  - Rekomendowany harmonogram pracy (`schedule`),
  - Listę wskazówek i porad (`recommendations`),
  - Listę obiektów z propozycjami zmian w zadaniach (`taskUpdates`).
* **`TaskUpdate`**: Zagnieżdżony rekord reprezentujący sugestię zmiany dla pojedynczego zadania. Zawiera identyfikator zadania (`taskId`), nowy priorytet (`priority`), szacowany czas trwania w godzinach (`estimatedHours`) oraz komentarz wyjaśniający zmianę (`notes`).
* **`createFallback()`**: Tworzy bezpieczny obiekt z domyślnymi tekstami informującymi o braku danych, w przypadku całkowitego błędu parsowania JSON.
* **`createWithDefaults(...)`**: Tworzy obiekt dbając o to, by żadne z pól nie było puste ani równe `null`. Zamiast wartości `null`, podstawia puste listy (`List.of()`) lub teksty domyślne.

### Pojęcia dla nowicjuszy

* **Rekord (Record)**: Specjalny rodzaj klasy w języku Java (wprowadzony na stałe od wersji 16). Służy do bardzo szybkiego i zwięzłego definiowania klas, które mają tylko przechowywać dane. Kompilator automatycznie tworzy dla nas konstruktor, gettery (np. `response.summary()`), oraz metody takie jak `equals()`, `hashCode()` i `toString()`. Pola rekordu są zawsze niezmienne (`private final`).
* **DTO (Data Transfer Object)**: Obiekt, którego jedynym zadaniem jest przesyłanie danych pomiędzy różnymi częściami systemu (np. z zewnętrznego API do wnętrza naszej aplikacji). Nie zawiera on żadnej logiki biznesowej, a jedynie surowe informacje.
* **Kopia defensywna (`List.copyOf`)**: Metoda tworząca nową, niezależną i niemodyfikowalną kopię przekazanej listy. Chroni to dane przed przypadkową zmianą z zewnątrz w późniejszym czasie.
