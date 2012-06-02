package org.mitre.giscore.test.utils;

import org.junit.Test;

import org.mitre.giscore.utils.DateParser;
import org.mitre.giscore.utils.SafeDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * @author Jason Mathews
 *         Date: 6/1/12 2:44 PM
 */
public class TestDateParser {

    private static final TimeZone tz = TimeZone.getTimeZone("UTC");
    private final SafeDateFormat dateFormat = new SafeDateFormat("yyyyMMdd");

    @Test
    public void testParser() {
        String[] dates = {
                "2012-05-29T17:00:00.000Z",
                "2012-05-29T17:00:00.000",
                "2012-05-29T17:00:00Z",
                "2012-05-29T17:00:00",
                "2012-05-29T17:00:00-0700",
                "2012-05-29T22:30+04",
                "2012-05-29T1130-0700",
                // YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
                // YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
                // YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
                "2012-05-29T15:00-03:30",
                "05/29/2012 17:00:00",
                "05/29/2012 17:00",
                "05/29/2012",
                "29-May-2012",
                "29 May 2012",
                " 29 May 2012 ",
                "29-MAY-2012",
                "May-29-2012",
                "2012-May-29",
                "20120529",
                "201205291730",
                "20120529170000",
                "5/29/2012 1:45:30 PM",
                "2012.05.29 AD at 12:08:56 PDT",
                "Tue, 29 May 2012 12:08:56 -0700",
                "Tuesday, May 29, 2012",
                "May 29, 2012",
        };
        Calendar cal = Calendar.getInstance(tz);
        cal.set(2012, Calendar.MAY, 29);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        for (String s : dates) {
            Date d = DateParser.parse(s);
            assertEquals(date, d);
        }
    }

    @Test
    public void testShortDate() {
        Date d = DateParser.parse("5 May 2012");
        assertNotNull(d);
        assertEquals("20120505", dateFormat.format(d));
    }

    @Test
    public void testLongMonth() {
        Date d = DateParser.parse("5 January 2012");
        assertNotNull(d);
        assertEquals("20120105", dateFormat.format(d));
    }

    @Test
    public void testBadDates() {
        String[] dates = {

                "05/31/000012:30PM", // bogus date
                "May 2012",
                "May 29",
                "01:45:30",
                "1 2 3",
                "abc",
                "a b c"

        };
        for (String s : dates) {
            //System.out.println("---");
            //System.out.println(s);
            Date d = DateParser.parse(s);
            // System.out.println("XXX d="+d);
            assertNull(d);
        }
    }

}
