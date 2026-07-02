## Class [[AwsBedrockClaudeClient]] - Adapter AWS Bedrock dla Claude AI

Implementacja interfejsu [[ClaudeApiClient]] łącząca się z modelami Claude przy użyciu chmury Amazon Web Services (AWS Bedrock) i oficjalnego AWS SDK v2 dla Javy.

```java
public class AwsBedrockClaudeClient implements ClaudeApiClient {
    private static final Logger logger = LoggerFactory.getLogger(AwsBedrockClaudeClient.class);

    private final ClaudeConfiguration config;
    private final BedrockRuntimeClient bedrockClient;

    public AwsBedrockClaudeClient(ClaudeConfiguration config) {
        this.config = config;
        this.bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.of(config.getAwsRegion()))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }

    @Override
    public String sendMessage(String systemPrompt, String userPrompt) throws ClaudeApiClient.ClaudeApiException {
        try {
            ConverseRequest.Builder requestBuilder = ConverseRequest.builder()
                .modelId(config.getModelId());

            if (systemPrompt != null && !systemPrompt.isBlank()) {
                SystemContentBlock systemBlock = SystemContentBlock.builder()
                    .text(systemPrompt)
                    .build();
                requestBuilder.system(List.of(systemBlock));
            }

            Message userMessage = Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(userPrompt))
                .build();
            requestBuilder.messages(List.of(userMessage));

            InferenceConfiguration inferenceConfig = InferenceConfiguration.builder()
                .maxTokens(config.getMaxTokens())
                .temperature((float) config.getTemperature())
                .build();
            requestBuilder.inferenceConfig(inferenceConfig);

            ConverseRequest request = requestBuilder.build();
            ConverseResponse response = bedrockClient.converse(request);

            return extractTextFromResponse(response);
        } catch (ThrottlingException e) {
            // ... translacja wyjątków AWS Bedrock na ClaudeApiException ...
        }
    }
}
```

### Działanie adaptera

* **`BedrockRuntimeClient`**: Narzędzie klienckie z biblioteki AWS SDK używane do komunikacji z Bedrock. Jest inicjalizowane w konstruktorze z określeniem regionu (np. `us-east-1`) oraz pobraniem domyślnych danych uwierzytelniających AWS z systemu (`DefaultCredentialsProvider`).
* **Converse API**: Metoda `converse()` z AWS SDK udostępnia zunifikowany interfejs do rozmów z modelami AI. Klasa buduje zapytanie `ConverseRequest` zawierające:
  - Identyfikator modelu (`modelId`),
  - Prompt systemowy (`systemPrompt`),
  - Wiadomość użytkownika (`userPrompt`),
  - Parametry generowania (temperatura, maksymalna liczba tokenów).
* **Translacja wyjątków (Error Translation)**: Metoda przechwytuje specyficzne wyjątki chmurowe AWS (np. brak uprawnień `AccessDeniedException`, przekroczenie limitów zapytań `ThrottlingException`, błędy serwera) i tłumaczy je na jeden spójny wyjątek `ClaudeApiException`, ukrywając specyfikę chmury przed resztą kodu aplikacji.

### Pojęcia dla nowicjuszy

* **SDK (Software Development Kit)**: Zbiór bibliotek dostarczanych przez zewnętrznego dostawcę (tu: Amazon AWS), który ułatwia pisanie programów integrujących się z ich usługami chmurowymi bez potrzeby ręcznego budowania zapytań HTTP.
* **Credentials (Dane uwierzytelniające)**: Klucze dostępowe (Access Key i Secret Key), które mówią chmurze AWS "kim jesteś" i sprawdzają, czy masz prawo korzystać z danej usługi. `DefaultCredentialsProvider` szuka ich automatycznie w bezpiecznych miejscach systemu (zmienne środowiskowe, pliki profilu AWS).
* **Throttling (Dławienie żądań)**: Sytuacja, w której wysyłamy zapytania do serwera zbyt szybko. Serwer odpowiada wtedy specjalnym błędem (kod 429), prosząc o spowolnienie.
* **Inference (Wnioskowanie)**: Proces generowania odpowiedzi przez wytrenowany model sztucznej inteligencji na podstawie dostarczonego promptu.
