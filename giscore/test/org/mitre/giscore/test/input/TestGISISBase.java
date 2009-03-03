/****************************************************************************************
 *  TestGISISBase.java
 *
 *  Created: Jan 26, 2009
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2009
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantibility and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.test.input;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import org.mitre.giscore.events.ContainerEnd;
import org.mitre.giscore.events.ContainerStart;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.geometry.Geometry;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.input.GISInputStreamBase;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.mitre.itf.geodesy.Latitude;
import org.mitre.itf.geodesy.Longitude;


/**
 * Check the base is
 * 
 * @author DRAND
 */
public class TestGISISBase {
	static class TestInputStream extends GISInputStreamBase {
		private int ptr = 0;
		private List<IGISObject> testobjects = new ArrayList<IGISObject>();
		
		public TestInputStream() {
			ContainerStart air = new ContainerStart("Folder");
			air.setName("air");
			testobjects.add(air);
			Feature f1 = new Feature();
			f1.setName("test1");
			f1.setDescription("desc1");
			f1.putData(new SimpleField("x"), "100");
			testobjects.add(f1);
			testobjects.add(new Point(new Geodetic2DPoint(new Longitude(.3),
					new Latitude(.5))));
			testobjects.add(new Point(new Geodetic2DPoint(new Longitude(.3),
					new Latitude(-.5))));
			testobjects.add(new ContainerEnd());
		}
		
		/* (non-Javadoc)
		 * @see org.mitre.giscore.input.GISInputStreamBase#read()
		 */
		public IGISObject read() {
			if (super.hasSaved())
				return super.readSaved();
			else if (ptr < testobjects.size()) { 
				IGISObject rval = testobjects.get(ptr++);
				save(rval);
				return rval;
			} else
				return null;
		}

		/* (non-Javadoc)
		 * @see org.mitre.giscore.input.IGISInputStream#close()
		 */
		public void close() {
			// Ignore
		}
		
	}
	
	@Test public void testInputStream() throws Exception {
		IGISInputStream tis = new TestInputStream();
		IGISObject rob = tis.read();
		Assert.assertNotNull(rob);
		ContainerStart ls = (ContainerStart) rob;
		Assert.assertEquals("air", ls.getName());
		tis.mark(10);
		
		rob = tis.read();
		Feature fs = (Feature) rob;
		Assert.assertEquals("test1", fs.getName());
		
		rob = tis.read();
		Geometry g = (Geometry) rob;
		Assert.assertEquals(1, g.getNumPoints());
		
		rob = tis.read();
		g = (Geometry) rob;
		Assert.assertEquals(1, g.getNumPoints());
		Assert.assertEquals(ContainerEnd.class, tis.read().getClass());
		
		tis.reset();
		rob = tis.read();
		fs = (Feature) rob;
		Assert.assertEquals("test1", fs.getName());
		
		rob = tis.read();
		g = (Geometry) rob;
		Assert.assertEquals(1, g.getNumPoints());
		
		rob = tis.read();
		g = (Geometry) rob;
		Assert.assertEquals(1, g.getNumPoints());
		Assert.assertEquals(ContainerEnd.class, tis.read().getClass());
		
		Assert.assertNull(tis.read());
	}
}
