
Zewnętrzna konfiguracja aplikacji (nie wkompilowana na sztywno w kod).
```java
app:
  name: Daily Task Orchestrator
  version: 1.0.0
logging:
  level: INFO
```
* *Struktura YAML opiera się na wcięciach (zamiast nawiasów klamrowych w JSON).
* `logging.level: INFO`: Zmienna określająca, jak szczegółowe mają być logi. W pliku testowym zmieniliśmy to na `DEBUG`, aby podczas testów widzieć więcej szczegółów pracy programu.