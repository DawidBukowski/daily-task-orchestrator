### 1. Dokumentacja i definicja klasy

```java
/**
 * Email notification adapter implementing TaskNotifier port.
 * Orchestrates email generation and SMTP sending with graceful error handling.
 */
public class EmailTaskNotifier implements TaskNotifier {
```

- **Komentarz (Javadoc):** Tłumaczy architektury. Klasa jest "adapterem" implementującym "port" `TaskNotifier`. Oznacza to, że gdzieś indziej w systemie istnieje reguła (interfejs [[TaskNotifier]]), która mówi: _"potrzebuję czegoś, co wyśle powiadomienie o zadaniach"_. Ta klasa jest jedną z możliwych odpowiedzi na tę potrzebę – w tym przypadku wysyła e-mail. "Graceful error handling" oznacza, że jeśli coś pójdzie nie tak, aplikacja nie wybuchnie, tylko po cichu obsłuży błąd.
    
- **`implements TaskNotifier`:** Klasa podpisuje kontrakt. Zobowiązuje się, że dostarczy kod dla wszystkich metod, których wymaga interfejs `TaskNotifier` (w tym przypadku jest to metoda `notify`).
    

### 2. Narzędzia i współdzielone zasoby (Logger)

```java
    private static final Logger logger = LoggerFactory.getLogger(EmailTaskNotifier.class);
```

- **`Logger`:** Służy do zapisywania informacji o tym, co robi aplikacja w trakcie działania (do pliku lub na konsolę). Znacznie ułatwia to szukanie błędów (debugowanie).
    
- **`private static final`:** Zmienna jest prywatna, niezmienna (`final`) i wspólna dla wszystkich obiektów tej klasy (`static`). Tworzymy tylko jeden logger dla całej klasy, co oszczędza pamięć.
    

### 3. Zależności klasy (Pola)

```java
    private final EmailConfiguration config;
    private final EmailTemplate emailTemplate;
    private final SmtpEmailSender emailSender;
```

Nasza klasa pełni rolę "kierownika" (w komentarzu nazwano to _orchestrates_). Nie wykonuje brudnej roboty sama. Posiada trzech pomocników:

- **[[EmailConfiguration]]:** Trzyma ustawienia (np. na jaki adres e-mail wysłać wiadomość).
    
- **[[EmailTemplate]]:** Wie, jak przekształcić suche dane w ładny tytuł i kod HTML wiadomości.
    
- **`emailSender`:** Skupia się wyłącznie na połączeniu z serwerem pocztowym (SMTP) i wypchnięciu maila w świat.
    
- Pola są **`private final`** – raz przypisane, nie mogą zostać zmienione. Gwarantuje to, że w połowie pracy nikt nie podmieni nam konfiguracji.
    

### 4. Konstruktor i wstrzykiwanie zależności ([[Dependency Injection]])

```java
    public EmailTaskNotifier(
        EmailConfiguration config,
        EmailTemplate emailTemplate,
        SmtpEmailSender emailSender
    ) {
        this.config = Objects.requireNonNull(config, "EmailConfiguration cannot be null");
        this.emailTemplate = Objects.requireNonNull(emailTemplate, "EmailTemplate cannot be null");
        this.emailSender = Objects.requireNonNull(emailSender, "SmtpEmailSender cannot be null");
    }
```

- **Dependency Injection (Wstrzykiwanie Zależności):** Klasa nie tworzy swoich pomocników sama (nie używa nigdzie słówka `new`). Wymaga, aby podano jej gotowe obiekty podczas jej tworzenia. Dzięki temu łatwo jest tę klasę przetestować (można podstawić fałszywych/testowych pomocników).
    
- **`Objects.requireNonNull(...)`:** To wzorzec **Fail-Fast** (szybkie usterki). Zamiast pozwalać na utworzenie uszkodzonego obiektu (który wygenerowałby `NullPointerException` gdzieś później w trakcie działania programu), konstruktor natychmiast sprawdza, czy nie przekazano mu wartości pustych (`null`). Jeśli tak, program od razu rzuci błędem z konkretnym, czytelnym komunikatem (np. _"EmailConfiguration cannot be null"_).
    

