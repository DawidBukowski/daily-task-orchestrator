```java
private final List<Task> allTasks;
private final String summary;
private final String schedule;
private final List<String> recommendations;
```
Klasa posiada cztery prywatne, niezmienne (oznaczone słowem kluczowym `final`) pola:

- `allTasks` – lista wszystkich przypisanych zadań (obiektów typu [[Task]]).
    
- `summary` – tekstowe podsumowanie.
    
- `schedule` – tekstowy opis harmonogramu.
    
- `recommendations` – lista z tekstowymi rekomendacjami lub poradami.
    

Ponieważ pola są `final`, muszą zostać zainicjowane podczas tworzenia obiektu i nie mogą zostać później nadpisane nowymi referencjami
## Konstruktor i podstawowe Gettery

```java
public TasksSummary(List<Task> allTasks, String summary, String schedule, List<String> recommendations) { ... }
```
Konstruktor służy do tworzenia nowego obiektu `TasksSummary` i przypisania wartości do wszystkich jego pól. Poniżej konstruktora znajdują się cztery **gettery** (np. `getAllTasks()`), które pozwalają na bezpieczny odczyt wartości tych prywatnych pól z zewnątrz klasy.
## Metody przetwarzające dane (z wykorzystaniem Java Streams)

Klasa zawiera trzy metody, które wykorzystują **[[Stream API]]** do wyciągania specyficznych informacji z listy `allTasks`. W każdej z nich znajduje się na początku zabezpieczenie przed błędem `NullPointerException` (jeśli `allTasks == null`, zwracana jest pusta lista `List.of()`).

Sortowanie po priorytecie
```java
public List<Task> getTasksSortedByPriority()
```
- **Działanie:** Zwraca listę zadań posortowaną malejąco według priorytetu (od najważniejszych do najmniej ważnych).
    
- **Mechanizm:** Używa metody `.stream()`, a następnie `.sorted(...)`. Sortowanie odbywa się na podstawie metody `getNumericValue()` z obiektu priorytetu zadania. Słowo kluczowe `.reversed()` sprawia, że sortowanie jest malejące. Na koniec `.collect(Collectors.toList())` zamienia strumień z powrotem w listę.

Zadania na dzisiaj
```java
public List<Task> getTodaysTasks()
```
- **Działanie:** Zwraca listę zadań, których termin wykonania (deadline) przypada na dzisiejszy dzień.
    
- **Mechanizm:** Najpierw pobiera aktualną datę (`LocalDate today = LocalDate.now();`). Następnie filtruje (`.filter(...)`) strumień zadań. Zostawia tylko te zadania, które mają zdefiniowany termin (`task.getDeadline() != null`) oraz których wyodrębniona data (`.toLocalDate()`) jest równa dzisiejszej dacie.

Zadania zaległe
```java
public List<Task> getOverdueTasks()
```
- **Działanie:** Zwraca listę zadań, które nie zostały wykonane na czas.
    
- **Mechanizm:** Przeszukuje listę i zostawia tylko te zadania, dla których metoda `isOverdue()` (zdefiniowana w klasie `Task`) zwraca wartość `true`. Składnia `Task::isOverdue` to tzw. _method reference_ (referencja do metody), która sprawia, że kod jest krótszy i bardziej czytelny.
## Metoda `toString()`
```java
@Override
public String toString() { ... }
```
Przesłania standardową metodę `toString()` z klasy `Object`. Dzięki temu, gdy spróbujesz wydrukować obiekt `TasksSummary` w konsoli (np. przez `System.out.println()`), zamiast niezrozumiałego adresu w pamięci, zobaczysz czytelny napis:

- Liczbę wszystkich zadań (korzysta z wyrażenia trójargumentowego `allTasks != null ? allTasks.size() : 0`, aby uniknąć błędu, gdy lista jest nullem).
    
- Tekst przypisany do pola `summary`