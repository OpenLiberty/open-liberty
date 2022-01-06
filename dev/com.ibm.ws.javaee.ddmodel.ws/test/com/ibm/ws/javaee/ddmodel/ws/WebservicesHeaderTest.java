/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ws;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.ws.javaee.dd.ws.Webservices;
import com.ibm.ws.javaee.ddmodel.DDParser;

@RunWith(Parameterized.class)
public class WebservicesHeaderTest extends WebservicesTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return WEBSERVICES_TEST_DATA;
    }
        
    public WebservicesHeaderTest(boolean isWar) {
        super(isWar);
    }

    // Now valid
    protected static String webservicesNoNamespace13 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<webservices" + "\n" +
            // " xmlns=\"http://java.sun.com/xml/ns/javaee\"" + "\n" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/javaee_web_services_1_3.xsd\"" + "\n" +
            " version=\"1.3\"" +
        ">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();

    // Not valid: Need 'xmlns:xsi' if 'xsi:schemaLocation' is present.
    protected static String webservicesNoSchemaInstance13 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<webservices" + "\n" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" + "\n" +
            // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/javaee_web_services_1_3.xsd\"" +
            " version=\"1.3\"" + "\n" +
        ">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();

    // Valid
    protected static String webservicesNoXSI13 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<webservices" + "\n" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" + "\n" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/javaee_web_services_1_3.xsd\"" + "\n" +
            " version=\"1.3\"" +
        ">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();
    
    protected static String webservicesNoSchemaLocation13 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<webservices" + "\n" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" + "\n" +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
            // "xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
            //     " http://java.sun.com/xml/ns/javaee/javaee_web_services_1_3.xsd\"" + "\n" +
            " version=\"1.3\"" +
        ">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();
    
    // Valid
    protected static String webservicesNoVersion13 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<webservices" + "\n" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" + "\n" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/javaee_web_services_1_3.xsd\"" +
            // " version=\"1.3\"" +
        ">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();

    // Version only is valid.

    protected static String webservicesVersionOnly11 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<webservices version=\"1.1\">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();

    protected static String webservicesVersionOnly12 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<webservices version=\"1.2\">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();

    protected static String webservicesVersionOnly13 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<webservices version=\"1.3\">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();
        
    protected static String webservicesVersionOnly14 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<webservices version=\"1.4\">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();

    protected static String webservicesVersionOnly20 =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<webservices version=\"2.0\">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();

    protected static String webservicesVersionOnlyUnknown =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
        "<webservices version=\"9.9\">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();

    // Schema only is valid

    protected static String webservicesSchemaOnly11 =
        "<webservices xmlns=\"http://java.sun.com/xml/ns/j2ee\">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();

    // 1.2 and 1.3 are the same
    protected static String webservicesSchemaOnly12 =
        "<webservices xmlns=\"http://java.sun.com/xml/ns/javaee\">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();

    protected static String webservicesSchemaOnly14 =
        "<webservices xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();
    
    protected static String webservicesSchemaOnly20 =
        "<webservices xmlns=\"https://jakarta.ee/xml/ns/jakartaee\">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();    

    protected static String webservicesSchemaOnlyUnknown =
        "<webservices xmlns=\"https://unknown\">" + "\n" +
            webservicesBody() + "\n" +
        webservicesTail();
    
    // Version takes precedence
    
    protected static String webservicesVersionMismatch13 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<webservices" + "\n" +                    
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" + "\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
                    " http://xmlns.jcp.org/xml/ns/javaee/javaee_web_services_1_4.xsd\"" + "\n" +
                " version=\"1.3\"" +
            ">" + "\n" +
                webservicesBody() + "\n" +
            webservicesTail();    
    //

    @Test
    public void testWebservices13NoSchema() throws Exception {
        Webservices webservices = parseWebservices(webservicesNoNamespace13);
        Assert.assertEquals( "1.3", webservices.getVersion() );
    }

    @Test
    public void testWebservices13NoSchemaInstance() throws Exception {
        parseWebservices(webservicesNoSchemaInstance13, "xml.error", "CWWKC2272E");
    }

    @Test
    public void testWebservices13NoSchemaLocation() throws Exception {
        Webservices webservices = parseWebservices(webservicesNoSchemaLocation13);
        Assert.assertEquals( "1.3", webservices.getVersion() );
    }

    @Test
    public void testWebservices13NoXSI() throws Exception {
        Webservices webservices = parseWebservices(webservicesNoXSI13);
        Assert.assertEquals( "1.3", webservices.getVersion() );
    }

    @Test
    public void testWebservices13NoVersion() throws Exception {
        Webservices webservices = parseWebservices(webservicesNoVersion13);
        Assert.assertEquals( "1.3", webservices.getVersion() );
    }

    //
    
    @Test
    public void testWebservices11SchemaOnly() throws Exception {
        Webservices webservices = parseWebservices(webservicesSchemaOnly11);
        Assert.assertEquals( "1.1", webservices.getVersion() );
    }
    
    @Test
    public void testWebservices12SchemaOnly() throws Exception {
        Webservices webservices = parseWebservices(webservicesSchemaOnly12);
        Assert.assertEquals( "1.3", webservices.getVersion() );
    }    

    @Test
    public void testWebservices14SchemaOnly() throws Exception {
        Webservices webservices = parseWebservices(webservicesSchemaOnly14);
        Assert.assertEquals( "1.4", webservices.getVersion() );
    }    

    @Test
    public void testWebservices20NamespaceOnly() throws Exception {
        Webservices webservices = parseWebservices(webservicesSchemaOnly20);
        Assert.assertEquals( "2.0", webservices.getVersion() );
    }    
    
    @Test
    public void testWebservices11VersionOnly() throws Exception {
        Webservices webservices = parseWebservices(webservicesVersionOnly11);
        Assert.assertEquals( "1.1", webservices.getVersion() );
    }
    
    @Test
    public void testWebservices12VersionOnly() throws Exception {
        Webservices webservices = parseWebservices(webservicesVersionOnly12);
        Assert.assertEquals( "1.2", webservices.getVersion() );
    }
    
    @Test
    public void testWebservices13VersionOnly() throws Exception {
        Webservices webservices = parseWebservices(webservicesVersionOnly13);
        Assert.assertEquals( "1.3", webservices.getVersion() );
    }
    
    @Test
    public void testWebservices14VersionOnly() throws Exception {
        Webservices webservices = parseWebservices(webservicesVersionOnly14);
        Assert.assertEquals( "1.4", webservices.getVersion() );
    }
    
    @Test
    public void testWebservices20VersionOnly() throws Exception {
        Webservices webservices = parseWebservices(webservicesVersionOnly20);
        Assert.assertEquals( "2.0", webservices.getVersion() );
    }    

    // A warning is issued, but otherwise the mismatch is ignored
    // and the version has precedence.

    @Test
    public void testWebservices13VersionMismatch() throws Exception {
        Webservices webservices = parseWebservices(webservicesVersionMismatch13);
        Assert.assertEquals( "1.3", webservices.getVersion() );
    }

    @Test
    public void testWebservicesVersionOnlyUnknown() throws Exception {
        parseWebservices(webservicesVersionOnlyUnknown,
                UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }

    @Test
    public void testWebservicesSchemaOnlyUnknown() throws Exception {
        parseWebservices(webservicesSchemaOnlyUnknown,
                UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
                UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES);
    }   

    //

    public static class VersionTestData {
        public final String xmlText;
        public final int version;
        public final String versionText;
        
        public VersionTestData(String xmlText, int version) {
            this.xmlText = xmlText;
            this.version = version;
            this.versionText = DDParser.getDottedVersionText(version);
        }
    }

    // Version based parsing always assigns the exact version
    // which is in the descriptor.

    public static final VersionTestData[] VERSION_TEST_DATA = {
        new VersionTestData(webservicesVersionOnly11, Webservices.VERSION_1_1),
        new VersionTestData(webservicesVersionOnly12, Webservices.VERSION_1_2),
        new VersionTestData(webservicesVersionOnly13, Webservices.VERSION_1_3),
        new VersionTestData(webservicesVersionOnly14, Webservices.VERSION_1_4),
        new VersionTestData(webservicesVersionOnly20, Webservices.VERSION_2_0)
    };
    
    @Test
    public void testWebservicesVersionOnly() throws Exception {
        for ( VersionTestData testData : VERSION_TEST_DATA ) {
            Webservices webservices = parseWebservices(testData.xmlText);
            Assert.assertEquals("Assigned descriptor version",
                    testData.versionText, webservices.getVersion());
        }
    }

    // The version must be known.

    @Test
    public void testVersionOnlyUnknown() throws Exception {
        parseWebservices(webservicesVersionOnlyUnknown,
                    UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                    UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }

    //
    
    public static class SchemaTestData {
        public final String xmlText;
        public final int expectedVersion;
        public final String expectedVersionText;

        public SchemaTestData(String xmlText, int expectedVersion) {
            
            this.xmlText = xmlText;
            this.expectedVersion = expectedVersion;
            this.expectedVersionText = DDParser.getDottedVersionText(expectedVersion);
        }
    }

    public static SchemaTestData[] SCHEMA_TEST_DATA = {
        new SchemaTestData(webservicesSchemaOnly11, Webservices.VERSION_1_1),
        new SchemaTestData(webservicesSchemaOnly12, Webservices.VERSION_1_3),
        new SchemaTestData(webservicesSchemaOnly14, Webservices.VERSION_1_4),
        new SchemaTestData(webservicesSchemaOnly20, Webservices.VERSION_2_0)
    };
    
    @Test
    public void testWebservicesSchemaOnly() throws Exception {
        for ( SchemaTestData testData : SCHEMA_TEST_DATA ) {
            Webservices webservices = parseWebservices(testData.xmlText);
            Assert.assertEquals("Assigned descriptor version",
                    testData.expectedVersionText, webservices.getVersion());
        }
    }
}
