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

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.dd.client.ApplicationClient;
import com.ibm.ws.javaee.ddmodel.DDTestBase;

public class AppClientTestBase extends DDTestBase {

    protected static ApplicationClientEntryAdapter createAdapter(int maxSchemaVersion) {
        @SuppressWarnings("unchecked")
        ServiceReference<ApplicationClientDDParserVersion> versionRef =
        mockery.mock(ServiceReference.class, "sr" + mockId++);

        mockery.checking(new Expectations() {
            {        
                allowing(versionRef).getProperty(ApplicationClientDDParserVersion.VERSION);
                will(returnValue(maxSchemaVersion));
            }
        });

        ApplicationClientEntryAdapter adapter = new ApplicationClientEntryAdapter();
        adapter.setVersion(versionRef);
        
        return adapter;
    }
    
    protected ApplicationClient parse(
            String ddText,
            int maxSchemaVersion) throws Exception {
        return parse(ddText, maxSchemaVersion, null);
    }

    protected static ApplicationClient parse(
        String ddText,
        int maxSchemaVersion,
        String altMessage, String... messages) throws Exception {

        String appPath = null;
        String modulePath = "/root/wlp/usr/servers/server1/apps/myClient.jar";
        String fragmentPath = null;
        String ddPath = ApplicationClient.DD_NAME;

        ApplicationClientEntryAdapter ddAdapter = createAdapter(maxSchemaVersion);

        return parse(appPath, modulePath, fragmentPath,
                ddText, ddAdapter, ddPath,
                altMessage, messages);
    }

    protected static String appClient12Head =
        "<!DOCTYPE application-client PUBLIC" +
               " \"-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.2//EN\"" + 
               " \"http://java.sun.com/j2ee/dtds/application_client_1_2.dtd\">" +
               "<application-client>";

    protected static String appClient13Head =
        "<!DOCTYPE application-client PUBLIC" +
                " \"-//Sun Microsystems, Inc.//DTD J2EE Application Client 1.3//EN\"" + 
                " \"http://java.sun.com/j2ee/dtds/application_client_1_3.dtd\">" +
                "<application-client>";

    protected static String appClient14Head =
        "<application-client" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
               " http://java.sun.com/xml/ns/javaee/application-client_1_4.xsd\"" +
               " version=\"1.4\"" +
               " id=\"ApplicationClient_ID\"" +
               ">";

    protected static String appClient50Head = 
        "<application-client" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                  " http://java.sun.com/xml/ns/javaee/application-client_5.xsd\"" +
                " version=\"5\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";

    protected static String appClient60Head =
        "<application-client" +
                " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                  " http://java.sun.com/xml/ns/javaee/application-client_6.xsd\"" +
                " version=\"6\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";

    protected static String appClient70Head =
        "<application-client" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
                  " http://xmlns.jcp.org/xml/ns/javaee/application-client_7.xsd\"" +
                " version=\"7\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";

    protected static String appClient80Head = 
        "<application-client" +
                " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
                  " http://xmlns.jcp.org/xml/ns/javaee/application-client_8.xsd\"" +
                " version=\"8\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";

    protected static String appClient90Head =
        "<application-client" +
                " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee " +
                  " https://jakarta.ee/xml/ns/jakartaee/application-client_9.xsd\"" +
                " version=\"9\"" +
                " id=\"ApplicationClient_ID\"" +
                ">";

    protected static String appClientTail =
        "</application-client>";
    
    protected static String appClientXML(int schemaVersion, String body) {
        String appClientHead;
        if ( schemaVersion == ApplicationClient.VERSION_1_2 ) {
            appClientHead = appClient12Head;
        } else if ( schemaVersion == ApplicationClient.VERSION_1_3) {
            appClientHead = appClient13Head;
        } else if ( schemaVersion == ApplicationClient.VERSION_1_4 ) {
            appClientHead = appClient14Head;
        } else if ( schemaVersion == ApplicationClient.VERSION_5) {
            appClientHead = appClient50Head;
        } else if ( schemaVersion == ApplicationClient.VERSION_6 ) {
            appClientHead = appClient60Head;            
        } else if ( schemaVersion == ApplicationClient.VERSION_7 ) {
            appClientHead = appClient70Head;            
        } else if ( schemaVersion == ApplicationClient.VERSION_8 ) {
            appClientHead = appClient80Head;
        } else if ( schemaVersion == ApplicationClient.VERSION_9 ) {
            appClientHead = appClient90Head;
        } else {
            throw new IllegalArgumentException("Unsupported application client version [ " + schemaVersion + " ]");
        }

        return appClientHead + body + appClientTail;
    }
}
