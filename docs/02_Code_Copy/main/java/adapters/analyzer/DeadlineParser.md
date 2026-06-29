## 🎯 Cel działania klasy
Klasa `DeadlineParser` to narzędzie służące do wyciągania (parsowania) ostatecznych terminów (tzw. deadline'ów) z tekstu zapisanego w języku angielskim. Potrafi ona rozpoznać różne potoczne formaty zapisu dat i zwraca precyzyjny obiekt `LocalDateTime`, reprezentujący obliczony termin.
## 📦 Zależności i pakiety
Kod wykorzystuje nowożytne API dat i czasu z Javy (`java.time.*`), które jest standardem od Javy 8, a także klasy do obsługi wyrażeń regularnych (`java.util.regex.*`), które służą do inteligentnego wyszukiwania wzorców w tekście.
## 🔍 Struktura kodu krok po kroku
### 1. Wyrażenia regularne (Wzorce - Patterns)

Na początku klasy zdefiniowano pięć statycznych, skompilowanych wyrażeń regularnych (`Pattern`). Flagi `(?i)` oraz `Pattern.CASE_INSENSITIVE` oznaczają, że wielkość liter w tekście nie ma znaczenia.

- `DUE_DAY_NAME_PATTERN` – Dopasowuje zwroty typu _"due Monday"_ (do poniedziałku).
    
- `DUE_NUMERIC_DATE_PATTERN` – Dopasowuje daty numeryczne, np. _"due 10/25"_ lub _"due 10/25/2023"_ (miesiąc/dzień lub miesiąc/dzień/rok).
    
- `DUE_MONTH_DAY_YEAR_PATTERN` – Dopasowuje daty opisowe, np. _"due October 25, 2023"_.
    
- `DUE_IN_DAYS_PATTERN` – Dopasowuje zwroty określające czas, np. _"due in 3 days"_ (za 3 dni).
    
- `FINAL_DAY_TIME_PATTERN` – Dopasowuje dokładny czas i dzień, np. _"final: Monday, 5 pm"_.
### 2. Główna metoda publiczna: `extractDeadline`

To serce tej klasy i punkt wejścia dla innych fragmentów programu.

- Przyjmuje dwie wartości: `text` (tekst do analizy) oraz `referenceTime` (czas bazowy, od którego obliczane są terminy, np. "za 3 dni" od _teraz_).
    
- Najpierw sprawdza, czy wprowadzony tekst nie jest pusty.
    
- Następnie działa metodą kaskadową: próbuje dopasować tekst do kolejnych wzorców po kolei.
    
- Zwraca _pierwszy_ pomyślnie znaleziony termin. Jeśli żaden wzorzec nie zostanie odnaleziony, zwraca wartość `null`.
### 3. Metody parsujące (prywatne)

Każda z tych metod używa `Matchera`, by wyciągnąć ze stringa poszczególne grupy znaków i przekształcić je w datę:

- `parseDueDayName` – Szuka w przyszłości pierwszego wystąpienia podanego dnia tygodnia używając `TemporalAdjusters.next()`. Ustawia godzinę upłynięcia terminu domyślnie na koniec dnia: **23:59:00**.
    
- `parseDueNumericDate` – Wyodrębnia miesiąc, dzień i opcjonalnie rok z formatu liczbowego. Jeśli rok jest dwucyfrowy (np. "23"), aplikacja zakłada, że chodzi o XXI wiek i dodaje 2000. Czas to również **23:59:00**.
    
- `parseDueMonthDayYear` – Działa jak metoda powyżej, ale wykorzystuje metodę pomocniczą do zamiany słownej nazwy miesiąca (np. _January_) na liczbę. Ustawia czas na **23:59:00**.
    
- `parseDueInDays` – Odczytuje liczbę dni z tekstu i dodaje ją do czasu bazowego (`referenceTime.plusDays(days)`). Ustawia czas na **23:59:00**.
    
- `parseFinalDayTime` – Znajduje najbliższy podany dzień tygodnia, a następnie odczytuje konkretną godzinę. Obsługuje format 12-godzinny (AM/PM) i odpowiednio przelicza go na format 24-godzinny (np. dodając 12 godzin dla _PM_, jeśli to nie jest 12:00).
### 4. Metody pomocnicze (tłumaczące tekst na typy systemowe)

- `parseDayOfWeek` – Instrukcja `switch`, która przyjmuje string z angielską nazwą dnia (np. _"monday"_) i przypisuje do niej systemowy typ wyliczeniowy `DayOfWeek.MONDAY`.
    
- `parseMonthName` – Instrukcja `switch`, która zamienia angielską nazwę miesiąca na numer od 1 do 12 (np. _"january"_ -> 1). Zwraca `-1`, gdy nazwa jest niepoprawna.
## 1. Czym jest `Pattern`?

`Pattern` to po prostu skompilowany **wzorzec** (szablon). Wyobraź sobie, że to bardzo zaawansowana wersja skrótu `Ctrl+F` (Znajdź). Zamiast szukać konkretnego słowa "poniedziałek", tworzysz wzorzec, który mówi komputerowi: _"Znajdź słowo 'due', po którym jest spacja, a potem dowolny dzień tygodnia"_.

Tworzy się go za pomocą metody `Pattern.compile("twój_wzorzec")`. Kompilacja polega na tym, że Java czyta ten skomplikowany ciąg znaków, analizuje go raz i buduje wewnętrzną strukturę, która pozwoli potem bardzo szybko przeszukiwać dowolny tekst.

### Co to są flagi i co zwracają?

Flagi modyfikują sposób, w jaki działa wzorzec. W Twoim kodzie użyto dwóch rzeczy, które robią dokładnie to samo:

- `(?i)` – to flaga wbudowana w sam tekst wzorca (tzw. inline flag).
    
- `Pattern.CASE_INSENSITIVE` – to stała przekazana jako drugi argument do metody `compile`.
    

Obie mówią Javie: **"Ignoruj wielkość liter"**. Dzięki nim wzorzec rozpozna "due MONDAY", "Due Monday" oraz "due monday" jako to samo dopasowanie.

**Co zwraca `Pattern.compile()`?** Zwraca gotowy do użycia obiekt klasy `Pattern`. Możesz go traktować jak "foremkę do ciastek". Sama foremka niczego nie wytnie, dopóki nie przyłożysz jej do ciasta (tekstu).

## 2. Czym jest `Matcher` i jak łączy się z `Pattern`?

Jeśli `Pattern` to foremka do ciastek (szablon), to `Matcher` jest mechanizmem, który bierze tę foremkę, przykłada ją do konkretnego ciasta (Twojego tekstu) i sprawdza, co udało się wyciąć.

**Jak powstaje połączenie?** Zawsze zaczynasz od wzorca i przekazujesz mu tekst do przeszukania:

Java

```java
Matcher matcher = MOJ_WZORZEC.matcher("tekst_od_uzytkownika");
```

W tym momencie `Matcher` jeszcze niczego nie znalazł. On tylko przygotował sobie tekst i wzorzec. Dopiero wywołanie metody **`matcher.find()`** puszcza go w ruch. `find()` skanuje tekst od lewej do prawej i szuka fragmentu pasującego do wzorca. Jeśli go znajdzie, zwraca `true`, a sam `Matcher` zapamiętuje, w którym miejscu tekstu to dopasowanie wystąpiło.

## 3. Jak działają metody prywatne (i czym są Grupy)?

Żeby zrozumieć metody prywatne, musimy poznać kluczowy mechanizm Regex: **Grupy (Groups)**.

W wyrażeniach regularnych, jeśli zamkniesz jakiś fragment w okrągłe nawiasy `()`, tworzysz grupę przechwytującą. `Matcher` nie tylko mówi "Znalazłem dopasowanie!", ale także pozwala Ci wyciągnąć konkretne kawałki tekstu, które wpadły w te nawiasy.

Spójrzmy, jak to działa na przykładzie metody `parseDueNumericDate`.

### Krok po kroku: Analiza metody `parseDueNumericDate`

Załóżmy, że użytkownik wpisał tekst: _"The homework is due 10/25/2023"_.

Wzorzec `DUE_NUMERIC_DATE_PATTERN` wygląda tak (w uproszczeniu): `due\s+(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?`

Zauważ nawiasy:

- Grupa 1: `(\d{1,2})` – łapie od 1 do 2 cyfr (to będzie miesiąc).
    
- Grupa 2: `(\d{1,2})` – łapie od 1 do 2 cyfr (dzień).
    
- Grupa 3: `(\d{2,4})` – łapie od 2 do 4 cyfr (rok, jest opcjonalny).
    

A teraz logika metody:

1. **`Matcher matcher = DUE_NUMERIC_DATE_PATTERN.matcher(text);`** Tworzymy `Matcher` dla tekstu _"The homework is due 10/25/2023"_.
    
2. **`if (matcher.find())`** Matcher szuka. Znajduje fragment _"due 10/25/2023"_. Wchodzi do środka bloku `if`.
    
3. **`int month = Integer.parseInt(matcher.group(1));`** Tu dzieje się magia. Pytamy matchera: "Co złapałeś w pierwszym nawiasie?". Odpowiada: _"10"_. Zamieniamy to na liczbę.
    
4. **`int day = Integer.parseInt(matcher.group(2));`** "Co masz w drugim nawiasie?". Odpowiedź: _"25"_.
    
5. **`String yearStr = matcher.group(3);`** "Co w trzecim?". Odpowiedź: _"2023"_.
    
6. **Tworzenie daty:** Mając już "czyste" liczby całkowite wyciągnięte z tekstu, metoda składa je w obiekt systemowy: `LocalDateTime.of(year, month, day, 23, 59, 0)`.
    

Wszystkie prywatne metody działają na tym samym schemacie:

1. Przyłóż `Pattern` do tekstu, by stworzyć `Matcher`.
    
2. Użyj `.find()`, żeby sprawdzić, czy wzorzec występuje.
    
3. Wyciągnij wybrane słowa lub liczby używając `.group(numer)`.
    
4. Przekształć wyciągnięte fragmenty tekstu (Stringi) na typy Javy (Integer, Enum, LocalDateTime).
    
5. Zwróć gotową datę (lub `null`, jeśli wzorzec nie pasował).