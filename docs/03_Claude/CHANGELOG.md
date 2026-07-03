# Claude Integration Changelog

## [2026-07-03] - Markdown Code Fence Handling

### Fixed
- **JSON Parsing with Markdown Code Fences** - Claude sometimes wraps JSON responses in markdown code blocks (` ```json ... ``` `), which broke the parser
  - Added `stripMarkdownCodeFences()` method to `ClaudeResponseParser`
  - Method detects and removes code fences before JSON parsing
  - Gracefully handles both ````json` and plain ```` ``` `` fences
  - Updated system prompt to explicitly instruct Claude NOT to use markdown formatting
  - Added comprehensive test coverage for markdown fence stripping scenarios

### Technical Details
**Problem:** 
```
ERROR com.dailytask.adapters.analyzers.ClaudeResponseParser -- Failed to parse JSON response: 
Unexpected character ('`' (code 96)): expected a valid value (JSON String, Number, Array, Object or token 'null', 'true' or 'false')
```

**Root Cause:**
Claude models occasionally return responses wrapped in markdown code fences:
```
```json
{"summary": "...", "schedule": "..."}
```
```

Instead of raw JSON:
```json
{"summary": "...", "schedule": "..."}
```

**Solution:**
1. **Pre-processing Step:** Added markdown fence detection and stripping
   - Detects opening fence: `` ```json `` or `` ``` ``
   - Finds closing fence: `` ``` ``
   - Extracts clean JSON content between fences
   - Falls back to original response if no valid fences found

2. **Prompt Engineering:** Updated system prompt to discourage markdown formatting
   ```java
   "You MUST respond with ONLY valid JSON. Do NOT use markdown code fences, 
   do NOT wrap in ```json blocks. Return ONLY the raw JSON object..."
   ```

3. **Defense in Depth:** Parser handles both scenarios:
   - If Claude follows instructions → parses raw JSON directly
   - If Claude uses fences anyway → strips them automatically

**Files Modified:**
- `src/main/java/com/dailytask/adapters/analyzers/ClaudeResponseParser.java`
  - Added `stripMarkdownCodeFences()` private method
  - Updated `parse()` to call stripping before JSON parsing
  - Added debug logging for fence stripping events

- `src/main/java/com/dailytask/adapters/analyzers/TaskSummarizationPromptBuilder.java`
  - Strengthened system prompt instructions
  - Explicitly forbid markdown formatting

- `src/test/java/com/dailytask/adapters/analyzers/ClaudeResponseParserTest.java`
  - Added 6 new test cases for markdown fence scenarios
  - Tests cover: `json` fence, plain fence, whitespace, incomplete fences, no fences
  - All 42 tests passing

**Testing:**
```bash
mvn test -Dtest=ClaudeResponseParserTest
# Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
```

### Impact
- **Robustness:** Application now handles both raw JSON and markdown-wrapped JSON responses
- **Reliability:** No more parsing failures due to markdown formatting
- **User Experience:** Seamless operation regardless of Claude's response formatting style
- **Backward Compatibility:** Existing raw JSON responses continue to work without modification

### References
- Issue: Application crashed with `Unexpected character ('`' (code 96))` error
- Claude API documentation: [Anthropic API Docs](https://docs.anthropic.com/claude/reference/getting-started-with-the-api)
- Related: Some LLMs tend to wrap structured outputs in markdown for readability in chat contexts

---

## Future Considerations

### Potential Enhancements
1. **Stricter JSON Schema Enforcement**
   - Consider using JSON Schema validation with Bedrock's schema enforcement
   - Anthropic API supports structured outputs (beta) - evaluate when stable

2. **Response Validation Metrics**
   - Log frequency of markdown-wrapped vs raw JSON responses
   - Track which Claude models exhibit this behavior most frequently
   - Adjust prompt engineering based on metrics

3. **Alternative Parsers**
   - Evaluate regex-based pre-processing for additional edge cases
   - Consider using a more lenient JSON parser that tolerates comments/trailing commas

### Known Edge Cases
- **Nested Code Fences:** If JSON content itself contains ` ``` ` strings, current implementation may have issues
  - Mitigation: JSON rarely contains literal triple backticks
  - Future: Implement more sophisticated fence detection if needed

- **Multiple JSON Blocks:** If Claude returns multiple JSON objects in separate fences
  - Current: Only first fence is processed
  - Future: Support multiple fences if use case arises

### Lessons Learned
- **Defensive Parsing:** Always sanitize external API responses before parsing
- **Prompt Engineering Limits:** Even explicit instructions may not guarantee format compliance
- **Defense in Depth:** Combine prompt engineering + parser robustness for reliability
