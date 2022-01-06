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
package com.ibm.ws.javaee.ddmodel.app;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.version.JavaEEVersion;

public class AppHeaderTest extends AppTestBase {
    
    // 1.4, http://java.sun.com/xml/ns/j2ee
    // 5,   http://java.sun.com/xml/ns/javaee
    // 7,   http://xmlns.jcp.org/xml/ns/javaee

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
        Application app = parseApp(app50NoSchema, JavaEEVersion.VERSION_9_0_INT);
        Assert.assertEquals( "5", app.getVersion() );
    }
    
    @Test
    public void testApp50NoSchemaInstance() throws Exception {
        parseApp(app50NoSchemaInstance, JavaEEVersion.VERSION_9_0_INT,
                XML_ERROR_ALT_MESSAGE, XML_ERROR_MESSAGES);
    }
    
    @Test
    public void testApp50NoSchemaLocation() throws Exception {
        Application app = parseApp(app50NoSchemaLocation, JavaEEVersion.VERSION_9_0_INT);
        Assert.assertEquals( "5", app.getVersion() );
    }
    
    @Test
    public void testApp50NoXSI() throws Exception {
        Application app = parseApp(app50NoXSI, JavaEEVersion.VERSION_9_0_INT);
        Assert.assertEquals( "5", app.getVersion() );
    }
    
    @Test
    public void testApp50NoVersion() throws Exception {
        Application app = parseApp(app50NoVersion, JavaEEVersion.VERSION_9_0_INT);
        Assert.assertEquals( "6", app.getVersion() );
    }

    @Test
    public void testApp50NoID() throws Exception {
        Application app = parseApp(app50NoID, JavaEEVersion.VERSION_9_0_INT);
        Assert.assertEquals( "5", app.getVersion() );
    }

    //
    
    @Test
    public void testApp14NamespaceOnly () throws Exception {
        Application app = parseApp(app14NamespaceOnly , JavaEEVersion.VERSION_9_0_INT);
        Assert.assertEquals( "1.4", app.getVersion() );
    }
    
    @Test
    public void testApp14VersionOnly () throws Exception {
        Application app = parseApp(app14VersionOnly , JavaEEVersion.VERSION_9_0_INT);
        Assert.assertEquals( "1.4", app.getVersion() );
    }

    @Test
    public void testApp50NamespaceOnlyAt50() throws Exception {
        Application app = parseApp(app50NamespaceOnly, JavaEEVersion.VERSION_5_0_INT);
        Assert.assertEquals( "6", app.getVersion() );
    }

    @Test
    public void testApp50NamespaceOnlyAt60() throws Exception {
        Application app = parseApp(app50NamespaceOnly, JavaEEVersion.VERSION_6_0_INT);
        Assert.assertEquals( "6", app.getVersion() );
    }

    @Test
    public void testApp50NamespaceOnlyAt70() throws Exception {
        Application app = parseApp(app50NamespaceOnly, JavaEEVersion.VERSION_7_0_INT);
        Assert.assertEquals( "6", app.getVersion() );
    }
    
    @Test
    public void testApp50VersionOnly() throws Exception {
        Application app = parseApp(app50VersionOnly, JavaEEVersion.VERSION_9_0_INT);
        Assert.assertEquals( "5", app.getVersion() );
    }

    @Test
    public void testApp70NamespaceOnlyAt70() throws Exception {
        Application app = parseApp(app70NamespaceOnly, JavaEEVersion.VERSION_7_0_INT);
        Assert.assertEquals( "7", app.getVersion() );
    }

    @Test
    public void testApp70NamespaceOnlyAt80() throws Exception {
        Application app = parseApp(app70NamespaceOnly, JavaEEVersion.VERSION_8_0_INT);
        Assert.assertEquals( "8", app.getVersion() );
    }

    @Test
    public void testApp70NamespaceOnlyAt90() throws Exception {
        Application app = parseApp(app70NamespaceOnly, JavaEEVersion.VERSION_9_0_INT);
        Assert.assertEquals( "8", app.getVersion() );
    }

    @Test
    public void testApp70VersionOnly() throws Exception {
        Application app = parseApp(app70VersionOnly, JavaEEVersion.VERSION_9_0_INT);
        Assert.assertEquals( "7", app.getVersion() );
    }

    //

    @Test
    public void testApp50VersionMismatch() throws Exception {
        Application app = parseApp(app50VersionMismatch, JavaEEVersion.VERSION_9_0_INT);
        Assert.assertEquals( "5", app.getVersion() );
    }

    @Test
    public void testApp50UnknownNamespace() throws Exception {
        parseApp(app50UnknownNamespace, JavaEEVersion.VERSION_9_0_INT,
                 UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
                 UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES);
    }

    @Test
    public void testApp50UnknownVersion() throws Exception {
        parseApp(app50UnknownVersion, JavaEEVersion.VERSION_9_0_INT,
                 UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                 UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
}
