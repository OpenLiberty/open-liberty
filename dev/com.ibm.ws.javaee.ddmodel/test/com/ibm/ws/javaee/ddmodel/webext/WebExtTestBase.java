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

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.webext.WebExt;
import com.ibm.ws.javaee.ddmodel.web.WebAppTestBase;

public class WebExtTestBase extends WebAppTestBase {
    protected boolean isWarModule = false;

    WebExt parseWebExtXML(final String xml) throws Exception {
        return parse(xml, new WebExtAdapter(), WebExt.XML_EXT_NAME, WebApp.class, null);
    }

    WebExt parseWebExtXMI(final String xml, final WebApp webApp) throws Exception {
        return parse(xml, new WebExtAdapter(), WebExt.XMI_EXT_NAME, WebApp.class, webApp);
    }

    static final String webAppExtension(String attrs) {
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

    static final String webExt10() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <web-ext" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_0.xsd\"" +
               " version=\"1.0\"" +
               ">";
    }

    static final String webExt11() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <web-ext" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-ext_1_1.xsd\"" +
               " version=\"1.1\"" +
               ">";
    }

}
