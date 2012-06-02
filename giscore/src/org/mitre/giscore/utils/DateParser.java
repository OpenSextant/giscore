package org.mitre.giscore.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Simple Date Stream parser that supports multiple string date formats.
 * Used by DbfOutputStream which by design discards timestamps.
 * <p/>
 * Following date formats are supported:
 * <pre>
 *     1. yyyy-MM-dd"T"HH:mm[:ss.SSS]["Z"]
 *     2. yyyyMMdd HHmm[ss]["Z"]
 *     3. yyyyMMddHHmm[ss]["Z"]
 *     4. yyyyMMdd
 *
 *     5. dd-MMM-yyyy[ hh:mm[:ss]] (e.g. 29-May-2012)
 *     6. MMM-dd-yyyy[ hh:mm[:ss]] (e.g. May-29-2012)
 *     7. yyyy-MMM-dd[ hh:mm[:ss]] (e.g. 2012-12-31)
 *
 *     8. MM/dd/yyyy hh:mm[:ss] (e.g. 12/31/2012 10:00:00)
 *     9. MM/dd/yyyy (e.g. 12/31/2012)
 *    10. d[ -]MMM[ -]YYYY (e.g. 1 May 2012)
 *    11. MMM[ -]d,?[ -]YYYY (e.g. May 1, 2012)
 * </pre>
 * Notes:
 * <p/>
 * Date parsing is time zone agnostic.
 * If date-timestamp has time zone offset whose offset would change the effective
 * date then the timestamp offset is ignored (e.g. 2012-05-29T01:00:00-0700 should
 * actually be 2012-05-28T18:00 in UTC but parsed simply instead as 2012-05-29).
 *
 * @author Jason Mathews
 *         Date: 6/1/12 2:02 PM
 */
public final class DateParser {

    private static final Logger log = LoggerFactory.getLogger(DateParser.class);

