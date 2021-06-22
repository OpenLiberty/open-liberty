/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.webext;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.ws.javaee.ddmodel.web.WebAppTestBase;

public class WebAppExtTestBase extends WebAppTestBase {
    protected static WebExtAdapter createWebExtAdapter() {
        return new WebExtAdapter();
    }
    
    protected WebExt parseWebAppExtXMI(String ddText, WebApp webApp) throws Exception {
        return parseAppExt(ddText, WebExt.XMI_EXT_NAME, webApp, null);
    }

    protected WebExt parseWebAppExtXMI(String ddText, WebApp webApp, String altMessage, String... messages) throws Exception {
        return parseAppExt(ddText, WebExt.XMI_EXT_NAME, webApp, altMessage, messages);
    }

    protected WebExt parseWebAppExtXML(String ddText) throws Exception {
        return parseAppExt(ddText, WebExt.XML_EXT_NAME, null, null);
    }
    
    protected WebExt parseWebAppExtXML(String ddText, String altMessage, String... messages) throws Exception {
        return parseAppExt(ddText, WebExt.XML_EXT_NAME, null, altMessage, messages);
    }    
    
    protected WebExt parseAppExt(String ddText, String ddPath, WebApp webApp) throws Exception {
        return parseAppExt(ddText, ddPath, webApp, null);
    }

    private WebExt parseAppExt(
            String ddText, String ddPath, WebApp webApp,
            String altMessage, String... messages) throws Exception {

        String appPath = null;
        String modulePath = "/root/wlp/usr/servers/server1/apps/MyWar.war";
        String fragmentPath = null;

        WebModuleInfo webInfo = mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++);

        return parse(
                appPath, modulePath, fragmentPath,
                ddText, createWebExtAdapter(), ddPath,
                WebApp.class, webApp,
                WebModuleInfo.class, webInfo,
                altMessage, messages);        
    }

    //

    protected static final String webAppExtension(String attrs) {
        return "<webappext:WebAppExtension" +
                   " xmlns:webappext=\"webappext.xmi\"" +
                   " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                   " xmlns:webapplication=\"webapplication.xmi\" " +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xmi:version=\"2.0\" " +
                   attrs +
               ">" +
               "<webApp href=\"WEB-INF/web.xml#WebApp_ID\"/>";
    }

    protected static final String webExt10() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<web-ext" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_0.xsd\"" +
                   " version=\"1.0\"" +
               ">";
    }

    protected static final String webExt11() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<web-ext" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_1.xsd\"" +
                   " version=\"1.1\"" +
               ">";
    }
}
