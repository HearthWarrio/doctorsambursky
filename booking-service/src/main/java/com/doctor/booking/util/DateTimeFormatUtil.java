package com.doctor.booking.util;

import com.doctor.booking.exception.InvalidTimeFormatException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class DateTimeFormatUtil {

    public static final String BOT_PATTERN = "HH:mm dd:MM:yy";
    private static final DateTimeFormatter BOT_FMT = DateTimeFormatter.ofPattern(BOT_PATTERN);

    private DateTimeFormatUtil() {}

    public static LocalDateTime parseBotDateTime(String value) {
        try {
            return LocalDateTime.parse(value, BOT_FMT);
        } catch (DateTimeParseException e) {
            throw new InvalidTimeFormatException("Неверный формат времени. Ожидается: " + BOT_PATTERN);
        }
    }
}