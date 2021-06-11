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

import org.junit.Test;

import com.ibm.ws.javaee.dd.web.WebApp;

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
        "</web-app>";

    // Not valid
    protected static String noSchemaInstanceWebApp30 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            // " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
            " version=\"3.0\"" +
            " id=\"WebApp_ID\"" +
            ">" + "\n" +
        "</web-app>";

    // Valid
    protected static String noSchemaLocationWebApp30 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            // " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +               
            " version=\"3.0\"" +
            " id=\"WebApp_ID\">" +
        "</web-app>";

    // Valid
    protected static String noVersionWebApp30 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
            // " version=\"3.0\"" +               
            " id=\"WebApp_ID\">" +
       "</web-app>";

    // Valid
    protected static String noIDWebApp30 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
            " version=\"3.0\"" +
            // " id=\"WebApp_ID\"" +
            ">" +
        "</web-app>";
    
    //
    
    protected static String webAppVersionOnly24 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " version=\"2.4\"" +
            ">" + "\n" +
       "</web-app>";

    protected static String webAppVersionOnly25 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " version=\"2.5\"" +
            ">" + "\n" +
       "</web-app>";
    
    protected static String webAppVersionOnly30 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " version=\"3.0\"" +
            ">" + "\n" +
       "</web-app>";
    
    protected static String webAppVersionOnly31 =
        "<web-app" +
            " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " version=\"3.1\"" +
            ">" + "\n" +
       "</web-app>";

    protected static String webAppVersionOnly40 =
        "<web-app" +
            " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " version=\"4.0\"" +
            ">" + "\n" +
       "</web-app>";

    protected static String webAppVersionOnly50 =
        "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd\"" +
            " version=\"5.0\"" +
            ">" + "\n" +
       "</web-app>";

    //

    protected static String webAppSchemaOnly24 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\"" +
            ">" + "\n" +
       "</web-app>";

    protected static String webAppSchemaOnly25 =
        "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\"" +
            " version=\"2.5\"" +
            " id=\"WebApp_ID\"" +
            ">" + "\n" +
        "</web-app>";
    
    protected static String webAppSchemaOnly30 =
        "<web-app" +
            " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
            ">" + "\n" +
        "</web-app>";
    
    protected static String webAppSchemaOnly31 =
        "<web-app" +
            " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd\"" +
            " version=\"3.1\"" +
            " id=\"WebApp_ID\"" +
            ">" + "\n" +
        "</web-app>";

    protected static String webAppSchemaOnly40 =
        "<web-app" +
            " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd\"" +
            ">" + "\n" +
        "</web-app>";

    protected static String webAppSchemaOnly50 =
        "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
            " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd\"" +
            ">" + "\n" +
        "</web-app>";
    
    //

    // A schema is no longer required.
    @Test
    public void testEE6Web30NoSchema() throws Exception {
        parse(noSchemaWebApp30);
    }

    // A schema instance is still required.
    @Test
    public void testEE6Web30NoSchemaInstance() throws Exception {
        parse(noSchemaInstanceWebApp30, "xml.error"); 
    }

    // A schema location is not required.
    @Test
    public void testEE6Web30NoSchemaLocation() throws Exception {
        parse(noSchemaLocationWebApp30); 
    }    

    // A version is not required.
    public void testEE6Web30NoVersion() throws Exception {
        parse(noVersionWebApp30); 
    }

    // An ID is not required.
    @Test
    public void testEE6Web30NoID() throws Exception {
        parse(noIDWebApp30);
    }
    
    //
    
    @Test
    public void testWeb24VersionOnly() throws Exception {
        parse(webAppVersionOnly24);
    }

    @Test
    public void testWeb25VersionOnly() throws Exception {
        parse(webAppVersionOnly25);
    }
    
    @Test
    public void testWeb30VersionOnly() throws Exception {
        parse(webAppVersionOnly30);
    }
    
    @Test
    public void testWeb31VersionOnly() throws Exception {
        parse(webAppVersionOnly31, WebApp.VERSION_5_0);
    }
    
    @Test
    public void testWeb40VersionOnly() throws Exception {
        parse(webAppVersionOnly40, WebApp.VERSION_5_0);
    }
    
    @Test
    public void testWeb50VersionOnly() throws Exception {
        parse(webAppVersionOnly50, WebApp.VERSION_5_0);
    }    
    
    //
    
    @Test
    public void testWeb24SchemaOnly() throws Exception {
        parse(webAppSchemaOnly24);
    }
    
    @Test
    public void testWeb25SchemaOnly() throws Exception {
        parse(webAppSchemaOnly25);
    }
    
    @Test
    public void testWeb30SchemaOnly() throws Exception {
        parse(webAppSchemaOnly30);
    }
    
    @Test
    public void testWeb31SchemaOnly() throws Exception {
        parse(webAppSchemaOnly31, WebApp.VERSION_5_0);
    }
    
    @Test
    public void testWeb40SchemaOnly() throws Exception {
        parse(webAppSchemaOnly40, WebApp.VERSION_5_0);
    }
    
    public void testWeb50SchemaOnly() throws Exception {
        parse(webAppVersionOnly50, WebApp.VERSION_5_0);
    }        
}
