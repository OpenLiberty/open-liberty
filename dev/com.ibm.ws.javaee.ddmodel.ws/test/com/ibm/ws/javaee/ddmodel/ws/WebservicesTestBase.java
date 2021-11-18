/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmodel.ws;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.container.service.app.deploy.EJBModuleInfo;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.ws.Webservices;
    
public class WebservicesTestBase extends DDTestBase_Webservices {

    protected static final boolean IS_WAR = true;

    protected static final List<Boolean[]> WEBSERVICES_TEST_DATA;
    
    static {
        List<Boolean[]> testData = new ArrayList<Boolean[]>(2);
        testData.add( new Boolean[] { !IS_WAR});
        testData.add( new Boolean[] { IS_WAR });
        WEBSERVICES_TEST_DATA = testData;
    }

    protected WebservicesTestBase(boolean isWar) {
        this.isWar = isWar;
    }
        
    private final boolean isWar;
    
    public boolean getIsWar() {
        return isWar;
    }
    
    public String getDDPath() {
        return ( getIsWar() ? Webservices.WEB_DD_NAME : Webservices.EJB_DD_NAME );
    }

    //

    protected WebservicesAdapter createWebservicesAdapter() {
        WebservicesAdapter ddAdapter = new WebservicesAdapter();
        return ddAdapter;
    }

    protected Webservices parseWebservices(String ddText) throws Exception {
        return parseWebservices(ddText, null);
    }

    protected Webservices parseWebservices(
            String ddText,
            String altMessage, String... messages) throws Exception {

        String appPath = "/root/wlp/usr/servers/server1/apps/MyEar.ear";        
        String modulePath = ( getIsWar() ? "MyWar.war" : "MyEjbJar.jar" );  
        String fragmentPath = null;
        String ddPath = getDDPath();

        // Webservices parsing requires WebModuleInfo or EJBModuleInfo.

        Class<?> moduleInfoClass;
        ModuleInfo moduleInfo;
        if ( getIsWar() ) {
            moduleInfoClass = WebModuleInfo.class;
            moduleInfo = mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++);
        } else {
            moduleInfoClass = EJBModuleInfo.class;
            moduleInfo = mockery.mock(EJBModuleInfo.class, "ejbModuleInfo" + mockId++);            
        }

        return parse(appPath, modulePath, fragmentPath,
                ddText, createWebservicesAdapter(), ddPath,
                null, null,
                moduleInfoClass, moduleInfo,
                altMessage, messages);
    }

    //

    // http://www.oracle.com/webfolder/technetwork/jsc/xml/ns/j2ee/j2ee_web_services_1_1.xsd
    
    protected static String webservices11Head() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<webservices xmlns=\"http://java.sun.com/xml/ns/j2ee\"" + "\n" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee" + "\n" +
                       " http://www.ibm.com/webservices/xsd/j2ee_web_services_1_1.xsd\"" + "\n" +
                   " version=\"1.1\"" +
               ">";
    }

    // http://www.oracle.com/webfolder/technetwork/jsc/xml/ns/javaee/javaee_web_services_1_2.xsd
    
    protected static String webservices12Head() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<webservices" + "\n" +
                   " xmlns=\"http://java.sun.com/xml/ns/javaee\"" + "\n" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" + "\n" +
                       " http://java.sun.com/xml/ns/javaee/javaee_web_services_1_2.xsd\"" + "\n" +
                   " version=\"1.2\"" +
               ">";
    }
    
    // http://www.oracle.com/webfolder/technetwork/jsc/xml/ns/javaee/javaee_web_services_1_3.xsd

    protected static String webservices13Head() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
                "<webservices" + "\n" +
                    " xmlns=\"http://java.sun.com/xml/ns/javaee\"" + "\n" +
                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                    " xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" + "\n" +
                        " http://java.sun.com/xml/ns/javaee/javaee_web_services_1_3.xsd\"" + "\n" +
                    " version=\"1.3\"" +
                ">";
    }

    // http://www.oracle.com/webfolder/technetwork/jsc/xml/ns/javaee/javaee_web_services_1_4.xsd
    
    protected static String webservices14Head() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
                "<webservices" + "\n" +
                    " xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\"" + "\n" +
                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                    " xsi:schemaLocation=\"http://xmlns.jcp.org/xml/ns/javaee" + "\n" +
                        " http://xmlns.jcp.org/xml/ns/javaee/javaee_web_services_1_4.xsd\"" + "\n" +
                    " version=\"1.4\"" +
                ">";
    }

    // https://jakarta.ee/xml/ns/jakartaee/jakartaee_web_services_2_0.xsd
    
    protected static String webservices20Head() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "\n" +
               "<webservices xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"" + "\n" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                   " xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee" + "\n" +
                       " https://jakarta.ee/xml/ns/jakartaee/jakartaee_web_services_2_0.xsd\"" + "\n" +
                   " version=\"2.0\"" +
               ">";
    }

    protected static String webservicesTail() {
        return "</webservices>";
    }

    protected static String webservicesBody() {
        return "<webservice-description>" + "\n" +
                   "<webservice-description-name>HelloService</webservice-description-name>" + "\n" +
                   "<wsdl-file>WEB-INF/wsdl/HelloService.wsdl</wsdl-file>" + "\n" +
                   "<port-component>" + "\n" +
                       "<port-component-name>Hello</port-component-name>" + "\n" +
                       "<service-impl-bean>" + "\n" +
                           "<servlet-link>com.ibm.ws.jaxws.test.endpoint.properties.server.Hello</servlet-link>" + "\n" +
                       "</service-impl-bean>" + "\n" +
                   "</port-component>" + "\n" +
               "</webservice-description>";
    }
    
    protected static String webservices(int version) {
        return webservices( version, webservicesBody() );
    }

    protected static String webservices(int version, String body) {
        String head;
        if ( version == Webservices.VERSION_1_1 ) {
            head = webservices11Head();
        } else if ( version == Webservices.VERSION_1_2 ) {
            head = webservices12Head();
        } else if ( version == Webservices.VERSION_1_3 ) {
            head = webservices13Head();
        } else if ( version == Webservices.VERSION_1_4 ) {
            head = webservices14Head();
        } else if ( version == Webservices.VERSION_2_0 ) {
            head = webservices20Head();
        } else {
            throw new IllegalArgumentException("Unexpected Webservices Version [ " + version + " ]");
        }
        return head + "\n" + body + "\n" + webservicesTail();
    }
    
    //

    private Webservices webservices14;

    protected Webservices getWebservices14() throws Exception {
        if ( webservices14 == null ) {
            webservices14 = parseWebservices(
                    webservices14Head() + "\n" +
                        webservicesBody() + "\n" +
                    "</web-app>" );
        }
        return webservices14;
    }    
}
