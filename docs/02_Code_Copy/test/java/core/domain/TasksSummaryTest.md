Testy jednostkowe weryfikujące logikę agregacji, filtrowania i sortowania zadań w klasie podsumowania [[TasksSummary]].

```java
class TasksSummaryTest {

    @Test
    void testGetTasksSortedByPriority() {
        Task task1 = createTask("1", Priority.LOW, LocalDateTime.now().plusDays(1));
        Task task2 = createTask("2", Priority.CRITICAL, LocalDateTime.now().plusDays(2));
        Task task3 = createTask("3", Priority.MEDIUM, LocalDateTime.now().plusDays(3));
        Task task4 = createTask("4", Priority.HIGH, LocalDateTime.now().plusDays(4));

        TasksSummary summary = new TasksSummary(
                Arrays.asList(task1, task2, task3, task4),
                "Test Summary",
                "Test Schedule",
                List.of("Rec1")
        );

        List<Task> sorted = summary.getTasksSortedByPriority();

        assertEquals(4, sorted.size());
        assertEquals(Priority.CRITICAL, sorted.get(0).getPriority());
        assertEquals(Priority.HIGH, sorted.get(1).getPriority());
        assertEquals(Priority.MEDIUM, sorted.get(2).getPriority());
        assertEquals(Priority.LOW, sorted.get(3).getPriority());
    }
}
```

* `testGetTasksSortedByPriority()`: Sprawdza, czy zadania są poprawnie sortowane malejąco według priorytetu: `CRITICAL` -> `HIGH` -> `MEDIUM` -> `LOW`.
* Klasa testuje również inne operacje na zbiorze zadań:
    - **Zadania na dziś (`getTodaysTasks()`)**: Weryfikuje, czy system poprawnie odfiltrowuje zadania, których termin wykonania przypada na bieżący dzień.
    - **Zadania zaległe (`getOverdueTasks()`)**: Sprawdza, czy poprawnie wybierane są zadania o przekroczonym terminie (`deadline` w przeszłości).
    - **Obsługa wartości null/pustych**: Testuje zachowanie klasy w przypadku przekazania pustej listy zadań (`testEmptyTaskList()`), gwarantując brak błędów typu `NullPointerException` i zwrócenie pustych, bezpiecznych kolekcji.
