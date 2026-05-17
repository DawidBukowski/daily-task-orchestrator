Ustandaryzowany format zadania, z którym aplikacja potrafi pracować.
```java
public class Task {
    private String id;
    private String title;
    // ...
}
```
* Tutaj zmienne **nie są** `final` i posiadają _settery_. Dlaczego? Ponieważ standardowe zadanie może ewoluować – sztuczna inteligencja może chcieć zaktualizować `priority` (priorytet) lub zmienić `status` w trakcie analizy.