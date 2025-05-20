package com.doctor.booking.mapper;

import com.doctor.booking.dto.AppointmentDTO;
import com.doctor.booking.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    /**
     * Маппим patient.name → patientName.
     * Остальные поля (id, appointmentTime, status) совпадают по именам.
     */
    @Mapping(source = "patient.name", target = "patientName")
    AppointmentDTO toDto(Appointment appointment);
}
