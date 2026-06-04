Ten kod to tzw. **klient API** (Application Programming Interface). Wyobraź go sobie jako „pośrednika” lub „tłumacza” między Twoją aplikacją a serwerami Gmaila. Zamiast ręcznie pisać skomplikowane zapytania sieciowe, używasz tej klasy, aby w prosty sposób prosić Gmaila o dane.
### 1. Przygotowanie "Bramki" (Konstruktor)

Zanim pobierzesz jakiegokolwiek maila, musisz udowodnić Gmailowi, że masz prawo to zrobić.

- **[[GmailOAuth2Handler]]**: To zewnętrzny element (inny fragment Twojego kodu), który zajmuje się bezpiecznym logowaniem (OAuth2). Zamiast podawać hasło, Twoja aplikacja dostaje specjalny "klucz" (Token).
    
- **`Gmail.Builder`**: To narzędzie z biblioteki Google, które bierze Twój "klucz" (Credential), sposób przesyłania danych (HTTP) oraz sposób rozumienia danych (JSON) i buduje gotowy obiekt `gmailService`. To właśnie ten obiekt jest Twoim bezpośrednim łączem z Gmailem.
### 2. Pobieranie maili (`getEmails` method)

Ta metoda wykonuje dwie główne operacje:

- **Wyszukiwanie (`list`)**:
    
    - Wysyłasz zapytanie do Gmaila: „Daj mi listę maili pasujących do tego zapytania (`query`)”.
        
    - Otrzymujesz `ListMessagesResponse`, ale to jeszcze nie są pełne treści maili – to tylko **lista identyfikatorów** (takie "spisy treści" z numerami ID).
        
- **Pobieranie szczegółów (`get`)**:
    
    - Kod wchodzi w pętlę `for`. Dla każdego znalezionego ID wykonuje osobne zapytanie (`gmailService.users().messages().get(...)`).
        
    - Dopiero tutaj otrzymujesz pełną treść wiadomości, którą dodajesz do listy `messages` i zwracasz do aplikacji.


| Element        | Rola                                                                                                                                                      |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| USER_ID = "me" | Specjalne słowo kluczowe w API Google, które oznacza "użytkownik, który jest aktualnie zalogowany".                                                       |
| IOException    | Błąd sieciowy. Zawsze musisz go obsłużyć, bo internet może działać niestabilnie, a Google może na chwilę zablokować Twoje zapytania (tzw. rate limiting). |
| Logger         | To "czarna skrzynka" Twojej aplikacji. Dzięki niemu wiesz w konsoli, co się dzieje (np. "pobieram maile") bez przerywania działania programu.             |
