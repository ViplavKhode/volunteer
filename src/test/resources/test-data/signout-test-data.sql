-- Test data for sign-out integration tests

-- Insert test user
INSERT INTO users (user_id, full_name, primary_email_address, primary_phone_number, time_zone, 
                   first_name, last_name, address_line1, city, zip_code, last_update_date)
VALUES ('test-user-123', 'Test User Full Name', 'testuser@example.com', '1234567890', 'UTC',
        'Test', 'User', '123 Test St', 'Test City', '12345', CURRENT_TIMESTAMP);

-- Insert volunteer details for the user
INSERT INTO volunteer_details (user_id, terms_and_conditions, notification, iscomplete)
VALUES ('test-user-123', true, true, false);

-- Insert user additional details
INSERT INTO user_additional_details (user_id, secondary_email_1, secondary_phone_1)
VALUES ('test-user-123', 'secondary@example.com', '9876543210');

-- Insert organization details
INSERT INTO organization (user_id, organization_name, organization_type, phone_number, email)
VALUES ('test-user-123', 'Test Organization', 'Non-Profit', '1112223333', 'org@example.com');

-- Insert user availability
INSERT INTO user_availability (user_id, day_of_week, start_time, end_time, last_update_date)
VALUES ('test-user-123', 'Monday', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('test-user-123', 'Wednesday', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('test-user-123', 'Friday', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

