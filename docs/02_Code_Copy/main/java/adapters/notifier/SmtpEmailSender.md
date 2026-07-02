### 1. Deklaracja klasy i Logowanie

```java
public class SmtpEmailSender {
    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailSender.class);
    private final EmailConfiguration config;
```

- **`SmtpEmailSender`**: Klasa odpowiada za jedną rzecz (zgodnie z zasadą Single Responsibility Principle) – wysyłkę e-maili przez protokół SMTP (Simple Mail Transfer Protocol).
    
- **`Logger logger`**: To instancja loggera (najpewniej z biblioteki SLF4J). Jest `private static final`, ponieważ chcemy mieć tylko jeden logger dla całej klasy, niezależnie od liczby utworzonych obiektów `SmtpEmailSender`. Pozwala to na śledzenie, co aplikacja robi w tle, bez używania `System.out.println()`.
    
- **`EmailConfiguration config`**: Obiekt przechowujący konfigurację (host, port, hasła). Jest `final`, co oznacza, że po utworzeniu instancji `SmtpEmailSender` nie można podmienić mu konfiguracji. To wymusza niemutowalność i bezpieczeństwo w środowiskach wielowątkowych.

### 2. Konstruktor i zasada "Fail-Fast"

```java
    public SmtpEmailSender(EmailConfiguration config) {
        this.config = Objects.requireNonNull(config, "EmailConfiguration cannot be null");
    }
```

- **[[Dependency Injection]]**: Zamiast tworzyć konfigurację wewnątrz klasy, wstrzykujemy ją przez konstruktor.
    
- **`Objects.requireNonNull`**: To świetna praktyka nazywana **Fail-Fast** (szybka porażka). Jeśli ktoś spróbuje utworzyć ten obiekt przekazując `null`, aplikacja od razu rzuci wyjątkiem `NullPointerException` z jasnym komunikatem. Dzięki temu błąd nie ujawni się dopiero w momencie próby wysłania maila (kiedy trudniej byłoby go namierzyć), ale natychmiast przy starcie.
    

### 3. Główna metoda wysyłająca: `send`

```java
    public void send(String subject, String htmlBody) throws EmailSendException {
        Objects.requireNonNull(subject, "Subject cannot be null");
        Objects.requireNonNull(htmlBody, "HTML body cannot be null");
```

- Metoda jest publiczna i deklaruje, że rzuca własny wyjątek `EmailSendException`.
- Ponownie widzimy walidację **Fail-Fast** dla tematu i treści wiadomości.

```java
        try {
            logger.debug("Creating SMTP session for {}:{}", config.getSmtpHost(), config.getSmtpPort());
            Session session = createSession();

            logger.debug("Creating email message: from={}, to={}", config.getFromEmail(), config.getToEmail());
            MimeMessage message = createMessage(session, subject, htmlBody);

            logger.debug("Sending email via SMTP...");
            Transport.send(message);

            logger.info("Email sent successfully to {}", config.getToEmail());

        } catch (MessagingException e) {
            logger.error("SMTP send failed: {}", e.getMessage());
            throw new EmailSendException("Failed to send email via SMTP", e);
        }
    }
```

- **Logika główna**: Tworzymy sesję (`Session`), z niej budujemy wiadomość (`MimeMessage`), a na koniec zlecamy wysyłkę klasie narzędziowej `Transport.send()`.
    
- **Logowanie**: Zwróć uwagę na poziomy logowania. Szczegóły techniczne (tworzenie sesji, wiadomości) idą na poziom `debug` (widoczne tylko przy szukaniu błędów), a sam fakt pomyślnego wysłania na poziom `info` (ważne zdarzenie biznesowe).
    
- **Exception Translation (Tłumaczenie wyjątków)**: Łapiemy techniczny wyjątek z biblioteki Jakarta Mail (`MessagingException`) i opakowujemy go w nasz własny `EmailSendException`. Dlaczego? Aby ukryć przed resztą aplikacji (np. warstwą serwisu), że pod spodem używamy akurat tej konkretnej biblioteki. Wrzucamy też `e` jako drugi argument, aby nie zgubić oryginalnego "stack trace'u" (przyczyny błędu).
    

