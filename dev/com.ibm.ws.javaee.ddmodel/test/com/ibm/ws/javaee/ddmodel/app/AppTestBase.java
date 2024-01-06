/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.version.JavaEEVersion;

public class AppTestBase extends DDTestBase {
    protected static ApplicationAdapter createAppAdapter(int maxSchemaVersion) {
        @SuppressWarnings("unchecked")
        ServiceReference<JavaEEVersion> versionRef =
            mockery.mock(ServiceReference.class, "sr" + mockId++);
        String versionText = getDottedVersionText(maxSchemaVersion);
        mockery.checking(new Expectations() {
            {
                allowing(versionRef).getProperty("version");
                will(returnValue(versionText));
            }
        });

        ApplicationAdapter ddAdapter = new ApplicationAdapter();
        ddAdapter.setVersion(versionRef);

        return ddAdapter;
    }

    protected static Application parseApp(String ddText) throws Exception {
        return parseApp(ddText, Application.VERSION_7, null);
    }
    
    protected static Application parseApp(String ddText, int maxSchemaVersion) throws Exception {
        return parseApp(ddText, maxSchemaVersion, null);
    }

    protected static Application parseApp(String ddText, int maxSchemaVersion, String altMessage, String... messages) throws Exception {
        String appPath = null;
        String modulePath = "/root/wlp/usr/servers/server1/apps/myEAR.ear";
        String fragmentPath = null;
        String ddPath = Application.DD_NAME;

        ApplicationAdapter ddAdapter = createAppAdapter(maxSchemaVersion);
        
        return parse(appPath, modulePath, fragmentPath,
                     ddText, ddAdapter, ddPath,
                     altMessage, messages);
    }

    protected static String app12Head =
        "<!DOCTYPE application PUBLIC" +
                " \"-//Sun Microsystems, Inc.//DTD J2EE Application 1.2//EN\"" +
                " \"http://java.sun.com/j2ee/dtds/application_1_2.dtd\">" +
        "<application>";

    protected static String app13Head = 
        "<!DOCTYPE application PUBLIC" +
               " \"-//Sun Microsystems, Inc.//DTD J2EE Application 1.3//EN\"" +
               " \"http://java.sun.com/j2ee/dtds/application_1_3.dtd\">" +
        "<application>";

    //
    
    protected static String app14Head = 
        "<application" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee" +
               " http://java.sun.com/xml/ns/j2ee/application_1_4.xsd\"" +
               " version=\"1.4\"" +
               " id=\"Application_ID\"" +
               ">";

    protected static String app50Head = 
        "<application" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
               " http://java.sun.com/xml/ns/javaee/application_5.xsd\"" +
               " version=\"5\"" +
               " id=\"Application_ID\"" +
               ">";

    protected static String app60Head =
        "<application" +
               " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
               " http://java.sun.com/xml/ns/javaee/application_6.xsd\"" +
               " version=\"6\"" +
               " id=\"Application_ID\"" +
               ">";

    protected static String app70Head =
        "<application" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
               " http://xmlns.jcp.org/xml/ns/javaee/application_7.xsd\"" +
               " version=\"7\"" +
               " id=\"Application_ID\"" +
               ">";

    protected static String app80Head =
        "<application" +
               " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" +
               " http://xmlns.jcp.org/xml/ns/javaee/application_8.xsd\"" +
               " version=\"8\"" +
               " id=\"Application_ID\"" +
               ">";

    protected static String app90Head =
        "<application" +
               " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee" +
               " https://jakarta.ee/xml/ns/jakartaee/application_9.xsd\"" +
               " version=\"9\"" +
               " id=\"Application_ID\"" +
               ">";

    protected static String app100Head =
            "<application" +
                   " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee" +
                   " https://jakarta.ee/xml/ns/jakartaee/application_10.xsd\"" +
                   " version=\"10\"" +
                   " id=\"Application_ID\"" +
                   ">";
    
    protected static String app110Head =
            "<application" +
                   " xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee" +
                   " https://jakarta.ee/xml/ns/jakartaee/application_11.xsd\"" +
                   " version=\"11\"" +
                   " id=\"Application_ID\"" +
                   ">";

    protected static String appBody =
            "<display-name>Deployment Descriptor FAT Enterprise Application</display-name>\n" +

            "<module>\n" +
                "<web>\n" +
                    "<web-uri>,ServletTest.war</web-uri>\n" +
                    "<context-root>,autoctx</context-root>\n" +
                "</web>\n" +
            "</module>\n" +

            "<module>\n" +
                "<web>\n" +
                    "<web-uri>ServletTestNoBnd.war</web-uri>\n" +
                    "<context-root>nobindings</context-root>\n" +
                "</web>\n" +
            "</module>\n" +

            "<module>\n" +
                "<ejb>EJBTest.jar</ejb>\n" +
            "</module>\n" +

            "<module>\n" +
                "<ejb>EJBTestNoBnd.jar</ejb>\n" +
            "</module>\n";    

    protected static String appTail =
        "</application>";

    //

    protected static String app(int schemaVersion, String appBody) {
        String appHead;
        
        if ( schemaVersion == VERSION_1_2_INT ) {
            appHead = app12Head;
        } else if ( schemaVersion == VERSION_1_3_INT ) {
            appHead = app13Head;
        } else if ( schemaVersion == VERSION_1_4_INT ) {
            appHead = app14Head;
        } else if ( schemaVersion == VERSION_5_0_INT ) {
            appHead = app50Head;
        } else if ( schemaVersion == VERSION_6_0_INT ) {
            appHead = app60Head;
        } else if ( schemaVersion == VERSION_7_0_INT ) {
            appHead = app70Head;
        } else if ( schemaVersion == VERSION_8_0_INT ) {
            appHead = app80Head;
        } else if ( schemaVersion == VERSION_9_0_INT ) {
            appHead = app90Head;
        } else if ( schemaVersion == VERSION_10_0_INT ) {
            appHead = app100Head;
        } else if ( schemaVersion == VERSION_11_0_INT ) {
            appHead = app110Head; 
        } else {
            throw new IllegalArgumentException("Unknown application version [ " + schemaVersion + " ]");
        }
        
        return appHead + "\n" +
               appBody + "\n" +
               appTail;
    }
    
    private Application app14;
    
    public Application app14() throws Exception {
        if ( app14 == null ) {
            app14 = parseApp( app(Application.VERSION_1_4, appBody), Application.VERSION_7 );
        }
        return app14;
    }    
}