    private static final String[] months = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};

    private static final Map<String, Integer> monthMap = new HashMap<String, Integer>(12);
    private static final TimeZone tz = TimeZone.getTimeZone("UTC");

    static {
        for (int i = 0; i < 12; i++) {
            monthMap.put(months[i], i + 1);
        }
    }

    public static Date parse(String str) {
        // System.out.println(str);
        String strToken = "";
        int val = 0;
        int year = -1;
        int month = -1;
        int day = -1;
        int digitCount = 0;
        int sz = str.length();
        for (int i = 0; i < sz; i++) {
            char ch = str.charAt(i);
            if (Character.isDigit(ch)) {
                if (year == -1 && digitCount == 4) {
                    /*
                        1. yyyy-MM-dd"T"HH:mm[:ss.SSS]["Z"]
                        2. yyyyMMdd HHmm[ss]["Z"]
                        3. yyyyMMddHHmm[ss]["Z"]
                        4. yyyyMMdd

                        8. MM/dd/yyyy hh:mm[:ss]
                    */
                    year = val;
                    val = 0;
                    digitCount = 0;
                    if (day != -1 && month != -1) {
                        /*
                        unlikely case: would have to seen month and day
                        then 4-digit year immediately followed by a digit
                        (e.g. 05/31/201212:30PM or May 31,201218:30:00)
                        */
                        if (year < 1900) return null; // sanity check
                        break; // completed
                    }
                } else if (year != -1 && digitCount == 2) {
                    digitCount = 0;
                    if (month == -1) {
                        /*
                        2. yyyy => MMdd HHmm[ss]["Z"]
                        3. yyyy => MMddHHmm[ss]["Z"]
                        4. yyyy => MMdd
                        */
                        month = val;
                    } else {
                        /*
                        2. yyyyMM => dd HHmm[ss]["Z"]
                        3. yyyyMM => ddHHmm[ss]["Z"]
                        4. yyyyMM => dd
                        */
                        day = val;
                        break; // completed
                    }
                    val = 0;
                }

                digitCount++;
                val = val * 10 + ch - '0';
                strToken = "";

            } else {

                // otherwise non-digit character (letter, space, punctuation, etc)

                if (digitCount == 4) {
                    year = val;
                    if (day != -1) {
                        digitCount = 0;
                        /*
                        5. dd-MMM-yyyy => [space]hh:mm[:ss]
                        6. MMM-dd-yyyy => [space]hh:mm[:ss]
                        8. MM/dd/yyyy => [space]hh:mm[:ss]
                        */
                        break; // complete
                    }
                    // otherwise
                    // 1. yyyy => -MM-dd"T"HH:mm[:ss.SSS]["Z"]
                } else if (digitCount == 1 || digitCount == 2) {
                    if (month == -1) {
                        /*
                        1. yyyy-MM => -dd"T"HH:mm[:ss.SSS]["Z"]
                        5. dd => -MMM-yyyy[ hh:mm[:ss]]
                        8. MM => /dd/yyyy hh:mm[:ss]
                        9. MM => /dd/yyyy
                        10. d => [ -]MMM[ -]YYYY (e.g. 1 May 2012)
                        */
                        month = val;
                    } else {
                        /*
                        1. yyyy-MM-dd => "T"HH:mm[:ss.SSS]["Z"]
                        2. yyyyMMdd => [space]HHmm[ss]["Z"]
                        6. MMM-dd => -yyyy[ hh:mm[:ss]]
                        7. yyyy-MMM-dd => [space]hh:mm[:ss]
                        8. MM/dd => /yyyy hh:mm[:ss]
                        9. MM/dd => /yyyy
                       11. MMM[ -]d => ,?[ -]YYYY (e.g. May 1, 2012)
                        */
                        day = val;
                        if (year != -1) {
                            // done if already got month & year (e.g. 2012/05/29...)
                            digitCount = 0;
                            break;
                        }
                    }
                } else {
                    // digitCount = 0,3
                    if (digitCount != 0) {
                        log.debug("discard number: val={}", val);
                        return null;
                    }
                }

                if ((ch == '-' || ch == ' ') && strToken.length() >= 3
                        && (month == -1 || day == -1)) {
                    String origStr = strToken;
                    if (strToken.length() > 3) {
                        strToken = strToken.substring(0, 3); // July -> Jul
                    }
                    Integer monthIdx = monthMap.get(strToken.toLowerCase());
                    if (monthIdx != null) {
                        if (month != -1) {
                            // 5. dd-MMM => -yyyy[ hh:mm[:ss]]
                            // 10. d[ -]MMM => [ -]YYYY (e.g. 1 May 2012)
                            // swap month value into day
                            day = month;
                        }
                        // otherwise month == -1
                        //  6. MMM => -dd-yyyy[ hh:mm[:ss]]
                        //  7. yyyy-MMM => -dd[space]hh:mm[:ss]
                        // 11. MMM => [ -]d,?[ -]YYYY (e.g. May 1, 2012)
                        month = monthIdx;
                    } else {
                        log.debug("invalid month abbreviation lookup {}", origStr);
                        return null;
                    }
                    strToken = "";
                } else {
                    // if (ch != ',' && ch != ' ' && ch != '-' && ch != '/') {
                    if (Character.isLetter(ch)) {
                        strToken += ch;
                    } else if (!strToken.isEmpty()) {
                        // discard comma, dash, space, etc.
                        log.debug("discard token: {}", strToken);
                        strToken = ""; // reset
                    }
                }
                digitCount = 0;
                val = 0;
            }
        } // for

        if (year == -1 && digitCount == 4) {
            // e.g. "05/29/2012"
            year = val;
        } else if (day == -1 && month != -1 && digitCount == 2) {
            // e.g. "20120529"
            day = val;
        }

        // System.out.printf("\tval=%04d cnt=%d [%04d/%02d/%02d]%n%n", val, digitCount, year, month, day);

        if (year <= 0 || month <= 0 || month > 12 || day <= 0) {
            return null;
        }

        Calendar cal = Calendar.getInstance(tz);
        cal.set(year, month - 1, day);
        //cal.set(Calendar.YEAR, year);
        //cal.set(Calendar.MONTH, month);
        //cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // System.out.printf("\tcal: %d/%d [%d]%n", cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), cal.getTimeInMillis());
        return cal.getTime();
    }
}

