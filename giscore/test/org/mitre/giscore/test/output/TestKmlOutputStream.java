/****************************************************************************************
 *  TestKmlOutputStream.java
 *
 *  Created: Feb 4, 2009
 *
 *  @author DRAND
 *
 *  (C) Copyright MITRE Corporation 2009
 *
 *  The program is provided "as is" without any warranty express or implied, including
 *  the warranty of non-infringement and the implied warranties of merchantability and
 *  fitness for a particular purpose.  The Copyright owner will not be liable for any
 *  damages suffered by you as a result of using the Program.  In no event will the
 *  Copyright owner be liable for any special, indirect or consequential damages or
 *  lost profits even if the Copyright owner has been advised of the possibility of
 *  their occurrence.
 *
 ***************************************************************************************/
package org.mitre.giscore.test.output;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mitre.giscore.DocumentType;
import org.mitre.giscore.GISFactory;
import org.mitre.giscore.Namespace;
import org.mitre.giscore.events.*;
import org.mitre.giscore.geometry.*;
import org.mitre.giscore.input.IGISInputStream;
import org.mitre.giscore.input.XmlInputStream;
import org.mitre.giscore.input.kml.IKml;
import org.mitre.giscore.input.kml.KmlReader;
import org.mitre.giscore.input.kml.KmlInputStream;
import org.mitre.giscore.output.IGISOutputStream;
import org.mitre.giscore.output.XmlOutputStreamBase;
import org.mitre.giscore.output.atom.IAtomConstants;
import org.mitre.giscore.output.kml.KmlOutputStream;
import org.mitre.giscore.test.TestGISBase;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;

/**
 * Test the KML output stream.
 *
 * @author DRAND
 * @author Mathews
 */
public class TestKmlOutputStream extends TestGISBase {

    private boolean autoDelete = !Boolean.getBoolean("keepTempFiles");

    @Test
    public void testSimpleCase() throws IOException {
        doTest(getStream("7084.kml"));
    }

