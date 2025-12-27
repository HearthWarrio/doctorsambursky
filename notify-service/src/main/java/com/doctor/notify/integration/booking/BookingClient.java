package com.doctor.notify.integration.booking;

import com.doctor.notify.integration.booking.dto.*;

import java.util.List;

public interface BookingClient {
    AppointmentDTO createAppointment(BotCreateAppointmentRequestDTO dto);
    AppointmentDTO doctorAction(long id, DoctorActionRequestDTO dto);
    AppointmentDTO patientReschedule(long id, PatientRescheduleRequestDTO dto);
    AppointmentDTO getAppointment(long id);

    List<AppointmentDTO> getSchedule(String fromIso, String toIso);
    AppointmentDTO cancelAppointment(long id, CancelAppointmentRequestDTO dto);
}