package org.opensextant.giscore.test;

import org.junit.Test;
import org.opensextant.giscore.Namespace;
import org.opensextant.giscore.events.Element;
import org.opensextant.giscore.output.atom.IAtomConstants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author MATHEWS
 * Date: 4/12/12 11:03 AM
 */
public class TestElement {

	final static Namespace atomNs = Namespace.getNamespace("atom", IAtomConstants.ATOM_URI_NS);

	@Test
	public void testElement() throws Exception {
		Element author = new Element(atomNs, "author");
		assertEquals(atomNs, author.getNamespace());
		Element name = new Element(atomNs, "name");
		name.setText("the Author");
		author.getChildren().add(name);
		assertEquals(1, author.getChildren().size());
	}

	@Test
	public void testEquals() {
		Element e = new Element();
		e.setName("result");
		Element child = new Element();
		child.setName("child");
		e.getChildren().add(child);
		Element child2 = new Element();
		child2.setName("c");
		child2.setText("hello");
		child2.getAttributes().put("name", "value");
		child2.getAttributes().put("foo", "bar");
		e.getChildren().add(child2);

		Element e2 = new Element(Namespace.NO_NAMESPACE, "result");
		e2.getChildren().add(new Element(Namespace.NO_NAMESPACE, "child"));
		assertFalse(e.equals(e2));
		e2.getChildren().add(child2);

		assertEquals(e, e2);
		assertEquals(e.hashCode(), e2.hashCode());
	}

	@Test
	public void testNotEquals() {
		Element e1 = new Element(atomNs, "author");
		Element e2 = new Element(atomNs, "link");
		assertFalse(e1.equals(e2));
		assertFalse(e2.equals(e1));

		e1.setName("link");
		assertEquals(e1, e2);
		e1.getAttributes().put("name", "value");
		assertFalse(e1.equals(e2));

		assertFalse(e1.equals(""));
		assertFalse(e1.equals(null));

		e1 = new Element(atomNs, "author");
		e1.setText("text1");
		e2 = new Element(atomNs, "author");
		e2.setText("text2");
		assertFalse(e1.equals(e2));
	}

	@Test
	public void testGetChild() {
		Element e = new Element();
		e.setName("result");
		Element child = new Element();
		child.setName("child");
		e.getChildren().add(child);

		assertEquals(child, e.getChild("child"));
		assertEquals(child, e.getChild("child", Namespace.NO_NAMESPACE));

		assertNull(e.getChild(null));
		assertNull(e.getChild("notfound"));
	}

	@Test
	public void testElementNamespaces()  {
		Namespace ns = Namespace.getNamespace("opensearch", "http://a9.com/-/spec/opensearch/1.1/");
		Namespace geoNs = Namespace.getNamespace("geo", "http://a9.com/-/opensearch/extensions/geo/1.0/");
		Namespace geoNs2 = Namespace.getNamespace("geo", "http://www.opengis.net/gml/geo/1.0/");

		Element el = new Element(ns, "results");
		assertTrue(el.getNamespaces().isEmpty());

		assertTrue(el.addNamespace(ns));
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
