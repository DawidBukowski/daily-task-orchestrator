Udaje mechanizm wysyłania powiadomień na adres e-mail użytkownika.
```java
public class EmailTaskNotifier implements TaskNotifier {
    private static final Logger logger = LoggerFactory.getLogger(EmailTaskNotifier.class);

    @Override
    public void notify(AnalyzedTasks tasks) {
        logger.info("Sending notification email to user...");
        logger.debug("Notification payload: {}", tasks.getSummary());
    }
}
```
* `logger.debug(...)`: Zauważ różnicę! Użyliśmy poziomu `DEBUG`, a nie `INFO`. Dlaczego? Zwykle nie chcemy zaśmiecać konsoli pełną treścią każdego powiadomienia, chyba że aplikacja działa w trybie testowym lub szukamy błędu (wtedy zmieniamy poziom logowania w `application.yaml` na `DEBUG`).