## 1. Deklaracja klasy

```java
public class PriorityColorScheme {
```

- **`public`**: Modyfikator dostępu. Oznacza, że ta klasa może być używana przez inne klasy w dowolnym miejscu Twojej aplikacji.
    
- **`class`**: Słowo kluczowe definiujące, że tworzymy nowy typ obiektowy (klasę).
    
- **`PriorityColorScheme`**: Nazwa klasy (z ang. _Schemat Kolorów Priorytetów_). Zaczyna się wielką literą, zgodnie z konwencjami Javy.
    

## 2. Zmienne klasy (Stałe)

```java
    // Background colors (Bootstrap-inspired)
    private static final String CRITICAL_COLOR = "#dc3545"; // Red
    // ... reszta kolorów ...
```

Ta sekcja definiuje kody kolorów w formacie HEX (szesnastkowym, używanym w HTML/CSS). Komentarz mówi, że kolory są inspirowane popularnym frameworkiem "Bootstrap".

Dlaczego użyto tu słów `private static final`?

- **`private`**: Zmienne te są ukryte wewnątrz tej klasy. Nikt z zewnątrz nie może bezpośrednio wpisać `PriorityColorScheme.CRITICAL_COLOR`. Muszą użyć do tego odpowiednich metod (opisanych niżej). To tzw. _hermetyzacja_.
    
- **`static`**: Zmienna należy do samej klasy, a nie do jej pojedynczych instancji (obiektów). Niezależnie od tego, ile obiektów `PriorityColorScheme` stworzysz, te kolory będą zapisane w pamięci tylko raz. Oszczędza to zasoby.
    
- **`final`**: Oznacza, że wartość tej zmiennej jest **stała** i nie może zostać nigdy zmieniona po jej zainicjowaniu. Dlatego nazwy tych zmiennych są pisane WIELKIMI_LITERAMI (to konwencja w Javie dla stałych).
    
- **`String`**: Typ danych – tekst.
    

**Ważny szczegół:** Komentarz `WCAG AA compliant` przy kolorach tekstu (`WHITE_TEXT`, `DARK_TEXT`) oznacza, że programista zadbał o dostępność (Web Content Accessibility Guidelines). Chodzi o to, aby kontrast między tłem a tekstem był wystarczający dla osób niedowidzących.

## 3. Metoda: getColor (Zwracanie koloru tła)

```java
    public String getColor(Priority priority) {
        if (priority == null) {
            return MEDIUM_COLOR; // Default to medium
        }

        return switch (priority) {
            case CRITICAL -> CRITICAL_COLOR;
            case HIGH -> HIGH_COLOR;
            case MEDIUM -> MEDIUM_COLOR;
            case LOW -> LOW_COLOR;
        };
    }
```

- **`public String getColor(Priority priority)`**: To publiczna metoda zwracająca tekst (`String`). Jako argument przyjmuje zmienną `priority` typu `Priority`. Choć tego nie ma w tym kodzie, `Priority` to z pewnością **Enum** (typ wyliczeniowy), który wygląda mniej więcej tak: `public enum Priority { CRITICAL, HIGH, MEDIUM, LOW }`.
    
- **`if (priority == null)`**: To jest tzw. _Defensive Programming_ (programowanie defensywne). Jeśli ktoś przez błąd przekaże do metody wartość pustą (`null`), program nie wyrzuci błędu krytycznego (słynnego `NullPointerException`), tylko bezpiecznie domyślnie zwróci kolor żółty (`MEDIUM_COLOR`).
    
- **`return switch (priority) { ... };`**: To jest **nowoczesne wyrażenie Switch** (wprowadzone w Javie 14).
    
    - W starym stylu Javy trzeba by pisać `case CRITICAL: return CRITICAL_COLOR; break;`.
        
    - Tutaj użyto składni ze strzałką `->`. Oznacza to: _"Jeśli priorytet to CRITICAL, zwróć CRITICAL_COLOR"_.
        
    - Cały blok `switch` od razu zwraca (`return`) wynik dopasowania, co sprawia, że kod jest bardzo czysty i krótki.
        

## 4. Metoda: getTextColor (Zwracanie koloru tekstu)

```java
    public String getTextColor(Priority priority) {
        if (priority == null) {
            return DARK_TEXT; // Default matches MEDIUM
        }

        return switch (priority) {
            case CRITICAL, HIGH -> WHITE_TEXT;  
            case MEDIUM, LOW -> DARK_TEXT;      
        };
    }
```

Działanie jest bardzo podobne do poprzedniej metody, ale rozwiązuje inny problem: **czytelność tekstu**. Jeśli tło zadania jest krytyczne (ciemnoczerwone), czarny tekst byłby na nim niewidoczny. Dlatego metoda zwraca `WHITE_TEXT`. Jeśli tło jest żółte (`MEDIUM`), biały tekst byłby nieczytelny, więc zwraca `DARK_TEXT`.

- **Ciekawostka składniowa (`case CRITICAL, HIGH ->`)**: Zauważ, że w tym nowym `switch` można grupować przypadki po przecinku. Znaczy to: _"Jeśli priorytet to CRITICAL lub HIGH, zwróć WHITE_TEXT"_.
    

## 5. Metoda: getColorName (Nazwa koloru)

```java
    public String getColorName(Priority priority) {
        if (priority == null) {
            return "yellow";
        }

        return switch (priority) {
            case CRITICAL -> "red";
            case HIGH -> "orange";
            case MEDIUM -> "yellow";
            case LOW -> "green";
        };
    }
```

Metoda ta zwraca po prostu ludzkie nazwy kolorów (np. "red"). Do czego to służy?

- **Dostępność (Accessibility):** Czytniki ekranu dla osób niewidomych nie przeczytają kodu "#dc3545", ale mogą przeczytać słowo "red".
    
- **Klasy CSS:** Może to być używane na froncie (w HTML) do dynamicznego przypisywania klas CSS (np. `<div class="badge badge-red">`).