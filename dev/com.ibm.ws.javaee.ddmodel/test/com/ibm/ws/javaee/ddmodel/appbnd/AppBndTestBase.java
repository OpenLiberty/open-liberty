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
import com.ibm.ws.javaee.ddmodel.app.AppTestBase;

public class AppBndTestBase extends AppTestBase {
    protected static ApplicationBndAdapter createAppBndAdapter() {
        return new ApplicationBndAdapter();
    }
    
    protected static ApplicationBnd parseAppBndXMI(String ddText, Application app) throws Exception {
        return parseAppBndXMI(ddText, app, null);
    }

    protected static ApplicationBnd parseAppBndXMI(
            String ddText,
            Application app,
            String altMessage, String... messages) throws Exception {

        String ddPath = ApplicationBnd.XMI_BND_NAME;         

        return parseAppBnd(ddText, ddPath, app, altMessage, messages);
    }
    
    protected static ApplicationBnd parseAppBndXML(String ddText) throws Exception {
            return parseAppBndXML(ddText, null);
    }

    protected static ApplicationBnd parseAppBndXML(
            String ddText,
            String altMessage, String... messages) throws Exception {

        String ddPath = ApplicationBnd.XML_BND_NAME; 
        
        return parseAppBnd(ddText, ddPath, null, altMessage, messages);
    }    

    protected static ApplicationBnd parseAppBnd(
            String ddText, String ddPath,
            Application app,
            String altMessage, String... messages) throws Exception {

        String appPath = null;
        String modulePath = "/root/wlp/usr/servers/server1/apps/myEAR.ear";
        String fragmentPath = null;

        return parse(
                appPath, modulePath, fragmentPath,
                ddText, createAppBndAdapter(), ddPath,
                Application.class, app,
                altMessage, messages);
    }

    //

    public static final String appBndXMI(String attrs, String body) {
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
    
    public static final String appBndTail =
            "</application-bnd>";
    
    public final String appBnd10() {
        return appBnd10("");
    }
    
    public final String appBnd10(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<application-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_0.xsd\"" +
                   " version=\"1.0\"" +
               ">" + "\n" +
                   body + "\n" +
               appBndTail;
    }
    
    public final String appBnd11() {
        return appBnd11("");
    }
    
    public final String appBnd11(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<application-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_1.xsd\"" +
                   " version=\"1.1\"" +
               ">" + "\n" +
                   body + "\n" +
               appBndTail;
    }

    public final String appBnd12() {
        return appBnd12("");
    }

    public final String appBnd12(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<application-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-bnd_1_2.xsd\"" +
                   " version=\"1.2\"" +
               ">" + "\n" +
                   body + "\n" +
               appBndTail;
    }
}
