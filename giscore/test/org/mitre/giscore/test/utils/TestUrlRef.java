package org.mitre.giscore.test.utils;

import junit.framework.TestCase;
import org.mitre.giscore.input.kml.UrlRef;

/**
 * User: MATHEWS
 * Date: Oct 28, 2010 5:19:13 PM
 */
public class TestUrlRef extends TestCase {

    public void testIsIdentifier() {
        
        String[]ids = { "X509Data", "abc-ABC_12.34", "id%20" };
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
            "bad id", "bad:id", // contains invalid characters
            "bad%zz", "bad%" // '%' must precede two hexadecimal digits otherwise must be escaped
        };
        for(String id : badIds) {
            assertFalse(UrlRef.isIdentifier(id));
        }
    }
}
