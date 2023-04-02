/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.app;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.app.Application;

public class AppHeaderTest extends AppTestBase {

    protected static String app50NoSchema =
            "<application" +
                // " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/application_5.xsd\"" +
                " version=\"5\"" +
                " id=\"Application_ID\"" +
                ">\n" +
                appBody + "\n" +
            "</application>";
               
    protected static String app50NoSchemaInstance =
            "<application" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/application_5.xsd\"" +
                " version=\"5\"" +
                " id=\"Application_ID\"" +
                ">\n" +
                appBody + "\n" +
            "</application>";

    protected static String app50NoSchemaLocation =
            "<application" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                // " http://java.sun.com/xml/ns/javaee/application_5.xsd\"" +
                " version=\"5\"" +
                " id=\"Application_ID\"" +
                ">\n" +
                appBody + "\n" +
            "</application>";

    protected static String app50NoXSI =
            "<application" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                // " http://java.sun.com/xml/ns/javaee/application_5.xsd\"" +
                " version=\"5\"" +
                " id=\"Application_ID\"" +
                ">\n" +
                appBody + "\n" +
            "</application>";

    protected static String app50NoVersion =
            "<application" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/application_5.xsd\"" +
                // " version=\"5\"" +
                " id=\"Application_ID\"" +
                ">\n" +
                appBody + "\n" +
            "</application>";

    protected static String app50NoID =
            "<application" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/application_5.xsd\"" +
                " version=\"5\"" +
                // " id=\"Application_ID\"" +
                ">\n" +
                appBody + "\n" +
            "</application>";

    //

    protected static String app14NamespaceOnly = 
        "<application xmlns=\"http://java.sun.com/xml/ns/j2ee\">" +
            appBody + "\n" +
        "</application>";
    
    protected static String app14VersionOnly = 
        "<application version=\"1.4\">" +
            appBody + "\n" +
        "</application>";

    protected static String app50NamespaceOnly =
        "<application xmlns=\"http://java.sun.com/xml/ns/javaee\">" +
            appBody + "\n" +
        "</application>";

    protected static String app50VersionOnly =
        "<application version=\"5\">" +
            appBody + "\n" +
        "</application>";

    protected static String app70NamespaceOnly =
        "<application xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\">" +
            appBody + "\n" +
        "</application>";

    protected static String app70VersionOnly =
        "<application version=\"7\">" +
            appBody + "\n" +
        "</application>";

    protected static String app90NamespaceOnly =
        "<application xmlns=\"https://jakarta.ee/xml/ns/jakartaee\">" +
            appBody + "\n" +
        "</application>";

    protected static String app90VersionOnly =
        "<application version=\"9\">" +
            appBody + "\n" +
        "</application>";  

    protected static String app100VersionOnly =
        "<application version=\"10\">" +
            appBody + "\n" +
        "</application>";

    //

    protected static String app50VersionMismatch =
        "<application" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " version=\"5\"" +
        ">" +
            appBody + "\n" +
        "</application>";

    protected static String app50UnknownNamespace =
        "<application xmlns=\"http://junk\">" +
            appBody + "\n" +
        "</application>";

    protected static String app50UnknownVersion =
        "<application version=\"99\">" +
            appBody + "\n" +
        "</application>";

    //

    @Test
    public void testApp50NoSchema() throws Exception {
        Application app = parseApp(app50NoSchema, VERSION_9_0_INT);
        Assert.assertEquals( "5", app.getVersion() );
    }
    
    @Test
    public void testApp50NoSchemaInstance() throws Exception {
        parseApp(app50NoSchemaInstance, VERSION_9_0_INT,
                XML_ERROR_ALT_MESSAGE, XML_ERROR_MESSAGES);
    }
    
    @Test
    public void testApp50NoSchemaLocation() throws Exception {
        Application app = parseApp(app50NoSchemaLocation, VERSION_9_0_INT);
        Assert.assertEquals( "5", app.getVersion() );
    }
    
    @Test
    public void testApp50NoXSI() throws Exception {
        Application app = parseApp(app50NoXSI, VERSION_9_0_INT);
        Assert.assertEquals( "5", app.getVersion() );
    }
    
