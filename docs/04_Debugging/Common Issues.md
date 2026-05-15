# Common Issues & Solutions

## Issue: "Maven command not found"

**Solution:**
```bash
# Check if Maven is installed
mvn -version

# If not installed:
# macOS: brew install maven
# Ubuntu: sudo apt install maven
# Windows: download from maven.apache.org
```

## Issue: "Java version not compatible"

**Solution:**
```bash
# Check Java version
java -version

# Need Java 17+
# If lower, upgrade from oracle.com/java

# Or tell Maven which version to use in pom.xml
```

## Issue: "Tests fail but I don't know why"

**Solution:**
```bash
# Run with verbose output
mvn test -X

# Run single test
mvn test -Dtest=MyTestClassName

# Check test output in console
```

## Issue: "Package 'com.google.api' cannot be resolved"

**Solution:**
```bash
# Maven dependencies not downloaded
mvn dependency:resolve

# Or clean and rebuild
mvn clean install
```

(More issues will be added as you encounter them)