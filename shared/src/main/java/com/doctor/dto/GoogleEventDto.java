package com.doctor.dto;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class GoogleEventDto {
    private String eventId;
    private String summary;
    private ZonedDateTime start;
    private ZonedDateTime end;
}
