package org.mitre.giscore.test.input;

import org.junit.Test;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.input.kml.KmlWriter;
import org.mitre.giscore.events.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.net.URI;

import junit.framework.TestCase;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: Mar 17, 2009 3:23:00 PM
 */
public class TestKmlReader extends TestCase {

    @Test
    public void test_read_write_Kml() {
        checkDir(new File("data/kml")); // few errors
    }

    private void checkDir(File dir) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) checkDir(file);
            else {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".kml") || name.endsWith(".kmz"))
                    try {
                        checkKmlFile(file);
                    } catch (IOException e) {
                        System.out.println("Failed to read/write: " + file + " " + e);
                    }
            }
        }
    }

    private void checkKmlFile(File file) throws IOException {
        System.out.println("Testing " + file);
        KmlReader reader = new KmlReader(file);
        List<IGISObject> objs = reader.getFeatures();
        List<IGISObject> linkedFeatures = new ArrayList<IGISObject>();
        List<URI> links = reader.importFromNetworkLinks(linkedFeatures);
        if (links.size() != 0)
            assertTrue(linkedFeatures.size() != 0);
		File temp = File.createTempFile("test", reader.isCompressed() ? ".kmz" : ".kml");
		//File temp = new File("test." + (reader.isCompressed() ? "kmz" : "kml"));
		try {
			System.out.println(">create " + temp);
			KmlWriter writer = new KmlWriter(temp);
			for (IGISObject o : objs) {
				writer.write(o);
			}
			writer.close();
			// Filter original list such that it will match the re-imported list
			List<IGISObject> objs2 = new ArrayList<IGISObject>();
			for (int i = 0; i < objs.size(); i++) {
				IGISObject o = objs.get(i);
				// KmlReader may introduce Comment Objects for skipped elements
				// so need to remove these since reading them back in will not preserve them
				if (o instanceof Comment) continue;
				// KmlWriter ignores any empty containers so any ContainerStart
				// followed by a ContainerEnd will be discarded.
				// need to remove any of these from the list from which
				// to compare to original list.
				if (o instanceof ContainerStart && i + 1 < objs.size()) {
					IGISObject next = objs.get(i + 1);
					if (next instanceof ContainerEnd) {
						if (i > 0) {
							IGISObject prev = objs.get(i - 1);
							// ignore unless previous elements are Style and StyleMaps
							// which are added to an empty container...
							if (prev instanceof Style || prev instanceof StyleMap) {
								objs2.add(o);
								continue;
							}
						}
						i++; // skip current and next items
						continue;
					}
				}
				objs2.add(o);
			}
			objs = objs2;
			reader = new KmlReader(temp);
			List<IGISObject> elements = reader.getFeatures();
			/*
			if (objs.size() != elements.size()) {
					for(Object o : objs) {
						System.out.println(" >" + o.getClass().getName());
					}
					System.out.println();
					for(Object o : elements) {
						System.out.println(" <" + o.getClass().getName());
					}
					//System.out.println("\nelts1=" + elements);
					//System.out.println("\nelts2=" + elements2);
					//System.out.println();
			}
			*/
			assertEquals(objs.size(), elements.size());
		} finally {
			// delete temp file
			if (temp != null && temp.exists()) temp.delete();
		}
	}
}
