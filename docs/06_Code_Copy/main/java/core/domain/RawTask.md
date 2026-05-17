Przechowuje surowe, nieprzetworzone dane pobrane ze źródeł (np. treść maila).
```java
public class RawTask {
    private final String source;
    private final String title;
    // ... reszta kodu ...

    public RawTask(String source, String title, ...) {
        this.source = source;
        this.title = title;
    }
    // ... gettery ...
}
```
* `private final`: Zmienna `final` oznacza, że po przypisaniu jej wartości w konstruktorze, nie można jej już zmienić. To tzw. **niezmienność (immutability)**. Gwarantuje, że nikt przypadkiem nie podmieni surowych danych po ich pobraniu. 
* Brak _setterów_: Ponieważ pola są `final`, nie mamy metod typu `setSource()`. Odczyt jest możliwy tylko przez gettery.