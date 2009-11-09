package org.mitre.giscore.test;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.TaggedMap;
import org.mitre.giscore.events.AltitudeModeEnumType;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.geometry.Point;

import java.util.Date;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: Sep 28, 2009 2:12:09 PM
 */
public class TestBaseFeature {

    @Test
    public void testGetSetDates() {
        Feature f = new Feature();
        Date now = new Date();
        Date start = (Date)now.clone();
        f.setStartTime(now);
        assertEquals(start, f.getStartTime());
        now.setTime(now.getTime() + 60000);
        Date endTime = (Date)now.clone();
        f.setEndTime(now);
        // if we're storing clones of Date objects then changing the internal structure of date
        // outside feature context won't affect what we set in our private Date field.
        assertEquals(start, f.getStartTime());
        assertEquals(endTime, f.getEndTime());
		/*
        now = f.getStartTime();
        now.setTime(now.getTime() + 60000);
        now = f.getEndTime();
        now.setTime(now.getTime() + 60000);
        // likewise when we return our private Dates the caller shouldn't be able to change
        // the internal structure
        assertEquals(start, f.getStartTime());
        assertEquals(endTime, f.getEndTime());
        */
    }

	@Test
	public void testView() {
		Feature f = new Feature();
		f.setGeometry(new Point(37,-122));
		TaggedMap viewGroup = new TaggedMap(IKml.LOOK_AT);
		viewGroup.put(IKml.LONGITUDE, "-122.081253214144");
		viewGroup.put(IKml.LATITUDE, "37.41937112712314");
		viewGroup.put(IKml.HEADING, "-145.6454960761126");
		viewGroup.put(IKml.TILT, "65.3863434407203");
		viewGroup.put(IKml.RANGE, "34.59480922516595");
		viewGroup.put(IKml.ALTITUDE_MODE, AltitudeModeEnumType.clampToGround.toString());
		viewGroup.put("foo", "bar"); // non-kml store tag
		f.setViewGroup(viewGroup);
		TaggedMap lookAt = f.getViewGroup();
		assertEquals(viewGroup.size(), lookAt.size());
	}

}

