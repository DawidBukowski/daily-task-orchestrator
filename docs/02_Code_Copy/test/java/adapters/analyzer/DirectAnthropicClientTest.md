## Test [[DirectAnthropicClientTest]] - Testy bezpośredniego klienta HTTP

Klasa testowa weryfikująca zachowanie [[DirectAnthropicClient]]. Sprawdza poprawność budowania zapytań HTTP do Anthropic API, obecność wymaganych nagłówków oraz reakcję na różne kody błędów HTTP.

```java
@ExtendWith(MockitoExtension.class)
class DirectAnthropicClientTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock(strictness = Mock.Strictness.LENIENT)
    private HttpResponse<String> mockResponse;

    private ClaudeConfiguration config;
    private DirectAnthropicClient client;

    @BeforeEach
    void setUp() {
        config = new ClaudeConfiguration.Builder()
            .provider(ClaudeConfiguration.Provider.ANTHROPIC)
            .modelId(TEST_MODEL_ID)
            .maxTokens(2000)
            .temperature(0.7)
            .timeoutSeconds(30)
            .anthropicApiKey(TEST_API_KEY)
            .anthropicApiUrl(TEST_API_URL)
            .build();

        client = new DirectAnthropicClient(config);
        injectHttpClient(client, mockHttpClient);
    }

    @Test
    void sendMessage_withValidResponse_shouldReturnTextContent() throws Exception {
        String successResponse = """
            {
              "content": [{"type": "text", "text": "Here is my analysis of your tasks."}]
            }
            """;

        doReturn(mockResponse)
            .when(mockHttpClient).send(any(HttpRequest.class), any());
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(successResponse);

        String result = client.sendMessage("You are a helpful assistant", "Analyze these tasks");

        assertNotNull(result);
        assertEquals("Here is my analysis of your tasks.", result);
    }
}
```

### Analiza technik testowych

* **Przechwytywanie argumentów (`ArgumentCaptor`)**: W teście `sendMessage_shouldIncludeRequiredHeaders` używamy narzędzia `ArgumentCaptor` do "złapania" obiektu `HttpRequest` wygenerowanego przez nasz kod. Pozwala to na upewnienie się, że wysyłane zapytanie zawiera poprawne nagłówki autoryzacyjne (`x-api-key`) i odpowiedni typ zawartości (`application/json`).
* **Mockowanie wbudowanego HttpClient**: Ponieważ `HttpClient` w Javie jest klasą abstrakcyjną o specyficznej strukturze, do mockowania jego metody `send` używamy zapisu `doReturn(...).when(...)` zamiast tradycyjnego `when(...)`. Zapobiega to problemom z typowaniem generycznym w Mockito.
* **Bezpieczeństwo w testach (Credential Leak Prevention)**: Test `sendMessage_with401Response_shouldThrowAuthenticationFailedException` upewnia się, że nawet w przypadku awarii autoryzacji, treść błędu rzucana w wyjątku `ClaudeApiException` nie zawiera poufnego klucza API (`TEST_API_KEY`), zapobiegając wyciekowi haseł do logów systemowych.

### Pojęcia dla nowicjuszy

* **`ArgumentCaptor` (Przechwytywacz argumentów)**: Funkcja w Mockito pozwalająca przechwycić obiekt, który został przekazany jako parametr do zamockowanej metody. Po przechwyceniu możemy dokładnie sprawdzić stan tego obiektu (np. czy zawiera odpowiednie nagłówki HTTP).
* **Mock strictness (Surowość mocka)**: Ustawienie w JUnit/Mockito. Wpis `strictness = Mock.Strictness.LENIENT` oznacza, że Mockito nie będzie zgłaszać błędów, jeśli w danym teście zdefiniowaliśmy zachowanie dla jakiejś metody mocka, ale z niej nie skorzystaliśmy. Zapobiega to nadmiernemu skomplikowaniu testów.
