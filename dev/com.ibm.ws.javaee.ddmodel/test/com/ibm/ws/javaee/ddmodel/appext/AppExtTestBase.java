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
package com.ibm.ws.javaee.ddmodel.appext;

import com.ibm.ws.javaee.dd.app.Application;
import com.ibm.ws.javaee.dd.appext.ApplicationExt;
import com.ibm.ws.javaee.ddmodel.app.AppTestBase;

public class AppExtTestBase extends AppTestBase {
    protected static ApplicationExtAdapter createAppExtAdapter() {
        return new ApplicationExtAdapter();
    }
    
    protected static ApplicationExt parseAppExtXMI(String ddText, Application app) throws Exception {
        return parseAppExtXMI(ddText, app, null);
    }

    protected static ApplicationExt parseAppExtXMI(
            String ddText,
            Application app,
            String altMessage, String... messages) throws Exception {

        String ddPath = ApplicationExt.XMI_EXT_NAME;         

        return parseAppExt(ddText, ddPath, app, altMessage, messages);
    }
    
    protected static ApplicationExt parseAppExtXML(String ddText) throws Exception {
            return parseAppExtXML(ddText, null);
    }

    protected static ApplicationExt parseAppExtXML(
            String ddText,
            String altMessage, String... messages) throws Exception {

        String ddPath = ApplicationExt.XML_EXT_NAME; 
        
        return parseAppExt(ddText, ddPath, null, altMessage, messages);
    }    

    protected static ApplicationExt parseAppExt(
            String ddText, String ddPath,
            Application app,
            String altMessage, String... messages) throws Exception {

        String appPath = null;
        String modulePath = "/root/wlp/usr/servers/server1/apps/myEAR.ear";
        String fragmentPath = null;

        return parse(
                appPath, modulePath, fragmentPath,
                ddText, createAppExtAdapter(), ddPath,
                Application.class, app,
                altMessage, messages);
    }

    //
    
    public static String appExtXMI() {
        return appExtXMI("", "");
    }

    public static final String appExtTailXMI =
            "</applicationext:ApplicationExtension>";
    
    public static String appExtXMI(String attrs, String body) {
        return "<applicationext:ApplicationExtension" +
                   " xmlns:applicationext=\"applicationext.xmi\"" +
                   " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xmi:version=\"2.0\"" +
                   " " + attrs +
               ">" + "\n" +
                   "<application href=\"META-INF/application.xml#Application_ID\"/>" + "\n" +
                   body + "\n" +
               appExtTailXMI;
    }

    public static final String appExt10() {
        return appExt10("", "");
    }

    public static final String appExtTailXML =
            "</application-ext>";
    
    public static final String appExt10(String attrs, String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<application-ext" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-ext_1_0.xsd\"" +
                   " version=\"1.0\"" +
                   " " + attrs +
               ">" + "\n" +
                   body + "\n" +
               appExtTailXML;
    }

    public static final String appExt11() {
        return appExt11("", "");
    }

    public static final String appExt11(String attrs, String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<application-ext" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-application-ext_1_1.xsd\"" +
                   " version=\"1.1\"" +
                   " " + attrs +
               ">" + "\n" +
                   body + "\n" +
               appExtTailXML;
    }
}
