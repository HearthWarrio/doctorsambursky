package com.doctor.notify.web;

import com.doctor.notify.bot.TelegramSendService;
import com.doctor.notify.web.dto.DoctorApprovalNotificationDTO;
import com.doctor.notify.web.dto.SendMessageNotificationDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/telegram")
@RequiredArgsConstructor
public class InternalTelegramController {

    private final TelegramSendService telegram;

    @PostMapping("/doctor-approval")
    public ResponseEntity<Void> doctorApproval(@Valid @RequestBody DoctorApprovalNotificationDTO dto) {
        telegram.sendDoctorApproval(dto.getDoctorChatId(), dto.getAppointmentId(), dto.getText());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-message")
    public ResponseEntity<Void> sendMessage(@Valid @RequestBody SendMessageNotificationDTO dto) {
        telegram.sendText(dto.getChatId(), dto.getText());
        return ResponseEntity.ok().build();
    }
}