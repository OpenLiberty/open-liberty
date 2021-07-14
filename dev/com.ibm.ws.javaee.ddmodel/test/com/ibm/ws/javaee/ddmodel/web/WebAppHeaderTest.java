/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.web;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.ddmodel.DDParser;

/**
 * Tests of web application descriptors which have partial headers.
 */
public class WebAppHeaderTest extends WebAppTestBase {
    // Now valid
    protected static String noSchemaWebApp30 =
        "<web-app" +
            // " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
            " version=\"3.0\"" +
            " id=\"WebApp_ID\"" +
        ">" + "\n" +
            webAppBody() + "\n" +
        webAppTail();

    // Not valid: Need 'xmlns:xsi' if 'xsi:schemaLocation' is present.
    protected static String noSchemaInstanceWebApp30 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
            " version=\"3.0\"" +
            " id=\"WebApp_ID\"" +
        ">" + "\n" +
            webAppBody() + "\n" +
        webAppTail();

    // Valid
    protected static String noXSIWebApp30 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
            " version=\"3.0\"" +
            " id=\"WebApp_ID\"" +
        ">" + "\n" +
            webAppBody() + "\n" +
        webAppTail();    

    // Valid
    protected static String noSchemaLocationWebApp30 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +               
            " version=\"3.0\"" +
            " id=\"WebApp_ID\"" +
        ">" +
            webAppBody() + "\n" +
        webAppTail();

    // Valid
    protected static String noVersionWebApp30 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
            // " version=\"3.0\"" +               
            " id=\"WebApp_ID\"" +
        ">" +
            webAppBody() + "\n" +
        webAppTail();

    // Valid
    protected static String noIDWebApp30 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
            " version=\"3.0\"" +
            // " id=\"WebApp_ID\"" +
        ">" +
            webAppBody() + "\n" +
        webAppTail();
    
    // Version only is valid.
    
    protected static String webAppVersionOnly24 =
            "<web-app version=\"2.4\">" + "\n" +
                webAppBody() + "\n" +
            webAppTail();

    protected static String webAppVersionOnly25 =
            "<web-app version=\"2.5\">" + "\n" +
                webAppBody() + "\n" +
            webAppTail();
    
    protected static String webAppVersionOnly30 =
            "<web-app version=\"3.0\">" + "\n" +
                webAppBody() + "\n" +
            webAppTail();
        
    protected static String webAppVersionOnly31 =
            "<web-app version=\"3.1\">" + "\n" +
                webAppBody() + "\n" +
            webAppTail();

    protected static String webAppVersionOnly40 =
            "<web-app version=\"4.0\">" + "\n" +
                webAppBody() + "\n" +
            webAppTail();

    protected static String webAppVersionOnly50 =
            "<web-app version=\"5.0\">" + "\n" +
                webAppBody() + "\n" +
            webAppTail();
    
    protected static String webAppVersionOnlyUnknown =
            "<web-app version=\"9.9\">" + "\n" +
                webAppBody() + "\n" +
            webAppTail();
    
    // Schema only is valid

    protected static String webAppSchemaOnly24 =
        "<web-app xmlns=\"http://java.sun.com/xml/ns/j2ee\">" + "\n" +
            webAppBody() + "\n" +
        webAppTail();

    // The schemas for 25 and 30 are the same.
    protected static String webAppSchemaOnly25 =
        "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\">" + "\n" +
            webAppBody() + "\n" +
        webAppTail();
    
    // The schemas for 31 and 40 are the same.
    protected static String webAppSchemaOnly31 =
        "<web-app xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\">" + "\n" +
            webAppBody() + "\n" +
        webAppTail();

    protected static String webAppSchemaOnly50 =
        "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\">" + "\n" +
            webAppBody() + "\n" +
        webAppTail();

    protected static String webAppSchemaOnlyUnknown =
        "<web-app xmlns=\"https://unknown\">" + "\n" +
            webAppBody() + "\n" +
        webAppTail();
    
    //

    @Test
    public void testEE6Web30NoSchema() throws Exception {
        WebApp webApp = parseWebApp(noSchemaWebApp30);
        Assert.assertEquals("Assigned descriptor version", "3.0", webApp.getVersion());        
    }

    // Not valid: Need 'xmlns:xsi' if 'xsi:schemaLocation' is present.    
    @Test
    public void testEE6Web30NoSchemaInstance() throws Exception {
        parseWebApp(noSchemaInstanceWebApp30, "xml.error", "CWWKC2272E");
    }

    @Test
    public void testEE6Web30NoSchemaLocation() throws Exception {
        WebApp webApp = parseWebApp(noSchemaLocationWebApp30);
        Assert.assertEquals("Assigned descriptor version", "3.0", webApp.getVersion());
    }    
    
    @Test
    public void testEE6Web30NoXSI() throws Exception {
        WebApp webApp = parseWebApp(noXSIWebApp30);
        Assert.assertEquals("Assigned descriptor version", "3.0", webApp.getVersion());
    }

    @Test
    public void testEE6Web30NoVersion() throws Exception {
        WebApp webApp = parseWebApp(noVersionWebApp30);
        Assert.assertEquals("Assigned descriptor version", "3.0", webApp.getVersion());
    }

