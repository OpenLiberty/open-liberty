/*******************************************************************************
 * Copyright (c) 2013, 2023 IBM Corporation and others.
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
package com.ibm.ws.javaee.ddmodel.web;

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.version.ServletVersion;

public class WebAppTestBase extends DDTestBase {

    protected static WebAppEntryAdapter createWebAppAdapter(int maxVersion) {
        @SuppressWarnings("unchecked")
        ServiceReference<ServletVersion> versionRef =
            mockery.mock(ServiceReference.class, "sr" + mockId++);

        mockery.checking(new Expectations() {
            {
                allowing(versionRef).getProperty("version");
                will(returnValue(maxVersion));
            }
        });

        WebAppEntryAdapter ddAdapter = new WebAppEntryAdapter();
        ddAdapter.setVersion(versionRef);
        
        return ddAdapter;
    }

    protected static WebModuleInfo createWebModuleInfo() {
        return mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++);
    }
    
    protected WebApp parseWebApp(String ddText) throws Exception {
        return parseWebApp(ddText, WebApp.VERSION_3_0, null);
    }

    protected WebApp parseWebApp(String ddText, String altMessage, String... messages)
            throws Exception {
        return parseWebApp(ddText, WebApp.VERSION_3_0, altMessage, messages);
    }

    protected WebApp parseWebApp(String ddText, int maxSchemaVersion)
            throws Exception {
        return parseWebApp(ddText, maxSchemaVersion, null);
    }

    protected WebApp parseWebApp(
            String ddText, int maxSchemaVersion,
            String altMessage, String... messages) throws Exception {

        String appPath = null;
        String modulePath = "/root/wlp/usr/servers/server1/apps/MyWar.war";
        String fragmentPath = null;

        return parse(appPath, modulePath, fragmentPath,
                ddText, createWebAppAdapter(maxSchemaVersion), WebApp.DD_NAME,
                null, null,
                WebModuleInfo.class, createWebModuleInfo(),
                altMessage, messages);
    }

    protected static String webApp22Head() {    
        return "<!DOCTYPE web-app PUBLIC" +
                   " \"-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN\"" +
                   " \"http://java.sun.com/j2ee/dtds/web-app_2.2.dtd\"" +
               ">" + '\n' +
                   "<web-app>";
    }

    protected static String webApp23Head() {
        return "<!DOCTYPE web-app PUBLIC" +
                   " \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\"" +
                   " \"http://java.sun.com/dtd/web-app_2_3.dtd\"" +
                   ">" + '\n' +
               "<web-app>";
    }

    protected static String webApp24Head() {
        return "<web-app" +
                   " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\"" +
                   " version=\"2.4\"" +
                   " id=\"WebApp_ID\"" +
               ">";
    }

    protected static String webApp25Head() {
        return "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd\"" +
                   " version=\"2.5\"" +
                   " id=\"WebApp_ID\"" +
               ">";
    }
    
    protected static String webApp30Head() {
        return "<web-app" +
                   " xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
                   " version=\"3.0\"" +
                   " id=\"WebApp_ID\"" +
               ">";
    }
    
    protected static String webApp31Head() {
        return "<web-app" +
                   " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd\"" +
                   " version=\"3.1\"" +
                   " id=\"WebApp_ID\"" +
               ">";
    }

    protected static String webApp40Head() {
        return "<web-app" +
                   " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd\"" +
                   " version=\"4.0\"" +
                   " id=\"WebApp_ID\"" +
               ">";
    }

    protected static String webApp50Head() {
        return "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd\"" +
                   " version=\"5.0\"" +
                   " id=\"WebApp_ID\"" +
               ">";
    }

    protected static String webApp60Head() {
        return "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd\"" +
                   " version=\"6.0\"" +
                   " id=\"WebApp_ID\"" +
               ">";
    }

    protected static String webApp61Head() {
        return "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_1.xsd\"" +
                   " version=\"6.1\"" +
                   " id=\"WebApp_ID\"" +
               ">";
    }

    protected static String webAppTail() {
        return "</web-app>";
    }

    protected static String webAppBody() {
        return "<display-name>Bindings and Extensions Override Test Web Application</display-name>" + '\n' +
               "<description>Used to run override tests for various bindings and extensions</description>" + '\n' +
               "<servlet id=\"Default\">" + '\n' +
               "<description>Print's to log... that's it!.</description>" + '\n' +
               "<servlet-name>Auto Servlet</servlet-name>" + '\n' +      
               "<servlet-class>servlettest.web.AutoServlet</servlet-class>" + '\n' +
               "<load-on-startup></load-on-startup>" + '\n' +
               "</servlet>" + '\n' +
               "<servlet-mapping id=\"ServletMapping_Default\">" + '\n' +
                   "<servlet-name>Auto Servlet</servlet-name>" + '\n' +
                   "<url-pattern>/*</url-pattern>" + '\n' +
               "</servlet-mapping>";
    }
    
    protected static String webApp(int version) {
        return webApp( version, webAppBody() );
    }

    protected static String webApp(int version, String body) {
        String head;
        if ( version == WebApp.VERSION_2_2 ) {
            head = webApp22Head();
        } else if ( version == WebApp.VERSION_2_3 ) {
            head = webApp23Head();
        } else if ( version == WebApp.VERSION_2_4 ) {
            head = webApp24Head();
        } else if ( version == WebApp.VERSION_2_5 ) {
            head = webApp25Head();
        } else if ( version == WebApp.VERSION_3_0 ) {
            head = webApp30Head();
        } else if ( version == WebApp.VERSION_3_1 ) {
            head = webApp31Head();
        } else if ( version == WebApp.VERSION_4_0 ) {
            head = webApp40Head();
        } else if ( version == WebApp.VERSION_5_0 ) {
            head = webApp50Head();
        } else if ( version == WebApp.VERSION_6_0 ) {
            head = webApp60Head();
        } else if ( version == WebApp.VERSION_6_1 ) {
            head = webApp61Head();
        } else {
            throw new IllegalArgumentException("Unexpected WebVersion [ " + version + " ]");
        }
        return head + '\n' + body + '\n' + webAppTail();
    }
    
    //

    private WebApp webApp24;

    protected WebApp getWebApp24() throws Exception {
        if ( webApp24 == null ) {
            webApp24 = parseWebApp(
                    webApp24Head() + '\n' +
                        webAppBody() + '\n' +
                    "</web-app>" );
        }
        return webApp24;
    }    
}
