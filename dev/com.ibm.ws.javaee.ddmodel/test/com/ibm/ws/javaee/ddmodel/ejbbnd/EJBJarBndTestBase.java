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
    
    // TODO: Haven't found the correct pattern for this ...
    //       Need to use 'getEJBInJar', which is instance
    //       state because of how repeat testing works.
    //       But the value should be initialized as a static
    //       variable, since it is to be shared between tests.
    
    private EJBJar ejbJar21;

    public EJBJar getEJBJar21() throws Exception {
        if ( ejbJar21 == null ) {
            ejbJar21 = parseEJBJar(ejbJar21() + "</ejb-jar>");
        }
        return ejbJar21;
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

    public static String ejbJar21() {
        return "<ejb-jar" +
                   " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                   " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
                   " version=\"2.1\"" +
                   " id=\"EJBJar_ID\"" +
               ">";
    }

    public String ejbJarBinding(String attrs) {
        return ejbJarBinding( attrs, getEJBJarPath() );
    }

    public static String ejbJarBinding(String attrs, String ddPath) {
        return "<ejbbnd:EJBJarBinding" +
                   " xmlns:ejbbnd=\"ejbbnd.xmi\"" +
                   " xmlns:xmi=\"http://www.omg.org/XMI\"" +
                   " xmlns:ejb=\"ejb.xmi\"" +
                   " xmi:version=\"2.0\"" +
                   " " + attrs +
               ">" +
                   "<ejbJar href=\"META-INF/" + ddPath + "#EJBJar_ID\"/>";
    }

    public static String ejbJarBnd10() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<ejb-jar-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"\n" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee" +
                   " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_0.xsd\"" +
                   " version=\"1.0\">";
    }

    public static String ejbJarBnd11() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<ejb-jar-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"\n" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee" +
                       " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_1.xsd\"\n" +
                   " version=\"1.1\">";
    }

    public static String ejbJarBnd12() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<ejb-jar-bnd" +
                   " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"\n" +
                   " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                   " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee" +
                       " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_2.xsd\"\n" +
                   " version=\"1.2\">";
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
                   "<enterpriseBean xmi:type=\"ejb:EnterpriseBean\" href=\"" + ddPath + "#EnterpriseBean\"/>" +
                   "<cmpConnectionFactory xmi:id=\"CMPConnectionFactoryBindingNAme\" jndiName=\"jdbc/CMPConnectionFactory\" resAuth=\"Container\" loginConfigurationName=\"DefaultCMPConnectionFactoryMapping\">" +
                       "<properties xmi:id=\"Property\" name=\"com.ibm.test.testProperty\" value=\"testData\" description=\"Test Post Pls Ignore\"/>" +
                   "</cmpConnectionFactory>" +
               "</ejbBindings>";
    }

    public static final String testCurrentBackendID =
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
            " <defaultDatasource xmi:id=\"ResourceRefBinding_123\"/>";

    public static final String defaultDataSourceXMI2 =
            "<defaultDatasource xmi:id=\"ResourceRefBinding_123\">" +
                "<defaultAuth xmi:type=\"xmiType\" xmi:id=\"BasicAuthData_123\"/>" +
            "</defaultDatasource>";
}