    @Test
    public void testElement() throws IOException, XMLStreamException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        KmlOutputStream kos = new KmlOutputStream(bos, XmlOutputStreamBase.ISO_8859_1);
        try {
            DocumentStart ds = new DocumentStart(DocumentType.KML);
            Namespace gxNs = Namespace.getNamespace("gx", IKml.NS_GOOGLE_KML_EXT);
            Namespace atomNs = Namespace.getNamespace("atom", IAtomConstants.ATOM_URI_NS);
            assertTrue(ds.addNamespace(gxNs));
            assertTrue(ds.addNamespace(gxNs)); // already in list
            ds.addNamespace(atomNs);
            assertEquals(2, ds.getNamespaces().size());
            kos.write(ds);
            Feature f = new Feature();
            f.setName("gx:atom:test");
            f.setDescription("this is a test placemark");
            /*
                <atom:author>
                    <atom:name>the Author</atom:name>
                 </atom:author>
                 <atom:link href="http://tools.ietf.org/html/rfc4287" />
                */
            List<Element> elements = new ArrayList<Element>(2);
            Element author = new Element(atomNs, "author");
            Element name = new Element(atomNs, "name");
            name.setText("the Author");
            author.getChildren().add(name);
            elements.add(author);
            Element link = new Element(atomNs, "link");
            link.getAttributes().put("href", "http://tools.ietf.org/html/rfc4287");
            elements.add(link);
            f.setElements(elements);
            Point point = new Point(12.233, 146.825);
            point.setAltitudeMode("clampToSeaFloor");
            f.setGeometry(point);
            kos.write(f);
            kos.close();
            kos = null;

            // System.out.println(bos.toString());

            KmlInputStream kis = new KmlInputStream(new ByteArrayInputStream(bos.toByteArray()));
            IGISObject o = kis.read();
            assertTrue(o instanceof DocumentStart);
            List<Namespace> namespaces = ((DocumentStart) o).getNamespaces();
            assertTrue(namespaces.contains(atomNs));
            assertTrue(namespaces.contains(gxNs));
            o = kis.read();
            assertTrue(o instanceof Feature);
            Feature f2 = (Feature) o;
            List<Element> elts = f2.getElements();
            assertTrue(elts != null && elts.size() == 2);
            checkApproximatelyEquals(f, f2);
            Element e = elts.get(0);
            assertNotNull(e.getNamespaceURI());
            assertEquals(atomNs, e.getNamespace());
            assertNotNull(e.getChildren());
            Element child = e.getChild("name", atomNs);
            assertNotNull(child);
            assertNotNull(e.getChild("name"));
            Point pt = (Point) f2.getGeometry();
            assertEquals(AltitudeModeEnumType.clampToSeaFloor, pt.getAltitudeMode());
            kis.close();
        } catch (AssertionError ae) {
            System.out.println("Failed with KML content:\n" + bos.toString("UTF-8"));
            throw ae;
        } finally {
            if (kos != null)
                kos.close();
        }
        // System.out.println(kml);
        //Assert.assertTrue(kml.contains("this is a test placemark"));
        //Assert.assertTrue(kml.contains(IKml.NS_GOOGLE_KML_EXT));
    }

    @Test
    public void testGxElementNsDeclared() throws IOException, XMLStreamException {
        outputGxElement(true);
    }

    @Test
    public void testGxElementNsUndeclared() throws IOException, XMLStreamException {
        outputGxElement(false);
    }

    private void outputGxElement(boolean declareNamespace) throws IOException, XMLStreamException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        KmlOutputStream kos = new KmlOutputStream(bos);
        if (declareNamespace) {
            DocumentStart ds = new DocumentStart(DocumentType.KML);
            Namespace gxNs = Namespace.getNamespace("gx", IKml.NS_GOOGLE_KML_EXT);
            ds.getNamespaces().add(gxNs);
            kos.write(ds);
        }
        Feature f = new Feature();
        TaggedMap lookAt = new TaggedMap("LookAt");
        if (declareNamespace) {
            // gx:TimeSpan is a complex element not a simple element
            lookAt.put("gx:TimeSpan/begin", "2011-03-11T01:00:24.012Z");
            lookAt.put("gx:TimeSpan/end", "2011-03-11T05:46:24.012Z");
        } else {
            lookAt.put("gx:TimeStamp", "2011-03-11T05:46:24.012Z");
        }
        lookAt.put("longitude", "143.1066665234362");
        lookAt.put("latitude", "37.1565775502346");
        f.setViewGroup(lookAt);
        Point cp = new Point(random3dGeoPoint());
        cp.setAltitudeMode(AltitudeModeEnumType.clampToSeaFloor);
        f.setGeometry(cp);
        kos.write(f);
        kos.close();
        KmlInputStream kis = new KmlInputStream(new ByteArrayInputStream(bos.toByteArray()));
        try {
            assertNotNull(kis.read()); // skip DocumentStart
            IGISObject obj = kis.read(); // Placemark
            kis.close();
            assert (obj instanceof Feature);
            checkApproximatelyEquals(f, obj);
            System.out.println(bos.toString("UTF-8")); // debug
        } catch (AssertionError ae) {
            System.out.println("Failed with KML content:\n" + bos.toString("UTF-8"));
            throw ae;
        }
    }

    @Test
    public void testRowData() throws XMLStreamException, IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        KmlOutputStream kos = new KmlOutputStream(bos);
        DocumentStart ds = new DocumentStart(DocumentType.KML);
        kos.write(ds);
        SimpleField date = new SimpleField("date", SimpleField.Type.DATE);
        SimpleField name = new SimpleField("name");
        kos.write(new ContainerStart(IKml.FOLDER));
        Row row = new Row();
        row.putData(name, "hello");
        row.putData(date, new Date());
        kos.write(row);
        row = new Row();
        row.putData(name, "world");
        row.putData(date, new Date());
        kos.write(row);
        kos.write(new ContainerEnd());
        kos.close();
        String kml = bos.toString("UTF-8");
        // System.out.println(kml);
        Assert.assertTrue(kml.contains(IKml.EXTENDED_DATA));
        Assert.assertTrue(kml.contains("hello"));
    }

    /**
     * Note, this test fails due to some sort of issue with geodesy, but the
     * actual output kml is fine.
     *
     * @throws IOException
     */
    @Test
    public void testCase2() throws IOException {
        doTest(getStream("KML_sample1.kml"));
    }

    @Test
    public void testCase3() throws IOException {
        doTest(getStream("schema_example.kml"));
    }

    @Test
    public void testRingOutput() throws IOException {
        File file = createTemp("testRings", ".kml");
        OutputStream fs = null;
        try {
            fs = new FileOutputStream(file);
            IGISOutputStream os = GISFactory.getOutputStream(DocumentType.KML, fs);
            os.write(new DocumentStart(DocumentType.KML));
            os.write(new ContainerStart(IKml.FOLDER));
            //Feature firstFeature = null;
            for (int i = 0; i < 5; i++) {
                Point cp = getRandomPoint();
                Feature f = new Feature();
                ContainerStart cs = new ContainerStart(IKml.DOCUMENT);
                cs.setName(Integer.toString(i));
                os.write(cs);
                //if (firstFeature == null) firstFeature = f;
                List<Point> pts = new ArrayList<Point>();
                pts.add(getRingPoint(cp, 4, 5, .3, .4));
                pts.add(getRingPoint(cp, 3, 5, .3, .4));
                pts.add(getRingPoint(cp, 2, 5, .3, .4));
                pts.add(getRingPoint(cp, 1, 5, .3, .4));
                pts.add(getRingPoint(cp, 0, 5, .3, .4));
                pts.add(pts.get(0));
                LinearRing ring = new LinearRing(pts, true);
                if (!ring.clockwise()) System.err.println("rings should be in clockwise point order");
                f.setGeometry(ring);
                os.write(f);
                // first and last points same: don't need to output it twice
                final int npoints = pts.size() - 1;
                for (int k = 0; k < npoints; k++) {
                    Point pt = pts.get(k);
                    f = new Feature();
                    f.setName(Integer.toString(k));
                    f.setGeometry(pt);
                    os.write(f);
                }
                os.write(new ContainerEnd());
            }
            os.close();

            /*
            KmlReader reader = new KmlReader(file);
            List<IGISObject> objs = reader.readAll();
            // imported features should be DocumentStart, Container, followed by Features
            assertEquals(8, objs.size());
            checkApproximatelyEquals(firstFeature, objs.get(2));
            */
        } finally {
            IOUtils.closeQuietly(fs);
            if (autoDelete && file.exists()) file.delete();
        }
    }

    @Test
    public void testPolyOutput() throws IOException {
        File file = createTemp("testPolys", ".kml");
        OutputStream fs = null;
        try {
            fs = new FileOutputStream(file);
            IGISOutputStream os = GISFactory.getOutputStream(DocumentType.KML, fs);
            os.write(new DocumentStart(DocumentType.KML));
            os.write(new ContainerStart(IKml.DOCUMENT));
            Schema schema = new Schema();
            SimpleField id = new SimpleField("testid");
            id.setLength(10);
            schema.put(id);
            SimpleField date = new SimpleField("today", SimpleField.Type.STRING);
            schema.put(date);
            os.write(schema);
            Feature firstFeature = null;
            for (int i = 0; i < 5; i++) {
                Point cp = getRandomPoint(25.0); // Center of outer poly
                Feature f = new Feature();
                if (firstFeature == null) firstFeature = f;
                f.putData(id, "id " + i);
                f.putData(date, new Date().toString());
                f.setSchema(schema.getId());
                List<Point> pts = new ArrayList<Point>();
                for (int k = 0; k < 5; k++) {
                    pts.add(getRingPoint(cp, k, 5, 1.0, 2.0));
                }
                pts.add(pts.get(0)); // should start and end with the same point
                LinearRing outerRing = new LinearRing(pts);
                List<LinearRing> innerRings = new ArrayList<LinearRing>();
                for (int j = 0; j < 4; j++) {
                    pts = new ArrayList<Point>();
                    Point ircp = getRingPoint(cp, j, 4, .5, 1.0);
                    for (int k = 0; k < 5; k++) {
                        pts.add(getRingPoint(ircp, k, 5, .24, .2));
                    }
                    pts.add(pts.get(0));
                    innerRings.add(new LinearRing(pts));
                }
                Polygon p = new Polygon(outerRing, innerRings);
                f.setGeometry(p);
                os.write(f);
            }
            os.close();

            KmlReader reader = new KmlReader(file);
            List<IGISObject> objs = reader.readAll(); // implicit close
            // imported features should be DocumentStart, Container, Schema, followed by Features
            assertEquals(9, objs.size());
            checkApproximatelyEquals(firstFeature, objs.get(3));
        } finally {
            IOUtils.closeQuietly(fs);
            if (autoDelete && file.exists()) file.delete();
        }
    }

    @Test
    public void testCircleOutput() throws XMLStreamException, IOException {
        Point pt = getRandomPoint();
        Circle c = new Circle(pt.getCenter(), 1000.0);
        c.setTessellate(true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        KmlOutputStream kos = new KmlOutputStream(bos);
        kos.setNumberCirclePoints(32);
        final int pointCount = kos.getNumberCirclePoints() + 1;
        kos.write(new ContainerStart(IKml.DOCUMENT));
        try {
            Feature f = new Feature();
            f.setName("P1");
            f.setDescription("this is a test placemark");
            // circle Hint = polygon (default)
            f.setGeometry(c);
            kos.write(f);
            f.setName("P2");
            c.setHint(Circle.HintType.LINE);
            kos.write(f);

            kos.setNumberCirclePoints(2);
            f.setName("P3");
            kos.write(f);

            kos.setNumberCirclePoints(1);
            f.setName("P4");
            kos.write(f);

            kos.write(new ContainerEnd());
        } finally {
            kos.close();
        }

        try {
            KmlInputStream kis = new KmlInputStream(new ByteArrayInputStream(bos.toByteArray()));
            assertNotNull(kis.read()); // skip DocumentStart
            assertNotNull(kis.read()); // skip Document
            IGISObject obj1 = kis.read(); // Placemark w/Circle as Polygon (n=32)
            IGISObject obj2 = kis.read(); // Placemark w/Circle as LineString (n=32)
            IGISObject obj3 = kis.read(); // Placemark w/Circle as LineString (n=2)
            IGISObject obj4 = kis.read(); // Placemark w/Circle as Point (n=1)
            kis.close();
            assert (obj1 instanceof Feature);
            Feature f = (Feature) obj1;
            Geometry geom = f.getGeometry();
            // by default the KmlOutputStream converts Circle into Polygon with 33 points
            assertTrue(geom instanceof Polygon);
            Polygon poly = (Polygon) geom;
            assertTrue(poly.getTessellate());
            assertEquals(pointCount, poly.getNumPoints());

            assert (obj2 instanceof Feature);
            f = (Feature) obj2;
            geom = f.getGeometry();
            assertTrue(geom instanceof Line);
            Line line = (Line) geom;
            assertTrue(line.getTessellate());
            assertEquals(pointCount, line.getNumPoints());

            assert (obj3 instanceof Feature);
            f = (Feature) obj3;
            geom = f.getGeometry();
            assertTrue(geom instanceof Line);
            assertEquals(2, geom.getNumPoints());

            assert (obj4 instanceof Feature);
            f = (Feature) obj4;
            geom = f.getGeometry();
            assertTrue(geom instanceof Point);
            assertEquals(1, geom.getNumPoints());

        } catch (AssertionError ae) {
            System.out.println("Failed with KML content:\n" + bos.toString("UTF-8"));
            throw ae;
        }
    }

    @Test
    public void testKmz() throws IOException, XMLStreamException {
        File file = createTemp("test", ".kmz");
        ZipOutputStream zoS = null;
        try {
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream boS = new BufferedOutputStream(os);
            // Create the doc.kml file inside of a zip entry
            zoS = new ZipOutputStream(boS);
            ZipEntry zEnt = new ZipEntry("doc.kml");
            zoS.putNextEntry(zEnt);
            KmlOutputStream kos = new KmlOutputStream(zoS);
            kos.write(new DocumentStart(DocumentType.KML));
            Feature f = new Feature();
            f.setGeometry(new Point(42.504733587704, -71.238861602674));
            f.setName("test");
            f.setDescription("this is a test placemark");
            kos.write(f);
            try {
                kos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            IOUtils.closeQuietly(zoS);
            zoS = null;
            KmlReader reader = new KmlReader(file);
            List<IGISObject> objs = reader.readAll(); // implicit close
            // imported features should be DocumentStart followed by Feature 
            assertEquals(2, objs.size());
            checkApproximatelyEquals(f, objs.get(1));
        } finally {
            IOUtils.closeQuietly(zoS);
            if (autoDelete && file.exists()) file.delete();
        }
    }

    @Test
    public void testRegion() throws XMLStreamException, IOException {
        // test all variations of Lod and LatLonAltBox combinations
        // test Region with LatLonAltBox only
        doTestRegion(new String[]{
                IKml.NORTH, "45",
                IKml.SOUTH, "35",
                IKml.EAST, "1",
                IKml.WEST, "10",
        });
        // test Region with LatLonAltBox + Lod
        doTestRegion(new String[]{
                IKml.NORTH, "45",
                IKml.SOUTH, "35",
                IKml.EAST, "1",
                IKml.WEST, "10",
                IKml.MIN_LOD_PIXELS, "256",
                IKml.MAX_LOD_PIXELS, "-1"
        });
        // test Region with Lod only
        doTestRegion(new String[]{
                IKml.MIN_LOD_PIXELS, "256",
                IKml.MAX_LOD_PIXELS, "-1"
        });
    }

    private void doTestRegion(String[] props) throws XMLStreamException, IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        KmlOutputStream kos = new KmlOutputStream(bos);
        kos.write(new DocumentStart(DocumentType.KML));
        Feature f = new Feature();
        f.setGeometry(new Point(42.504733587704, -71.238861602674));
        f.setName("test");
        f.setDescription("this is a test placemark");
        TaggedMap region = new TaggedMap(IKml.LAT_LON_ALT_BOX);
        for (int i = 0; i < props.length; i += 2) {
            region.put(props[i], props[i + 1]);
        }
        f.setRegion(region);
        kos.write(f);
        kos.close();
        XmlInputStream kis = new KmlInputStream(new ByteArrayInputStream(bos.toByteArray()));
        IGISObject o;
        while ((o = kis.read()) != null) {
            if (o instanceof Feature) {
                Feature f2 = (Feature) o;
                assertEquals(region, f2.getRegion());
                /*
                    if(!(region.equals(f2.getRegion()))) {
                        System.out.println("== region mismatch ==");
                        System.out.println("1:" + region);
                        System.out.println("2:" + f2.getRegion());
                        System.out.println(new String(bos.toByteArray()));
                        System.out.println();
                    }
                    */
            }
        }
        kis.close();
    }

    public void doTest(InputStream fs) throws IOException {
        File temp = null;
        try {
            IGISInputStream is = GISFactory.getInputStream(DocumentType.KML, fs);
            temp = createTemp("test", ".kml");
            OutputStream fos = new FileOutputStream(temp);
            IGISOutputStream os = GISFactory.getOutputStream(DocumentType.KML, fos);
            List<IGISObject> elements = new ArrayList<IGISObject>();
            IGISObject current;
            while ((current = is.read()) != null) {
                os.write(current);
                elements.add(current);
            }

            is.close();
            fs.close();

            os.close();
            fos.close();

            // Test for equivalence
            fs = new FileInputStream(temp);
            is = GISFactory.getInputStream(DocumentType.KML, fs);
            int index = 0;
            while ((current = is.read()) != null) {
                checkApproximatelyEquals(elements.get(index++), current);
            }
            is.close();
        } finally {
            IOUtils.closeQuietly(fs);
            if (temp != null && autoDelete && temp.exists()) temp.delete();
        }
    }

    /**
     * For most objects they need to be exactly the same, but for some we can
     * approximate equality
     *
     * @param source expected feature object
     * @param test   actual feature object
     */
    public static void checkApproximatelyEquals(IGISObject source, IGISObject test) {
        if (source instanceof Feature && test instanceof Feature) {
            Feature sf = (Feature) source;
            Feature tf = (Feature) test;

            boolean ae = sf.approximatelyEquals(tf);

            if (!ae) {
                System.out.println("Expected: " + source);
                System.out.println("Actual: " + test);
                fail("Found unequal objects");
            }
        } else {
            assertEquals(source, test);
        }
    }

    private InputStream getStream(String filename) throws FileNotFoundException {
        System.out.println("Test " + filename);
        File file = new File("test/org/mitre/giscore/test/input/" + filename);
        if (file.exists()) return new FileInputStream(file);
        System.out.println("File does not exist: " + file);
        return getClass().getResourceAsStream(filename);
    }

    @Test
    public void testMultiGeometries() throws IOException, XMLStreamException {
        File out = new File("testOutput/testMultiGeometries.kml");
        KmlOutputStream os = new KmlOutputStream(new FileOutputStream(out),
                XmlOutputStreamBase.ISO_8859_1);
        List<Feature> feats;
        try {
            os.write(new DocumentStart(DocumentType.KML));
            os.write(new ContainerStart(IKml.DOCUMENT));
            feats = getMultiGeometries();
            for (Feature f : feats) {
                os.write(f);
            }
        } finally {
            os.close();
        }

        KmlInputStream kis = new KmlInputStream(new FileInputStream(out));
        assertNotNull(kis.read()); // skip DocumentStart
        assertNotNull(kis.read()); // skip Document
        for (Feature expected : feats) {
            IGISObject current = kis.read();
            assertTrue(current instanceof Feature);
            // Note: GeometryBag with Multiple Points converted to MultiPoint geometry on reading
            // but number of points must be the same
            Geometry geom = expected.getGeometry();
            // System.out.format("%n%s %d %d%n", expected.getName(), geom.getNumPoints(), ((Feature)current).getGeometry().getNumPoints());
            // Note: circles are written as Polygons, Line, or LinearRings depending on the hint preference so number of points is *NOT* the same
            // and GeometryBags of only multiple points are converted to single MultiPoint Geometry so geometries are *NOT* the same
            boolean testFeature = true;
            if (geom instanceof GeometryBag) {
                int pointCount = 0;
                for (Geometry g : (GeometryBag) geom) {
                    // System.out.println("XXX: " + g.getClass().getName());
                    if (g instanceof Circle) {
                        // System.out.println("XXX: skip circle");
                        testFeature = false;
                        break;
                    }
                    if (g.getClass() == Point.class) pointCount++;
                }
                if (pointCount == geom.getNumParts()) {
                    // GeometryBags of multiple points are converted to single MultiPoint Geometry so geometries are *NOT* the same
                    // and cannot be compared using checkApproximatelyEquals()
                    // System.out.println("XXX: skip multiPoints");
                    assertEquals(pointCount, ((Feature) current).getGeometry().getNumPoints());
                    testFeature = false;
                }
            } // else System.out.println("other: " + geom.getClass().getName()); // e.g. MultiLine, MultiPoint, etc.
            if (testFeature) checkApproximatelyEquals(expected, current);
        }
        kis.close();
    }

    @Test
    public void testWrapper() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        KmlOutputStream kos = new KmlOutputStream(bos);

        Feature f = new Feature();
        f.setName("test place");
        f.setDescription("POI");
        // f.setGeometry(getRandomPoint());
        WrappedObject obj = new WrappedObject(f);
        kos.write(obj);

        Comment comment = new Comment("This is a comment");
        kos.write(comment);

        kos.close();
        // System.out.println(bos);
        final String content = bos.toString();
        assertTrue(content.contains("<!-- This is a comment -->"));
        assertTrue(content.contains("<!-- [WrappedObject: "));
    }

}
