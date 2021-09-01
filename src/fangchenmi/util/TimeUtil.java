package fangchenmi.util;

import java.util.Calendar;
import java.util.Date;

public class TimeUtil {
    public static boolean checkAdult(Date date) {
        Calendar current = Calendar.getInstance();
        Calendar birthDay = Calendar.getInstance();
        birthDay.setTime(date);

        int year = current.get(Calendar.YEAR) - birthDay.get(Calendar.YEAR);
        if (year > 18) {
            return true;
        } else if (year < 18) {
            return false;
        }

        int month = current.get(Calendar.MONTH) - birthDay.get(Calendar.MONTH);
        if (month > 0) {
            return true;
        } else if (month < 0) {
            return false;
        }

        int day = current.get(Calendar.DAY_OF_MONTH) - birthDay.get(Calendar.DAY_OF_MONTH);
        return day >= 0;
    }

    public static boolean allowMinor() {
        Calendar current = Calendar.getInstance();
        return (current.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY || current.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || current.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) && current.get(Calendar.HOUR_OF_DAY) >= 20 && current.get(Calendar.HOUR_OF_DAY) <= 21;
    }
}
