Kontrakt definiujący, jak aplikacja powinna zlecać analizę zadań (np. do sztucznej inteligencji).
```java
public interface TaskAnalyzer {
    AnalyzedTasks analyze(List<Task> tasks);
}
```
* **Wejście:** `List<Task>` – przyjmuje listę znormalizowanych zadań (czyli już po odfiltrowaniu zanieczyszczeń z surowych emaili).
* **Wyjście:** `AnalyzedTasks` – musi zwrócić jeden zbiorczy obiekt, który zawiera posegregowane zadania, harmonogram i podsumowanie.