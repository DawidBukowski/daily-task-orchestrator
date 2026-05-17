# Daily Task Orchestrator

A foundational Java application that fetches tasks from various sources, analyzes them via Claude AI, and notifies the user with a prioritized list. Built with Hexagonal Architecture principles.

## Prerequisites
- Java 17 or higher
- Maven 3.8+

## Build and Run Instructions

# run tests
- mvn test
# execute the application
- mvn --% exec:java -Dexec.mainClass=com.dailytask.Main
# run specific test
- mvn test -Dtest=DailyTsaskOrchestratorTest