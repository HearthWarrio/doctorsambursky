package com.doctor.booking.mapper;

import com.doctor.booking.dto.BookingRequest;
import com.doctor.booking.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientMapper {
    /**
     * MapStruct сам подтянет name, phone и email.
     */
    Patient toEntity(BookingRequest req);
}
