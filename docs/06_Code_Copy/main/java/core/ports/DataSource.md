Definiuje, jak aplikacja komunikuje się z każdym zewnętrznym źródłem danych.
```java
public interface DataSource {
    List<RawTask> fetch();
    String getName();
}
```
* `interface`: Nie ma tu ciała metod (brak klamerek `{}`). Wymuszamy tylko, aby każda klasa udająca źródło danych miała metodę `fetch()`, która zwraca listę obiektów `RawTask`