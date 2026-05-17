Definiuje, jak zbudować projekt i z jakich zewnętrznych bibliotek (zależności) korzystamy.
```java
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.dailytask</groupId>
    <artifactId>daily-task-orchestrator</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <!-- ... (reszta kodu z poprzedniej odpowiedzi) ... -->
</project>
```

* *`<groupId>`, `<artifactId>`, `<version>`: Unikalne współrzędne Twojego projektu. `SNAPSHOT` oznacza wersję w trakcie developmentu.
* `<properties>`: Zmienne w Mavenie. Ustawiamy tu wersję Javy (`17`) oraz wersje bibliotek (np. `slf4j.version`), żeby móc je łatwo aktualizować w jednym miejscu.
* `<dependencies>`: Lista zewnętrznych narzędzi, które Maven pobierze automatycznie (np. JUnit do testów, SLF4J do logowania). Zwróć uwagę na `<scope>test</scope>` – oznacza to, że narzędzia takie jak Mockito będą dostępne tylko w kodzie testowym, a nie w finalnej aplikacji.
* `<plugins>`: Wtyczki wykonujące pracę podczas budowania (np. `maven-compiler-plugin` tłumaczy kod Javy na kod maszynowy/bajtowy).