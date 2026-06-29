Test integracyjny weryfikujący poprawność działania całego potoku (pipeline) przetwarzania wiadomości e-mail na ustrukturyzowane zadania.

```java
class TaskPipelineIntegrationTest {

    private EmailToRawDataConverter rawDataConverter;
    private SimpleTaskExtractor taskExtractor;

    @BeforeEach
    void setUp() {
        rawDataConverter = new EmailToRawDataConverter();
        taskExtractor = new SimpleTaskExtractor();
    }

    @Test
    void testFullPipeline_emailWithClearDeadline() {
        GmailMessage gmailMessage = new GmailMessage();
        gmailMessage.setMessageId("msg-123");
        gmailMessage.setSubject("Assignment 5: Data Structures");
        gmailMessage.setFrom("sender@school.edu");
        gmailMessage.setBody("Complete the assignment. Due Friday");
        gmailMessage.setReceivedDate(LocalDateTime.of(2026, 6, 29, 10, 0));
        gmailMessage.setLabels(List.of("INBOX", "IMPORTANT"));

        RawData rawData = rawDataConverter.convert(gmailMessage);
        List<Task> tasks = taskExtractor.extract(List.of(rawData));

        assertNotNull(tasks);
        assertEquals(1, tasks.size());

        Task task = tasks.get(0);
        assertNotNull(task.getId());
        assertEquals("Assignment 5: Data Structures", task.getTitle());
        assertTrue(task.getDescription().contains("Complete the assignment"));
        assertEquals("Gmail", task.getSource());
        assertEquals("msg-123", task.getOriginalId());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        assertNotNull(task.getDeadline());
        assertNotNull(task.getCreatedAt());
        assertNotNull(task.getUpdatedAt());
    }
}
```

* `testFullPipeline_emailWithClearDeadline()`: Testuje pełne przejście („happy path”) wiadomości e-mail od surowego obiektu [[GmailMessage]], przez konwersję na [[RawData]] za pomocą [[EmailToRawDataConverter]], aż do ekstrakcji końcowego obiektu [[Task]] przez [[SimpleTaskExtractor]].
* `setUp()`: Przed każdym testem inicjalizuje konwerter oraz ekstraktor zadań.
* Test weryfikuje poprawność mapowania pól takich jak tytuł, opis, źródło, oryginalny identyfikator oraz status zadania (który domyślnie powinien wynosić `PENDING`).
* Sprawdza również, czy potok prawidłowo rozpoznał i wyekstrahował termin wykonania zadania (`deadline`) na podstawie treści wiadomości (w tym przypadku „Due Friday”).
