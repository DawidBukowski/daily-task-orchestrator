## 1. Zmienne statyczne i pola klasy

```java
private static final Logger logger = Logger.getLogger(GmailOAuth2Handler.class.getName());
private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
```

- **`Logger`**: Służy do zapisywania informacji o działaniu programu (logowania). Zamiast używać `System.out.println()`, używamy loggera, aby móc określać poziom ważności (np. INFO, WARNING, SEVERE).
    
- **`JSON_FACTORY`**: Biblioteki Google API komunikują się w formacie JSON. Tutaj używamy `GsonFactory`, co oznacza, że do przetwarzania tych danych klasa używa popularnej biblioteki Gson (od Google). Zmienna jest stała (`static final`), by nie tworzyć jej wielokrotnie w pamięci.

```java
private final GmailConfiguration config;
private final NetHttpTransport httpTransport;
private final boolean isLambdaExecution;
```

- **`config`**: To obiekt Twojej innej klasy, która przechowuje konfigurację (np. Client ID, Client Secret, jakich uprawnień wymaga aplikacja).
    
- **`httpTransport`**: Obiekt odpowiedzialny za fizyczne wysyłanie żądań HTTP przez sieć. Biblioteki Google tego wymagają.
    
- **`isLambdaExecution`**: Zmienna typu prawda/fałsz (boolean). Mówi programowi, czy został uruchomiony w chmurze AWS Lambda, czy u Ciebie na komputerze.
    
## 2. Konstruktor (Inicjalizacja)

```java
public GmailOAuth2Handler(GmailConfiguration config, NetHttpTransport httpTransport) {
    this.config = config;
    this.httpTransport = httpTransport;
    this.isLambdaExecution = "lambda".equalsIgnoreCase(System.getenv("DEPLOYMENT_ENV"));

    logger.info("GmailOAuth2Handler initialized in " +
               (isLambdaExecution ? "LAMBDA" : "LOCAL") + " mode");
}
```

Kiedy tworzysz obiekt tej klasy (używasz `new GmailOAuth2Handler(...)`), uruchamia się ten blok:

- Pola klasy są uzupełniane danymi, które przekazujesz w parametrach.
    
- **Magia dzieje się w 4 linijce:** `System.getenv("DEPLOYMENT_ENV")` zagląda do zmiennych środowiskowych systemu operacyjnego. Jeśli znajdzie tam zmienną o nazwie `DEPLOYMENT_ENV` i jej wartość to `lambda`, zmienna `isLambdaExecution` ustawi się na `true`. Jeśli nie, będzie `false`. Dzięki temu program sam wie, gdzie się znajduje.
    
## 3. Główna metoda: `authenticate()`

Ta metoda zwraca obiekt `Credential`, który jest dla Google jak "dowód osobisty". Aplikacja pokazuje go Google i mówi: "Hej, to ja, mam uprawnienia, żeby czytać/wysyłać maile tego użytkownika".

### Przygotowanie danych do Google

```java
GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
details.setClientId(config.getClientId());
details.setClientSecret(config.getClientSecret());

GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
clientSecrets.setInstalled(details);
```

- **Client ID i Client Secret**: To "login i hasło" samej aplikacji (nie użytkownika Gmaila, tylko Twojego programu). Uzyskujesz je w Google Cloud Console. Powinny być trzymane w tajemnicy (zwłaszcza Secret). Tutaj są pakowane w specjalny obiekt `GoogleClientSecrets`, którego wymaga biblioteka Google.
    

```java
GoogleAuthorizationCodeFlow.Builder flowBuilder = new GoogleAuthorizationCodeFlow.Builder(
        httpTransport, JSON_FACTORY, clientSecrets, config.getScopes())
        .setAccessType("offline");
```

- **`flowBuilder`**: To kreator całego procesu logowania (OAuth2). Dostaje obiekt transportu HTTP, obsługę JSON, Twoje sekrety i `Scopes` (czyli zakres uprawnień, np. "tylko czytaj maile" albo "czytaj i wysyłaj").
    
- **`.setAccessType("offline")`**: **To krytyczna linijka.** Mówi Google: "Moja aplikacja będzie potrzebowała dostępu do konta, nawet gdy użytkownika nie będzie przy komputerze". Dzięki temu Google wyda nie tylko krótko żyjący _Access Token_ (ważny zazwyczaj godzinę), ale też _Refresh Token_ (który nie wygasa i pozwala aplikacji samej odnawiać _Access Token_).
    
### Ścieżka 1: Uruchomienie w AWS Lambda

```java
if (isLambdaExecution) {
    logger.info("Using SecretsManagerDataStoreFactory for token storage");
    String awsRegion = System.getenv("AWS_REGION");
    // ... sprawdzanie czy awsRegion nie jest pusty ...
```

Jeśli kod działa w AWS Lambda:

- Ponieważ nie ma tu ekranu ani przeglądarki, nie możemy prosić użytkownika o kliknięcie "Zezwalam". Program musi pobrać wcześniej zapisany token z bezpiecznego miejsca.
    

