### 1. Deklaracja klasy i zmienne (pola)

```java
public class EmailTemplate {
    private final HtmlContentBuilder htmlBuilder;
```

- **`public class EmailTemplate`**: Tworzymy nową klasę o nazwie `EmailTemplate`. Słowo kluczowe `public` oznacza, że inne części Twojego programu będą miały do niej swobodny dostęp. Z nazwy wynika, że klasa ta służy jako szablon do generowania e-maili.
    
- **`private final HtmlContentBuilder htmlBuilder;`**: Jest to tzw. "pole" klasy (zmienna przypisana do konkretnego obiektu tej klasy).
    
    - **`private`**: Oznacza, że ta zmienna jest ukryta przed światem zewnętrznym. Tylko metody wewnątrz klasy `EmailTemplate` mogą jej używać. To dobra praktyka (tzw. hermetyzacja/enkapsulacja).
        
    - **`final`**: Bardzo ważne słowo! Oznacza, że gdy raz przypiszemy wartość do tej zmiennej (w konstruktorze), nie będzie można jej już nigdy zmienić. Chroni to przed przypadkowym zepsuciem działania klasy w przyszłości.
        
    - **`HtmlContentBuilder`**: To typ zmiennej. Gdzieś w kodzie musi istnieć inna klasa (lub interfejs) o tej nazwie, która zajmuje się budowaniem kodu HTML.
        

### 2. Konstruktor

```java
    public EmailTemplate(HtmlContentBuilder htmlBuilder) {
        this.htmlBuilder = Objects.requireNonNull(htmlBuilder, "HtmlContentBuilder cannot be null");
    }
```

- **Konstruktor** to specjalna metoda uruchamiana tylko raz, podczas tworzenia (instancjonowania) obiektu przy użyciu słowa `new` (np. `new EmailTemplate(jakisBuilder)`).
    
- Konstruktor wymaga podania obiektu `HtmlContentBuilder` jako argumentu. Jest to tzw. **Wstrzykiwanie Zależności ([[Dependency Injection]])** – ta klasa potrzebuje "budowniczego HTML", więc wymuszamy, aby ktoś z zewnątrz jej go dostarczył.
    
- **`Objects.requireNonNull(...)`**: To genialna funkcja z biblioteki standardowej Javy. Sprawdza ona, czy przekazany `htmlBuilder` nie jest `null` (czyli pusty, nieistniejący).
    
    - Jeśli jest `null`, program natychmiast wyrzuci błąd `NullPointerException` z wiadomością _"HtmlContentBuilder cannot be null"_.
        
    - Jeśli wszystko jest w porządku, zwraca ten sam obiekt, który przypisujemy do naszego pola `this.htmlBuilder = ...` (słowo `this` oznacza "moje własne pole z tej klasy").
        
    - **Dlaczego tak?** To taktyka _"Fail-fast"_ (ponieś porażkę szybko). Lepiej wyrzucić błąd od razu podczas tworzenia obiektu, niż szukać później dziwnej usterki w innej części aplikacji.
        

### 3. Komentarz dokumentacyjny (Javadoc)

```java
    /**
     * Generates email subject line from task summary.
     * Format: "Daily Task Summary: X tasks (Y overdue)" or "⚠️ Daily Task Summary: X tasks (Y overdue)"
     *
     * @param summary the task summary
     * @return formatted subject line
     */
```

- Taki blok tekstu (zaczynający się od `/`) to oficjalny komentarz dokumentacyjny Javy. Opisuje on to, co robi metoda pod nim. Edytory kodu (np. IntelliJ, Eclipse) wyświetlą ten tekst jako "dymek z podpowiedzią", gdy będziesz używać tej metody w innym miejscu. Pokazuje przykładowy format i wyjaśnia argumenty (`@param`) oraz co metoda zwraca (`@return`).

### 4. Metoda: Tworzenie tematu e-maila

```java
    public String generateSubject(TasksSummary summary) {
        Objects.requireNonNull(summary, "TasksSummary cannot be null");
```

- **`public String generateSubject(TasksSummary summary)`**: To metoda publiczna, która zwraca tekst (`String`), czyli gotowy tytuł e-maila. Wymaga podania obiektu [[TasksSummary]] .
    
- Ponownie użyto **`requireNonNull`**, aby upewnić się, że nie próbujemy generować tytułu z "niczego" (co spowodowałoby wybuch aplikacji błędem, gdybyśmy dalej próbowali pobrać z "niczego" dane).
    

```java
        int taskCount = summary.getAllTasks().size();
        int overdueCount = summary.getOverdueTasks().size();
```

- Tutaj kod pyta przekazany obiekt `summary` o dwie rzeczy:
    
    - Ile ma w sumie wszystkich zadań (`getAllTasks().size()`) i przypisuje tę liczbę do zmiennej całkowitej `taskCount` (typu `int`).
        
    - Ile z tych zadań jest zaległych (`getOverdueTasks().size()`) i przypisuje to do `overdueCount`.
        

```java
        StringBuilder subject = new StringBuilder();
```

