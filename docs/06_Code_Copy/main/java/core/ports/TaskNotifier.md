Kontrakt dla systemu powiadomień.
```java
public interface TaskNotifier {
    void notify(AnalyzedTasks tasks);
}
```
* Typ zwracany `void`: Ta metoda niczego nie zwraca. Jej jedynym zadaniem jest wykonanie akcji (np. wysłanie maila, SMS-a, komunikatu na Slacku). W inżynierii nazywamy to "efektem ubocznym" (side effect).