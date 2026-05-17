Sprawdzenie, czy metoda wysyłająca powiadomienie faktycznie się wywołuje.
```java
@Test
void testNotifierContract() {
    AnalyzedTasks payload = TestDataBuilder.buildAnalyzedTasks();
    
    taskNotifier.notify(payload);
    
    verify(taskNotifier).notify(payload);
}
```
* Ponieważ metoda `notify` jest typu `void` (nic nie zwraca), nie możemy użyć tu `assertEquals` (bo nie ma czego z czym porównać!).
* Zamiast tego używamy funkcji `verify()` z biblioteki Mockito. Mówi ona: "Sprawdź, czy na obiekcie `taskNotifier` została wywołana metoda `notify`, przyjmująca dokładnie te same dane (`payload`), które przekazałem". To tzw. **Testowanie Zachowania (Behavior Verification)**.