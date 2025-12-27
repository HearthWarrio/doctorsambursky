package com.doctor.booking.mapper;

import com.doctor.booking.dto.AppointmentDTO;
import com.doctor.booking.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {

    @Mapping(source = "patient.name", target = "patientName")
    @Mapping(source = "patient.phone", target = "patientPhone")
    @Mapping(source = "patient.email", target = "patientEmail")
    @Mapping(source = "patient.address", target = "patientAddress")
    @Mapping(source = "patient.telegramUsername", target = "patientTelegramUsername")
    @Mapping(source = "patient.whatsappNumber", target = "patientWhatsappNumber")
    @Mapping(source = "patient.telegramChatId", target = "patientTelegramChatId")
    AppointmentDTO toDto(Appointment appointment);
}