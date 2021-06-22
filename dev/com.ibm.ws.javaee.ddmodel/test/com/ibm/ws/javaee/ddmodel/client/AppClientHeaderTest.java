/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.client;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.client.ApplicationClient;

public class AppClientHeaderTest extends AppClientTestBase {
    protected static String appClient14NoNamespace =
            "<application-client" +
                // " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/application-client_1_4.xsd\"" +
                " version=\"1.4\"" +
                " id=\"ApplicationClient_ID\"" +
            "/>";

    protected static String appClient14NoSchemaInstance =
            "<application-client" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/application-client_1_4.xsd\"" +
                " version=\"1.4\"" +
                " id=\"ApplicationClient_ID\"" +
            "/>";
        
    protected static String appClient14NoSchemaLocation =
            "<application-client" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                // " http://java.sun.com/xml/ns/javaee/application-client_1_4.xsd\"" +
                " version=\"1.4\"" +
                " id=\"ApplicationClient_ID\"" +
            "/>";

    
    protected static String appClient14NoXMI =
        "<application-client" +
            " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
            // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
            // " http://java.sun.com/xml/ns/javaee/application-client_1_4.xsd\"" +
            " version=\"1.4\"" +
            " id=\"ApplicationClient_ID\"" +
        "/>";

    protected static String appClient14NoVersion =
            "<application-client" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/application-client_1_4.xsd\"" +
                // " version=\"1.4\"" +
                " id=\"ApplicationClient_ID\"" +
            "/>";

    protected static String appClient14NoId =
            "<application-client" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/application-client_1_4.xsd\"" +
                " version=\"1.4\"" +
                // " id=\"ApplicationClient_ID\"" +
            "/>";

    //
    
    protected static String appClient14NamespaceOnly =
            "<application-client xmlns=\"http://java.sun.com/xml/ns/j2ee\"/>";

    protected static String appClient14VersionOnly =
            "<application-client version=\"1.4\"/>";
    
    // 50 and 60 use sun.javaee

    protected static String appClient50NamespaceOnly =
            "<application-client xmlns=\"http://java.sun.com/xml/ns/javaee\"/>";

    protected static String appClient50VersionOnly =
            "<application-client version=\"5\"/>";

    // 70 and 80 use jcp.javaee

    protected static String appClient70NamespaceOnly =
            "<application-client xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"/>";

    protected static String appClient70VersionOnly =
            "<application-client version=\"7\"/>";

    //

    protected static String appClient50VersionMismatch =
            "<application-client" +
                " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                " http://java.sun.com/xml/ns/javaee/application-client_1_4.xsd\"" +
                " version=\"5\"" +
                " id=\"ApplicationClient_ID\"" +
            "/>";

    protected static String appClientNamespaceBad =
            "<application-client xmlns=\"http://junk\"/>";

    protected static String appClientVersionBead =
            "<application-client version=\"9.9\"/>";

    //

    @Test
    public void testAppClient14NoNamespace() throws Exception {
        ApplicationClient appClient =
            parse(appClient14NoNamespace, ApplicationClient.VERSION_6);
        Assert.assertEquals( 14, appClient.getVersionID() );
    }

    @Test
    public void testAppClient14NoSchemaInstance() throws Exception {
        parse(appClient14NoSchemaInstance, ApplicationClient.VERSION_6,
                XML_ERROR_ALT_MESSAGE, XML_ERROR_MESSAGES);
    }

    @Test
    public void testAppClient14NoSchemaLocation() throws Exception {
        ApplicationClient appClient =
            parse(appClient14NoSchemaLocation, ApplicationClient.VERSION_6);
        Assert.assertEquals( 14, appClient.getVersionID() );
    }
   
    @Test
    public void testAppClient14NoXMI() throws Exception {
        ApplicationClient appClient =
            parse(appClient14NoXMI, ApplicationClient.VERSION_6);
        Assert.assertEquals( 14, appClient.getVersionID() );
    }

    @Test
    public void testAppClient14NoVersion() throws Exception {
        ApplicationClient appClient =
            parse(appClient14NoVersion, ApplicationClient.VERSION_6);
        Assert.assertEquals( 14, appClient.getVersionID() );
    }