    @Test
    public void testEE6Web30NoID() throws Exception {
        WebApp webApp = parseWebApp(noIDWebApp30);
        Assert.assertEquals("Assigned descriptor version", "3.0", webApp.getVersion());        
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
        new VersionTestData(webAppVersionOnly24, WebApp.VERSION_2_4),
        new VersionTestData(webAppVersionOnly25, WebApp.VERSION_2_5),
        new VersionTestData(webAppVersionOnly30, WebApp.VERSION_3_0),
        new VersionTestData(webAppVersionOnly31, WebApp.VERSION_3_1),
        new VersionTestData(webAppVersionOnly40, WebApp.VERSION_4_0),
        new VersionTestData(webAppVersionOnly50, WebApp.VERSION_5_0),
    };
    
    @Test
    public void testWebVersionOnly() throws Exception {
        for ( VersionTestData testData : VERSION_TEST_DATA ) {
            WebApp webApp = parseWebApp(testData.xmlText, testData.version);
            Assert.assertEquals("Assigned descriptor version",
                    testData.versionText, webApp.getVersion());
        }
    }

    // The version must be known.

    @Test
    public void testVersionOnlyUnknown() throws Exception {
        parseWebApp(webAppVersionOnlyUnknown,
                    UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                    UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }

    // A un-provisioned version message is generated when the version is known
    // but not currently provisioned.
    
    @Test
    public void testVersionOnlyUnprovisioned() throws Exception {
        parseWebApp(webAppVersionOnly50, WebApp.VERSION_3_1,
                    UNPROVISIONED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                    UNPROVISIONED_DESCRIPTOR_VERSION_MESSAGES);
    }

    //
    
    public static class SchemaTestData {
        public final int version;
        public final String xmlText;
        public final int maxVersion;
        public final int expectedVersion;
        public final String expectedVersionText;

        public SchemaTestData(
            int version, String xmlText,
            int maxVersion,
            int expectedVersion) {
            
            this.version = version;
            this.xmlText = xmlText;
            this.maxVersion = maxVersion;
            this.expectedVersion = expectedVersion;
            this.expectedVersionText = DDParser.getDottedVersionText(expectedVersion);
        }
    }
    
    public static SchemaTestData[] SCHEMA_TEST_DATA = {
        // The 2.4 schema is for 2.4, only.
        new SchemaTestData(WebApp.VERSION_2_4, webAppSchemaOnly24, WebApp.VERSION_2_4, WebApp.VERSION_2_4),
        new SchemaTestData(WebApp.VERSION_2_4, webAppSchemaOnly24, WebApp.VERSION_2_5, WebApp.VERSION_2_4),
        new SchemaTestData(WebApp.VERSION_2_4, webAppSchemaOnly24, WebApp.VERSION_3_0, WebApp.VERSION_2_4),            

        // The 2.5 schema is for 2.5 and for 3.0.
        // Since the minimum provisioning is 3.0, the assigned version is always 3.0.
        new SchemaTestData(WebApp.VERSION_2_5, webAppSchemaOnly25, WebApp.VERSION_2_5, WebApp.VERSION_3_0),
        new SchemaTestData(WebApp.VERSION_2_5, webAppSchemaOnly25, WebApp.VERSION_3_0, WebApp.VERSION_3_0),
        new SchemaTestData(WebApp.VERSION_2_5, webAppSchemaOnly25, WebApp.VERSION_3_1, WebApp.VERSION_3_0),            
        new SchemaTestData(WebApp.VERSION_2_5, webAppSchemaOnly25, WebApp.VERSION_4_0, WebApp.VERSION_3_0),            

        // The 3.1 schema is for 3.1 and for 4.0.
        // The assigned version can be 3.1 or 4.0, depending on the provisioning.
        new SchemaTestData(WebApp.VERSION_3_1, webAppSchemaOnly31, WebApp.VERSION_3_1, WebApp.VERSION_3_1),                        
        new SchemaTestData(WebApp.VERSION_3_1, webAppSchemaOnly31, WebApp.VERSION_4_0, WebApp.VERSION_4_0),                                    
        new SchemaTestData(WebApp.VERSION_3_1, webAppSchemaOnly31, WebApp.VERSION_5_0, WebApp.VERSION_4_0),                                    

        // The 5.0 schema is for 5.0, only.
        new SchemaTestData(WebApp.VERSION_5_0, webAppSchemaOnly50, WebApp.VERSION_5_0, WebApp.VERSION_5_0)                                                
    };
    
    @Test
    public void testWebSchemaOnly() throws Exception {
        for ( SchemaTestData testData : SCHEMA_TEST_DATA ) {
            WebApp webApp = parseWebApp(testData.xmlText, testData.maxVersion);
            Assert.assertEquals("Assigned descriptor version",
                    testData.expectedVersionText, webApp.getVersion());
        }
    }
    
    // The schema must be valid.
    @Test
    public void testSchemaOnlyUnknown() throws Exception {
        parseWebApp(webAppSchemaOnlyUnknown,
                    UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
                    UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES);
    }
    
    // A un-provisioned schema message is generated when the schema is known
    // but not currently provisioned.

    @Test
    public void testSchemaOnlyUnprovisioned() throws Exception {
        parseWebApp(webAppSchemaOnly50, WebApp.VERSION_3_1,
                    UNPROVISIONED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                    UNPROVISIONED_DESCRIPTOR_VERSION_MESSAGES);

    }    
}