```java
    SecretsManagerClient secretsClient = SecretsManagerClient.builder()
        .region(Region.of(awsRegion))
        .credentialsProvider(DefaultCredentialsProvider.create())
        .build();

    flowBuilder.setDataStoreFactory(
        new SecretsManagerDataStoreFactory(secretsClient, "daily-task-orchestrator")
    );
    GoogleAuthorizationCodeFlow flow = flowBuilder.build();
```

- Program łączy się z **AWS Secrets Manager** (to sejf chmurowy na hasła i tokeny).
    
- `SecretsManagerDataStoreFactory` mówi procesowi logowania: "Jeśli szukasz zapisanych tokenów, nie szukaj ich na dysku twardym, tylko połącz się z sejfem w AWS o nazwie `daily-task-orchestrator`".
    

```java
    Credential credential = flow.loadCredential("user");
    if (credential == null) {
        throw new RuntimeException("No OAuth tokens found...");
    }
```

- Program próbuje pobrać gotowy token dla identyfikatora `"user"`. Jeśli go nie ma w sejfie AWS, wyrzuca błąd. Dlaczego? Bo w chmurze Lambda program nie ma fizycznej możliwości samodzielnego wygenerowania nowego tokenu (potrzebna jest do tego przeglądarka internetowa na początku).


```java
    if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() < 60) {
        logger.info("Token expired, refreshing...");
        credential.refreshToken();
    }
    return credential;
```

- Zabezpieczenie: Jeśli token straci ważność za mniej niż minutę (60 sekund), program od razu go odświeża (wykorzystując _Refresh Token_ zdobyty wcześniej). Na koniec zwraca uprawnienia.
    

### Ścieżka 2: Uruchomienie lokalne (Na Twoim komputerze)

```java
} else {
    logger.info("Using FileDataStoreFactory for token storage");
    File tokenFolder = new File(config.getTokenDirectory());
    // ... tworzenie folderu ...
```

Jeśli odpaliłeś ten kod na swoim komputerze (brak zmiennej środowiskowej "lambda"):

- Aplikacja użyje `FileDataStoreFactory`, co oznacza, że zapisze i będzie odczytywać tokeny ze zwykłego folderu na Twoim dysku (np. w katalogu projektu).
    

```java
    flowBuilder.setDataStoreFactory(new FileDataStoreFactory(tokenFolder));
    GoogleAuthorizationCodeFlow flow = flowBuilder.build();
```

- Konfigurujemy proces, by patrzył na dysk, i budujemy przepływ autoryzacji (`flow`).
    

```java
    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
```

- **To najważniejszy moment logowania lokalnego.**
    
- `LocalServerReceiver` otwiera na Twoim komputerze mikro-serwer pod adresem `localhost:8888`.
    
- `AuthorizationCodeInstalledApp` automatycznie otwiera Twoją domyślną przeglądarkę internetową ze stroną logowania Google. Gdy się zalogujesz i zaakceptujesz uprawnienia, Google przekieruje Cię na adres `localhost:8888` razem z tajnym kodem.
    
- Z racji tego, że nasz `LocalServerReceiver` tam nasłuchuje, przejmuje ten kod, automatycznie wymienia go w tle z Google na _Access Token_ i _Refresh Token_, a na koniec zapisuje je do pliku we wcześniej zdefiniowanym folderze.
    

```java
    if (credential.getRefreshToken() != null) {
        logger.info("OAuth2 flow successful. Refresh token acquired and securely stored.");
    } else if (credential.getExpiresInSeconds() != null && credential.getExpiresInSeconds() < 60) {
        logger.info("Token expired, refreshing...");
        credential.refreshToken();
    }
    return credential;
}
```

- Sprawdzenie i zwrócenie obiektu, z którym reszta Twojej aplikacji może już swobodnie pobierać/wysyłać maile.
    

## 4. Obsługa błędów

```java
} catch (IOException e) {
    logger.severe("Authentication failed due to network or IO error: " + e.getMessage());
    throw new RuntimeException("Failed to authenticate with Gmail API", e);
}
```

Cały kod owinięty jest w blok `try-catch`. Gdyby na jakimkolwiek etapie zabrakło internetu, dysk był zabezpieczony przed zapisem albo AWS odrzucił połączenie, zostanie rzucony wyjątek `IOException`. Klasa go przechwytuje, loguje informację o krytycznym błędzie (`logger.severe`), a następnie "ubija" aplikację rzucając `RuntimeException`, ponieważ bez uwierzytelnienia program i tak nie może kontynuować działania.

## Podsumowanie procesu w praktyce

Aby ta aplikacja działała poprawnie, deweloper robi następującą rzecz:

1. Odpala kod na swoim komputerze (Local Mode). Otwiera się przeglądarka, loguje się, a program generuje tokeny i zapisuje je do pliku.
    
2. Deweloper bierze te tokeny z pliku i ręcznie wrzuca je do sejfu AWS (Secrets Manager).
    
3. Deweloper wgrywa kod do chmury (Lambda Mode). Od teraz Lambda sama co jakiś czas odświeża token komunikując się w tle z AWS Secrets Manager, bez żadnej ingerencji człowieka.