    @Test
    public void testApp50NoVersion() throws Exception {
        Application app = parseApp(app50NoVersion, VERSION_9_0_INT);
        Assert.assertEquals( "6", app.getVersion() );
    }

    @Test
    public void testApp50NoID() throws Exception {
        Application app = parseApp(app50NoID, VERSION_9_0_INT);
        Assert.assertEquals( "5", app.getVersion() );
    }

    //
    
    @Test
    public void testApp14NamespaceOnly () throws Exception {
        Application app = parseApp(app14NamespaceOnly , VERSION_9_0_INT);
        Assert.assertEquals( "1.4", app.getVersion() );
    }
    
    @Test
    public void testApp14VersionOnly () throws Exception {
        Application app = parseApp(app14VersionOnly , VERSION_9_0_INT);
        Assert.assertEquals( "1.4", app.getVersion() );
    }

    @Test
    public void testApp50NamespaceOnlyAt50() throws Exception {
        Application app = parseApp(app50NamespaceOnly, VERSION_5_0_INT);
        Assert.assertEquals( "6", app.getVersion() );
    }

    @Test
    public void testApp50NamespaceOnlyAt60() throws Exception {
        Application app = parseApp(app50NamespaceOnly, VERSION_6_0_INT);
        Assert.assertEquals( "6", app.getVersion() );
    }

    @Test
    public void testApp50NamespaceOnlyAt70() throws Exception {
        Application app = parseApp(app50NamespaceOnly, VERSION_7_0_INT);
        Assert.assertEquals( "6", app.getVersion() );
    }
    
    @Test
    public void testApp50VersionOnly() throws Exception {
        Application app = parseApp(app50VersionOnly, VERSION_9_0_INT);
        Assert.assertEquals( "5", app.getVersion() );
    }

    @Test
    public void testApp70NamespaceOnlyAt70() throws Exception {
        Application app = parseApp(app70NamespaceOnly, VERSION_7_0_INT);
        Assert.assertEquals( "7", app.getVersion() );
    }

    @Test
    public void testApp70NamespaceOnlyAt80() throws Exception {
        Application app = parseApp(app70NamespaceOnly, VERSION_8_0_INT);
        Assert.assertEquals( "8", app.getVersion() );
    }

    @Test
    public void testApp70NamespaceOnlyAt90() throws Exception {
        Application app = parseApp(app70NamespaceOnly, VERSION_9_0_INT);
        Assert.assertEquals( "8", app.getVersion() );
    }

    @Test
    public void testApp70VersionOnly() throws Exception {
        Application app = parseApp(app70VersionOnly, VERSION_9_0_INT);
        Assert.assertEquals( "7", app.getVersion() );
    }

    // Jakarta EE

    @Test
    public void testApp90VersionOnly() throws Exception {
        Application app = parseApp(app90VersionOnly, VERSION_9_0_INT);
        Assert.assertEquals( "9", app.getVersion() );
    }
    
    @Test
    public void testApp90NamespaceOnlyAt90() throws Exception {
        Application app = parseApp(app90NamespaceOnly, VERSION_9_0_INT);
        Assert.assertEquals( "9", app.getVersion() );
    }
    
    @Test
    public void testApp90NamespaceOnlyAt100() throws Exception {
        Application app = parseApp(app90NamespaceOnly, VERSION_10_0_INT);
        Assert.assertEquals( "10", app.getVersion() );
    }    

    // There is no 100 namespace only, since the same namespace is used for 90 and 100.

    @Test
    public void testApp100VersionOnly() throws Exception {
        Application app = parseApp(app100VersionOnly, VERSION_10_0_INT);
        Assert.assertEquals( "10", app.getVersion() );
    }
    
    //

    @Test
    public void testApp50VersionMismatch() throws Exception {
        Application app = parseApp(app50VersionMismatch, VERSION_9_0_INT);
        Assert.assertEquals( "5", app.getVersion() );
    }

    @Test
    public void testApp50UnknownNamespace() throws Exception {
        parseApp(app50UnknownNamespace, VERSION_9_0_INT,
                 UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
                 UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES);
    }

    @Test
    public void testApp50UnknownVersion() throws Exception {
        parseApp(app50UnknownVersion, VERSION_9_0_INT,
                 UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                 UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
}
