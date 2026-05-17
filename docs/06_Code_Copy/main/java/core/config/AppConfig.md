Ręczny "Wstrzykiwacz Zależności" (Dependency Injection Container). Zamiast tworzyć obiekty byle gdzie, centralizujemy to.
```java
public static List<DataSource> createDataSources() {
    return List.of(new GmailDataSource());
}
```
* Metody są `static` – można je wywołać bezpośrednio z nazwy klasy (np. `AppConfig.createAnalyzer()`), bez potrzeby tworzenia obiektu (zmiennego instancji) klasy `AppConfig`. Zwracają typy interfejsów, co ułatwia ewentualną zmianę z `GmailDataSource` na np. `OutlookDataSource`.