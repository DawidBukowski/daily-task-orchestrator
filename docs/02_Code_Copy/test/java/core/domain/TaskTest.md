Testy jednostkowe klasy domenowej [[Task]], weryfikujące logikę biznesową związaną z pojedynczym zadaniem.

```java
class TaskTest {

    @Test
    void testTaskCreation() {
        LocalDateTime now = LocalDateTime.now();
        List<String> tags = Arrays.asList("urgent", "homework");

        Task task = new Task(
                "123",
                "Complete Assignment",
                "Math homework",
                now.plusDays(2),
                Priority.HIGH,
                "Gmail",
                "msg-456",
                TaskStatus.PENDING,
                3.5,
                tags,
                now,
                now,
                "Important task"
        );

        assertEquals("123", task.getId());
        assertEquals("Complete Assignment", task.getTitle());
        assertEquals("Math homework", task.getDescription());
        assertEquals(Priority.HIGH, task.getPriority());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertEquals("Gmail", task.getSource());
        assertEquals("msg-456", task.getOriginalId());
        assertEquals(3.5, task.getEstimatedHours());
        assertEquals(2, task.getTags().size());
        assertTrue(task.getTags().contains("urgent"));
    }
}
```

* `testTaskCreation()`: Sprawdza poprawne działanie pełnego konstruktora encji [[Task]] oraz poprawne zwracanie wartości przez gettery.
* Klasa testuje również kluczowe reguły biznesowe:
    - **Sprawdzanie przeterminowania (`testIsOverdue_...`)**: Weryfikuje zachowanie metody `isOverdue()` dla terminów w przeszłości (powinna zwrócić `true`), w przyszłości (`false`) oraz gdy brak jest określonego terminu (`false`).
    - **Obliczanie czasu do wykonania (`testDaysUntilDue_...`)**: Testuje poprawność obliczania pozostałych dni (metoda `daysUntilDue()`) w przypadku podanego terminu oraz gdy termin nie jest zdefiniowany (powinna zwrócić `0`).
    - **Porównywanie obiektów (`testEqualsAndHashCode()`)**: Weryfikuje zgodność kontraktu metod `equals()` i `hashCode()` dla dwóch obiektów o tożsamych danych.
    - **Modyfikacja pól (`testSettersAndGetters()`)**: Potwierdza poprawne działanie setterów i zmianę stanu encji (np. zmiana statusu na `COMPLETED` czy modyfikacja priorytetu).
