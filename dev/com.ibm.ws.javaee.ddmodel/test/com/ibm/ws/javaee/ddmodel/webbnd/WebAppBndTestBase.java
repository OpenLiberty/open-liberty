/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.webbnd;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;
import com.ibm.ws.javaee.ddmodel.web.WebAppTestBase;

public class WebAppBndTestBase extends WebAppTestBase {
    protected static WebBndAdapter createWebBndAdapter() {
        return new WebBndAdapter();
    }
    
    protected WebBnd parseWebAppBndXMI(String ddText, WebApp webApp) throws Exception {
        return parseAppBnd(ddText, WebBnd.XMI_BND_NAME, webApp, null);
    }

    protected WebBnd parseWebAppBndXMI(String ddText, WebApp webApp, String altMessage, String... messages) throws Exception {
        return parseAppBnd(ddText, WebBnd.XMI_BND_NAME, webApp, altMessage, messages);
    }

    protected WebBnd parseWebAppBndXML(String ddText) throws Exception {
        return parseAppBnd(ddText, WebBnd.XML_BND_NAME, null, null);
    }
    
    protected WebBnd parseWebAppBndXML(String ddText, String altMessage, String... messages) throws Exception {
        return parseAppBnd(ddText, WebBnd.XML_BND_NAME, null, altMessage, messages);
    }    
    
    protected WebBnd parseAppBnd(String ddText, String ddPath, WebApp webApp) throws Exception {
        return parseAppBnd(ddText, ddPath, webApp, null);
    }

    private WebBnd parseAppBnd(
            String ddText, String ddPath, WebApp webApp,
            String altMessage, String... messages) throws Exception {

        String appPath = null;
        String modulePath = "/root/wlp/usr/servers/server1/apps/MyWar.war";
        String fragmentPath = null;

        WebModuleInfo webInfo = mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++);

        return parse(
                appPath, modulePath, fragmentPath,
                ddText, createWebBndAdapter(), ddPath,
                WebApp.class, webApp,
                WebModuleInfo.class, webInfo,
                altMessage, messages);        
    }

    protected static String webAppBinding(String attrs) {
        return "<webappbnd:WebAppBinding" +
               " xmlns:webappbnd=\"webappbnd.xmi\"" +
               " xmlns:xmi=\"http://www.omg.org/XMI\"" +
               " xmlns:webapplication=\"webapplication.xmi\" " +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xmi:version=\"2.0\" " +
               attrs +
               ">" +
               "<webapp href=\"WEB-INF/web.xml#WebApp_ID\"/>";
    }

    protected static String webBnd10() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <web-bnd" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_0.xsd\"" +
               " version=\"1.0\"" +
               ">";
    }

    protected static String webBnd11() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <web-bnd" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_1.xsd\"" +
               " version=\"1.1\"" +
               ">";
    }

    protected static String webBnd12() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <web-bnd" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_2.xsd\"" +
               " version=\"1.2\"" +
               ">";
    }

    protected static String webAppBnd12() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<web-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_2.xsd\"" +
                   " version=\"1.2\"" +
               ">";
    }    
    
    protected static String webAppBnd20(String attrs) {
        return "<webappbnd:WebAppBinding" +
                   " xmlns:webappbnd=\"webappbnd.xmi\"" +
                   " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                   " xmlns:commonbnd=\"commonbnd.xmi\"" +
                   " xmi:version=\"2.0\" " +
                   attrs +
               ">" +
               "<webapp href=\"WEB-INF/web.xml#WebApp_ID\"/>";
    }
}
