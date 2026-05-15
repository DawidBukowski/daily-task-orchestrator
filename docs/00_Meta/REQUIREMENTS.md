# Requirements - What We're Building

## Functional Requirements

### FR1: Data Collection
- [ ] Fetch from Gmail API
- [ ] Fetch from university portal
- [ ] Fetch from professor websites
- [ ] Secure authentication

### FR2: Data Processing
- [ ] Parse emails to extract deadlines
- [ ] Normalize data from multiple sources
- [ ] Create unified Task objects
- [ ] Handle malformed data gracefully

### FR3: AI Analysis
- [ ] Call Claude API
- [ ] Prioritize tasks
- [ ] Generate schedule
- [ ] Create summary

### FR4: User Notification
- [ ] Generate formatted email
- [ ] Send via Gmail SMTP
- [ ] Include all relevant info
- [ ] Make it human-readable

### FR5: Automation
- [ ] Run daily at 9 AM
- [ ] Deploy to AWS Lambda
- [ ] Handle errors gracefully
- [ ] Log execution

## Non-Functional Requirements

### Code Quality
- Clean code, no duplication
- Proper error handling
- Good naming conventions
- Clear comments where needed

### Architecture
- Hexagonal pattern
- Dependency injection
- Interface-based design
- Separation of concerns

### Testing
- Unit tests for all classes
- Integration tests
- Mock external dependencies
- >80% code coverage

### Security
- No credentials in code
- Use environment variables
- AWS Secrets Manager for prod
- OAuth2 for authentication

### Performance
- Complete execution <1 minute
- Efficient API calls
- No unnecessary DB queries