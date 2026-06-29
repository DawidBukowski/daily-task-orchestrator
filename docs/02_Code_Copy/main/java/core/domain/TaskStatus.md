## Enum [[TaskStatus]] - Status zadania

Typ wyliczeniowy (enum) reprezentujący aktualny stan realizacji zadania w systemie.

```java
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public static TaskStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PENDING;
        }
        String upperValue = value.trim().toUpperCase();
        try {
            return TaskStatus.valueOf(upperValue);
        } catch (IllegalArgumentException e) {
            return PENDING;
        }
    }
}
```

### Dostępne statusy

- `PENDING` - Zadanie oczekuje na rozpoczęcie (domyślny status dla nowych zadań)
- `IN_PROGRESS` - Zadanie jest w trakcie realizacji
- `COMPLETED` - Zadanie zostało ukończone
- `CANCELLED` - Zadanie zostało anulowane

### Cykl życia zadania

Typowy przepływ statusów:
```
PENDING → IN_PROGRESS → COMPLETED
          ↓
          CANCELLED
```

### Metoda `fromString(String)`

Bezpieczna konwersja tekstowej reprezentacji statusu na wartość enum.

**Przykłady**:
```java
TaskStatus.fromString("COMPLETED")    → TaskStatus.COMPLETED
TaskStatus.fromString("in_progress")  → TaskStatus.IN_PROGRESS
TaskStatus.fromString("TODO")         → TaskStatus.PENDING  // fallback
TaskStatus.fromString(null)           → TaskStatus.PENDING  // fallback
```

### Użycie w systemie

TaskStatus jest używany w:
- Modelu domenowym [[Task]] (pole `status`)
- [[TaskFactory]] - nowe zadania otrzymują domyślnie status `PENDING`
- [[TasksSummary]] - filtrowanie zadań według statusu
