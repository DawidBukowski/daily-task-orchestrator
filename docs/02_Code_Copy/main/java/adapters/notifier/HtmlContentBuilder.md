### 1. Pola klasy (Zmienne)

```java
private static final DateTimeFormatter DATETIME_FORMATTER =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

private final PriorityColorScheme colorScheme;
```

- **`DATETIME_FORMATTER`**: To narzędzie do formatowania daty i czasu. Słowa `private static final` oznaczają, że jest to stała, współdzielona przez wszystkie obiekty tej klasy (istnieje tylko raz w pamięci). Format `"yyyy-MM-dd HH:mm"` oznacza, że data będzie wyglądać np. tak: _2026-07-02 11:53_.
    
- **`colorScheme`**: Obiekt klasy `PriorityColorScheme`. Ta klasa (której kodu tu nie ma, ale znamy jej cel) przechowuje informacje o tym, jakich kolorów użyć dla konkretnego priorytetu zadań (np. High = czerwony, Low = zielony). Słowo `final` oznacza, że po przypisaniu w konstruktorze, nie można już tego zmienić.

### 2. Konstruktor

```java
public HtmlContentBuilder(PriorityColorScheme colorScheme) {
    this.colorScheme = Objects.requireNonNull(colorScheme, "PriorityColorScheme cannot be null");
}
```

Kiedy tworzymy obiekt tej klasy (tzw. instancję), musimy jej przekazać paletę kolorów (`colorScheme`).

- **`Objects.requireNonNull(...)`**: To bardzo dobra praktyka (tzw. _fail-fast_). Jeśli programista przez pomyłkę przekaże tutaj `null` (nic), program natychmiast wyrzuci błąd z konkretnym komunikatem. Dzięki temu błąd nie wybuchnie gdzieś głębiej w kodzie w losowym momencie.

### 3. Główna metoda: Serce klasy

```java
public String buildHtml(TasksSummary summary) {
    Objects.requireNonNull(summary, "TasksSummary cannot be null");

    StringBuilder html = new StringBuilder();
    html.append(buildHtmlHeader());
    html.append(buildSummarySection(summary.getSummary()));
    html.append(buildTasksSection(summary.getTasksSortedByPriority()));
    html.append(buildScheduleSection(summary.getSchedule()));

    if (summary.getRecommendations() != null && !summary.getRecommendations().isEmpty()) {
        html.append(buildRecommendationsSection(summary.getRecommendations()));
    }

    html.append(buildHtmlFooter());
    return html.toString();
}
```

To jest jedyna publiczna metoda (poza konstruktorem). To ją wywołuje reszta aplikacji. Co tu się dzieje?

1. Sprawdza, czy przekazane dane (`summary`) nie są nullem.
    
2. Tworzy **`StringBuilder`**. W Javie łączenie wielu stringów za pomocą znaku `+` jest powolne i zużywa dużo pamięci. `StringBuilder` to specjalny worek, do którego dorzucamy (metodą `.append()`) kolejne kawałki tekstu, a na koniec sklejamy je w jeden ciąg za pomocą `.toString()`. To optymalizuje działanie programu.
    
3. Wywołuje kolejne metody (omówione niżej), które "doklejają" odpowiednie sekcje e-maila: Nagłówek -> Podsumowanie -> Lista zadań -> Harmonogram.
    
4. **Zalecenia (Recommendations)** mają warunek `if`. Zostaną dodane tylko wtedy, gdy w ogóle istnieją i lista nie jest pusta.
    
5. Dokleja stopkę i zwraca gotowy kod HTML.
    

### 4. Metody pomocnicze (Prywatne) - Budowanie sekcji HTML

Większość z poniższych metod wykorzystuje genialną funkcjonalność nowszych wersji Javy: **Text Blocks** (Bloki tekstu). Używa się do nich potrójnego cudzysłowu `"""`. Pozwalają one wklejać wielolinijkowy tekst (jak HTML) bezpośrednio do kodu Javy bez uciążliwego dodawania znaków nowej linii (`\n`) na końcu każdego wiersza. Ponadto, w całym kodzie HTML widzimy **Inline CSS** (np. `style="color: red;"`) – jest to konieczne, ponieważ wiele programów pocztowych (jak Gmail czy Outlook) usuwa zewnętrzne arkusze stylów CSS, akceptując tylko style wpisane bezpośrednio w tagi HTML.

#### Nagłówek i Podsumowanie

- **`buildHtmlHeader()`**: Generuje samą górę dokumentu HTML (`<!DOCTYPE html>`, `<head>`, `<body>` z podstawową czcionką i szarym tłem).
    
- **`buildSummarySection()`**: Tworzy białą sekcję (kartę) z podsumowaniem dnia. Używa `String.format(tekst, argument)`. Miejsce `%s` w tekście HTML zostanie zastąpione przez to, co przekażemy jako argument. Tutaj dodatkowo tekst przepuszczany jest przez metodę `escape()` (omówioną na końcu).

