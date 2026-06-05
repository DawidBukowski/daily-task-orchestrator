
To, co widzisz, to **plik konfiguracyjny** . Wyobraź sobie go jako **listę ustawień lub "instrukcję obsługi"** dla programu komputerowego, który automatycznie zarządza Twoimi zadaniami na podstawie wiadomości e-mail.

Zamiast wpisywać te dane bezpośrednio w kodzie (co byłoby niebezpieczne), program odczytuje je z tego pliku, aby wiedzieć, jak ma działać.
#### 1. `app` (Informacje o aplikacji)

To sekcja "wizytówki".

- **`name` i `version`**: Mówią nam, że to program "Daily Task Orchestrator" (Orkiestrator Codziennych Zadań) w wersji 1.0.0.
    

#### 2. `gmail` (Połączenie z Twoją skrzynką)

Tutaj program dowiaduje się, jak połączyć się z Twoim kontem Gmail.

- **`client-id` i `client-secret`**: To Twoje "klucze dostępu". Zapis `${...}` oznacza, że program pobiera je bezpiecznie z systemu, a nie z pliku, żeby nikt postronny nie mógł ich wykraść.
    
- **`redirect-uri`**: Adres strony, na którą zostaniesz odesłany po zalogowaniu się do Gmaila.
    
- **`token-directory`**: Miejsce na Twoim komputerze, gdzie program zapisze "cyfrowy bilet", dzięki któremu nie musisz logować się przy każdym uruchomieniu.
    
- **`scopes`**: To zakres uprawnień. Program mówi Google: "Chcę tylko czytać maile (`readonly`) i móc dodawać do nich etykiety lub je zmieniać (`modify`)". **Nie ma dostępu do Twoich haseł ani możliwości wysyłania maili w Twoim imieniu.**
    

#### 3. `email-fetch` (Jak szukać zadań)

Jak program ma przeglądać Twoją pocztę?

- **`query-limit: 20`**: Przejrzyj tylko 20 ostatnich wiadomości za jednym razem.
    
- **`include-labels`**: Szukaj tylko w folderze `INBOX` (Odebrane) i tylko wśród wiadomości, które są `UNREAD` (Nieprzeczytane).
    
- **`exclude-labels`**: Tu jest pusto, czyli program nie ignoruje żadnych dodatkowych folderów.
    

#### 4. `task-keywords` (Wyszukiwanie zadań)

To najważniejsza część dla "inteligentnej" funkcji programu. Program skanuje treść Twoich e-maili i jeśli znajdzie w nich te słowa (np. _deadline, assignment, quiz, homework_), uzna, że dany e-mail to zadanie do wykonania i doda je do Twojej listy.

#### 5. `logging` (Dziennik zdarzeń)

- **`level: INFO`**: Program będzie zapisywał w pliku dziennika najważniejsze informacje o tym, co robi (np. "Uruchomiono", "Znaleziono 3 nowe zadania"). Poziom `INFO` jest "średnio szczegółowy" – daje wystarczająco dużo wiedzy, nie zaśmiecając pamięci komputera.