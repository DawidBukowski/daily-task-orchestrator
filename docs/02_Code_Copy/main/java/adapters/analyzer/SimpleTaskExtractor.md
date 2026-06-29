- **Implementacja interfejsu**: Klasa implementuje interfejs [[TaskExtractor]], deklarując, że dostarcza konkretną logikę dla operacji zdefiniowanych w tym interfejsie.
    
- **Pole `taskFactory`**: Służy do tworzenia nowych obiektów typu [[Task]]. Zostało oznaczone jako `final`, co oznacza, że po przypisaniu w konstruktorze nie można go już zmienić.
    
- **Konstruktor**: Inicjalizuje pole `taskFactory`, tworząc nową instancję klasy [[TaskFactory]]. _(Uwaga architektoniczna: w bardziej zaawansowanych projektach częściej używa się tzw. wstrzykiwania zależności np. przez [[Spring]], zamiast używać słowa kluczowego `new` bezpośrednio w konstruktorze)._
    

### 2. Metoda `extract`
```java
@Override
public List<Task> extract(List<RawData> rawDataList) {
```

Jest to główna metoda klasy, nadpisana (`@Override`) z interfejsu. Przyjmuje listę surowych danych i zwraca listę przetworzonych zadań.

#### Walidacja wejścia (Defensive programming)
```java
if (rawDataList == null || rawDataList.isEmpty()) {
    return new ArrayList<>();
}
```

Kod na samym początku sprawdza, czy przekazana lista nie jest pusta, ani czy nie jest tzw. `nullem`. Jeśli tak jest, bezpiecznie zwraca nową, pustą listę. Zapobiega to wystąpieniu popularnego błędu `NullPointerException` w dalszej części kodu.

#### Główna pętla i przetwarzanie
```java
List<Task> tasks = new ArrayList<>();
for (RawData rawData : rawDataList) {
    try {
        Task task = taskFactory.createFromRawData(rawData);
        tasks.add(task);
    } catch (Exception e) {
        System.err.println("Failed to extract task from raw data: " + e.getMessage());
    }
}
```

- Tworzona jest pusta lista `tasks`, która będzie przechowywać prawidłowo utworzone zadania.
    
- Pętla `for` przechodzi przez każdy element (`rawData`) z dostarczonej listy.
    
- **Blok `try-catch`**: Konwersja danych (linijka `taskFactory.createFromRawData(rawData)`) znajduje się wewnątrz bloku `try`. To celowy zabieg zapewniający odporność na błędy (tzw. fault tolerance).
    
    - Jeśli dane są uszkodzone i fabryka wyrzuci błąd (wyjątek), aplikacja nie zawiesi się.
        
    - Błąd zostanie przechwycony przez blok `catch`, w konsoli na czerwono (`System.err.println`) pojawi się komunikat informujący o problemie, a pętla po prostu przejdzie do kolejnego elementu. Wadliwe dane zostaną zignorowane.