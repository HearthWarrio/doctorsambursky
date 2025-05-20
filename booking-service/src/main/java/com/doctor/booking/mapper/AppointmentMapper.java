package com.doctor.booking.mapper;

import com.doctor.booking.dto.AppointmentDTO;
import com.doctor.booking.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public class AppointmentMapper {
    @Mapping(source = "patient.name", target = "patientName")
    @Mapping(source = "appointmentTime", target = "appointmentTime")
    AppointmentDTO toDto(Appointment appt);
}
