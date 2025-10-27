package org.sfa.volunteer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sfa.volunteer.dto.response.SignOutResponse;
import org.sfa.volunteer.exception.UserNotFoundException;
import org.sfa.volunteer.model.*;
import org.sfa.volunteer.repository.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl - Sign-Out Feature Tests")
class UserServiceImplSignOutTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserStatusRepository userStatusRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserCategoryRepository userCategoryRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private VolunteerRepository volunteerRepository;

    @Mock
    private UserAdditionalDetailRepository userAdditionalDetailRepository;

    @Mock
    private VolunteerUserAvailabilityRepository volunteerUserAvailabilityRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private String testUserId;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUserId = "user-123";

        // Create a test user with sample data
        testUser = User.builder()
                .id(testUserId)
                .firstName("Nish")
                .middleName("Mario")
                .lastName("Rao")
                .fullName("Nish Mario Rao")
                .primaryEmailAddress("nish.rao@example.com")
                .primaryPhoneNumber("1234567890")
                // .profilePicturePath("/path/to/picture.jpg")
                .addressLine1("Main St")
                .addressLine2("Apt 2A")
                .addressLine3("Building C")
                .city("San Jose")
                .zipCode("94102")
                .gender("Male")
                .lastLocation("Location1")
                .language1("English")
                .language2("Spanish")
                .language3("French")
                .volunteerStage(1)
                .volunteerUpdateDate(ZonedDateTime.now())
                .lastUpdateDate(ZonedDateTime.now())
                .timeZone("UTC")
                .build();
    }

    @Test
    @DisplayName("Test successful sign-out - user exists with all related data")
    void testSignOut_WithAllRelatedData_Success() {
        // Arrange
        Volunteer volunteer = mock(Volunteer.class);
        UserAdditionalDetail additionalDetail = mock(UserAdditionalDetail.class);
        Organization organization = mock(Organization.class);
        List<VolunteerUserAvailability> availabilityList = new ArrayList<>();
        availabilityList.add(mock(VolunteerUserAvailability.class));

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(volunteerRepository.findVolunteerByUserId(testUserId)).thenReturn(volunteer);
        when(userAdditionalDetailRepository.findAll()).thenReturn(List.of(additionalDetail));
        when(additionalDetail.getUser()).thenReturn(testUser);
        when(organizationRepository.findByUserId(testUserId)).thenReturn(Optional.of(organization));
        when(volunteerUserAvailabilityRepository.findUserAvailability(testUserId)).thenReturn(availabilityList);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // Act
        SignOutResponse response = userService.signOut(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("User signed out successfully and all user data cleared", response.getMessage());
        assertNotNull(response.getSignOutTime());

        // Verify that user personal data is cleared
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertNull(savedUser.getFirstName());
        assertNull(savedUser.getMiddleName());
        assertNull(savedUser.getLastName());
        assertNull(savedUser.getPrimaryEmailAddress());
        assertNull(savedUser.getPrimaryPhoneNumber());
        assertNull(savedUser.getProfilePicturePath());
        assertNull(savedUser.getAddressLine1());
        assertNull(savedUser.getAddressLine2());
        assertNull(savedUser.getAddressLine3());
        assertNull(savedUser.getCity());
        assertNull(savedUser.getZipCode());
        assertNull(savedUser.getGender());
        assertNull(savedUser.getLastLocation());
        assertNull(savedUser.getLanguage1());
        assertNull(savedUser.getLanguage2());
        assertNull(savedUser.getLanguage3());
        assertNull(savedUser.getVolunteerStage());
        assertNull(savedUser.getVolunteerUpdateDate());
        assertNotNull(savedUser.getLastUpdateDate());

        // Verify related data is deleted
        verify(volunteerRepository, times(1)).delete(volunteer);
        verify(userAdditionalDetailRepository, times(1)).delete(additionalDetail);
        verify(organizationRepository, times(1)).delete(organization);
        verify(volunteerUserAvailabilityRepository, times(1)).deleteAll(availabilityList);
    }

    @Test
    @DisplayName("Test successful sign-out - user exists with no related data")
    void testSignOut_WithNoRelatedData_Success() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(volunteerRepository.findVolunteerByUserId(testUserId)).thenReturn(null);
        when(userAdditionalDetailRepository.findAll()).thenReturn(new ArrayList<>());
        when(organizationRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(volunteerUserAvailabilityRepository.findUserAvailability(testUserId)).thenReturn(new ArrayList<>());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // Act
        SignOutResponse response = userService.signOut(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("User signed out successfully and all user data cleared", response.getMessage());
        assertNotNull(response.getSignOutTime());

        // Verify user is saved with cleared data
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertNull(savedUser.getFirstName());

        // Verify no deletions were attempted for non-existent data
        verify(volunteerRepository, never()).delete(any());
        verify(userAdditionalDetailRepository, never()).delete(any());
        verify(organizationRepository, never()).delete(any());
        verify(volunteerUserAvailabilityRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("Test sign-out - user exists with only volunteer data")
    void testSignOut_WithOnlyVolunteerData_Success() {
        // Arrange
        Volunteer volunteer = mock(Volunteer.class);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(volunteerRepository.findVolunteerByUserId(testUserId)).thenReturn(volunteer);
        when(userAdditionalDetailRepository.findAll()).thenReturn(new ArrayList<>());
        when(organizationRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(volunteerUserAvailabilityRepository.findUserAvailability(testUserId)).thenReturn(new ArrayList<>());

        // Act
        SignOutResponse response = userService.signOut(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());

        // Verify volunteer is deleted
        verify(volunteerRepository, times(1)).delete(volunteer);
    }

    @Test
    @DisplayName("Test sign-out - user exists with only additional details")
    void testSignOut_WithOnlyAdditionalDetails_Success() {
        // Arrange
        UserAdditionalDetail additionalDetail = mock(UserAdditionalDetail.class);
        when(additionalDetail.getUser()).thenReturn(testUser);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(volunteerRepository.findVolunteerByUserId(testUserId)).thenReturn(null);
        when(userAdditionalDetailRepository.findAll()).thenReturn(List.of(additionalDetail));
        when(organizationRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(volunteerUserAvailabilityRepository.findUserAvailability(testUserId)).thenReturn(new ArrayList<>());

        // Act
        SignOutResponse response = userService.signOut(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());

        // Verify additional detail is deleted
        verify(userAdditionalDetailRepository, times(1)).delete(additionalDetail);
    }

    @Test
    @DisplayName("Test sign-out - user exists with only organization data")
    void testSignOut_WithOnlyOrganization_Success() {
        // Arrange
        Organization organization = mock(Organization.class);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(volunteerRepository.findVolunteerByUserId(testUserId)).thenReturn(null);
        when(userAdditionalDetailRepository.findAll()).thenReturn(new ArrayList<>());
        when(organizationRepository.findByUserId(testUserId)).thenReturn(Optional.of(organization));
        when(volunteerUserAvailabilityRepository.findUserAvailability(testUserId)).thenReturn(new ArrayList<>());

        // Act
        SignOutResponse response = userService.signOut(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());

        // Verify organization is deleted
        verify(organizationRepository, times(1)).delete(organization);
    }

    @Test
    @DisplayName("Test sign-out - user exists with only availability data")
    void testSignOut_WithOnlyAvailability_Success() {
        // Arrange
        List<VolunteerUserAvailability> availabilityList = new ArrayList<>();
        availabilityList.add(mock(VolunteerUserAvailability.class));

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(volunteerRepository.findVolunteerByUserId(testUserId)).thenReturn(null);
        when(userAdditionalDetailRepository.findAll()).thenReturn(new ArrayList<>());
        when(organizationRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(volunteerUserAvailabilityRepository.findUserAvailability(testUserId)).thenReturn(availabilityList);

        // Act
        SignOutResponse response = userService.signOut(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());

        // Verify availability is deleted
        verify(volunteerUserAvailabilityRepository, times(1)).deleteAll(availabilityList);
    }

    @Test
    @DisplayName("Test sign-out - user not found")
    void testSignOut_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.signOut(testUserId)
        );

        assertEquals(testUserId, exception.getUserId());

        // Verify no data clearing operations were attempted
        verify(volunteerRepository, never()).delete(any());
        verify(userAdditionalDetailRepository, never()).delete(any());
        verify(organizationRepository, never()).delete(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test sign-out - multiple additional details for same user")
    void testSignOut_MultipleAdditionalDetails_Success() {
        // Arrange
        UserAdditionalDetail detail1 = mock(UserAdditionalDetail.class);
        UserAdditionalDetail detail2 = mock(UserAdditionalDetail.class);
        when(detail1.getUser()).thenReturn(testUser);
        when(detail2.getUser()).thenReturn(testUser);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(volunteerRepository.findVolunteerByUserId(testUserId)).thenReturn(null);
        when(userAdditionalDetailRepository.findAll()).thenReturn(List.of(detail1, detail2));
        when(organizationRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(volunteerUserAvailabilityRepository.findUserAvailability(testUserId)).thenReturn(new ArrayList<>());

        // Act
        SignOutResponse response = userService.signOut(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());

        // Verify both additional details are deleted
        verify(userAdditionalDetailRepository, times(1)).delete(detail1);
        verify(userAdditionalDetailRepository, times(1)).delete(detail2);
    }

    @Test
    @DisplayName("Test sign-out - multiple availability records")
    void testSignOut_MultipleAvailabilityRecords_Success() {
        // Arrange
        List<VolunteerUserAvailability> availabilityList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            availabilityList.add(mock(VolunteerUserAvailability.class));
        }

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(volunteerRepository.findVolunteerByUserId(testUserId)).thenReturn(null);
        when(userAdditionalDetailRepository.findAll()).thenReturn(new ArrayList<>());
        when(organizationRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(volunteerUserAvailabilityRepository.findUserAvailability(testUserId)).thenReturn(availabilityList);

        // Act
        SignOutResponse response = userService.signOut(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());

        // Verify all availability records are deleted
        verify(volunteerUserAvailabilityRepository, times(1)).deleteAll(availabilityList);
        assertEquals(5, availabilityList.size());
    }

    @Test
    @DisplayName("Test sign-out - additional details for different user should not be deleted")
    void testSignOut_AdditionalDetailsForDifferentUser_NotDeleted() {
        // Arrange
        User differentUser = mock(User.class);
        when(differentUser.getId()).thenReturn("different-user-id");

        UserAdditionalDetail detailForDifferentUser = mock(UserAdditionalDetail.class);
        when(detailForDifferentUser.getUser()).thenReturn(differentUser);

        UserAdditionalDetail detailForCurrentUser = mock(UserAdditionalDetail.class);
        when(detailForCurrentUser.getUser()).thenReturn(testUser);

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(volunteerRepository.findVolunteerByUserId(testUserId)).thenReturn(null);
        when(userAdditionalDetailRepository.findAll()).thenReturn(List.of(detailForCurrentUser, detailForDifferentUser));
        when(organizationRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(volunteerUserAvailabilityRepository.findUserAvailability(testUserId)).thenReturn(new ArrayList<>());

        // Act
        SignOutResponse response = userService.signOut(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());

        // Verify only current user's detail is deleted
        verify(userAdditionalDetailRepository, times(1)).delete(detailForCurrentUser);
        verify(userAdditionalDetailRepository, never()).delete(detailForDifferentUser);
    }

    @Test
    @DisplayName("Test sign-out - last update date is set to current time")
    void testSignOut_LastUpdateDateUpdated_Success() {
        // Arrange
        ZonedDateTime beforeSignOut = ZonedDateTime.now(ZoneId.of("UTC"));
        
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(volunteerRepository.findVolunteerByUserId(testUserId)).thenReturn(null);
        when(userAdditionalDetailRepository.findAll()).thenReturn(new ArrayList<>());
        when(organizationRepository.findByUserId(testUserId)).thenReturn(Optional.empty());
        when(volunteerUserAvailabilityRepository.findUserAvailability(testUserId)).thenReturn(new ArrayList<>());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // Act
        userService.signOut(testUserId);

        // Assert
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        ZonedDateTime afterSignOut = ZonedDateTime.now(ZoneId.of("UTC"));
        assertNotNull(savedUser.getLastUpdateDate());
        assertTrue(savedUser.getLastUpdateDate().isAfter(beforeSignOut.minusSeconds(1)));
        assertTrue(savedUser.getLastUpdateDate().isBefore(afterSignOut.plusSeconds(1)));
    }

    @Test
    @DisplayName("Test sign-out - all data cleared simultaneously")
    void testSignOut_AllDataTypesPresent_Success() {
        // Arrange
        Volunteer volunteer = mock(Volunteer.class);
        UserAdditionalDetail additionalDetail = mock(UserAdditionalDetail.class);
        when(additionalDetail.getUser()).thenReturn(testUser);
        Organization organization = mock(Organization.class);
        List<VolunteerUserAvailability> availabilityList = new ArrayList<>();
        availabilityList.add(mock(VolunteerUserAvailability.class));

        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(volunteerRepository.findVolunteerByUserId(testUserId)).thenReturn(volunteer);
        when(userAdditionalDetailRepository.findAll()).thenReturn(List.of(additionalDetail));
        when(organizationRepository.findByUserId(testUserId)).thenReturn(Optional.of(organization));
        when(volunteerUserAvailabilityRepository.findUserAvailability(testUserId)).thenReturn(availabilityList);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // Act
        SignOutResponse response = userService.signOut(testUserId);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());

        // Verify all types of data are deleted
        verify(volunteerRepository, times(1)).delete(volunteer);
        verify(userAdditionalDetailRepository, times(1)).delete(additionalDetail);
        verify(organizationRepository, times(1)).delete(organization);
        verify(volunteerUserAvailabilityRepository, times(1)).deleteAll(availabilityList);
        verify(userRepository, times(1)).save(userCaptor.capture());

        // Verify user data is cleared
        User savedUser = userCaptor.getValue();
        assertNull(savedUser.getFirstName());
        assertNull(savedUser.getPrimaryEmailAddress());
    }

    @Test
    @DisplayName("Test sign-out - database transaction rollback on error")
    void testSignOut_DatabaseError_ThrowsException() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(volunteerRepository.findVolunteerByUserId(testUserId)).thenReturn(null);
        when(userRepository.save(any())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(
                RuntimeException.class,
                () -> userService.signOut(testUserId)
        );

        // Verify repository operations were attempted
        verify(userRepository, times(1)).findById(testUserId);
    }
}
