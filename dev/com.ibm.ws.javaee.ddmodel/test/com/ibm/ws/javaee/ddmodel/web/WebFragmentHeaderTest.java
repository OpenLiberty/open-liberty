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
import com.ibm.ws.javaee.dd.web.WebFragment;
import com.ibm.ws.javaee.ddmodel.DDParser;

/**
 * Tests of web application descriptors which have partial headers.
 */
public class WebFragmentHeaderTest extends WebFragmentTestBase {
    // Now valid
    protected static String noSchemaWebFragment30 =
        "<web-fragment" +
            // " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-fragment_3_0.xsd\"" +
            " version=\"3.0\"" +
            " id=\"WebFragment_ID\"" +
        ">" + "\n" +
            WebAppTestBase.webAppBody() + "\n" +
        webFragmentTail();

    // Not valid: Need 'xmlns:xsi' if 'xsi:schemaLocation' is present.
    protected static String noSchemaInstanceWebFragment30 =
        "<web-fragment" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-fragment_3_0.xsd\"" +
            " version=\"3.0\"" +
            " id=\"WebFragment_ID\"" +
        ">" + "\n" +
            WebAppTestBase.webAppBody() + "\n" +
        webFragmentTail();

    // Valid
    protected static String noXSIWebFragment30 =
        "<web-fragment" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-fragment_3_0.xsd\"" +
            " version=\"3.0\"" +
            " id=\"WebFragment_ID\"" +
        ">" + "\n" +
            WebAppTestBase.webAppBody() + "\n" +
        webFragmentTail();

    // Valid
    protected static String noSchemaLocationWebFragment30 =
        "<web-fragment" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-fragment_3_0.xsd\"" +               
            " version=\"3.0\"" +
            " id=\"WebFragment_ID\"" +
        ">" +
            WebAppTestBase.webAppBody() + "\n" +
        webFragmentTail();

    // Valid
    protected static String noVersionWebFragment30 =
        "<web-fragment" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-fragment_3_0.xsd\"" +
            // " version=\"3.0\"" +               
            " id=\"WebFragment_ID\"" +
        ">" +
            WebAppTestBase.webAppBody() + "\n" +
        webFragmentTail();

    // Valid
    protected static String noIDWebFragment30 =
        "<web-fragment" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-fragment_3_0.xsd\"" +
            " version=\"3.0\"" +
            // " id=\"WebFragment_ID\"" +
        ">" +
            WebAppTestBase.webAppBody() + "\n" +
        webFragmentTail();
    
    // Version only is valid.
    
    protected static String webFragmentVersionOnly30 =
            "<web-fragment version=\"3.0\">" + "\n" +
                WebAppTestBase.webAppBody() + "\n" +
            webFragmentTail();

    protected static String webFragmentVersionOnly31 =
            "<web-fragment version=\"3.1\">" + "\n" +
                WebAppTestBase.webAppBody() + "\n" +
            webFragmentTail();

    protected static String webFragmentVersionOnly40 =
            "<web-fragment version=\"4.0\">" + "\n" +
                WebAppTestBase.webAppBody() + "\n" +
            webFragmentTail();

    protected static String webFragmentVersionOnly50 =
            "<web-fragment version=\"5.0\">" + "\n" +
                WebAppTestBase.webAppBody() + "\n" +
            webFragmentTail();

    protected static String webFragmentVersionOnlyUnknown =
            "<web-fragment version=\"9.9\">" + "\n" +
                WebAppTestBase.webAppBody() + "\n" +
            webFragmentTail();

    // Schema only is valid

    protected static String webFragmentSchemaOnly30 =
        "<web-fragment xmlns=\"http://java.sun.com/xml/ns/javaee\">" + "\n" +
            WebAppTestBase.webAppBody() + "\n" +
        webFragmentTail();

    // The schemas for 31 and 40 are the same.
    protected static String webFragmentSchemaOnly31 =
        "<web-fragment xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\">" + "\n" +
            WebAppTestBase.webAppBody() + "\n" +
        webFragmentTail();

    protected static String webFragmentSchemaOnly50 =
        "<web-fragment xmlns=\"https://jakarta.ee/xml/ns/jakartaee\">" + "\n" +
            WebAppTestBase.webAppBody() + "\n" +
        webFragmentTail();

    protected static String webFragmentSchemaOnlyUnknown =
        "<web-fragment xmlns=\"https://unknown\">" + "\n" +
            WebAppTestBase.webAppBody() + "\n" +
        webFragmentTail();
    
    //

    @Test
    public void testEE6Web30NoSchema() throws Exception {
        WebFragment WebFragment = parse(noSchemaWebFragment30);
        Assert.assertEquals("Assigned descriptor version",
                "3.0", WebFragment.getVersion());        
    }

    // Not valid: Need 'xmlns:xsi' if 'xsi:schemaLocation' is present.    
    @Test
    public void testEE6Web30NoSchemaInstance() throws Exception {
        parse(noSchemaInstanceWebFragment30, "xml.error", "CWWKC2272E");
    }

