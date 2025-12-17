package org.sfa.volunteer.controller;

import org.sfa.volunteer.dto.common.SaayamResponse;
import org.sfa.volunteer.dto.common.SaayamStatusCode;
import org.sfa.volunteer.dto.response.NotificationsResponse;
import org.sfa.volunteer.dto.response.PaginationResponse;
import org.sfa.volunteer.dto.response.VolunteerResponse;
import org.sfa.volunteer.service.RequestService;
import org.sfa.volunteer.util.ResponseBuilder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/0.0.1/requests")
public class RequestController {
    private final ResponseBuilder responseBuilder;
    private final RequestService requestService;


    public RequestController(ResponseBuilder responseBuilder, RequestService requestService) {
        this.responseBuilder = responseBuilder;
        this.requestService = requestService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello World!";
    }

    @GetMapping("/counts")
    public Long count() {
        return 10L;
    }

    //    Current status - returns the result, but the ID does not exist, getting the error related to volunteer id not existing
    @GetMapping("/count")
    public SaayamResponse<Long> getNotificationsCount(@RequestParam String userId) {
//        Actual code for getting the notifications count.
        Long notificationCount = requestService.getNotificationsCountAfterLastAccessed(userId);
        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, notificationCount);

//        below code is without using the clientTime
//        Long notificationCount = requestService.getNotificationsCount(userId);
//        return responseBuilder.buildSuccessResponse(SaayamStatusCode.SUCCESS, notificationCount);
    }

    //    Need to test this
//    http://localhost:8080/0.0.1/requests/getNotifications?userId=1
    @GetMapping("/getNotifications")
    public List<NotificationsResponse> getAllNotifications(@RequestParam String userId) {
        System.out.println("Here");
        return requestService.getNotificationsList(userId);
//        List<NotificationsResponse> notificationsList = requestService.getNotifications(userId);

//        NotificationsResponse response1 = new NotificationsResponse("Volunteer", "New Match Request", "You have new Volunteer match request in Logistics", LocalDateTime.of(2025, 10, 13, 15, 30));
//        NotificationsResponse response2 = new NotificationsResponse("helpRequest", "Educational Help", "You have new helpRequest match request in Logistics", LocalDateTime.of(2025, 10, 14, 15, 30));
//        NotificationsResponse response3 = new NotificationsResponse("Volunteer", "New Match Request", "You have new Volunteer match request in Logistics", LocalDateTime.of(2025, 10, 15, 15, 30));
//        NotificationsResponse response4 = new NotificationsResponse("helpRequest", "Educational Help", "You have new helpRequest match request in Logistics", LocalDateTime.of(2025, 10, 16, 15, 30));
//
//        List<NotificationsResponse> notificationsListTest = new ArrayList<>();
//        notificationsListTest.add(response1);
//        notificationsListTest.add(response2);
//        notificationsListTest.add(response3);
//        notificationsListTest.add(response4);
//
//        return notificationsListTest;

    }
}