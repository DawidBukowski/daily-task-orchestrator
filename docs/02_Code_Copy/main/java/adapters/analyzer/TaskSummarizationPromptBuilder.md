## Class [[TaskSummarizationPromptBuilder]] - Kreator promptów dla Claude AI

Klasa odpowiedzialna za generowanie i formatowanie instrukcji (promptów) wysyłanych do modeli Claude. Odpowiada zarówno za zdefiniowanie roli sztucznej inteligencji (system prompt), jak i sformatowanie danych o zadaniach (user prompt).

```java
public class TaskSummarizationPromptBuilder {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd (EEEE)");

    public String buildSystemPrompt() {
        return """
            You are a task prioritization and scheduling expert. Your role is to analyze a list of tasks
            and provide actionable insights to help the user manage their workload effectively.

            You MUST respond with valid JSON matching this exact schema:
            ...
            """;
    }

    public String buildUserPrompt(List<Task> tasks, LocalDate today) {
        // ... walidacja i budowanie tekstu promptu ...
    }
}
```

### Funkcje i działanie klasy

* **`buildSystemPrompt()`**: Zwraca zestaw instrukcji systemowych. Nakazuje modelowi językowemu wejście w rolę eksperta ds. produktywności oraz **bezwzględne** zwrócenie odpowiedzi w formacie JSON zgodnym z podanym schematem (JSON Schema). Narzuca surowe zasady, takie jak zachowanie identycznych identyfikatorów zadań (`taskId`) czy ograniczenie priorytetów do konkretnych słów (CRITICAL, HIGH, MEDIUM, LOW).
* **`buildUserPrompt(...)`**: Zwraca treść zapytania użytkownika. Przekazuje aktualną datę (niezbędną dla Claude do określenia, które zadania są pilne) oraz iteruje przez przekazaną listę zadań [[Task]], zamieniając je na czytelną listę tekstową z polami takimi jak ID, tytuł, opis, deadline, priorytet i dotychczasowe szacunki czasowe.

### Pojęcia dla nowicjuszy

* **Prompt Engineering (Projektowanie instrukcji)**: Praktyka precyzyjnego konstruowania zapytań do modeli sztucznej inteligencji. Poprzez dostarczenie jasnych reguł oraz schematu wyjściowego (np. JSON), gwarantujemy, że model odpowie w sposób powtarzalny i łatwy do przetworzenia przez kod programu.
* **Bloki Tekstowe (Text Blocks)**: Funkcja Javy (od wersji 15) pozwalająca na tworzenie wieloliniowych ciągów znaków przy użyciu potrójnych cudzysłowów `"""`. Dzięki temu nie trzeba łączyć wielu linii za pomocą znaku `+` ani ręcznie wpisywać znaków nowej linii `\n`, co bardzo zwiększa czytelność szablonów promptów czy kodu JSON w kodzie Javy.
* **`DateTimeFormatter`**: Klasa służąca do formatowania i parsowania dat. Wzorzec `"yyyy-MM-dd (EEEE)"` zamienia standardową datę (np. `2026-06-30`) na czytelny format tekstowy zawierający dzień tygodnia (np. `2026-06-30 (wtorek)`).
### Fail-fast i Guard Clauses (Wczesne powroty z błędem)

```java
        if (tasks == null) {
            throw new IllegalArgumentException("tasks must not be null");
        }
        if (today == null) {
            throw new IllegalArgumentException("today must not be null");
        }
```

- To świetna praktyka "Clean Code". Zamiast pozwolić, by program wybuchnął z trudnym do namierzenia błędem `NullPointerException` (NPE) gdzieś dalej w kodzie, metoda od razu sprawdza argumenty. Jeśli przekażesz null, aplikacja rzuci jasny, opisowy błąd `IllegalArgumentException`.

### Optymalne sklejanie tekstu (StringBuilder)

```java
        StringBuilder prompt = new StringBuilder();
        prompt.append("Today's date: ").append(today.format(DATE_FORMATTER)).append("\n\n");
```

- **Dlaczego nie użyto zwykłego `String prompt = ""` i operatora `+`?** W Javie Stringi są niemutowalne (immutable). Używanie `+` w pętli powodowałoby, że przy każdym doklejeniu tekstu w pamięci tworzony byłby zupełnie nowy obiekt String, a stary trafiałby do Garbage Collectora. Przy dużej liście zadań to drastycznie obniża wydajność (tzw. wyciek wydajności). `StringBuilder` rozwiązuje ten problem – tworzy jeden mutowalny bufor w pamięci, do którego efektywnie dokleja (`append`) kolejne fragmenty.

### Logika iteracji i budowania promptu

```java
        if (tasks.isEmpty()) {
            prompt.append("(No tasks provided)\n");
        } else {
            for (Task task : tasks) {
                prompt.append("---\n");
                prompt.append("Task ID: ").append(task.getId()).append("\n");
                // ...
```

- **Obsługa przypadków brzegowych**: Jeśli lista jest pusta, model dostaje jasną informację `(No tasks provided)`.
- Zwykła pętla `for-each`, która dla każdego zadania buduje blok tekstu oddzielony kreskami (`---`). Takie wizualne oddzielenie pomaga modelom LLM lepiej rozróżniać poszczególne obiekty w czystym tekście.

### Zabezpieczenia przy pobieraniu opcjonalnych danych

```java
                if (task.getDescription() != null && !task.getDescription().isBlank()) {
                    prompt.append("Description: ").append(task.getDescription()).append("\n");
                }
```

- Twórca (lub Ty) dodał tu podwójne zabezpieczenie. Wyciąga opis zadania tylko wtedy, gdy:
    
    1. Nie jest nullem (`!= null`).
        
    2. Nie jest pusty ani nie składa się z samych białych znaków (`!isBlank()`).
        
- **`isBlank()` vs `isEmpty()`**: W metodzie niżej (dla tagów) użyto `isEmpty()`. Dlaczego przy opisie jest `isBlank()`? Metoda `isBlank()` (wprowadzona w Java 11) sprawdza, czy string zawiera widoczne znaki. Jeśli w opisie zadania jest tylko spacja `" "`, to `isEmpty()` zwróci `false` (bo string ma długość 1), ale `isBlank()` zwróci `true`. Dzięki temu do promptu nie trafi pusty merytorycznie tekst.

### Finalizacja

```java
        prompt.append("\nPlease analyze these tasks and provide your response in the JSON format specified in the system prompt.\n");
        return prompt.toString();
    }
```

- Na końcu `StringBuilder` za pomocą metody `.toString()` jest "pieczętowany" i zamieniany na jeden ostateczny obiekt typu `String`, który wędruje do AWS Bedrock czy innego klienta obsługującego Claude'a.