    @Test
    public void testEE6Web30NoSchemaLocation() throws Exception {
        WebFragment WebFragment = parse(noSchemaLocationWebFragment30);
        Assert.assertEquals("Assigned descriptor version",
                "3.0", WebFragment.getVersion());
    }    
    
    @Test
    public void testEE6Web30NoXSI() throws Exception {
        WebFragment WebFragment = parse(noXSIWebFragment30);
        Assert.assertEquals("Assigned descriptor version",
                "3.0", WebFragment.getVersion());
    }

    @Test
    public void testEE6Web30NoVersion() throws Exception {
        WebFragment WebFragment = parse(noVersionWebFragment30);
        Assert.assertEquals("Assigned descriptor version",
                "3.0", WebFragment.getVersion());
    }

    @Test
    public void testEE6Web30NoID() throws Exception {
        WebFragment WebFragment = parse(noIDWebFragment30);
        Assert.assertEquals("Assigned descriptor version",
                "3.0", WebFragment.getVersion());        
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
        new VersionTestData(webFragmentVersionOnly30, WebApp.VERSION_3_0),
        new VersionTestData(webFragmentVersionOnly31, WebApp.VERSION_3_1),
        new VersionTestData(webFragmentVersionOnly40, WebApp.VERSION_4_0),
        new VersionTestData(webFragmentVersionOnly50, WebApp.VERSION_5_0),
    };
    
    @Test
    public void testWebVersionOnly() throws Exception {
        for ( VersionTestData testData : VERSION_TEST_DATA ) {
            WebFragment WebFragment = parse(testData.xmlText, testData.version);
            Assert.assertEquals("Assigned descriptor version",
                    testData.versionText, WebFragment.getVersion());
        }
    }

    // The version must be known.

    @Test
    public void testVersionOnlyUnknown() throws Exception {
        parse(webFragmentVersionOnlyUnknown,
              UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
              UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }

    // A un-provisioned version message is generated when the version is known
    // but not currently provisioned.
    
    @Test
    public void testVersionOnlyUnprovisioned() throws Exception {
        parse(webFragmentVersionOnly50, WebApp.VERSION_3_1,
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
        // The 3.0 schema is for 3.0, only.
        new SchemaTestData(WebApp.VERSION_3_0, webFragmentSchemaOnly30, WebApp.VERSION_3_0, WebApp.VERSION_3_0),
        new SchemaTestData(WebApp.VERSION_3_0, webFragmentSchemaOnly30, WebApp.VERSION_3_0, WebApp.VERSION_3_0),
        new SchemaTestData(WebApp.VERSION_3_0, webFragmentSchemaOnly30, WebApp.VERSION_3_1, WebApp.VERSION_3_0),            
        new SchemaTestData(WebApp.VERSION_3_0, webFragmentSchemaOnly30, WebApp.VERSION_4_0, WebApp.VERSION_3_0),            

        // The 3.1 schema is for 3.1 and for 4.0.
        // The assigned version can be 3.1 or 4.0, depending on the provisioning.
        new SchemaTestData(WebApp.VERSION_3_1, webFragmentSchemaOnly31, WebApp.VERSION_3_1, WebApp.VERSION_3_1),                        
        new SchemaTestData(WebApp.VERSION_3_1, webFragmentSchemaOnly31, WebApp.VERSION_4_0, WebApp.VERSION_4_0),                                    
        new SchemaTestData(WebApp.VERSION_3_1, webFragmentSchemaOnly31, WebApp.VERSION_5_0, WebApp.VERSION_4_0),                                    

        // The 5.0 schema is for 5.0, only.
        new SchemaTestData(WebApp.VERSION_5_0, webFragmentSchemaOnly50, WebApp.VERSION_5_0, WebApp.VERSION_5_0)                                                
    };
    
    @Test
    public void testWebSchemaOnly() throws Exception {
        for ( SchemaTestData testData : SCHEMA_TEST_DATA ) {
            WebFragment WebFragment = parse(testData.xmlText, testData.maxVersion);
            Assert.assertEquals("Assigned descriptor version",
                    testData.expectedVersionText, WebFragment.getVersion());
        }
    }
    
    // The schema must be valid.
    @Test
    public void testSchemaOnlyUnknown() throws Exception {
        parse(webFragmentSchemaOnlyUnknown,
              UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
              UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES);

    }
    
    // A un-provisioned schema message is generated when the schema is known
    // but not currently provisioned.

    @Test
    public void testSchemaOnlyUnprovisioned() throws Exception {
        parse(webFragmentSchemaOnly50, WebApp.VERSION_3_1,
              UNPROVISIONED_DESCRIPTOR_VERSION_ALT_MESSAGE,
              UNPROVISIONED_DESCRIPTOR_VERSION_MESSAGES);

    }    
}
