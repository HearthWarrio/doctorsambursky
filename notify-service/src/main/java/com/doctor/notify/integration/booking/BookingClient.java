package com.doctor.notify.integration.booking;

import com.doctor.notify.integration.booking.dto.*;

public interface BookingClient {
    AppointmentDTO createAppointment(BotCreateAppointmentRequestDTO dto);
    AppointmentDTO doctorAction(long id, DoctorActionRequestDTO dto);
    AppointmentDTO patientReschedule(long id, PatientRescheduleRequestDTO dto);
    AppointmentDTO getAppointment(long id);
}