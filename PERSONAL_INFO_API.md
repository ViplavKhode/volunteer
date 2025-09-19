# Personal Information API Documentation

This document describes the new Personal Information API endpoints implemented to address issue #86.

## Endpoints

### GET /0.0.1/users/{userId}/personal-info

Fetches personal information for a specific user.

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "saayamCode": "SAAAYAM-1200",
  "message": "Operation successful",
  "data": {
    "id": "user-id",
    "firstName": "John",
    "middleName": "Middle",
    "lastName": "Doe",
    "fullName": "John Middle Doe",
    "primaryEmailAddress": "john.doe@example.com",
    "primaryPhoneNumber": "123-456-7890",
    "timeZone": "America/New_York",
    "gender": "Male",
    "lastUpdateDate": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### PUT /0.0.1/users/{userId}/personal-info

Updates personal information for a specific user.

**Request Body:**
```json
{
  "firstName": "John",
  "middleName": "Middle",
  "lastName": "Doe",
  "fullName": "John Middle Doe",
  "primaryEmailAddress": "john.doe@example.com",
  "primaryPhoneNumber": "123-456-7890",
  "timeZone": "America/New_York",
  "gender": "Male"
}
```

**Response:**
```json
{
  "success": true,
  "statusCode": 200,
  "saayamCode": "SAAAYAM-1204",
  "message": "User Personal Info Profile updated",
  "data": {
    "id": "user-id",
    "firstName": "John",
    "middleName": "Middle",
    "lastName": "Doe",
    "fullName": "John Middle Doe",
    "primaryEmailAddress": "john.doe@example.com",
    "primaryPhoneNumber": "123-456-7890",
    "timeZone": "America/New_York",
    "gender": "Male",
    "lastUpdateDate": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Features

- **Focused on Personal Information**: These endpoints specifically handle personal information fields (name, phone, email, etc.) separate from other profile data
- **Partial Updates**: The PUT endpoint supports partial updates - only provided fields are updated
- **Proper Error Handling**: Returns appropriate HTTP status codes and error messages
- **Standardized Response Format**: Uses the existing SaayamResponse format for consistency
- **Internationalization Support**: Uses existing message source configuration for proper localization

## Fields Supported

- `firstName`: User's first name
- `middleName`: User's middle name
- `lastName`: User's last name
- `fullName`: User's full name
- `primaryEmailAddress`: User's primary email address
- `primaryPhoneNumber`: User's primary phone number
- `timeZone`: User's time zone
- `gender`: User's gender

All fields are optional in the update request, allowing for flexible partial updates.