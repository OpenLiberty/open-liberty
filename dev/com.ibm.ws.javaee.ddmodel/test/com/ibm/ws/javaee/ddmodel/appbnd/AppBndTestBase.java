/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.appbnd;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.appbnd.ApplicationBnd;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.ddmodel.app.ApplicationAdapter;

public class AppBndTestBase extends DDTestBase {
    ApplicationBnd parse(final String xml) throws Exception {
        return parse(xml, new ApplicationBndAdapter(), ApplicationBnd.XML_BND_NAME, Application.class, null);
    }

    Application parseApplication(final String xml) throws Exception {
        return parse(xml, new ApplicationAdapter(), Application.DD_NAME);
    }

    ApplicationBnd parseApplicationBinding(final String xml, final Application app) throws Exception {
        return parse(xml, new ApplicationBndAdapter(), ApplicationBnd.XMI_BND_NAME, Application.class, app);
    }

    static String application14() {
        return "<application" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/application_1_4.xsd\"" +
               " version=\"1.4\"" +
               " id=\"Application_ID\"" +
               ">";
    }

    static String applicationBinding(String attrs) {
        return "<applicationbnd:ApplicationBinding" +
               " xmlns:applicationbnd=\"applicationbnd.xmi\"" +
               " xmlns:commonbnd=\"commonbnd.xmi\"" +
               " xmlns:xmi=\"http://www.omg.org/XMI\"" +
               " xmi:version=\"2.0\"" +
               " " + attrs +
               ">" +
               "<application href=\"META-INF/application.xml#Application_ID\"/>";
    }

    static final String appBnd10() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <application-bnd" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_0.xsd\"" +
               " version=\"1.0\"" +
               ">";
    }

    static final String appBnd11() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <application-bnd" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_1.xsd\"" +
               " version=\"1.1\"" +
               ">";
    }

    static final String appBnd12() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               " <application-bnd" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_2.xsd\"" +
               " version=\"1.2\"" +
               ">";
    }

}
