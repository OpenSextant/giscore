package org.opensextant.giscore.filegdb;

import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

public class DatasetDefinition {

    private CoordinateReferenceSystem coordinateReferenceSystem = DefaultGeographicCRS.WGS84;
    private String definition;

    public DatasetDefinition(String definition) {
        if (null == definition) {
            return;
        }
        this.definition = definition;
        setCRS();
    }

    public CoordinateReferenceSystem getCRS() {
        return coordinateReferenceSystem;
    }

    private void setCRS() {
        try {
            String wkt = getNode("/*/SpatialReference/WKT");
            if (wkt != null) {
                coordinateReferenceSystem = CRS.parseWKT(wkt);
            }
        } catch (FactoryException e) {
            e.printStackTrace();
        }
    }


    private String getNode(String path)  {
        XPath xpath = XPathFactory.newInstance().newXPath();
        InputSource inputSource = new InputSource(new StringReader(definition));
        try {
            Node node = (Node) xpath.evaluate(path, inputSource, XPathConstants.NODE);
            if (node == null) {
                return null;
            }
            return node.getTextContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
