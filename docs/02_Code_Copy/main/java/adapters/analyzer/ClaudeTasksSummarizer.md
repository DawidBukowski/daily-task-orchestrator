### 1. Deklaracja Klasy i Zależności (Pola klasy)
```java
public class ClaudeTasksSummarizer implements TaskSummarizer {
```

- Klasa nazywa się [[ClaudeTasksSummarizer]]. Jej zadaniem jest podsumowywanie zadań z wykorzystaniem sztucznej inteligencji (Claude AI).
    
- `implements TaskSummarizer`: Oznacza to, że ta klasa "podpisuje kontrakt" (interfejs) [[TaskSummarizer]]. Reszta aplikacji nie musi wiedzieć, że używamy Claude'a. Aplikacja prosi po prostu o "jakiś" podsumowywacz zadań. To pozwala w przyszłości łatwo podmienić Claude'a na np. ChatGPT, tworząc nową klasę implementującą ten sam interfejs.
```java
private static final Logger logger = LoggerFactory.getLogger(ClaudeTasksSummarizer.class);
```
- **Logger (SLF4J/Logback):** To narzędzie do zapisywania informacji o tym, co robi aplikacja (zamiast używać `System.out.println`). `static final` oznacza, że jest jeden logger dla wszystkich instancji tej klasy i nie można go podmienić.

```java
private final ClaudeApiClient apiClient;
private final TaskSummarizationPromptBuilder promptBuilder;
private final ClaudeResponseParser responseParser;
```

- To są **zależności (Dependencies)** tej klasy. Klasa `ClaudeTasksSummarizer` nie potrafi sama wysłać żądania HTTP, zbudować tekstu, ani przetworzyć JSON-a. Deleguje te zadania do innych, wyspecjalizowanych narzędzi:
    
    - [[ClaudeApiClient]]: Komunikuje się przez sieć z serwerami Claude.
        
    - [[TaskSummarizationPromptBuilder]]: Zamienia obiekty Javy (zadania) na tekst (prompt), który zrozumie sztuczna inteligencja.
        
    - [[ClaudeResponseParser]]: Bierze surowy tekst (pewnie JSON) od Claude'a i zamienia go z powrotem na obiekty Javy.
        
- Słowo `final` oznacza, że raz przypisane w konstruktorze, nie mogą zostać zmienione. To chroni przed błędami.
    

### 2. Konstruktor (Wstrzykiwanie Zależności i Walidacja)
```java
public ClaudeTasksSummarizer(...) {
    if (apiClient == null) { throw new IllegalArgumentException(...); }
    // ... i tak dla każdego parametru
    this.apiClient = apiClient;
    // ...
}
```

- **Wstrzykiwanie zależności ([[Dependency Injection]]):** Zamiast tworzyć te obiekty wewnątrz klasy (np. `this.apiClient = new ClaudeApiClient()`), klasa _wymaga_, aby ktoś jej je przekazał. Dzięki temu klasę można łatwo przetestować (wstrzykując tzw. "moki" - sztuczne obiekty udające prawdziwe API).
    
- **Defensive Programming (Programowanie defensywne):** Kod na samym początku sprawdza, czy ktoś nie przekazał wartości `null`. Jeśli tak, natychmiast rzuca błąd `IllegalArgumentException` ("Zły argument"). Lepiej wywalić błąd przy starcie programu niż później podczas działania (tzw. zasada _Fail-Fast_).

### 3. Główne Serce Klasy: Metoda `summarize`

To jest główna funkcja, która "odwala całą robotę". Przyjmuje listę zadań (`List<Task> tasks`) i zwraca obiekt `TasksSummary`.
```java
@Override
public TasksSummary summarize(List<Task> tasks) {
    if (tasks == null) { throw new IllegalArgumentException("tasks must not be null"); }
```

Znowu programowanie defensywne. Zabezpieczamy się przed listą `null`.
```java
    try {
        // Krok 1: Budowanie promptów (zapytań dla AI)
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(tasks, LocalDate.now());
```

