## Test [[AwsBedrockClaudeClientTest]] - Testy adaptera AWS Bedrock

Klasa testowa weryfikująca zachowanie adaptera [[AwsBedrockClaudeClient]]. Wykorzystuje Mockito do imitowania działania chmury AWS Bedrock, sprawdzając zarówno poprawne odpowiedzi, jak i obsługę awarii.

```java
@ExtendWith(MockitoExtension.class)
class AwsBedrockClaudeClientTest {

    @Mock
    private BedrockRuntimeClient mockBedrockClient;

    private ClaudeConfiguration config;
    private AwsBedrockClaudeClient client;

    @BeforeEach
    void setUp() {
        config = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.AWS_BEDROCK)
            .modelId(TEST_MODEL_ID)
            .maxTokens(2000)
            .temperature(0.7f)
            .timeoutSeconds(30)
            .awsRegion(TEST_REGION)
            .build();

        client = new AwsBedrockClaudeClient(config);
        injectBedrockClient(client, mockBedrockClient);
    }

    @Test
    void sendMessage_withValidResponse_shouldReturnTextContent() throws Exception {
        ContentBlock contentBlock = ContentBlock.fromText("Here is my analysis of your tasks.");
        Message responseMessage = Message.builder()
            .role(ConversationRole.ASSISTANT)
            .content(contentBlock)
            .build();

        ConverseOutput output = ConverseOutput.builder()
            .message(responseMessage)
            .build();

        ConverseResponse bedrockResponse = ConverseResponse.builder()
            .output(output)
            .usage(TokenUsage.builder().inputTokens(100).outputTokens(50).build())
            .build();

        when(mockBedrockClient.converse((ConverseRequest) any()))
            .thenReturn(bedrockResponse);

        String result = client.sendMessage("You are a helpful assistant", "Analyze these tasks");

        assertNotNull(result);
        assertEquals("Here is my analysis of your tasks.", result);
    }
    
    // ... testy obsługi błędów AWS Bedrock ...
}
```

### Wyjaśnienie testów i technik testowych

* **Mockowanie chmury (`@Mock`)**: Aby test był szybki, stabilny i darmowy, nie łączymy się z rzeczywistą chmurą AWS. Zamiast tego tworzymy obiekt imitujący (`mockBedrockClient`), któremu możemy "nakazać" zwrócić konkretną odpowiedź (`when(...).thenReturn(...)`) lub rzucić wyjątek chmurowy (`when(...).thenThrow(...)`).
* **Wstrzykiwanie przez refleksję (`injectBedrockClient`)**: Klasa [[AwsBedrockClaudeClient]] celowo ukrywa pole `bedrockClient` jako prywatne i nie udostępnia dla niego publicznego setter-a (ze względów bezpieczeństwa). W testach obchodzimy to ograniczenie przy użyciu mechanizmu **Refleksji** Javy, aby podmienić prawdziwy klient AWS na naszą atrapę (Mock).
* **Testowanie wyjątków (`assertThrows`)**: Testy takie jak `sendMessage_withAccessDeniedException_shouldThrowAuthenticationFailedException` upewniają się, że w przypadku błędu uprawnień w AWS Bedrock, nasz kod poprawnie tłumaczy go na wyjątek biznesowy `AUTHENTICATION_FAILED`.

### Pojęcia dla nowicjuszy

* **Mock (Atrapa/Mockowanie)**: Sztuczny obiekt, który udaje zachowanie prawdziwego komponentu (np. bazy danych czy zewnętrznego API). Programista określa dokładnie, jak Mock ma zareagować na dane wywołanie metody.
* **Mockito / MockitoExtension**: Najpopularniejsza biblioteka w Javie ułatwiająca tworzenie mocków, sprawdzanie czy metody zostały wywołane (`verify`) oraz konfigurowanie zachowania obiektów testowych.
* **Refleksja (Reflection)**: Zaawansowany mechanizm w Javie umożliwiający inspekcję i modyfikację klas, pól oraz metod w czasie działania programu, nawet jeśli są one oznaczone jako prywatne (`private`). Stosowana najczęściej w bibliotekach testowych oraz frameworkach (np. Spring).
