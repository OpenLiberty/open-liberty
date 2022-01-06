/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
    
    protected WebExt parseWebExtXMI(String ddText, WebApp webApp) throws Exception {
        return parseAppExt(ddText, WebExt.XMI_EXT_NAME, webApp, null);
    }

    protected WebExt parseWebExtXMI(String ddText, WebApp webApp, String altMessage, String... messages) throws Exception {
        return parseAppExt(ddText, WebExt.XMI_EXT_NAME, webApp, altMessage, messages);
    }

    protected WebExt parseWebExtXML(String ddText) throws Exception {
        return parseAppExt(ddText, WebExt.XML_EXT_NAME, null, null);
    }
    
    protected WebExt parseWebExtXML(String ddText, String altMessage, String... messages) throws Exception {
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

    protected static final String webExtBodyXMI =
            "<webApp href=\"WEB-INF/web.xml#WebApp_ID\"/>" + "\n" +
            "<jspAttributes xmi:id=\"JSPAttribute_1318820843637\" name=\"jdkSourceLevel\" value=\"15\"/>";
    
    protected static final String webExtTailXMI =
            "</webappext:WebAppExtension>";

    protected static final String webExtXMI20() {
        return webExt20XMI("", webExtBodyXMI);
    }

    protected static final String webExt20XMI(String attrs, String body) {
        return "<webappext:WebAppExtension" +
                   " xmlns:webappext=\"webappext.xmi\"" +
                   " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                   " xmlns:webapplication=\"webapplication.xmi\" " +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xmi:version=\"2.0\" " +
                   attrs +
               ">" +
                   "<webApp href=\"WEB-INF/web.xml#WebApp_ID\"/>" +
                   body +
               webExtTailXMI;
    }

    protected static final String webExtBodyXML =
            "<servlet name=\"Auto Servlet\">" + "\n" +
                "<local-transaction" + "\n" +
                    " boundary=\"BEAN_METHOD\"" + "\n" +
                    " resolver=\"CONTAINER_AT_BOUNDARY\"" + "\n" +
                    " unresolved-action=\"COMMIT\"/>" + "\n" +
            "</servlet>" + "\n" +
            "<reload-interval value=\"3\"/>" + "\n" +
            "<enable-directory-browsing value=\"false\"/>" + "\n" +
            "<enable-file-serving value=\"true\"/>" + "\n" +
            "<enable-reloading value=\"true\"/>" + "\n" +
            "<enable-serving-servlets-by-class-name value=\"true\"/>";
    
    protected static int webExtBodyXml_reloadInterval = 3;
    protected static boolean webExtBody_enableDirectoryBrowsing = false;
    protected static boolean webExtBody_enableFileServing = true;
    protected static boolean webExtBody_enableReloading = true;
    protected static boolean webExtBody_enableServletsByClassName = true;
    
    protected static final String webExtTailXML =
            "</web-ext>";

    protected static final String webExtXML10() {
        return webExt10XML(webExtBodyXML);
    }
    
    protected static final String webExt10XML(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<web-ext" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_0.xsd\"" +
                   " version=\"1.0\"" +
               ">" +
                   body +
               webExtTailXML;
    }

    protected static final String webExtXML11() {
        return webExt11XML(webExtBodyXML);
    }
    
    protected static final String webExt11XML(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<web-ext" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_1.xsd\"" +
                   " version=\"1.1\"" +
               ">" +
                   body +
               webExtTailXML;
    }
}