Cała logika zamknięta jest w bloku `try-catch`. Jeśli cokolwiek pójdzie nie tak (brak internetu, AI się zawiesi, błąd przetwarzania), aplikacja nie "wybuchnie", tylko przejdzie do bloku `catch`. Wysyłamy do AI dwie rzeczy: Prompt Systemowy (np. "Jesteś asystentem, który zarządza zadaniami...") i Prompt Użytkownika (lista zadań i dzisiejsza data, żeby AI wiedziało, czy zadanie jest po terminie).
```java
        // Krok 2: Wysłanie do API
        String rawResponse = apiClient.sendMessage(systemPrompt, userPrompt);
        if (rawResponse == null || rawResponse.isBlank()) {
            return createFallbackSummary(tasks);
        }
```

Wysyłamy zapytanie do Claude'a. Jeśli Claude odpowie pustym tekstem (awaria API), od razu zwracamy "Fallback Summary" (o tym za chwilę).

```java
        // Krok 3: Parsowanie (tłumaczenie z tekstu na obiekty Javy)
        ClaudeTaskSummaryResponse response = responseParser.parse(rawResponse);
```

Oczekujemy, że Claude zwrócił np. strukturę JSON. Ten parser zamienia ją na zrozumiały dla Javy obiekt `ClaudeTaskSummaryResponse`.
```java
        // Krok 4: Aplikowanie aktualizacji zadań
        List<Task> updatedTasks = applyTaskUpdates(tasks, response.taskUpdates());
```

AI mogło zasugerować np. zmianę priorytetu z "Niski" na "Wysoki" dla jakiegoś zadania. Tutaj te zmiany są nakładane na nasze oryginalne zadania.
```java
        // Krok 5: Zwrócenie gotowego wyniku
        return new TasksSummary(updatedTasks, response.summary(), response.schedule(), response.recommendations());
```

Na koniec pakujemy wszystko w jeden ładny obiekt `TasksSummary` i go zwracamy.

```java
    } catch (ClaudeApiClient.ClaudeApiException e) {
        logger.error("Claude API call failed: {}", e.getMessage());
        return createFallbackSummary(tasks);
    } catch (Exception e) {
        logger.error("Unexpected error...", e.getMessage(), e);
        return createFallbackSummary(tasks);
    }
}
```

- **Graceful Degradation (Miękka degradacja):** To jest kluczowe! Jeśli serwery Claude (lub coś innego) padną, aplikacja nie przestanie działać dla użytkownika. Pojawi się błąd w logach (`logger.error`), ale użytkownik po prostu zobaczy swoje oryginalne zadania bez analizy AI (dzięki metodzie `createFallbackSummary`). Z punktu widzenia użytkownika aplikacja nadal działa, po prostu nie ma "bajerów" AI w tym momencie.

### 4. Metoda `applyTaskUpdates` (Optymalizacja i złączenia)

Jej celem jest połączenie listy oryginalnych zadań z listą "sugestii poprawek" od AI.
```java
private List<Task> applyTaskUpdates(List<Task> originalTasks, List<ClaudeTaskSummaryResponse.TaskUpdate> updates) {
    // Zabezpieczenie: brak aktualizacji = zwracamy oryginały
    if (updates == null || updates.isEmpty()) { return new ArrayList<>(originalTasks); }
```

```java
    // Budowa Mapy (Słownika) dla optymalizacji
    Map<String, ClaudeTaskSummaryResponse.TaskUpdate> updateMap = new HashMap<>();
    for (ClaudeTaskSummaryResponse.TaskUpdate update : updates) {
        updateMap.put(update.taskId(), update);
    }
```

**Dlaczego mapa (HashMap)?** Mamy listę oryginalnych zadań i listę aktualizacji. Gdybyśmy użyli dwóch list, dla każdego z np. 100 zadań musielibyśmy przeszukiwać 100 aktualizacji (złożoność O(n²)). Dzięki mapie (która działa jak słownik z kluczem w postaci ID zadania), wyciągnięcie aktualizacji dla danego ID zajmuje ułamek sekundy (złożoność O(1)). To świetna praktyka wydajnościowa.

