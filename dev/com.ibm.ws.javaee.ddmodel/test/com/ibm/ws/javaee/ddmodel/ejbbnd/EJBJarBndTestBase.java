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
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd;
import com.ibm.ws.javaee.ddmodel.DDParserBndExt;
import com.ibm.ws.javaee.ddmodel.ejb.EJBJarTestBase;

/**
 * Test the ejb-jar-bnd.xml parser
 * -concentrate on the pristine path where the ejb-jar-bnd.xml file is
 * well formed.
 */
public class EJBJarBndTestBase extends EJBJarTestBase {
    
    public EJBJarBndTestBase(boolean ejbInWar) {
        super(ejbInWar);
    }

    //
    
    protected EJBJarBndAdapter createEJBJarBndAdapter() {
        return new EJBJarBndAdapter();
    }

    public EJBJarBnd parseEJBJarBndXMI(String ddText, EJBJar ejbJar) throws Exception {
        return parseEJBJarBnd(ddText, DDParserBndExt.IS_XMI, ejbJar, null);
    }

    public EJBJarBnd parseEJBJarBndXML(String ddText) throws Exception {
        return parseEJBJarBnd(ddText, !DDParserBndExt.IS_XMI, null, null);
    }
    
    public EJBJarBnd parseEJBJarBndXML(String ddText, String altMessage, String...messages) throws Exception {
        return parseEJBJarBnd(ddText, !DDParserBndExt.IS_XMI, null, altMessage, messages);
    }    

    public EJBJarBnd parseEJBJarBnd(
            String ddText, boolean xmi, EJBJar ejbJar,
            String altMessage, String ... messages) throws Exception {

        boolean ejbInWar = getEJBInWar();

        String appPath = null;

        String modulePath;
        if ( ejbInWar ) {
            modulePath = "/root/wlp/usr/servers/server1/apps/myWEB.war";
        } else {
            modulePath = "/root/wlp/usr/servers/server1/apps/myEJB.jar";
        }

        String fragmentPath = null;

        String ddPath;
        if ( ejbInWar ) {
            ddPath = ( xmi ? EJBJarBndAdapter.XMI_BND_IN_WEB_MOD_NAME : EJBJarBndAdapter.XML_BND_IN_WEB_MOD_NAME );
        } else {
            ddPath = ( xmi ? EJBJarBndAdapter.XMI_BND_IN_EJB_MOD_NAME : EJBJarBndAdapter.XML_BND_IN_EJB_MOD_NAME );            
        }

        WebModuleInfo moduleInfo =
            ( ejbInWar ? mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++) : null );

