Sprawdzenie, czy kontrakt pobierania danych działa prawidłowo.
```java
@ExtendWith(MockitoExtension.class)
class DataSourceTest {
    @Mock
    private DataSource dataSource;

    @Test
    void testDataSourceContract() {
        List<RawTask> mockData = List.of(TestDataBuilder.buildRawTask());
        
        when(dataSource.fetch()).thenReturn(mockData);
        when(dataSource.getName()).thenReturn("MockSource");

        List<RawTask> result = dataSource.fetch();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("MockSource", dataSource.getName());
    }
}
```
* `@Test`: Mówi narzędziu JUnit: "To jest metoda testowa, uruchom ją, gdy wpiszę `mvn test`".
* `List.of(...)`: Nowoczesny (Java 9+) i bardzo wygodny sposób na stworzenie listy z elementami w jednej linijce.
* Asercje (`Asserts`): To są weryfikatory naszego testu.
* `assertNotNull(result)`: Sprawdza, czy wynik przypadkiem nie jest `null` (pusty, nieistniejący). Jeśli jest, test "oblewa".
* `assertEquals(oczekiwanaWartość, faktycznaWartość)`: Sprawdza, czy to co zwróciła aplikacja, jest równe temu, czego oczekiwaliśmy.