```java
    List<Task> updatedTasks = new ArrayList<>();
    for (Task task : originalTasks) {
        ClaudeTaskSummaryResponse.TaskUpdate update = updateMap.get(task.getId());
        if (update != null) {
            Task updatedTask = applyUpdateToTask(task, update);
            updatedTasks.add(updatedTask);
            updateMap.remove(task.getId()); // Oznaczamy jako "użyte"
        } else {
            updatedTasks.add(task); // Zadanie bez poprawek od AI
        }
    }
```

Pętla przechodzi przez oryginalne zadania. Jeśli w mapie jest poprawka do ID tego zadania, nakładamy ją. Jeśli nie, dodajemy do nowej listy zadanie w formie niezmienionej.

### 5. Metoda `applyUpdateToTask` (Niezmienność - Immutability)

Ta metoda tworzy i zwraca pojedyncze zaktualizowane zadanie.
```java
private Task applyUpdateToTask(Task task, ClaudeTaskSummaryResponse.TaskUpdate update) {
    Priority newPriority = Priority.fromString(update.priority());
    String mergedNotes = mergeNotes(task.getNotes(), update.notes());

    Task updatedTask = new Task(
            task.getId(),
            task.getTitle(),
            // ... kopiujemy stare pola ...
            newPriority, // ... wstawiamy nowe zmienione pola ...
            // ...
            update.estimatedHours() != null ? update.estimatedHours() : task.getEstimatedHours(),
            // ...
            mergedNotes
    );
    return updatedTask;
}
```

**Niezmienność (Immutability):** Zwróć uwagę, że programista **NIE** zrobił tu np. `task.setPriority(newPriority)`. Zamiast modyfikować istniejący obiekt zadania w pamięci, tworzy za pomocą `new Task(...)` **zupełnie nowy obiekt**, przekazując do niego większość starych danych i wklejając parę nowych. _Dlaczego?_ To chroni przed bugami trudnymi do wyśledzenia (tzw. side-effects). Jeśli inny fragment programu w tym samym czasie czyta to zadanie, zmiana go w locie mogłaby spowodować awarię. Zawsze bezpieczniej jest stworzyć i zwrócić nową kopię.

Zauważ też użycie tzw. _Ternary operator_ (Operatora warunkowego): `warunek ? jeśli_tak : jeśli_nie`. `update.estimatedHours() != null ? update.estimatedHours() : task.getEstimatedHours()` oznacza: Jeśli AI podało nowy czas szacowany (nie jest null), użyj go. Jeśli nie podało, zostaw stary.

### 6. Metody pomocnicze: `mergeNotes` i `createFallbackSummary`

```java
private String mergeNotes(String existingNotes, String updateNotes) { ... }
```

Po prostu sprytne łączenie tekstu. Jeśli zadanie miało notatki użytkownika i AI dodało swoje, połączy je wpisując przerwę i znacznik `[AI Analysis]`. Obsługuje przypadki, gdy brakuje notatki oryginalnej lub notatki od AI.

```java
private TasksSummary createFallbackSummary(List<Task> tasks) {
    return new TasksSummary(
            new ArrayList<>(tasks),
            "Task analysis unavailable...",
            "Claude AI analysis temporarily unavailable.",
            List.of("Please try again later or check API connectivity.")
    );
}
```

To jest nasza "poduszka bezpieczeństwa" (wspomniany wcześniej Fallback). Zwraca po prostu oryginalne zadania jako nową listę oraz umieszcza statyczne komunikaty mówiące: "Hej, AI teraz nie działa, spróbuj później".

Elementy `@param` i `@throws`, które widziałeś w kodzie nad deklaracjami metod i konstruktorów, to **tagi (znaczniki) systemu Javadoc**.

Javadoc to standardowy sposób dokumentowania kodu w języku Java. Zapisuje się go w specjalnych blokach komentarzy, które zawsze zaczynają się od `/` (dwie gwiazdki na początku) i kończą na `*/`.