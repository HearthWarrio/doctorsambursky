package com.doctor.booking.mapper;

import com.doctor.booking.dto.BookingRequest;
import com.doctor.booking.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public class PatientMapper {
    @Mapping(source = "name",  target = "name")
    @Mapping(source = "phone", target = "phone")
    @Mapping(source = "email", target = "email")
    Patient toEntity(BookingRequest req);
}