        return parse(
                appPath, modulePath, fragmentPath,
                ddText, createEJBJarBndAdapter(), ddPath,
                EJBJar.class, ejbJar,
                WebModuleInfo.class, moduleInfo,
                altMessage, messages);
    }

    //

    public String ejbJarBndXMI() {
        return ejbJarBndXMI( "", ejbBndBodyXMI() );
    }

    public String ejbJarBndXMI(String attrs, String body) {
        return ejbJarBndXMI( attrs, getEJBJarPath(), body );
    }

    public static final String ejbJarTailXMI =
            "</ejbbnd:EJBJarBinding>";

    public static String ejbJarBndXMI(String attrs, String ddPath, String body) {
        return "<ejbbnd:EJBJarBinding" +
                   " xmlns:ejbbnd=\"ejbbnd.xmi\"" +
                   " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                   " xmlns:ejb=\"ejb.xmi\"" +
                   " xmi:version=\"2.0\"" +
                   " " + attrs +
                   " xmi:id=\"ejbbnd_1\"" +
               ">" +
                   "<ejbJar href=\"META-INF/" + ddPath + "#EJBJar_ID\"/>" + "\n" +
                   body + "\n" +
               ejbJarTailXMI;
    }

    //
    
    public String ejbBndBodyXMI() {
        return ejbBndBodyXMI( getEJBJarPath() );
    }

    public static final String ejbBndBodyXMI(String ddPath) {
        return "<ejbBindings>" + "\n" +
                    "<enterpriseBean xmi:type=\"ejb:Session\" href=\"" + ddPath + "#s0\"/>" + "\n" +
               "</ejbBindings>";
    }

    //

    public static final String ejbJarTailXML =
            "</ejb-jar-bnd>";

    public static final String ejbBndBodyXML() {
        return "<session name=\"SessionBean1\" simple-binding-name=\"SimpleBindingName2\"/>";
    }
    
    //
    
    public static String ejbJarBnd10() {
        return ejbJarBnd10( ejbBndBodyXML() );
    }

    public static String ejbJarBnd10(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<ejb-jar-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"\n" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee" +
                   " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_0.xsd\"" +
                   " version=\"1.0\">" + "\n" +
                   body + "\n" +
               ejbJarTailXML;
    }

    public static String ejbJarBnd11() {
        return ejbJarBnd11( ejbBndBodyXML() );
    }
    
    public static String ejbJarBnd11(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<ejb-jar-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"\n" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee" +
                       " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_1.xsd\"\n" +
                   " version=\"1.1\">" + "\n" +
                   body + "\n" +
               ejbJarTailXML;
    }

    public static String ejbJarBnd12() {
        return ejbJarBnd12( ejbBndBodyXML() );
    }

    public static String ejbJarBnd12(String body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<ejb-jar-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"\n" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee" +
                       " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_2.xsd\"\n" +
                   " version=\"1.2\">" + "\n" +
                   body + "\n" +
               ejbJarTailXML;
    }

    public static final String sessionXML8 =
            "<session name=\"SessionBean8\">\n" +
                "<resource-ref name=\"resourceRefName8\"\n" +
                "binding-name=\"resourceRefBinding8\"/>\n" +
            "</session>\n";

    public static final String sessionXML11 =
            "<session name=\"SessionBean11\">" +
                "<resource-ref name=\"ResourceRef11\" binding-name=\"ResourceRefBindingName11\">" +
                    "<authentication-alias name=\"AuthAlias11\"/>" +
                    "<custom-login-configuration name=\"customLoginConfiguration11\">" +
                        "<property name=\"propname\" value=\"propvalue\"/>" +
                    "</custom-login-configuration>" +
                "</resource-ref>" +
            "</session>";

    public static final String messageDrivenXML9 =
            "<message-driven name=\"MessageDrivenBean9\">\n" +
                "<jca-adapter activation-spec-binding-name=\"ActivationSpecBindingName9\"/>\n" +
                "<resource-env-ref name=\"ResourceEnvRefName9a\" binding-name=\"ResourceEnvRefBindingName9a\"/>\n" +
                "<resource-env-ref name=\"ResourceEnvRefName9b\" binding-name=\"ResourceEnvRefBindingName9b\"/>\n" +
            "</message-driven>";

    public static final String messageDrivenXML7 =
        "<message-driven name=\"MessageDrivenBean7\">\n" +
            "<listener-port name=\"lpName\"/>\n" +
            "<resource-ref name=\"ResourceRef7a\" binding-name=\"ResourceRefBindingName7a\"/>\n" +
            "<resource-ref name=\"ResourceRef7b\" binding-name=\"ResourceRefBindingName7b\"/>\n" +
        "</message-driven>";

    public static final String interceptorXML1 =
        "<interceptor class=\"com.ibm.test.Interceptor1\">\n" +
        "</interceptor>";

    public static final String messageDetinationXML1 =
            "<message-destination name=\"messageDestName1\" binding-name=\"messageDestBinding1\">\n" +
            "</message-destination>\n";

    public static final String defaultCMPConnectionFactoryXMI1 =
            "<defaultCMPConnectionFactory xmi:id=\"CMPConnectionFactoryBindingName\" jndiName=\"jdbc/CMPConnectionFactory\" resAuth=\"Container\" properties=\"\"/>";

    public String testCMPConnectionFactoryXMI1() {
        return testCMPConnectionFactoryXMI1( getEJBJarPath() );
    }

    public static String testCMPConnectionFactoryXMI1(String ddPath) {
        return "<ejbBindings xmi:id=\"EnterpriseBeanBindingName\" jndiName=\"ejb/EnterpriseBeanBinding\">" +
                   "<enterpriseBean xmi:type=\"ejb:EnterpriseBean\" href=\"" + ddPath + "#s0\"/>" +
                   "<cmpConnectionFactory xmi:id=\"CMPConnectionFactoryBindingNAme\" jndiName=\"jdbc/CMPConnectionFactory\" resAuth=\"Container\" loginConfigurationName=\"DefaultCMPConnectionFactoryMapping\">" +
                       "<properties xmi:id=\"Property\" name=\"com.ibm.test.testProperty\" value=\"testData\" description=\"Test Post Pls Ignore\"/>" +
                   "</cmpConnectionFactory>" +
               "</ejbBindings>";
    }

    public static final String ejbBndXMLCurrentBackendID =
            "<ejbbnd:EJBJarBinding" +
                " xmi:version=\"2.0\"" +
                " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                " xmlns:ejbbnd=\"ejbbnd.xmi\"" +
                " xmlns:ejb=\"ejb.xmi\"" +
                " xmi:id=\"ejb-jar_ID_Bnd\"" +
                " currentBackendId=\"testID\"" + 
            ">" +
            "</ejbbnd:EJBJarBinding>";

    public static final String defaultDataSourceXMI1 =
            "<defaultDatasource xmi:id=\"ResourceRefBinding_123\"/>";

    public static final String defaultDataSourceXMI2 =
            "<defaultDatasource xmi:id=\"ResourceRefBinding_123\">" +
                "<defaultAuth xmi:type=\"xmiType\" xmi:id=\"BasicAuthData_123\"/>" +
            "</defaultDatasource>";
}