    @Test
    public void testAppClient14NoId() throws Exception {
        ApplicationClient appClient =
            parse(appClient14NoId, ApplicationClient.VERSION_6);
        Assert.assertEquals( 14, appClient.getVersionID() );
    }

    //
    
    @Test
    public void testAppClient14VersionOnly() throws Exception {
        ApplicationClient appClient =
            parse(appClient14VersionOnly, ApplicationClient.VERSION_6);
        Assert.assertEquals( 14, appClient.getVersionID() );
    }

    @Test
    public void testAppClient14NamespaceOnly() throws Exception {
        ApplicationClient appClient = 
            parse(appClient14NamespaceOnly, ApplicationClient.VERSION_5);
        // The maximum schema version has a minimum value of 6.0.
        Assert.assertEquals( 14, appClient.getVersionID() );
    }
    
    // 50 and 60 use sun.javaee

    @Test
    public void testAppClient50VersionOnlyAt50() throws Exception {
        ApplicationClient appClient =
            parse(appClient50VersionOnly, ApplicationClient.VERSION_5);
        Assert.assertEquals( 50, appClient.getVersionID() );
    }
    
    @Test
    public void testAppClient50VersionOnlyAt60() throws Exception {
        ApplicationClient appClient =
            parse(appClient50VersionOnly, ApplicationClient.VERSION_6);
        Assert.assertEquals( 50, appClient.getVersionID() );
    }

    @Test
    public void testAppClient50NamespaceOnlyAt50() throws Exception {
        ApplicationClient appClient = 
            parse(appClient50NamespaceOnly, ApplicationClient.VERSION_5);
        // The maximum schema version has a minimum value of 6.0.
        Assert.assertEquals( 60, appClient.getVersionID() );
    }

    @Test
    public void testAppClient50NamespaceOnlyAt60() throws Exception {
        ApplicationClient appClient = 
            parse(appClient50NamespaceOnly, ApplicationClient.VERSION_6);
        Assert.assertEquals( 60, appClient.getVersionID() );
    }

    // 70 and 80 use jcp.javaee

    @Test
    public void testAppClient70VersionOnlyAt70() throws Exception {
        ApplicationClient appClient =
            parse(appClient70VersionOnly, ApplicationClient.VERSION_7);
        Assert.assertEquals( 70, appClient.getVersionID() );
    }
    
    @Test
    public void testAppClient70VersionOnlyAt80() throws Exception {
        ApplicationClient appClient =
            parse(appClient70VersionOnly, ApplicationClient.VERSION_8);
        Assert.assertEquals( 70, appClient.getVersionID() );
    }    

    @Test
    public void testAppClient70NamespaceOnlyAt70() throws Exception {
        ApplicationClient appClient =
            parse(appClient70NamespaceOnly, ApplicationClient.VERSION_7);
        Assert.assertEquals( 70, appClient.getVersionID() );
    }

    @Test
    public void testAppClient70NamespaceOnlyAt80() throws Exception {
        ApplicationClient appClient =
            parse(appClient70NamespaceOnly, ApplicationClient.VERSION_8);
        Assert.assertEquals( 80, appClient.getVersionID() );
    }

    //

    @Test
    public void testAppClient50VersionMismatch() throws Exception {
        ApplicationClient appClient =
            parse(appClient50VersionMismatch, ApplicationClient.VERSION_5);
        Assert.assertEquals( 50, appClient.getVersionID() );
    }

    @Test
    public void testAppClientNamespaceBad() throws Exception {
        parse(appClientNamespaceBad, ApplicationClient.VERSION_6,
                UNSUPPORTED_DESCRIPTOR_NAMESPACE_ALT_MESSAGE,
                UNSUPPORTED_DESCRIPTOR_NAMESPACE_MESSAGES);
    }

    @Test
    public void testAppClientVersionBead() throws Exception {
        parse(appClientVersionBead, ApplicationClient.VERSION_6,
                UNSUPPORTED_DESCRIPTOR_VERSION_ALT_MESSAGE,
                UNSUPPORTED_DESCRIPTOR_VERSION_MESSAGES);
    }
}
