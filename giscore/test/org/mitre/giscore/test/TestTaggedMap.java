package org.mitre.giscore.test;

import org.mitre.giscore.events.TaggedMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: Jun 16, 2009 11:23:20 AM
 */
public class TestTaggedMap {
    
    @Test
	public void testGetPuts() {
        TaggedMap tm = new TaggedMap("extra");
        tm.put("str", "this is a string");
        tm.put("int", "123");
        tm.put("double", "2.2");
        tm.put("neg", "-3.14");
        
        assertEquals("extra", tm.getTag());
        assertNotNull(tm.get("str"));
        assertNull(tm.get("missing"));
        assertEquals(Integer.valueOf(123), tm.getIntegerValue("int"));
        assertEquals(2.2, tm.getDoubleValue("double"), 1e-5);
        assertEquals(-3.14, tm.getDoubleValue("neg"), 1e-5);
    }
}
