Masz przed sobą kod napisany w formacie **YAML**, który definiuje infrastrukturę w chmurze AWS przy użyciu narzędzia **AWS SAM (Serverless Application Model)**.

AWS SAM to nakładka na standardowe AWS CloudFormation, która ułatwia i drastycznie skraca zapis potrzebny do stworzenia zasobów bezserwerowych (takich jak funkcje Lambda czy bazy danych).

## 1. Sekcje główne szablonu (Metadata)

Na samym początku definiujemy, z jakich narzędzi AWS ma skorzystać do odczytania tego pliku.

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: Daily Task Orchestrator - AWS Lambda Deployment Template
```

- **`AWSTemplateFormatVersion: '2010-09-09'`**: To standardowa deklaracja wersji silnika CloudFormation. Choć data brzmi historycznie, jest to najnowsza i jedyna wspierana wersja szablonów.
    
- **`Transform: AWS::Serverless-2016-10-31`**: To kluczowa linijka! Informuje AWS, że ten szablon nie jest zwykłym szablonem CloudFormation, ale szablonem **AWS SAM**. AWS najpierw "przetłumaczy" (przetransformuje) ten uproszczony kod na setki linii czystego CloudFormation, zanim go uruchomi.
    
- **`Description`**: Po prostu opis tekstowy projektu, widoczny w konsoli AWS.
    

## 2. Sekcja `Resources` (Zasoby)

To jest serce szablonu. Tutaj deklarujesz, jakie fizyczne (lub wirtualne) usługi AWS chcesz stworzyć. Masz tutaj zdefiniowany jeden główny zasób: `DailyTaskFunction`.

```yaml
Resources:
  DailyTaskFunction:
    Type: AWS::Serverless::Function
```

- **`DailyTaskFunction`**: To logiczna nazwa Twojego zasobu w szablonie (możesz ją nazwać jak chcesz, np. `MojaSuperFunkcja`).
    
- **`Type: AWS::Serverless::Function`**: Dzięki temu typowi AWS wie, że ma stworzyć funkcję AWS Lambda wraz z powiązaną z nią rolą IAM (uprawnieniami) i logami CloudWatch. Gdyby nie użycie `Transform` na początku, musiałbyś rozpisywać te powiązane zasoby ręcznie.
    

### Właściwości funkcji (`Properties`)

Tutaj konfigurujesz, jak ta konkretna funkcja Lambda ma działać.

```yaml
    Properties:
      FunctionName: daily-task-orchestrator
      Description: Automated daily task orchestration with Gmail and Claude AI integration
```

- **`FunctionName`**: Fizyczna nazwa funkcji, którą zobaczysz w konsoli AWS Lambda.
    
- **`Description`**: Opis funkcji w konsoli.
    

```yaml
      CodeUri: target/daily-task-orchestrator-1.0.0-SNAPSHOT.jar
      Handler: com.dailytask.lambda.DailyTaskLambdaHandler::handleRequest
      Runtime: java21
```

- **`CodeUri`**: Wskazuje ścieżkę do skompilowanego kodu Twojej aplikacji na Twoim lokalnym dysku (zazwyczaj przed deployem za pomocą komendy `sam deploy` lub `sam build`). Tutaj jest to plik `.jar` (Java), znajdujący się w katalogu `target/` (standardowy folder wyjściowy dla narzędzi takich jak Maven).
    
- **`Handler`**: Punkt wejścia do Twojej aplikacji. Mówi Lambdzie: _"Gdy zostaniesz uruchomiona, znajdź pakiet `com.dailytask.lambda`, klasę [[DailyTaskLambdaHandler]] i wywołaj w niej metodę `handleRequest`"_.
    
- **`Runtime`**: Środowisko uruchomieniowe. Twój kod jest napisany w **Javie 21**, więc Lambda uruchomi maszynę wirtualną (JVM) wspierającą tę wersję Javy.
    

```yaml
      MemorySize: 1024
      Timeout: 300
