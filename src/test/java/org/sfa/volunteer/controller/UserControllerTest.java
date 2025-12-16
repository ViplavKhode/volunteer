package org.sfa.volunteer.controller;

import org.junit.jupiter.api.Test;
import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.AddressStatusResponse;
import org.sfa.volunteer.dto.response.WizardStatusResponse;
import org.sfa.volunteer.service.UserService;
import org.sfa.volunteer.util.MessageSourceUtil;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest(UserController.class)
public class UserControllerTest {
    @MockBean
    private UserService userService;
    @MockBean
    private ResponseBuilder responseBuilder;
    @MockBean
    private MessageSourceUtil messageSourceUtil;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAddressStatus() throws Exception {
        String userId = "ID_121";
        AddressStatusResponse mockResponse = new AddressStatusResponse(userId, true);
        SaayamResponse<AddressStatusResponse> expectedResponse = SaayamResponse.success(SaayamStatusCode.SUCCESS, "Success", mockResponse);

        when(userService.getAddressStatus(userId)).thenReturn(mockResponse);
        when(responseBuilder.buildSuccessResponse(any(), any(), eq(mockResponse))).thenReturn(expectedResponse);

        mockMvc.perform(get("/0.0.1/users/addressStatus/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.addressAvailable").value(true));
    }

    @Test
    void testGetWizardStatus() throws Exception {
        String userId = "ID_121";
        int volunteerStage = 2;
        WizardStatusResponse mockResponse = new WizardStatusResponse(userId, volunteerStage);
        SaayamResponse<WizardStatusResponse> expectedResponse = SaayamResponse.success(SaayamStatusCode.SUCCESS, "Success", mockResponse);

        when(userService.getWizardStatus(userId)).thenReturn(mockResponse);
        when(responseBuilder.buildSuccessResponse(any(), any(), eq(mockResponse))).thenReturn(expectedResponse);

        mockMvc.perform(get("/0.0.1/users/wizard/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.promotion_wizard_stage").value(volunteerStage));
    }
}
