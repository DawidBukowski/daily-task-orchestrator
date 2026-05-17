Konkretna implementacja łącząca się z Gmailem.
```java
public class GmailDataSource implements DataSource {
    private static final Logger logger = LoggerFactory.getLogger(GmailDataSource.class);

    @Override
    public List<RawTask> fetch() {
        logger.info("Fetching raw tasks from Gmail...");
        return new ArrayList<>();
    }
}
```
* `implements DataSource`: Ta klasa podpisuje kontrakt. Musi posiadać metody `fetch()` i `getName()`.
* `Logger logger`: Obiekt tworzony raz dla całej klasy (`static final`). Służy do wypisywania informacji w konsoli. Zastępuje archaiczne `System.out.println()`, dodając datę, czas i poziom powagi (INFO, DEBUG, ERROR).
* `@Override`: Adnotacja informująca kompilator i innych programistów: "Ta metoda pochodzi z interfejsu. Upewnij się, że użyłem poprawnej nazwy".