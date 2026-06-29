## Enum [[Priority]] - Priorytety zadań

Typ wyliczeniowy (enum) reprezentujący poziom ważności zadania w systemie.

```java
public enum Priority {
    CRITICAL(4),
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int numericValue;

    Priority(int numericValue) {
        this.numericValue = numericValue;
    }

    public int getNumericValue() {
        return numericValue;
    }

    public static Priority fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return MEDIUM;
        }
        String upperValue = value.trim().toUpperCase();
        try {
            return Priority.valueOf(upperValue);
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
```

### Struktura i wartości

Każdy priorytet ma przypisaną **wartość numeryczną**:
- `CRITICAL(4)` - Krytyczny, najwyższy priorytet
- `HIGH(3)` - Wysoki priorytet
- `MEDIUM(2)` - Średni priorytet (domyślny)
- `LOW(1)` - Niski priorytet

Wartości numeryczne są używane do **sortowania** zadań - wyższa liczba = wyższy priorytet.

### Metoda `fromString(String)`

Bezpieczna konwersja tekstowej reprezentacji priorytetu na wartość enum.

**Przykłady**:
```java
Priority.fromString("HIGH")    → Priority.HIGH
Priority.fromString("critical") → Priority.CRITICAL  // case-insensitive
Priority.fromString("invalid")  → Priority.MEDIUM    // fallback
Priority.fromString(null)       → Priority.MEDIUM    // fallback
```

**Cechy**:
- **Case-insensitive**: wielkość liter nie ma znaczenia
- **Bezpieczny fallback**: nieprawidłowe wartości zwracają `MEDIUM`
- **Null-safe**: null lub pusty string zwraca `MEDIUM`

### Użycie w systemie

Priority jest używany w:
- Modelu domenowym [[Task]] (pole `priority`)
- [[TaskFactory]] - mapowanie z [[RawData]] (tekst → enum)
- [[TasksSummary]] - sortowanie zadań metodą `getTasksSortedByPriority()`

### Korzyści z użycia enum zamiast String

1. **Type safety**: kompilator nie pozwoli przypisać nieprawidłowej wartości
2. **Autocomplete**: IDE podpowiada dostępne wartości
3. **Łatwiejsze sortowanie**: wbudowane wartości numeryczne
