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
package com.ibm.ws.javaee.ddmodel.appbnd;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.appbnd.ApplicationBnd;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.ddmodel.app.ApplicationAdapter;

public class AppBndTestBase extends DDTestBase {
    Application parseApplication(String xml) throws Exception {
        return parse(xml, new ApplicationAdapter(), Application.DD_NAME);
    }

    protected static String app14(String body) {
        return "<application" +
                   " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/application_1_4.xsd\"" +
                   " version=\"1.4\"" +
                   " id=\"Application_ID\"" +
               ">" + "\n" +
               body + "\n" +
               "</application>" + "\n";
    }

    //

    ApplicationBnd parseXML(String xml, String... messages) throws Exception {
        return parse(xml,
                new ApplicationBndAdapter(),
                ApplicationBnd.XML_BND_NAME,
                Application.class,
                null,
                messages);
    }

    ApplicationBnd parseXMI(String xml, Application app, String... messages) throws Exception {
        return parse(xml,
                new ApplicationBndAdapter(),
                ApplicationBnd.XMI_BND_NAME,
                Application.class,
                app,
                messages);
    }

    protected static final String appBnd10Head = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_0.xsd\"" +
                " version=\"1.0\"" +
            ">";

    protected static final String appBnd11Head =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_1.xsd\"" +
                " version=\"1.1\"" +
            ">";

    protected static final String appBnd12Head =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
            "<application-bnd" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_2.xsd\"" +
                " version=\"1.2\"" +
            ">";
    
    protected static final String appBndTail =
            "</application-bnd>";

    //

    protected static final String appBndXMI(String attrs, String body) {
        return "<applicationbnd:ApplicationBinding" +
                    " xmlns:applicationbnd=\"applicationbnd.xmi\"" +
                    " xmlns:commonbnd=\"commonbnd.xmi\"" +
                    " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                    " xmi:version=\"2.0\"" +
                    " " + attrs +
                ">" + "\n" +
                    "<application href=\"META-INF/application.xml#Application_ID\"/>" + "\n" +
                    body + "\n" +
                "</applicationbnd:ApplicationBinding>";        
    }                  
}
