# Roadmap - Timeline & Phases

## Overall Timeline

**Duration:** 11-16 weeks
**Commitment:** 5-10 hours per week
**Status:** Ready to start

## Phase Breakdown

### Phase 1: Project Setup (Week 1-2)
**Goal:** Build foundation with clean architecture

What to build:
- Maven project structure
- Core interfaces
- Manual dependency injection
- Test framework

Learning:
- Hexagonal architecture
- SOLID principles
- Maven basics
- JUnit + Mockito

Status: ⏳ Ready

---

### Phase 2: Gmail Integration (Week 3-4)
**Goal:** Fetch and parse Gmail emails

What to build:
- OAuth2 authentication
- GmailDataSource implementation
- Email parsing & filtering
- RawTask objects

Learning:
- REST APIs
- OAuth2 flow
- Error handling
- Data extraction

Status: ⏳ Waiting for Phase 1

---

### Phase 3: Data Normalization (Week 5-6)
**Goal:** Normalize to unified Task model

What to build:
- Task domain model
- Deadline parsing
- Title extraction
- TaskNormalizer

Learning:
- Domain-driven design
- Data modeling
- Regex & text parsing
- Graceful degradation

Status: ⏳ Waiting for Phase 2

---

### Phase 4: Claude API (Week 7-8)
**Goal:** AI-powered prioritization

What to build:
- Anthropic API client
- Prompt engineering
- Response parsing
- TaskAnalyzer

Learning:
- LLM integration
- Prompt design
- JSON parsing
- Cost awareness

Status: ⏳ Waiting for Phase 3

---

### Phase 5: Email Output (Week 9-10)
**Goal:** Send formatted daily reports

What to build:
- Email templates (HTML)
- SMTP configuration
- Main orchestrator
- Integration tests

Learning:
- Email generation
- SMTP protocol
- HTML templating
- End-to-end testing

Status: ⏳ Waiting for Phase 4

---

### Phase 6: AWS Deployment (Week 11)
**Goal:** Automate with Lambda

What to build:
- Lambda handler
- Secrets Manager integration
- CloudWatch Events (9 AM trigger)
- Deployment scripts

Learning:
- AWS Lambda
- IAM permissions
- CloudWatch
- Infrastructure

Status: ⏳ Waiting for Phase 5

---

### Phase 7: University Portal (Week 12+)
**Goal:** Multi-source aggregation

What to build:
- HTML parser (Jsoup)
- Portal authentication
- UniversityDataSource
- Duplicate detection

Learning:
- Web scraping
- HTML parsing
- Enterprise SSO
- Data deduplication

Status: ⏳ Waiting for Phase 6

---

## Weekly Schedule

| Time | Activity | Hours |
|------|----------|-------|
| Monday | Read CODE prompt | 1 |
| Tue-Wed | Implement code | 3-4 |
| Thursday | EXPLANATION + Learning | 1-2 |
| Friday | Review + Document | 1 |
| **Total** | **Per week** | **6-8** |

## Milestones

- [ ] Week 2: Phase 1 complete, tests pass
- [ ] Week 4: Can fetch Gmail
- [ ] Week 6: Can normalize to Tasks
- [ ] Week 8: Can analyze with Claude
- [ ] Week 10: Can send emails
- [ ] Week 11: Running on Lambda
- [ ] Week 12+: Multi-source working