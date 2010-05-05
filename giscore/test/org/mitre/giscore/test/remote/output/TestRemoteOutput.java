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
package org.mitre.giscore.test.remote.output;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.geometry.Point;
import org.mitre.giscore.output.IGISOutputStream;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/org/mitre/giscore/test/remote/output/remote-output.xml" })
public class TestRemoteOutput {
	@Test public void testRemote() throws Exception {
		File test = File.createTempFile("test", ".zip");
		FileOutputStream fos = new FileOutputStream(test);
		
		IGISOutputStream stream = GISFactory.getClientOutputStream(DocumentType.FileGDB, fos);
		for(int i = 0; i < 100; i++) {
			Point p = new Point(RandomUtils.nextDouble() * 20.0, RandomUtils.nextDouble() * 30.0);
			stream.write(p);
		}
		stream.close();
		
		System.out.println("File is " + test.getAbsolutePath());
	}
}