### 5. Główna metoda działania (Serce klasy)

```java
    @Override
    public void notify(TasksSummary tasks) {
        Objects.requireNonNull(tasks, "TasksSummary cannot be null");
```

- **`@Override`:** Informacja dla kompilatora: "Hej, ta metoda pochodzi z interfejsu `TaskNotifier`, pilnuj, czy jej nazwa i argumenty są w 100% zgodne z oryginałem".
    
- Program upewnia się, że nie poproszono go o wysłanie powiadomienia dla pustego (nieistniejącego) obiektu zadań.

### 6. Właściwa praca i generowanie treści

```java
        try {
            logger.info("Generating email for {} tasks", tasks.getAllTasks().size());

            String subject = emailTemplate.generateSubject(tasks);
            String htmlBody = emailTemplate.generateHtml(tasks);
```

- **`try { ... }`:** Rozpoczynamy blok bezpieczny. Kod wewnątrz może potencjalnie rzucić błędem (np. serwer e-mail nie odpowiada), ale dzięki `try`, w razie awarii zapanujemy nad sytuacją.
    
- **`logger.info(...)`:** Aplikacja zostawia w logach ślad: _"Zaczynam generować e-mail dla X zadań"_. Klamerki `{}` to miejsce, w które zostanie wstawiona wartość pobrana z `tasks.getAllTasks().size()`.
    
- Zlecamy pomocnikowi (`emailTemplate`) stworzenie **tematu (`subject`)** oraz **treści (`htmlBody`)** wiadomości. Zwróć uwagę na to, że nasza główna klasa w ogóle nie wie, jak wygląda struktura tego maila – realizuje to zasada pojedynczej odpowiedzialności (SRP).
    

### 7. Wysyłanie wiadomości

```java
            emailSender.send(subject, htmlBody);

            logger.info("Email notification sent successfully to {}", config.getToEmail());
```

- Zlecamy drugiemu pomocnikowi (`emailSender`) wysłanie gotowego tekstu.
    
- Jeśli wysyłka się powiedzie, zostawiamy w logach notatkę o sukcesie, odczytując z konfiguracji (`config`) adres e-mail, na który poszła wiadomość.

### 8. Bezpieczna obsługa błędów (Graceful Degradation)

```java
        } catch (SmtpEmailSender.EmailSendException e) {
            // Graceful degradation: log error but don't crash application
            logger.error("Email notification failed: {}", e.getMessage(), e);
        } catch (Exception e) {
            // Catch-all for unexpected errors (HTML generation, etc.)
            logger.error("Unexpected error during email notification", e);
        }
    }
}
```

Jeśli gdziekolwiek w bloku `try` nastąpiłby błąd, program natychmiast przerywa jego wykonywanie i przeskakuje do bloków `catch`.

- **Pierwszy `catch`:** Wyłapuje konkretny błąd pochodzący od klienta SMTP (`EmailSendException`), czyli np. przerwane połączenie, błędne hasło do skrzynki. Aplikacja zapisuje zły obrót spraw jako BŁĄD w logach (`logger.error`), ale **nie pozwala aplikacji się zamknąć / wywalić**. Użytkownik pewnie robił coś ważnego (np. zamykał projekt) – fakt, że mail potwierdzający nie doszedł, nie powinien "psuć" całego systemu.
    
- **Drugi `catch` (Catch-all):** Łapie każdy inny, niespodziewany błąd klasy `Exception`. Co jeśli metoda `generateHtml` nagle rzuci błędem? Ten blok sprawia, że jesteśmy na to gotowi. Nawet przy całkowicie zaskakującym zdarzeniu aplikacja wypluje log o nazwie _"Unexpected error..."_ i będzie działać bezpiecznie dalej.