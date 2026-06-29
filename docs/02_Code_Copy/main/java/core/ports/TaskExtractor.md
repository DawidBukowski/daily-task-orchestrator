```markdown
Kontrakt odpowiedzialny za zamianę surowych danych zewnętrznych na znormalizowane `Task`.
```java
public interface TaskExtractor {
    List<Task> extract(List<RawData> rawData);
}
```
* **Wejście:** `List<RawData>` — lista surowych danych pobranych z Gmaila lub stron profesorów.
* **Wyjście:** `List<Task>` — już znormalizowane zadania, które można dalej analizować i podsumowywać.
* Ten port oddziela pobieranie danych od ich interpretacji, dzięki czemu łatwiej podmienić logikę z reguł na Claude AI.