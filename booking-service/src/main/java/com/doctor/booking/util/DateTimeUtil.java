package com.doctor.booking.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public static String format(LocalDateTime dt) {
        return dt.format(FMT);
    }
}
