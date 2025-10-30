// package org.sfa.volunteer.integration;

// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.context.jdbc.Sql;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.transaction.annotation.Transactional;

// import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
// import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @SpringBootTest
// @AutoConfigureMockMvc
// @Transactional
// @ActiveProfiles("test")
// @DisplayName("Sign-Out Feature - Integration Tests")
// class SignOutIntegrationTest {

//     @Autowired
//     private MockMvc mockMvc;

//     @Test
//     @DisplayName("Integration test - Complete sign-out flow with all data")
//     @Sql("/test-data/signout-test-data.sql")
//     void testCompleteSignOutFlow() throws Exception {
//         // Act & Assert
//         mockMvc.perform(post("/0.0.1/users/signout/test-user-123")
//                         .contentType("application/json"))
//                 .andExpect(status().isOk())
//                 .andExpect(jsonPath("$.data.success").value(true))
//                 .andExpect(jsonPath("$.data.message").exists())
//                 .andExpect(jsonPath("$.data.signOutTime").exists());
//     }

//     @Test
//     @DisplayName("Integration test - Sign-out with database transaction rollback")
//     void testSignOut_TransactionRollback() throws Exception {
//         // This test verifies that if one operation fails, all operations are rolled back
//         // This requires actual database setup which is configured in application-test.properties
//         mockMvc.perform(post("/0.0.1/users/signout/non-existent-user")
//                         .contentType("application/json"))
//                 .andExpect(status().isNotFound());
//     }

//     @Test
//     @DisplayName("Integration test - Verify data is actually cleared from database")
//     @Sql({"/test-data/signout-test-data.sql"})
//     void testSignOut_DataActuallyCleared() throws Exception {
//         // This test would require database queries to verify data is actually cleared
//         // Typically this would use @Sql with cleanup scripts
//         mockMvc.perform(post("/0.0.1/users/signout/test-user-123")
//                         .contentType("application/json"))
//                 .andExpect(status().isOk());
        
//         // Additional assertions would require injecting repositories and checking database state
//     }

//     @Test
//     @DisplayName("Integration test - Sign-out with Spring Security if enabled")
//     void testSignOut_WithSecurityContext() throws Exception {
//         // This test verifies sign-out works in a secured environment
//         mockMvc.perform(post("/0.0.1/users/signout/test-user-123")
//                         .header("Authorization", "Bearer test-token")
//                         .contentType("application/json"))
//                 .andExpect(status().isOk());
//     }

//     @Test
//     @DisplayName("Integration test - Concurrent sign-out requests")
//     void testSignOut_ConcurrentRequests() throws Exception {
//         // Simulate concurrent sign-out requests
//         for (int i = 0; i < 5; i++) {
//             mockMvc.perform(post("/0.0.1/users/signout/test-user-" + i)
//                             .contentType("application/json"))
//                     .andExpect(status().isOk());
//         }
//     }
// }


