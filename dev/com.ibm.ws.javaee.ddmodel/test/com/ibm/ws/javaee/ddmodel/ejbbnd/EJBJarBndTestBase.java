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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jmock.Expectations;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.javaee.dd.ejb.EJBJar;
import com.ibm.ws.javaee.dd.ejbbnd.EJBJarBnd;
import com.ibm.ws.javaee.ddmodel.DDTestBase;
import com.ibm.ws.javaee.ddmodel.ejb.EJBJarDDParserVersion;
import com.ibm.ws.javaee.ddmodel.ejb.EJBJarEntryAdapter;
import com.ibm.ws.javaee.ddmodel.ejb.EJBJarTestBase;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.adaptable.module.NonPersistentCache;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.artifact.overlay.OverlayContainer;

/**
 * Test the ejb-jar-bnd.xml parser
 * -concentrate on the pristine path where the ejb-jar-bnd.xml file is
 * well formed.
 */
public class EJBJarBndTestBase extends EJBJarTestBase {
    protected boolean isWarModule = false;

    protected String getEJBJarPath() {
        return isWarModule ? "WEB-INF/ejb-jar.xml" : "META-INF/ejb-jar.xml";
    }

    public EJBJarBnd parseEJBJarBnd(String xml) throws Exception {
        return parseEJBJarBnd(xml, (EJBJar) null);
    }

    public EJBJarBnd getEJBJarBnd(String xml) throws Exception {
        return parseEJBJarBnd(xml, (EJBJar) null);
    }

    public EJBJarBnd parseEJBJarBnd(String xml, EJBJar ejbJar) throws Exception {
        boolean xmi = ejbJar != null;
        WebModuleInfo moduleInfo = isWarModule ? mockery.mock(WebModuleInfo.class, "webModuleInfo" + mockId++) : null;
        String entryName = isWarModule ?
                        (xmi ? EJBJarBndAdapter.XMI_BND_IN_WEB_MOD_NAME : EJBJarBndAdapter.XML_BND_IN_WEB_MOD_NAME) :
                        (xmi ? EJBJarBndAdapter.XMI_BND_IN_EJB_MOD_NAME : EJBJarBndAdapter.XML_BND_IN_EJB_MOD_NAME);
        return parse(xml, new EJBJarBndAdapter(), entryName, EJBJar.class, ejbJar, WebModuleInfo.class, moduleInfo);
    }

    static final String ejbJar21() {
        return "<ejb-jar" +
               " xmlns=\"http://java.sun.com/xml/ns/j2ee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
               " xsi:schemaLocation=\"http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/ejb-jar_2_1.xsd\"" +
               " version=\"2.1\"" +
               " id=\"EJBJar_ID\"" +
               ">";
    }

    static final String ejbJarBinding(String attrs) {
        return "<ejbbnd:EJBJarBinding" +
               " xmlns:ejbbnd=\"ejbbnd.xmi\"" +
               " xmlns:xmi=\"http://www.omg.org/XMI\"" +
               " xmlns:ejb=\"ejb.xmi\"" +
               " xmi:version=\"2.0\"" +
               " " + attrs +
               ">" +
               "<ejbJar href=\"META-INF/ejb-jar.xml#EJBJar_ID\"/>";
    }