#### Budowanie listy zadań (`buildTasksSection`)

```java
private String buildTasksSection(List<Task> tasks) {
    StringBuilder section = new StringBuilder();
    section.append(String.format("... Tasks (%d) ...", tasks.size()));

    for (Task task : tasks) {
        section.append(buildTaskCard(task));
    }
    // ...
}
```

Ta metoda dostaje listę zadań. Zapisuje w tytule ich liczbę (`tasks.size()`), a następnie za pomocą pętli `for-each` ("dla każdego zadania w liście zadań") wywołuje metodę `buildTaskCard` i dokleja jej wynik (pojedynczą kartę zadania) do sekcji.

#### Najbardziej złożona metoda: `buildTaskCard(Task task)`

To ona rysuje pojedyncze zadanie. Co robi krok po kroku?

1. **Kolory priorytetu**: Pobiera z `colorScheme` kolory na podstawie priorytetu (np. High, Low). Zabezpiecza się przed brakiem priorytetu: `task.getPriority() != null ? ... : "MEDIUM"`.
    
2. **Tytuł i Badge**: Rysuje lewą krawędź karty w kolorze priorytetu, wstawia tytuł (zabezpieczony przez `escape`) i dodaje "plakietkę" (badge) z nazwą priorytetu (np. napis HIGH na czerwonym tle).
    
3. **Opis**: Jeśli zadanie ma opis (`!isBlank()`), dodaje go.
    
4. **Sekcja Deadline (Termin)**:
    
    - Jeśli jest termin (`if (task.getDeadline() != null)`):
        
        - Czy jest po terminie? (`isOverdue()`). Jeśli tak, wypisuje to na czerwono, pogrubionym drukiem.
            
        - Jeśli nie jest, oblicza ile dni zostało i pisze np. "📅 Due: 2026-07-04 12:00 (2 days)".
            
        - Jeśli zadanie powiązane jest z e-mailem, dorzuca klikalny link (tag `<a>`) z tekstem "View in Gmail".
            
    - Jeśli nie ma terminu: wypisuje "No deadline set".
        
5. **Notatki**: Jeśli są dołączone dodatkowe notatki, dodaje je na samym dole karty na delikatnym szarym tle, obramowane, ze znaczkiem 📝.
    

#### Harmonogram (`buildScheduleSection`)

Wstawia proponowany plan dnia w znacznikach `<pre>`. Tagi `<pre>` oznaczają w HTML "preformatted text" – wymuszają na przeglądarce/poczcie zachowanie dokładnie takich spacji, wcięć i enterów, jakie podał system generujący harmonogram. Użyto w nim stałej czcionki (jak na maszynie do pisania - _Courier New_), co jest typowe dla kodu lub surowych harmonogramów.

#### Rekomendacje i Stopka

- **`buildRecommendationsSection()`**: Zwróć uwagę na zieloną ramkę (`border: 2px solid #28a745;`). Tworzy listę wypunktowaną (`<ul>`), iterując przez rekomendacje w pętli i tworząc elementy listy (`<li>`).
    
- **`buildHtmlFooter()`**: Generuje tekst na dole strony. Najpierw sprawdza aktualną datę serwera: `LocalDateTime.now().format(DATETIME_FORMATTER)`. Wstawia ją w formatce: _Generated on [DATA] by Daily Task Orchestrator_. Na koniec zamyka tagi `</body>` i `</html>`.
    

### 5. Bezpieczeństwo - metoda `escape(String text)`

```java
private String escape(String text) {
    if (text == null) {
        return "";
    }
    return StringEscapeUtils.escapeHtml4(text);
}
```

**To jest jedna z najważniejszych linijek w tym kodzie pod kątem bezpieczeństwa.** Dlaczego metoda wywoływana jest w każdym miejscu, gdzie do HTML-a wstawiany jest tekst z zewnątrz (tytuł, opis, podsumowanie)?

Chodzi o obronę przed atakiem typu **XSS (Cross-Site Scripting)**. Wyobraź sobie, że ktoś złośliwy wpisze jako nazwę zadania taki tekst: `<script>wykradnijHaslaOrazCiasteczka();</script>`

Gdyby program wkleił ten tekst wprost do HTML-a e-maila, u odbiorcy (gdyby jego klient poczty był dziurawy lub była to aplikacja webowa) ten skrypt by się wykonał. `StringEscapeUtils.escapeHtml4()` (zapewne pochodzi z popularnej biblioteki _Apache Commons Text_) zamienia niebezpieczne znaki HTML na ich bezpieczne odpowiedniki. Na przykład znak `<` zostanie zamieniony na `&lt;`, a `>` na `&gt;`. Dzięki temu przeglądarka wyświetli użytkownikowi **zwykły tekst** o treści `<script>...`, ale przeglądarka uzna to za zwykły tekst do przeczytania, a **nie** kod do uruchomienia.