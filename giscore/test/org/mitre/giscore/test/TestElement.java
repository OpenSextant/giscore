package org.mitre.giscore.test;

import org.junit.Test;
import org.mitre.giscore.Namespace;
import org.mitre.giscore.events.Element;
import org.mitre.giscore.output.atom.IAtomConstants;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author MATHEWS
 * Date: 4/12/12 11:03 AM
 */
public class TestElement {

	@Test
	public void testElement() throws Exception {
		Namespace atomNs = Namespace.getNamespace("atom", IAtomConstants.ATOM_URI_NS);
		Element author = new Element(atomNs, "author");
		assertEquals(atomNs, author.getNamespace());
		Element name = new Element(atomNs, "name");
		name.setText("the Author");
		author.getChildren().add(name);
		assertEquals(1, author.getChildren().size());
	}

	@Test
	public void testElementNamespaces()  {
		Namespace ns = Namespace.getNamespace("opensearch", "http://a9.com/-/spec/opensearch/1.1/");
		Namespace geoNs = Namespace.getNamespace("geo", "http://a9.com/-/opensearch/extensions/geo/1.0/");
		Namespace geoNs2 = Namespace.getNamespace("geo", "http://www.opengis.net/gml/geo/1.0/");

		Element el = new Element(ns, "results");
		assertTrue(el.getNamespaces().isEmpty());

		assertFalse(el.addNamespace(ns));
		assertFalse(el.addNamespace(null));
		assertFalse(el.addNamespace(Namespace.NO_NAMESPACE));
		assertTrue(el.getNamespaces().isEmpty());

		// declare additional namespaces on the element
		assertTrue(el.addNamespace(geoNs));
		assertEquals(1, el.getNamespaces().size());

		// add an existing namespace - should not change the set
		assertTrue(el.addNamespace(geoNs));
		assertEquals(1, el.getNamespaces().size());

		// try to add different namespace with duplicate prefix
		assertFalse(el.addNamespace(geoNs2));
		assertEquals(1, el.getNamespaces().size());
	}

}