### 4. Konfiguracja połączenia: `createSession`

```java
    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.getSmtpHost()); // np. smtp.gmail.com
        props.put("mail.smtp.port", String.valueOf(config.getSmtpPort())); // np. 587
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isEnableTls())); // Szyfrowanie
        props.put("mail.smtp.auth", String.valueOf(config.isEnableAuth())); // Logowanie hasłem
```

- Biblioteka Jakarta Mail wymaga obiektu `Properties` (czyli takiej starej mapy klucz-wartość) do konfiguracji. Parametry mówią bibliotece, gdzie się połączyć i jak zabezpieczyć połączenie (TLS szyfruje ruch, żeby nikt nie podsłuchał hasła i treści e-maila w locie).

```java
        props.put("mail.smtp.timeout", String.valueOf(config.getTimeoutMs()));
        props.put("mail.smtp.connectiontimeout", String.valueOf(config.getTimeoutMs()));
        props.put("mail.smtp.writetimeout", String.valueOf(config.getTimeoutMs()));
```

- **Timeouty to krytyczna rzecz w backendzie!** Gdyby serwer SMTP nagle przestał odpowiadać (zawiesił się), a my nie mielibyśmy timeoutów, wątek naszej aplikacji w Javie czekałby w nieskończoność. To prowadzi do wyczerpania puli wątków i awarii całej aplikacji. Tutaj zabezpieczamy czas łączenia, czytania i pisania.

```java
        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getUsername(), config.getPassword());
            }
        };

        return Session.getInstance(props, authenticator);
    }
```

- **`Authenticator`**: Tworzymy klasę anonimową nadpisującą metodę `getPasswordAuthentication()`. Zawsze, gdy serwer pocztowy poprosi o dane logowania, ta metoda dostarczy mu nazwę użytkownika i hasło z naszego obiektu `config`.
- **`Session.getInstance`**: Tworzy ostateczny obiekt sesji na podstawie właściwości i uwierzytelnienia.

### 5. Budowanie wiadomości: `createMessage`

```java
    private MimeMessage createMessage(Session session, String subject, String htmlBody)
            throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getFromEmail()));
        message.setRecipients(
            Message.RecipientType.TO,
            InternetAddress.parse(config.getToEmail()));
        message.setSubject(subject, "UTF-8");
        message.setContent(htmlBody, "text/html; charset=UTF-8");
        message.setSentDate(new Date());
        return message;
    }
```

- **`MimeMessage`**: Reprezentuje standardowego e-maila z obsługą załączników, HTML-a i różnych zestawów znaków (MIME to standard formatowania poczty).
    
- **`InternetAddress`**: Specjalna klasa, która przy okazji waliduje format adresu e-mail (np. czy zawiera `@`).
    
- **Kodowanie UTF-8**: Niezbędne! Bez parametru `"UTF-8"` polskie znaki w temacie lub treści (ą, ę, ł) mogłyby zamienić się w "krzaczki".
    
- **`text/html`**: Ważny nagłówek dla serwerów odbiorczych. Informuje klienta poczty (np. Gmaila), że treść to kod HTML, a nie zwykły tekst, dzięki czemu tagi typu `<b>` czy `<a>` zostaną poprawnie wyrenderowane.

### 6. Własny wyjątek biznesowy

```java
    public static class EmailSendException extends Exception {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
```

- To tzw. **Checked Exception** (dziedziczy po `Exception`, a nie po `RuntimeException`). Zmusza każdego, kto wywołuje metodę `send(...)`, do jawnego obsłużenia błędu za pomocą `try-catch`.
    
- Przyjmuje `Throwable cause` – dzięki temu, przekazując oryginalny wyjątek `MessagingException`, zachowujemy pełną historię błędu.