```

- **`MemorySize: 1024`**: Przypisuje funkcji 1024 MB (czyli 1 GB) pamięci RAM. W AWS Lambda ilość przydzielonego procesora (CPU) skaluje się proporcjonalnie do pamięci RAM, więc 1 GB daje też całkiem przyzwoitą moc obliczeniową (istotne przy integracji z AI i parsowaniu poczty).
    
- **`Timeout: 300`**: Maksymalny czas (w sekundach), przez jaki funkcja może działać przy pojedynczym uruchomieniu. 300 sekund to **5 minut**. Jeśli funkcja nie skończy pracy w tym czasie, AWS ją siłą ubije. Ponieważ integrujesz się z zewnętrznymi API (Gmail, Claude AI), taki zapas czasu jest bardzo bezpieczny.
    

```yaml
      Environment:
        Variables:
          DEPLOYMENT_ENV: lambda
          AWS_REGION: us-east-1
```

- **`Environment` -> `Variables`**: Zmienne środowiskowe. Twój kod w Javie może je odczytać podczas działania (np. używając `System.getenv("DEPLOYMENT_ENV")`). Pomaga to aplikacji "dowiedzieć się", w jakim środowisku działa i w jakim regionie AWS się znajduje (`us-east-1` to Północna Wirginia).
    

### Wyzwalacz automatyczny (`Events`)

Ta sekcja definiuje, co automatycznie "kopnie" (uruchomi) Twoją funkcję Lambda.

```yaml
      Events:
        DailySchedule:
          Type: Schedule
          Properties:
            Schedule: cron(0 9 * * ? *)
            Description: Trigger daily at 9:00 AM UTC
            Enabled: true
```

- **`DailySchedule`**: Nazwa własna reguły wyzwalania.
    
- **`Type: Schedule`**: Oznacza, że funkcja będzie uruchamiana według harmonogramu czasowego (AWS EventBridge).
    
- **`Schedule: cron(0 9 * * ? *)`**: Klasyczny zapis CRON, który mówi: uruchamiaj funkcję codziennie.
    
    - `0` - minuta 0
        
    - `9` - godzina 9:00
        
    - `*` - każdy dzień miesiąca
        
    - `*` - każdy miesiąc
        
    - `?` - dowolny dzień tygodnia (używane w AWS zamiast `*` gdy zdefiniowany jest dzień miesiąca)
        
    - `*` - każdy rok
        
    - **Ważne**: Czas w AWS jest zawsze podawany w strefie **UTC**. W Polsce będzie to 10:00 rano (czas zimowy) lub 11:00 rano (czas letni).
        
- **`Enabled: true`**: Reguła jest aktywna od razu po wdrożeniu szablonu.
    

## 3. Sekcja `Outputs` (Wyjścia)

Na samym dole mamy sekcję `Outputs`. Nie tworzy ona żadnej infrastruktury, ale działa jak "podsumowanie" po udanym wdrożeniu. Wyświetla przydatne informacje w terminalu lub konsoli AWS CloudFormation.

```yaml
Outputs:
  DailyTaskFunctionArn:
    Description: ARN of the Daily Task Orchestrator Lambda function
    Value: !GetAtt DailyTaskFunction.Arn
```

- **`DailyTaskFunctionArn`**: Klucz wyjściowy.
    
- **`!GetAtt DailyTaskFunction.Arn`**: Funkcja wewnętrzna CloudFormation (`GetAtt` to skrót od _Get Attribute_). Pobiera **ARN** (Amazon Resource Name - unikalny identyfikator zasobu w całym AWS) nowo stworzonej funkcji Lambda.
    

```yaml
  DailyTaskFunctionName:
    Description: Name of the Lambda function
    Value: !Ref DailyTaskFunction
```

- **`DailyTaskFunctionName`**: Kolejny klucz wyjściowy.
    
- **`!Ref DailyTaskFunction`**: Funkcja wewnętrzna `!Ref` (skrót od _Reference_). W przypadku funkcji Lambda, zwraca jej fizyczną nazwę (czyli `"daily-task-orchestrator"`).