- **`StringBuilder`**: Dlaczego go używamy zamiast po prostu sklejać tekst plusem (np. `"Tytuł" + " " + "reszta"`)? Java to język, w którym zwykły tekst (`String`) jest niezmienny. Sklejanie wielu fragmentów za pomocą `+` jest powolne i pożera pamięć, bo pod spodem za każdym razem tworzy się nowy wycinek tekstu. `StringBuilder` to specjalny obiekt służący do sprawnego, "brudnego" i szybkiego klejenia wielu fragmentów tekstu w jeden.
    

```java
        // Add warning emoji if there are overdue tasks
        if (overdueCount > 0) {
            subject.append("⚠️ ");
        }
```

- Sprawdzamy, czy liczba zaległych zadań jest większa niż 0. Jeśli tak, doklejamy (używając metody `.append()`) emoji ostrzeżenia ze spacją na sam początek.
    

```java
        subject.append("Daily Task Summary: ")
               .append(taskCount)
               .append(" task");
```

- Doklejamy główną treść e-maila: napis `"Daily Task Summary: "`, potem naszą liczbę wyciągniętą wcześniej ze zmiennej `taskCount`, i dopisujemy słowo `" task"`. Zauważ, że te wywołania są połączone kropkami (jedno za drugim). Działa to dlatego, że metoda `append` za każdym razem zwraca tego samego `StringBuildera`. Programiści nazywają to "Fluent API" lub kaskadowym wywoływaniem metod. Wygląda ładnie i czytelnie.
    

```java
        // Pluralize
        if (taskCount != 1) {
            subject.append("s");
        }
```

- Bardzo miły, pro-użytkownikowy akcent! Sprawdzamy liczbę zadań. W języku angielskim liczba mnoga dostaje "s" na końcu. Jeśli zadań NIE jest 1 (czyli jest ich 0, 2, 3, 10 itd.), kod dokleja literę `"s"` do słowa "task". Więc zamiast błędnego "0 task" lub "5 task", dostaniemy poprawne "0 tasks" / "5 tasks" (ale nadal "1 task").
    

```java
        // Add overdue count if present
        if (overdueCount > 0) {
            subject.append(" (").append(overdueCount).append(" overdue)");
        }
```

- Znowu sprawdzamy, czy są zaległe zadania. Jeśli tak, na sam koniec doklejamy fragment w nawiasie, np. `" (3 overdue)"`.
    

```java
        return subject.toString();
    }
```

- **`return subject.toString();`**: Sam `StringBuilder` jeszcze nie jest zwykłym tekstem (`String`). Ta metoda zmienia "budowniczego" w ostateczny, sklejony z tych wszystkich kawałków tekst (String) i zwraca go jako wynik (output) z całej metody.
    

### 5. Metoda: Tworzenie treści (HTML) e-maila

```java
    /**
     * Generates HTML email body from task summary.
     *
     * @param summary the task summary
     * @return complete HTML document
     */
    public String generateHtml(TasksSummary summary) {
        Objects.requireNonNull(summary, "TasksSummary cannot be null");
        return htmlBuilder.buildHtml(summary);
    }
}
```

- Tak jak poprzednio, mamy tu Javadoc i sprawdzenie bezpieczeństwa (`requireNonNull`).
    
- **`return htmlBuilder.buildHtml(summary);`**: To jest fascynująca linijka ze względu na architekturę oprogramowania. Zauważ, co ta metoda robi... kompletnie nic nie robi sama! Ona **deleguje zadanie**. Mówi: _"Hej, htmlBuilder, ty jesteś specjalistą od tworzenia HTML-a. Zrób ten HTML na podstawie `summary`, a ja to co wyprodukujesz po prostu podam dalej (zwrócę przez `return`)"_.
    
- **Dlaczego tak?** To zastosowanie kluczowej zasady dobrego kodowania: **Single Responsibility Principle (Zasada Pojedynczej Odpowiedzialności)**. Klasa `EmailTemplate` nie powinna się martwić, czy to będą tagi `<div>`, czy tabele `<table>`, i jak wyglądają ostylowania kolorów. Ona zarządza tylko **złożeniem e-maila jako całości**. Brudną robotą od HTMLa zajmuje się inna klasa ukryta pod polem `htmlBuilder`. Dzięki temu kod jest modularny – łatwiej go później przetestować, zaktualizować i poprawiać błędy.
    

### Podsumowanie (TL;DR)

Ten kod to "kierownik", który przyjmuje zestawienie zadań pracownika na dany dzień i pakuje je w e-mail.

1. Samodzielnie wylicza sobie tytuł, sprytnie zarządzając gramatyką (dodając "s") i używając emoji ⚠️, jeśli ktoś spóźnia się z zadaniami. Robi to wydajnie za pomocą `StringBuilder`.
    
2. Do treści samego e-maila (środka) wyznacza jednak eksperta w postaci pomocnika (klasy od `htmlBuilder`), którego otrzymał jeszcze zanim w ogóle wystartował (w konstruktorze).
    
3. Na każdym kroku ubezpiecza się od sytuacji, w której ktoś podałby mu błędne (puste/null) informacje do przetworzenia (metoda `Objects.requireNonNull`).