package org.mitre.giscore.test.utils;

import junit.framework.TestCase;
import org.mitre.giscore.input.kml.UrlRef;

/**
 * User: MATHEWS
 * Date: Oct 28, 2010 5:19:13 PM
 */
public class TestUrlRef extends TestCase {

    public void testEscapeUri() {
        String uri = "#foo%20";
        assertTrue(uri.equals(UrlRef.escapeUri(uri)));
        
        String[] uris = {
                "foo?x=<B>{String} to [encode]</B>",
                "http://localhost/foo?x=|^\\"
        };
        for(String id : uris) {
            assertFalse(id.equals(UrlRef.escapeUri(id)));
        }
    }

    public void testIsIdentifier() {
        
        String[]ids = { "X509Data", "abc-ABC_12.34", "id%20",
                "_\u00B7\u3005\u30FE",  // XMLExtender
                "_\u309A", // XMLCombiningChar
                "_\uD7A3",  // XMLLetter
                "_\u0ED9"   // XMLDigit
        };
        /*
         * valid identifier follows NCName production in [Namespaces in XML]:
         *  NCName ::=  (Letter | '_') (NCNameChar)*  -- An XML Name, minus the ":"
         *  NCNameChar ::=  Letter | Digit | '.' | '-' | '_' | CombiningChar | Extender
         * 
         * Also allowing the URI escaping mechanism %HH,
         * where HH is the hexadecimal notation of a byte value.  
         */
        for(String id : ids) {
            assertTrue(UrlRef.isIdentifier(id));
        }
        
        String[] badIds = {
            null, "", " ", "124_Must start with alpha",
            "_\uABFF",
            "bad id", "x<bad>", "bad:id", // contains invalid characters
            "bad%zz", "bad%" // '%' must precede two hexadecimal digits otherwise must be escaped
        };
        for(String id : badIds) {
            assertFalse(UrlRef.isIdentifier(id));
        }
    }
}