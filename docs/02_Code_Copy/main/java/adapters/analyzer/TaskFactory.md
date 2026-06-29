Klasa posiada dwie zależności (narzędzia pomocnicze), których wartości nie można zmienić po utworzeniu (`final`):

- **`DeadlineParser`** – obiekt odpowiedzialny za odczytywanie terminu wykonania (deadline) np. z tekstu.
    
- **`TitleExtractor`** – obiekt służący do wyodrębniania tytułu zadania z podanych danych.
    

W konstruktorze (metodzie wywoływanej przy tworzeniu instancji `TaskFactory`) obiekty te są inicjalizowane, aby były gotowe do użycia.

## 1. Główna metoda: `createFromRawData`

To serce tej klasy. Metoda przyjmuje surowe dane ([[RawData]]) i zamienia je w ustrukturyzowane Zadanie ([[Task]]).

### Krok po kroku wewnątrz metody:

**A. Walidacja danych wejściowych:**
```java
if (rawData == null) {
    throw new IllegalArgumentException("RawData cannot be null");
}
```

Aplikacja zabezpiecza się przed błędem. Jeśli spróbujemy stworzyć zadanie z niczego (`null`), program natychmiast przerwie działanie tej metody rzucając odpowiedni wyjątek.

**B. Przetwarzanie i wyciąganie informacji (Ekstrakcja):**

- `id` – program generuje w 100% losowy, unikalny na skalę światową identyfikator tekstowy przy użyciu `UUID.randomUUID().toString()`.
    
- `title` – używa wbudowanego narzędzia `titleExtractor`, by stworzyć z surowych danych krótki tytuł.
    
- `description` – pobiera treść z surowych danych. Konstrukcja z `? :` upewnia się, że jeśli treść nie istnieje (`null`), to jako opis przypisany zostanie bezpieczny pusty ciąg znaków (`""`).
    
- `deadline` – korzysta z parsera, który analizuje surowy tekst i czas pobrania danych, żeby zgadnąć lub wyciągnąć ostateczny termin wykonania zadania.
    
- `priority` – zamienia tekstową reprezentację priorytetu z surowych danych na bezpieczny typ wyliczeniowy (Enum) `Priority`.
    

**C. Przenoszenie metadanych:**

- `source` oraz `originalId` – to informacje informujące z jakiego zewnętrznego źródła i pod jakim pierwotnym ID pochodzą dane.
    

**D. Ustawianie wartości domyślnych dla nowego zadania:**

- `status` – każde nowo utworzone zadanie automatycznie dostaje status `PENDING` (Oczekujące).
    
- `estimatedHours` – początkowo jest ustawione na `null` (brak oszacowanego czasu wykonania).
    
- `tags` – zadanie startuje z pustą listą tagów (`new ArrayList<>()`).
    
- `createdAt` oraz `updatedAt` – system zapisuje dokładny moment utworzenia obiektu (aktualna data i godzina) jako czas stworzenia oraz ostatniej aktualizacji.
    
- `notes` – notatki na start są puste (`""`).
    

**E. Budowanie i zwracanie obiektu:**
```java
return new Task( id, title, description, ... );
```

Na samym końcu, wszystkie zgromadzone, wygenerowane i przetworzone zmienne są wstrzykiwane do konstruktora klasy `Task`. Metoda zwraca ten nowy, pełnoprawny obiekt, z którym reszta Twojej aplikacji może już swobodnie pracować.