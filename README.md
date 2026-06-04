# Daily Task Orchestrator

A Java-based application for orchestrating and managing daily tasks efficiently.

## Overview

Daily Task Orchestrator is a task management system designed to help you organize, schedule, and execute daily tasks with ease. Built with Java, it provides a robust and scalable solution for task automation and coordination.

## Features

- **Task Management**: Create, update, and manage daily tasks
- **Task Scheduling**: Schedule tasks for specific times and dates
- **Task Orchestration**: Coordinate multiple tasks efficiently
- **Progress Tracking**: Monitor task execution and completion status

## Requirements

- Java 21
- Maven 3.6 or higher (if using Maven)

## Installation

### Clone the Repository

```bash
git clone https://github.com/Alucart558/daily-task-orchestrator.git
cd daily-task-orchestrator
```

### Build the Project

Using Maven:
```bash
mvn clean install
```

## Usage

### Running the Application

```bash
java -jar target/daily-task-orchestrator.jar
```

## Project Structure

```
daily-task-orchestrator/
├── src/
│   ├── main/
│   │   └── java/
│   └── test/
│       └── java/
├── pom.xml (Maven)
└── README.md
```

## Gmail API Integration Setup

This project uses the official Google Gmail API to fetch raw emails.

### 1. Configure Credentials
1. Go to the [Google Cloud Console](https://console.cloud.google.com/).
2. Create a new project or select an existing one.
3. Enable the **Gmail API** in "APIs & Services".
4. Go to **Credentials** -> Create Credentials -> **OAuth client ID**.
5. Application type: **Desktop app** (or Web application with redirect URI `http://localhost:8888/Callback`).
6. Copy your Client ID and Client Secret.

### 2. Environment Variables
Export the credentials before running the application:
```bash
export GMAIL_CLIENT_ID="your-client-id"
export GMAIL_CLIENT_SECRET="your-client-secret"
