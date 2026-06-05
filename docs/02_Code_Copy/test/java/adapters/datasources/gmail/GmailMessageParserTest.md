### 1. Co to jest? (Kontekst)

- **Technologia:** Java z użyciem frameworka **JUnit 5**.
    
- **Cel:** Klasa [[GmailMessageParser]] ma za zadanie wziąć skomplikowany obiekt `Message` (dostarczany przez bibliotekę Google API) i wyciągnąć z niego to, co nas interesuje (ID wiadomości, temat, nadawcę i treść), zamieniając to na nasz własny, prostszy obiekt `GmailMessage`.
### 2. Kluczowe elementy testu

- **`@BeforeEach` (Metoda `setUp`)**: To instrukcja "przygotuj się". Przed każdym pojedynczym testem tworzymy nową instancję `parsera`. Dzięki temu każdy test zaczyna "z czystą kartą".
    
- **`@Test`**: Oznacza metodę, która jest właściwym testem. JUnit uruchomi każdą taką metodę po kolei.
    
- **`assertEquals` / `assertNull`**: To "sędziowie" meczu. Sprawdzają: „Czy to, co wyszło z metody `parse`, jest dokładnie tym, czego oczekuję?”. Jeśli wynik się zgadza, test przechodzi. Jeśli nie – test wywala błąd.
    

### 3. Analiza dwóch scenariuszy

Kod sprawdza dwie sytuacje:

#### Scenariusz A: `parse_Success` (Szczęśliwa ścieżka)

Tutaj udajemy sytuację idealną:

1. **Tworzymy sztuczną wiadomość:** Symulujemy wiadomość z Google API, dodając jej ID, nagłówki (temat, od kogo) oraz treść (ciało wiadomości).
    
2. **Kodowanie Base64:** Google API często wysyła tekst zakodowany (Base64). Test sprawdza, czy Twój parser potrafi to poprawnie "odkodować" do zwykłego tekstu.
    
3. **Weryfikacja:** Sprawdzamy, czy wszystkie dane (ID, temat, nadawca, treść) po przejściu przez parser są dokładnie takie same, jak te, które wcześniej przygotowaliśmy.
    

#### Scenariusz B: `parse_MissingHeaders_HandlesGracefully` (Odporność na błędy)

Programista musi przewidzieć, co się stanie, gdy dane będą niepełne:

1. **Pusta wiadomość:** Tworzymy wiadomość, która nie ma żadnych nagłówków ani treści.
    
2. **Sprawdzenie zachowania:** Test upewnia się, że program się nie "wywali" (nie rzuci błędem), tylko grzecznie zwróci wartości `null` dla tematu/nadawcy i komunikat "No plain text body found." dla treści.