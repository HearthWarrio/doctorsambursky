package com.doctor.review.utils;

import com.doctor.review.entity.Review;

public class ReviewUtils {
    private ReviewUtils() {}

    /** Короткая выжимка текста до 100 символов. */
    public static String summarize(Review r) {
        String t = r.getText();
        return t.length() <= 100 ? t : t.substring(0, 100) + "...";
    }
}
