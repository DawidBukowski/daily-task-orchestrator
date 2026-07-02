## Class [[EmailTaskNotifier]] - Powiadomienia e-mail

Klasa odpowiedzialna za wysyłanie (w tej fazie udawanie/logowanie) powiadomień z podsumowaniem codziennych zadań na adres e-mail użytkownika.

```java
public class EmailTaskNotifier implements TaskNotifier {
    private static final Logger logger = LoggerFactory.getLogger(EmailTaskNotifier.class);

    @Override
    public void notify(TasksSummary tasks) {
        logger.info("Sending notification email to user...");
        logger.debug("Notification payload: {}", tasks.getSummary());
    }
}
```

### Wyjaśnienie kodu

* **`implements TaskNotifier`**: Klasa implementuje (wdraża) interfejs [[TaskNotifier]]. Oznacza to, że podpisuje "kontrakt" na dostarczenie konkretnej logiki dla metody `notify`.
* **`logger.info(...)` vs `logger.debug(...)`**: 
  - Poziom `INFO` służy do zapisywania ogólnych kroków w aplikacji (np. "Wysyłanie maila"). 
  - Poziom `DEBUG` służy do logowania szczegółowych danych technicznych przydatnych podczas programowania (np. zawartość wysyłanego obiektu). Dzięki temu nie zaśmiecamy konsoli produkcyjnej długimi tekstami podsumowań, dopóki celowo nie włączymy poziomu debugowania w `application.yaml`.

### Pojęcia dla nowicjuszy

* **Logger / Logowanie**: Zamiast wypisywać komunikaty na ekran przy pomocy prostego `System.out.println`, w profesjonalnych projektach używamy bibliotek logujących (np. SLF4J). Pozwalają one sterować tym, jak szczegółowe informacje mają być widoczne i zapisywać je do plików.
* **Payload**: Dane robocze (treść) przesyłane w wiadomości lub wywołaniu. W tym przypadku jest to sformatowany tekst podsumowania zadań zebrany w obiekcie [[TasksSummary]].
