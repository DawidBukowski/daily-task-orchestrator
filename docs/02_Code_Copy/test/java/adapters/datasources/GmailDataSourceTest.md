### 1. Przygotowanie "aktorów" (Mockowanie)

Zauważ, że w metodzie `setUp` używamy funkcji `mock(...)`. W programowaniu **mock** to taki "podstawiony aktor".

- Zamiast używać prawdziwego klienta Gmaila (który wymaga hasła i internetu), używamy "aktora", który udaje klienta.
    
- Dzięki temu test jest szybki i nie zależy od tego, czy Google akurat działa.
    

### 2. Scenariusz testowy (`@Test`)

Test o nazwie `fetch_ProcessesEmailsAndSkipsFailures` sprawdza konkretną sytuację: **co się stanie, gdy przyjdzie jedna wiadomość poprawna, a jedna zepsuta?**

Oto kroki, które wykonuje test:

- **Ustawienie zachowań (`when`):** Mówimy naszym "aktorom", jak mają się zachować:
    
    - [[GmailApiClient]] ma zwrócić dwie wiadomości (jedną "1", drugą "2").
        
    - `parser` ma pomyślnie przetworzyć pierwszą wiadomość, ale przy drugiej ma zgłosić błąd (`throw new RuntimeException`).
        
- **Akcja:** Uruchamiamy główną metodę `dataSource.fetch(...)`.
    
- **Weryfikacja (`assertEquals` i `verify`):** Sprawdzamy, czy system zachował się zgodnie z oczekiwaniami:
    
    - Czy zwrócił tylko 1 poprawną wiadomość (mimo że dostał dwie)?
        
    - Czy `parser` faktycznie próbował przetworzyć obie wiadomości?