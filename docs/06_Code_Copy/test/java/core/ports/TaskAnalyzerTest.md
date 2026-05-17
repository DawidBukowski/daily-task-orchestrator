Sprawdzenie zachowania analizatora.
```java
@Test
void testAnalyzerContract() {
    AnalyzedTasks mockAnalyzed = TestDataBuilder.buildAnalyzedTasks();
    when(taskAnalyzer.analyze(anyList())).thenReturn(mockAnalyzed);

    // ... wywołanie metody i asercje ...
}
```
* `anyList()`: Argument Matcher z biblioteki Mockito. Bardzo przydatna rzecz! Oznacza to: "Nie obchodzi mnie, jaka konkretnie lista zostanie przekazana do metody `analyze`. Nieważne co to będzie, zmuś atrapę (mock), aby zwróciła przygotowany obiekt `mockAnalyzed`".