    static final String ejbJarBnd10() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
               "<ejb-jar-bnd  xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" \n" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n " +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee  \n " +
               " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_0.xsd\" version=\"1.0\"> ";
    }

    static final String ejbJarBnd11() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<ejb-jar-bnd  xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" \n" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n " +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee  \n " +
               " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_1.xsd\" version=\"1.1\"> ";
    }

    static final String ejbJarBnd12() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<ejb-jar-bnd  xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" \n" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n " +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee  \n " +
               " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_2.xsd\" version=\"1.2\"> ";
    }

    String sessionXML8 = "<session name=\"SessionBean8\"> \n" +
                         "<resource-ref name=\"resourceRefName8\" \n" +
                         "binding-name=\"resourceRefBinding8\" /> \n" +
                         "</session> \n";

    String sessionXML11 = "<session name=\"SessionBean11\"> " +
                          "<resource-ref name=\"ResourceRef11\" binding-name=\"ResourceRefBindingName11\"> " +
                          "<authentication-alias name=\"AuthAlias11\" />" +
                          "<custom-login-configuration name=\"customLoginConfiguration11\"> " +
                          "<property name=\"propname\" value=\"propvalue\"/> " +
                          "</custom-login-configuration> " +
                          "</resource-ref>" +
                          "</session>";

    String messageDrivenXML9 = "<message-driven name=\"MessageDrivenBean9\">  \n"
                               + "<jca-adapter activation-spec-binding-name=\"ActivationSpecBindingName9\"/> \n"
                               + "<resource-env-ref name=\"ResourceEnvRefName9a\" binding-name=\"ResourceEnvRefBindingName9a\"/>  \n"
                               + "<resource-env-ref name=\"ResourceEnvRefName9b\" binding-name=\"ResourceEnvRefBindingName9b\"/>  \n"
                               + "</message-driven>  \n";

    String messageDrivenXML7 = "<message-driven name=\"MessageDrivenBean7\"> \n"
                               + "  <listener-port name=\"lpName\"/> \n "
                               + "  <resource-ref name=\"ResourceRef7a\" binding-name=\"ResourceRefBindingName7a\"/>  \n"
                               + "  <resource-ref name=\"ResourceRef7b\" binding-name=\"ResourceRefBindingName7b\"/>  \n"
                               + "</message-driven>  \n";

    final String interceptorXML1 = "<interceptor class=\"com.ibm.test.Interceptor1\"> \n" +
                                   "</interceptor> \n";

    final String messageDetinationXML1 =
                    "<message-destination name=\"messageDestName1\" binding-name=\"messageDestBinding1\"> \n" +
                                    "</message-destination> \n";

    final String defaultCMPConnectionFactoryXMI1 =
                    " <defaultCMPConnectionFactory xmi:id=\"CMPConnectionFactoryBindingName\" jndiName=\"jdbc/CMPConnectionFactory\" resAuth=\"Container\" properties=\"\"/>";

    final String testCMPConnectionFactoryXMI1 =
                    "<ejbBindings xmi:id=\"EnterpriseBeanBindingName\" jndiName=\"ejb/EnterpriseBeanBinding\">"
                                    + "  <enterpriseBean xmi:type=\"ejb:EnterpriseBean\" href=\"META-INF/ejb-jar.xml#EnterpriseBean\"/>"
                                    + "  <cmpConnectionFactory xmi:id=\"CMPConnectionFactoryBindingNAme\" jndiName=\"jdbc/CMPConnectionFactory\" resAuth=\"Container\" loginConfigurationName=\"DefaultCMPConnectionFactoryMapping\">"
                                    + "    <properties xmi:id=\"Property\" name=\"com.ibm.test.testProperty\" value=\"testData\" description=\"Test Post Pls Ignore\"/>"
                                    + "  </cmpConnectionFactory>"
                                    + "</ejbBindings>";

    final String testCurrentBackendID =
                    "<ejbbnd:EJBJarBinding"
                                    + " xmi:version=\"2.0\""
                                    + " xmlns:xmi=\"http://www.omg.org/XMI\""
                                    + " xmlns:ejbbnd=\"ejbbnd.xmi\""
                                    + " xmlns:ejb=\"ejb.xmi\" "
                                    + " xmi:id=\"ejb-jar_ID_Bnd\""
                                    + " currentBackendId=\"testID\""
                                    + ">"
                                    + " </ejbbnd:EJBJarBinding>";

    final String defaultDataSourceXMI1 =
                    " <defaultDatasource xmi:id=\"ResourceRefBinding_123\"/>";

    final String defaultDataSourceXMI2 =
                    "<defaultDatasource xmi:id=\"ResourceRefBinding_123\">"
                                    + "   <defaultAuth xmi:type=\"xmiType\" xmi:id=\"BasicAuthData_123\"/>"
                                    + "</defaultDatasource>";

    //

    protected void verifyMissingAttribute(Exception e, String elementName, String attributeName) {
        verifyFailure(e,
                "required.attribute.missing",
                "CWWKC2251", elementName, attributeName, "ibm-ejb-jar-bnd.xml");
    }

    protected void verifyDuplicateEJBName(Exception e, String beanName) {
        verifyFailure(e,
                "found.duplicate.ejbname",
                "CWWKC2269", beanName, "ibm-ejb-jar-bnd.xml");        
    }    
}
