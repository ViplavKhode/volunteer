# How to Run JUnit Tests for UpdateVolunteerStep1Handler

## Prerequisites
- Java 17 or higher
- Maven installed and configured
- All project dependencies resolved

## Running Tests

### Method 1: Run All Tests for UpdateVolunteerStep1Handler
```bash
mvn test -Dtest=UpdateVolunteerStep1HandlerTest
```

### Method 2: Run a Specific Test Method
```bash
mvn test -Dtest=UpdateVolunteerStep1HandlerTest#handleRequest_ShouldReturnCreatedResponse_WhenRequestIsValid
```

### Method 3: Run All Tests in the Project
```bash
mvn test
```

### Method 4: Run Tests with Verbose Output
```bash
mvn test -Dtest=UpdateVolunteerStep1HandlerTest -X
```

### Method 5: Run Tests and Skip Compilation (if already compiled)
```bash
mvn surefire:test -Dtest=UpdateVolunteerStep1HandlerTest
```

## Using the Batch Script (Windows)
Double-click `run-tests.bat` or run from command prompt:
```bash
run-tests.bat
```

## Running from IDE

### IntelliJ IDEA
1. Right-click on `UpdateVolunteerStep1HandlerTest.java`
2. Select "Run 'UpdateVolunteerStep1HandlerTest'"
3. Or use shortcut: `Ctrl+Shift+F10` (Windows/Linux) or `Cmd+Shift+R` (Mac)

### Eclipse
1. Right-click on `UpdateVolunteerStep1HandlerTest.java`
2. Select "Run As" → "JUnit Test"

### VS Code
1. Click on the test icon next to the test class or method
2. Or use command palette: "Java: Run Tests"

## Test Coverage

The test suite includes the following test cases:

1. ✅ **handleRequest_ShouldReturnCreatedResponse_WhenRequestIsValid** - Tests successful volunteer update
2. ✅ **handleRequest_ShouldReturnBadRequest_WhenEnumUnspecifiedExceptionOccurs** - Tests EnumUnspecifiedException handling
3. ✅ **handleRequest_ShouldReturnBadRequest_WhenInvalidRequestExceptionOccurs** - Tests InvalidRequestException handling
4. ✅ **handleRequest_ShouldHandleGeneralException** - Tests general exception handling
5. ✅ **handleRequest_ShouldHandleInvalidJsonBody** - Tests invalid JSON parsing
6. ✅ **handleRequest_ShouldHandleNullBody** - Tests null body handling
7. ✅ **handleRequest_ShouldHandleEmptyBody** - Tests empty body handling
8. ✅ **handleRequest_ShouldHandleMissingRequiredFields** - Tests missing required fields

## Troubleshooting

### Issue: Tests fail with NullPointerException
**Solution**: Make sure SpringContext is properly mocked before the handler class loads.

### Issue: Tests fail with MockitoException
**Solution**: Ensure MockedStatic is kept open during test execution and closed in @AfterEach.

### Issue: Tests timeout
**Solution**: Check if Spring context is trying to initialize. The mocks should prevent this.

### Issue: Compilation errors
**Solution**: Run `mvn clean compile test-compile` first to ensure everything compiles.

## Expected Output

When tests pass, you should see:
```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```






