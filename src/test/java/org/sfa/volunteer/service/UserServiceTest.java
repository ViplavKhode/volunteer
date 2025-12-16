package org.sfa.volunteer.service;

import lombok.Getter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.response.AddressStatusResponse;
import org.sfa.volunteer.dto.response.WizardStatusResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.Country;
import org.sfa.volunteer.model.State;
import org.sfa.volunteer.model.User;
import org.sfa.volunteer.repository.UserRepository;

import org.sfa.volunteer.service.impl.UserServiceImpl;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getAddressStatus_shouldReturnFalse_whenIncompleteAddress() {

        String userId = "ID_121";
        User mockUser = new UserTestBuilder()
                .withId(userId)
                .withoutCompleteAddress()
                .getUser();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        AddressStatusResponse response = userService.getAddressStatus(userId);
        assertEquals(false, response.addressAvailable());

        mockUser = new UserTestBuilder()
                .withId(userId)
                .withoutCountry()
                .getUser();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        response = userService.getAddressStatus(userId);
        assertEquals(false, response.addressAvailable());

        mockUser = new UserTestBuilder()
                .withId(userId)
                .withoutAddressLine1()
                .withoutCity()
                .getUser();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        response = userService.getAddressStatus(userId);
        assertEquals(false, response.addressAvailable());
    }


    @Test
    void getAddressStatus_shouldReturnTrue_whenCompleteAddress() {
        String userId = "ID_121";

        User mockUser = new UserTestBuilder()
                .withId(userId)
                .getUser();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        AddressStatusResponse response = userService.getAddressStatus(userId);
        assertTrue(response.addressAvailable());
    }

    @Test
    void testWizardStatusRequest() {
        String userId = "ID_121";
        int volunteerStage = 3;

        User mockUser = new UserTestBuilder()
                .withId(userId)
                .withVolunteerStage(volunteerStage)
                .getUser();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        WizardStatusResponse response = userService.getWizardStatus(userId);
        assertEquals(volunteerStage, response.promotion_wizard_stage());
    }

    @Test
    void getWizardStatus_shouldThrowError_whenNoMatchingUser() {
        String userId = "ID_121";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> userService.getWizardStatus(userId));
    }
}


@Getter
class UserTestBuilder {
    private final User user;

    public UserTestBuilder() {
        user = new User();
        user.setId("U001");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setVolunteerStage(1);
        user.setAddressLine1("123 Main St");
        user.setCity("New York");
        user.setZipCode("10001");

        Country country = new Country();
        country.setCountryName("US");
        State state = new State();
        state.setStateName("NY");

        user.setCountry(country);
        user.setState(state);
    }

    public UserTestBuilder withId(String id) {
        user.setId(id);
        return this;
    }

    public UserTestBuilder withoutCompleteAddress() {
        user.setAddressLine1(null);
        user.setCity(null);
        user.setZipCode(null);
        user.setCountry(null);
        user.setState(null);
        return this;
    }

    public UserTestBuilder withoutCountry() {
        user.setCountry(null);
        user.setState(null);
        return this;
    }

    public UserTestBuilder withoutAddressLine1() {
        user.setAddressLine1(null);
        return this;
    }

    public UserTestBuilder withoutZipCode() {
        user.setZipCode(null);
        return this;
    }

    public UserTestBuilder withVolunteerStage(int stage) {
        user.setVolunteerStage(stage);
        return this;
    }

    public UserTestBuilder withoutCity() {
        user.setCity(null);
        return this;
    }

}
