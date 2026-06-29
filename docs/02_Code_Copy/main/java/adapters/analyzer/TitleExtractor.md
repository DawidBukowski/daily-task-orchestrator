Klasa `TitleExtractor` (Narzędzie do wyodrębniania tytułów) służy do inteligentnego generowania lub wydobywania tytułu dla danego zadania na podstawie obiektu [[RawData]] (surowych danych).

Jeśli obiekt posiada już sensowny tytuł, klasa go zwraca. Jeśli nie, analizuje treść (zawartość) w poszukiwaniu określonych wzorców (np. "Assignment 1: Math"), a jeśli to zawiedzie, generuje tytuł ze skróconego fragmentu treści.

## 1. Wyrażenia regularne (Regex) - Wzorce poszukiwań

Na samej górze klasy zdefiniowano trzy stałe typu `Pattern`, które wykorzystują wyrażenia regularne do szukania konkretnych fraz w tekście niezależnie od wielkości liter (`(?i)` oraz `Pattern.CASE_INSENSITIVE`):

- **`ASSIGNMENT_PATTERN`**: Szuka tekstu w formacie `Assignment [liczba]: [tekst]`.
    
    - _Przykład pasujący:_ "Assignment 12: Math Homework"
        
- **`PROJECT_PATTERN`**: Szuka tekstu w formacie `Project: [tekst]`.
    
    - _Przykład pasujący:_ "project: Science Fair"
        
- **`QUIZ_PATTERN`**: Szuka tekstu w formacie `Quiz on [tekst]`.
    
    - _Przykład pasujący:_ "Quiz on Biology"
        

## 2. Główna metoda: `extractTitle(RawData rawData)`

To serce tej klasy. Jej działanie można podzielić na następujące kroki:

1. **Zabezpieczenie przed brakiem danych (Null check):** Jeśli do metody zostanie przekazany obiekt `null`, natychmiast zwracana jest wartość domyślna `"Untitled Task"`.
    
2. **Sprawdzenie istniejącego tytułu:** Pobiera oryginalny tytuł z obiektu (`rawData.getTitle()`). Jeśli ten tytuł nie jest pusty i **nie jest tzw. "wypełniaczem"** (np. słowem "Brak tytułu" – co weryfikuje metoda `isPlaceholder`), zwraca ten oryginalny tytuł, usuwając białe znaki na początku i na końcu (`trim()`).
    
3. **Brak tytułu – analiza treści:** Jeśli pierwotny tytuł był pusty lub był wypełniaczem, metoda pobiera treść (`rawData.getRawContent()`). Jeśli treść również jest pusta, zwraca `"Untitled Task"`.
    
4. **Szukanie wzorców (Regex) w treści:** Kod kolejno próbuje dopasować wyrażenia regularne w treści zadania:
    
    - Najpierw szuka słowa "Assignment...". Jeśli znajdzie, zwraca dopasowany fragment.
        
    - Jeśli nie, szuka słowa "Project...".
        
    - Jeśli nie, szuka słowa "Quiz...".
        
5. **Ostateczność – ucięcie treści:** Jeśli w tekście nie odnaleziono żadnego z powyższych wzorców, metoda pobiera pierwsze 50 znaków z treści zadania (za pomocą metody `extractFirstNCharacters`) i traktuje je jako tytuł.
    

## 3. Metody pomocnicze (prywatne)

Klasa korzysta z trzech mniejszych, prywatnych metod, które pomagają utrzymać główny kod w czystości:

### `isPlaceholder(String title)`

Sprawdza, czy dostarczony tytuł jest "śmieciowy" (nie niesie ze sobą wartości informacyjnej). Zamienia tytuł na małe litery i sprawdza, czy zawiera frazy takie jak:

- `untitled`
    
- `no subject`
    
- `(no title)`
    
- lub czy jest równe `n/a` (not applicable). Jeśli tak, zwraca `true`, co oznacza, że klasa powinna spróbować wygenerować własny tytuł.
    

### `tryExtractPattern(String content, Pattern pattern)`

Przyjmuje treść tekstu i konkretny wzorzec (Regex). Używa obiektu `Matcher` do znalezienia pierwszego dopasowania w tekście. Jeśli znajdzie pasujący fragment, zwraca go (pozbawionego zbędnych spacji). W przeciwnym razie zwraca `null`.

### `extractFirstNCharacters(String content, int maxLength)`

Służy do skracania długich tekstów:

- Pobiera tekst i ucina spacje.
    
- Sprawdza jego długość. Jeśli jest równy lub krótszy niż `maxLength` (w tym wypadku 50 znaków), zwraca go w całości.
    
- Jeżeli tekst jest dłuższy, ucina go do ustalonego limitu i dodaje wielokropek (`...`) na końcu, dając użytkownikowi znać, że tytuł to tylko fragment większej całości.