package org.mitre.giscore.test;

import org.junit.Test;
import org.mitre.giscore.events.Style;

import java.awt.Color;

import static org.junit.Assert.*;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: 4/18/12 3:15 PM
 */
public class TestStyle {

	@Test
	public void testStyle() {
		Style style = new Style("123");
		style.setIconStyle(Color.red, 1.4, "http://maps.google.com/mapfiles/kml/shapes/airports.png");
		style.setListStyle(Color.GREEN, Style.ListItemType.check);
		style.setBalloonStyle(Color.BLUE, "text $[description]", Color.BLACK, "default");
		assertNotNull(style.getId());
		assertNotNull(style.getIconUrl());
		assertTrue(style.hasIconStyle());
		assertTrue(style.hasListStyle());
		assertTrue(style.hasBalloonStyle());
		assertFalse(style.hasLineStyle());
		assertFalse(style.hasPolyStyle());
		assertFalse(style.hasLabelStyle());
		assertNull(style.getLineColor());
	}

	@Test
	public void testStyleCopyEquals() {
		Style s1 = new Style("123");
		s1.setIconStyle(Color.red, 1.4, "http://maps.google.com/mapfiles/kml/shapes/airports.png");
		s1.setListStyle(Color.GREEN, Style.ListItemType.check);
		s1.setBalloonStyle(Color.BLUE, "text $[description]", Color.BLACK, "default");

		Style s2 = new Style(s1);
		assertEquals(s1, s2);

		s2.setListStyle(Color.BLACK, Style.ListItemType.check);
		assertFalse(s1.equals(s2));
		assertFalse(s2.equals(s1));
	}
}
