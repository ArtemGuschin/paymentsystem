
package com.artem.notificationservice.controller;

import com.artem.common.notification.dto.NotificationResponse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import com.artem.common.notification.dto.NotificationStatus;
import com.artem.notificationservice.config.SecurityConfig;
import com.artem.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationControllerV1.class)
@Import(SecurityConfig.class)
class NotificationControllerSecurityTest {

    private static final String USER_UID =
            "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void getWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(
                        get("/api/v1/notifications/{userUid}", USER_UID)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getWithoutInternalRoleReturns403() throws Exception {
        mockMvc.perform(
                        get("/api/v1/notifications/{userUid}", USER_UID)
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @WithMockUser(roles = "NOTIFICATION_INTERNAL")
    void getWithInternalRoleReturns200() throws Exception {
        when(notificationService.getNotificationsByUserUid(USER_UID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/notifications/{userUid}", USER_UID)
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(notificationService)
                .getNotificationsByUserUid(USER_UID);
    }

    @Test
    void patchWithoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/notifications/17/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"COMPLETED\"}")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }

    @Test
    @WithMockUser(roles = "USER")
    void patchWithoutInternalRoleReturns403() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/notifications/17/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"COMPLETED\"}")
                )
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @WithMockUser(roles = "NOTIFICATION_INTERNAL")
    void patchWithInternalRoleReturns200() throws Exception {
        NotificationResponse response = new NotificationResponse(
                17L,
                null,
                null,
                "Notification message",
                USER_UID,
                NotificationStatus.COMPLETED,
                "Notification subject",
                "SYSTEM"
        );

        when(notificationService.completeNotification(17L))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/v1/notifications/17/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"COMPLETED\"}")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.notificationStatus")
                                .value("COMPLETED")
                );

        verify(notificationService).completeNotification(17L);
    }

    @Test
    void getWithWrongBasicPasswordReturns401() throws Exception {
        mockMvc.perform(
                        get("/api/v1/notifications/{userUid}", USER_UID)
                                .with(httpBasic(
                                        "notification-test",
                                        "wrong-password"
                                ))
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }

    @Test
    void getWithValidBasicCredentialsReturns200() throws Exception {
        when(notificationService.getNotificationsByUserUid(USER_UID))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/v1/notifications/{userUid}", USER_UID)
                                .with(httpBasic(
                                        "notification-test",
                                        "test-password-only"
                                ))
                )
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(notificationService)
                .getNotificationsByUserUid(USER_UID);
    }



}
