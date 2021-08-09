/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.commonbnd;

import com.ibm.ws.javaee.dd.web.WebApp;
import com.ibm.ws.javaee.dd.webbnd.WebBnd;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.ddmodel.web.WebAppEntryAdapter;
import com.ibm.ws.javaee.ddmodel.webbnd.WebBndAdapter;

public class RefBindingsGroupTestBase extends DDTestBase {
    protected boolean isWarModule = false;

    WebBnd parse(final String xml) throws Exception {
        return parse(xml, new WebBndAdapter(), WebBnd.XML_BND_NAME, WebApp.class, null);
    }

    WebApp parseWebApp(final String xml) throws Exception {
        return parse(xml, new WebAppEntryAdapter(), WebApp.DD_NAME);
    }

    WebBnd parseWebAppBinding(final String xml, final WebApp webApp) throws Exception {
        return parse(xml, new WebBndAdapter(), WebBnd.XMI_BND_NAME, WebApp.class, webApp);
    }

    static final String webApp24() {
        return "<web-app" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd\"" +
               " version=\"2.4\"" +
               " id=\"WebApp_ID\"" +
               ">";
    }

    static final String webAppBinding(String attrs) {
        return "<webappbnd:WebAppBinding" +
               " xmlns:webappbnd=\"webappbnd.xmi\"" +
               " xmlns:xmi=\"http://www.omg.org/XMI\"" +
               " xmlns:commonbnd=\"commonbnd.xmi\"" +
               " xmi:version=\"2.0\" " +
               attrs +
               ">" +
               "<webapp href=\"WEB-INF/web.xml#WebApp_ID\"/>";
    }

    static final String webBnd12() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <web-bnd" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-web-bnd_1_2.xsd\"" +
               " version=\"1.2\"" +
               ">";
    }
}
