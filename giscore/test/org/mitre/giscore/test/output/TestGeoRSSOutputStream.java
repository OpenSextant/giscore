package org.mitre.giscore.test.output;

import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.Namespace;
import org.mitre.giscore.test.TestGISBase;
import org.mitre.giscore.geometry.*;
import org.mitre.giscore.utils.SimpleObjectOutputStream;
import org.mitre.giscore.events.IGISObject;
import org.mitre.giscore.events.Feature;
import org.mitre.giscore.events.SimpleField;
import org.mitre.giscore.output.rss.GeoRSSOutputStream;
import org.mitre.giscore.output.rss.IRss;
import org.mitre.giscore.output.XmlOutputStreamBase;
import org.mitre.itf.geodesy.FrameOfReference;
import org.mitre.itf.geodesy.Geodetic2DPoint;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.*;

/**
 * @author Jason Mathews, MITRE Corp.
 * Date: Jun 6, 2009 7:43:55 PM
 */
public class TestGeoRSSOutputStream extends TestGISBase implements IRss {

    /**
     * Simple test to output GeoRSS
     * @throws IOException
     * @throws XMLStreamException
     */
    @Test
    public void testGeoRss() throws IOException, XMLStreamException {
        exportRss(new File("data/kml/Placemark/placemarks.kml"));
        exportRss(new File("data/kml/MultiGeometry/testLayers.kml"));
    }

    private void exportRss(File file) throws IOException, XMLStreamException {
        Namespace NS = Namespace.getNamespace("ext", "http://giscore.mitre.org/ext");
        IGISInputStream kis = GISFactory.getInputStream(DocumentType.KML, new FileInputStream(file));
        String name = file.getName();
        int ind = name.lastIndexOf('.');
        if (ind > 0) name = name.substring(0,ind) + ".xml";
        else name += ".xml";
        File out = new File("testOutput/" + name);

        // create mappings of non-rss element names to explicit namespaces
        Map<String, Namespace> namespaceMap = new HashMap<String, Namespace>(1);
        namespaceMap.put("test", NS);

        Map<String,Object> channelMap = new HashMap<String,Object>();
        channelMap.put(TITLE, "Test GeoRSS Feed");
        channelMap.put(LINK, "http://giscore.mitre.org");
        channelMap.put(DESCRIPTION, "this is a test");
        channelMap.put("test", "this is an extended element"); // uses "ext:" namespace prefix in output

        GeoRSSOutputStream os = new GeoRSSOutputStream(new FileOutputStream(out), XmlOutputStreamBase.ISO_8859_1,
                namespaceMap, channelMap);
        try {
            IGISObject current;
            while ((current = kis.read()) != null) {
                if (current instanceof Feature) {
                    Feature f = (Feature)current;
                    Date date = new Date();
                    f.putData(new SimpleField("test"), Long.toHexString(date.getTime()));
                    f.putData(new SimpleField(PUB_DATE), date); // or use start/end times ??
                    //System.out.println(f);
                }
                os.write(current);
            }
        } finally {
            kis.close();
        }
        try {
            os.close();
        } catch (IOException e) {
        }
    }

    @Test
	public void testMultiGeometries() throws Exception {
        File out = new File("testOutput/testMultiGeometries-rss.xml");
        GeoRSSOutputStream os = new GeoRSSOutputStream(new FileOutputStream(out),
                XmlOutputStreamBase.ISO_8859_1, null, null);
        List<Feature> feats = getMultiGeometries();
        for (Feature f : feats) {
            os.write(f);
        }
        os.close();
    }

}
