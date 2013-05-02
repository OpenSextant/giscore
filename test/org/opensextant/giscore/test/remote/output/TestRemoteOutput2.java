/****************************************************************************************
 *  TestRemoteOutput.java
 *
 *  Created: May 3, 2010
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2010
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
package org.opensextant.giscore.test.remote.output;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opensextant.giscore.DocumentType;
import org.opensextant.giscore.GISFactory;
import org.opensextant.giscore.events.ContainerEnd;
import org.opensextant.giscore.events.ContainerStart;
import org.opensextant.giscore.events.Feature;
import org.opensextant.giscore.geometry.Point;
import org.opensextant.giscore.output.IGISOutputStream;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Config file for this case goes to an actual remote server
 * @author DRAND
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/org/mitre/giscore/test/remote/output/remote-output2.xml" })
public class TestRemoteOutput2 {
	@Test public void testRemote() throws Exception {
		File test = File.createTempFile("test", ".zip");
		FileOutputStream fos = new FileOutputStream(test);
		
		IGISOutputStream stream = GISFactory.getClientOutputStream(DocumentType.FileGDB, fos);
		ContainerStart c = new ContainerStart("A_POINTS");
		stream.write(c);
		for(int i = 0; i < 100; i++) {
			Feature f = new Feature();
			Point p = new Point(RandomUtils.nextDouble() * 20.0, RandomUtils.nextDouble() * 30.0);
			f.setGeometry(p);
			stream.write(f);
		}
		stream.write(new ContainerEnd());
		stream.close();
		
		System.out.println("File is " + test.getAbsolutePath());
	}
	
	@Test public void testRemote2() throws Exception {
		File test = File.createTempFile("test", ".zip");
		FileOutputStream fos = new FileOutputStream(test);
		
		IGISOutputStream stream = GISFactory.getClientOutputStream(DocumentType.FileGDB, fos);
		ContainerStart c = new ContainerStart("A_POINTS");
		stream.write(c);
		for(int i = 0; i < 100000; i++) {
			Feature f = new Feature();
			Point p = new Point(RandomUtils.nextDouble() * 20.0, RandomUtils.nextDouble() * 30.0);
			f.setGeometry(p);
			stream.write(f);
		}
		stream.write(new ContainerEnd());
		stream.close();
		
		System.out.println("File is " + test.getAbsolutePath());
	}
	
	
}
