package com.doctor.booking.integration.notify;

import com.doctor.booking.integration.notify.dto.DoctorApprovalNotificationDTO;
import com.doctor.booking.integration.notify.dto.SendMessageNotificationDTO;

public interface NotifyClient {
    void sendDoctorApproval(DoctorApprovalNotificationDTO dto);
    void sendMessage(SendMessageNotificationDTO dto);
}