package org.mitre.giscore.test.utils;

import org.junit.Test;
import org.mitre.giscore.utils.Color;

import static org.junit.Assert.*;

/**
 * Tests for the Color class.
 * @author Jason Mathews, MITRE Corporation
 * Date: 7/5/12 7:02 PM
 */
public class TestColor {

    @Test
    public void testCreateWithAlpha() {
        Color c = new Color(0x11223344, true);
        assertEquals(c.getAlpha(), 0x11);
        assertEquals(c.getRed(), 0x22);
        assertEquals(c.getGreen(), 0x33);
        assertEquals(c.getBlue(), 0x44);
        assertEquals(c.getRGB(), 0x11223344);
    }

    @Test
    public void testCreateNoAlpha() {
        Color c = new Color(0x223344);
        assertEquals(c.getAlpha(), 0xff);
        assertEquals(c.getRed(), 0x22);
        assertEquals(c.getGreen(), 0x33);
        assertEquals(c.getBlue(), 0x44);
        assertEquals(c.getRGB(), 0xff223344);
    }

    @Test
    public void testEquals() {
        assertEquals(new Color(0,0,0), Color.BLACK);
    }

    @Test
    public void testFromAwtColor() {
        assertEquals(Color.BLACK, new Color(java.awt.Color.BLACK));
    }

    @Test
    public void testToAwtColor() {
        assertEquals(Color.BLACK.getRGB(), Color.BLACK.toAwtColor().getRGB());
    }

    @Test
    public void testToAwtColorAlpha() {
        Color c = new Color(10, 20, 30, 128);
        assertEquals(128, c.toAwtColor(true).getAlpha());
        assertEquals(255, c.toAwtColor(false).getAlpha());

        c = new Color(10, 20, 30);
        assertEquals(255, c.toAwtColor(true).getAlpha());
        assertEquals(255, c.toAwtColor(false).getAlpha());
    }

}
