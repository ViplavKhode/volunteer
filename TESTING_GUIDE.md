# Testing Guide for UpdateVolunteerStep1Handler and BaseRequestHandler

## Overview

This guide explains how to test `UpdateVolunteerStep1Handler` and `BaseRequestHandler` classes, including the test files created and how to run them.

## Test Files Created

### 1. BaseRequestHandlerTest.java
**Location**: `src/test/java/org/sfa/volunteer/handler/BaseRequestHandlerTest.java`

**Test Coverage**:
- `getLocaleFromRequest()` - Tests locale extraction from request headers
- `createResponse()` - Tests response creation with valid and invalid data
- `createErrorResponse()` - Tests error response creation with various status codes
- Edge cases: null bodies, different locales, serialization failures

**Total Test Cases**: 11 test methods

### 2. UpdateVolunteerStep1HandlerTest.java (Enhanced)
**Location**: `src/test/java/org/sfa/volunteer/handler/UpdateVolunteerStep1HandlerTest.java`

**Test Coverage**:
- Successful volunteer update requests
- Exception handling (EnumUnspecifiedException, InvalidRequestException, general exceptions)
- Invalid JSON body handling
- Null/empty body handling
- Missing required fields
- Different locale handling
- Request parsing validation
- Malformed JSON handling

**Total Test Cases**: 11 test methods (8 original + 3 new)

## Known Issue: Static Final Field Modification

### Problem
Both `UpdateVolunteerStep1Handler` and `BaseRequestHandler` use `static final` fields that are initialized from `SpringContext.getContext()`. These fields are initialized when the class is loaded, making them difficult to mock in tests.

### Current Solution
The tests attempt to use reflection to modify `static final` fields by:
1. Removing the `final` modifier temporarily
2. Setting the field value
3. Using JVM arguments (`--add-opens`) to allow reflection access

### Alternative Solutions

#### Option 1: Use JVM Arguments (Recommended)
Run tests with JVM arguments that allow reflection:
```bash
mvn test -DargLine="--add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED"
```

The `pom.xml` has been updated to include these arguments in the Surefire plugin configuration.

#### Option 2: Refactor Code (Long-term Solution)
Consider refactoring the handlers to use dependency injection instead of static final fields:
- Use constructor injection
- Or use a factory pattern
- Or use Spring's `@Autowired` with instance fields instead of static fields

#### Option 3: Use PowerMock (Not Recommended)
PowerMock can mock static final fields but is deprecated and not recommended for new projects.

## How to Run Tests

### Method 1: Run All Tests for Both Classes
```bash
mvn clean test "-Dtest=BaseRequestHandlerTest,UpdateVolunteerStep1HandlerTest"
```

### Method 2: Run Tests Individually
```bash
# Run BaseRequestHandler tests only
mvn test "-Dtest=BaseRequestHandlerTest"

# Run UpdateVolunteerStep1Handler tests only
mvn test "-Dtest=UpdateVolunteerStep1HandlerTest"
```

### Method 3: Run Specific Test Method
```bash
mvn test "-Dtest=BaseRequestHandlerTest#getLocaleFromRequest_ShouldReturnLocaleFromHeaders_WhenAcceptLanguageHeaderExists"
```

### Method 4: Run from IDE

**IntelliJ IDEA:**
1. Right-click on the test class file
2. Select "Run 'BaseRequestHandlerTest'" or "Run 'UpdateVolunteerStep1HandlerTest'"
3. Or use shortcut: `Ctrl+Shift+F10` (Windows/Linux) or `Cmd+Shift+R` (Mac)

**Eclipse:**
1. Right-click on the test class file
2. Select "Run As" → "JUnit Test"

**VS Code:**
1. Click the test icon (▶) next to the test class or method
2. Or use Command Palette: "Java: Run Tests"

## Test Structure

### BaseRequestHandlerTest
- Uses a concrete test implementation (`TestConcreteHandler`) to test the abstract `BaseRequestHandler`
- Tests protected methods using reflection
- Mocks `SpringContext` and `MessageSource`
- Tests locale handling, response creation, and error handling

### UpdateVolunteerStep1HandlerTest
- Mocks `VolunteerService`, `ResponseBuilder`, and `MessageSource`
- Uses reflection to inject mocks into static final fields
- Tests the complete request handling flow
- Validates exception handling and error responses

## Expected Test Results

When tests pass successfully, you should see:
```
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

## Troubleshooting

### Issue: `IllegalAccessException: Can not set static final field`
**Solution**: Ensure JVM arguments are set. The `pom.xml` has been updated with the necessary `--add-opens` arguments. If issues persist:
1. Verify Java version is 17+
2. Check that `mockito-inline` dependency is present (already in pom.xml)
3. Try running with explicit JVM arguments:
   ```bash
   mvn test -DargLine="--add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED" "-Dtest=BaseRequestHandlerTest"
   ```

### Issue: Tests fail with NullPointerException
**Solution**: Ensure `SpringContext` mock is set up before handler instantiation. The tests use `MockedStatic` to handle this.

### Issue: Tests timeout
**Solution**: Check if Spring context is trying to initialize. The mocks should prevent this. Ensure `MockedStatic` is properly closed in `@AfterEach`.

## Test Coverage Summary

### BaseRequestHandler
- ✅ Locale extraction from headers
- ✅ Default locale handling
- ✅ Response creation (success and error cases)
- ✅ Error response creation with different status codes
- ✅ Serialization failure handling
- ✅ Null body handling
- ✅ Complex object serialization

### UpdateVolunteerStep1Handler
- ✅ Successful request handling
- ✅ Exception handling (specific and general)
- ✅ JSON parsing (valid and invalid)
- ✅ Null/empty body handling
- ✅ Missing required fields
- ✅ Locale handling
- ✅ Request validation

## Next Steps

1. **If tests fail due to static final field issues**: Use the JVM arguments approach or consider refactoring the code
2. **To add more test cases**: Follow the existing patterns in the test files
3. **To improve test coverage**: Add tests for edge cases specific to your domain

## Files Modified/Created

- ✅ Created: `src/test/java/org/sfa/volunteer/handler/BaseRequestHandlerTest.java`
- ✅ Enhanced: `src/test/java/org/sfa/volunteer/handler/UpdateVolunteerStep1HandlerTest.java`
- ✅ Updated: `pom.xml` (added Surefire plugin configuration with JVM arguments)
- ✅ Created: `TESTING_GUIDE.md` (this file)

