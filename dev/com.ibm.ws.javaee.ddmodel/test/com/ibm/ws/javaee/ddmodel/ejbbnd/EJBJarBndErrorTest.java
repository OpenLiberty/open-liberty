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
package com.ibm.ws.javaee.ddmodel.ejbbnd;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class EJBJarBndErrorTest extends EJBJarBndTestBase {
    @Parameters
    public static Iterable<? extends Object> data() {
        return TEST_DATA;
    }
    
    public EJBJarBndErrorTest(boolean ejbInWar) {
        super(ejbInWar);
    }

    protected static final String ejbJarBndInvalidVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
             "<ejb-jar-bnd  xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"\n" +
                 " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                 " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee\n" +
                     " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_3_0.xsd\"" +
                 " version=\"3.0\"/>";

    // 1.0 & 1.1 are the only valid versions
    @Test
    public void testGetVersionError() throws Exception {
        parseEJBJarBndXML(ejbJarBndInvalidVersion,
                          "unsupported.descriptor.version",
                          "CWWKC2261", "3.0", "ibm-ejb-jar-bnd.xml"); 
    }

    @Test
    public void testEjbJarBndBadRoot() throws Exception {
        parseEJBJarBndXML(ejbJarExt10,
                          "unexpected.root.element",
                          "CWWKC2252", "ejb-jar-ext", "ibm-ejb-jar-bnd.xml");
    }

    protected static final String ejbJarBndNoNameSpace =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<ejb-jar-bnd\n" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee\n" +
                    " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_0.xsd\"" +
                " version=\"1.0\">";

    @Test
    public void testEjbJarBndNoNamespaceError() throws Exception {
        parseEJBJarBndXML(ejbJarBndNoNameSpace + "</ejb-jar-bnd>");
    }

    protected static final String ejbJarBndMissingVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<ejb-jar-bnd  xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"\n" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee\n" +
                " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_0.xsd\">";

    @Test
    public void testEjbJarBndMissingVersionError() throws Exception {
        parseEJBJarBndXML(ejbJarBndMissingVersion + "</ejb-jar-bnd>");
    }

    @Test
    public void testSessionBeanNoName() throws Exception {
        parseEJBJarBndXML(ejbJarBnd10(
                              "<session>" + //no name
                              "</session>"),
                         "required.attribute.missing",
                         "CWWKC2251", "session", "name");
    }

    @Test
    public void testMessageDrivenBeanNoName() throws Exception {
        parseEJBJarBndXML(ejbJarBnd10(
                              "<message-driven>" + //no name
                              "</message-driven>"),
                          "required.attribute.missing",
                          "CWWKC2251", "message-driven", "name");                         
    }

    protected static final String ejbJarExt10 =
            "<?xml version=\"1.1\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_0.xsd\"" +
                " version=\"1.0\"" +
            "/>";
    
    protected static final String interfaceErrorBindingName =
        ejbJarBnd10(
            "<session name=\"VersionedDriver\"> " +
                "<interface class=\"com.ibm.test.ClassBN\"/> \n " +
            "</session>");

    @Test
    public void testInterfaceMissingBindingName() throws Exception {
        parseEJBJarBndXML(interfaceErrorBindingName,
                "required.attribute.missing",
                "CWWKC2251", "interface", "binding-name");
    }

    protected static final String interfaceClassError =
            ejbJarBnd10(
                "<session name=\"SessionBean\"> " +
                    "<interface binding-name=\"interfaceBindingName\"/>" +
                "</session>");

    @Test
    public void testInterfaceMissingClassName() throws Exception {
        parseEJBJarBndXML(interfaceClassError,
                "required.attribute.missing",
                "CWWKC2251", "interface", "class");
    }

    protected static final String listenerPortNoNameError =
            ejbJarBnd11(
                "<message-driven name=\"MessageDrivenBean\"> \n" +
                    "<listener-port></listener-port> \n " +
                "</message-driven>");

    @Test
    public void testListenerPortMissingName() throws Exception {
        parseEJBJarBndXML(listenerPortNoNameError,
                "required.attribute.missing",
                "CWWKC2251", "listener-port", "name");            
    }

    protected static final String jcaAdapterXML2 =
        ejbJarBnd10(
            "<message-driven name=\"MessageDrivenBean\">\n" +
                "<jca-adapter destination-binding-name=\"jcaAdapterDestinationBindingName\"\n" +
                    " activation-spec-auth-alias=\"\"/>\n" +
            "</message-driven>");

    //listener port requires activation-spec-binding-name
    @Test
    public void testJCAAdapterMissingActivationSpecBinding() throws Exception {
        parseEJBJarBndXML(jcaAdapterXML2,
                "required.attribute.missing",
                "CWWKC2251", "jca-adapter", "activation-spec-binding-name");
    }

    protected static final String jcaAdapterBothListenerPortAndJcaAdapterError =
            "<message-driven name=\"MessageDrivenBean\">\n" +
                "<listener-port name=\"lpName\"/>\n" +
                "<jca-adapter activation-spec-binding-name=\"jcaAdapterActivationSpecBindingName\"" +
                    " destination-binding-name=\"jcaAdapterDestinationBindingName\"" +
                    " activation-spec-auth-alias=\"\"/>\n" +
            "</message-driven>";

    // no longer validating the jca-adapter and listener-port content.
    // Ignore if they are both defined.
    @Test
    public void testMDBListenerPortandJCAAdapterError() throws Exception {
        parseEJBJarBndXML(ejbJarBnd11(jcaAdapterBothListenerPortAndJcaAdapterError));
    }

    protected static final String jcaMDBNeitherListenerPortNorJcaAdapterError =
            "<message-driven name=\"InterceptorMDB02Bean\">\n" +
            "</message-driven>\n";

    // removed validation that either a jca-adapter or listener-port is included.
    @Test
    public void testJCAAdapterNeitherLPandJCAAdapterError() throws Exception {
        parseEJBJarBndXML(ejbJarBnd11(jcaMDBNeitherListenerPortNorJcaAdapterError));
    }

    protected static final String sessionXML =
        "<session name=\"CommonBeanName\">\n" +
            "<interface class=\"com.ibm.test.SessionBean2Local\"" +
                " binding-name=\"interfaceBinding\"/>\n" +
        "</session>\n";

    protected static final String messageDrivenXML =
        "<message-driven name=\"CommonBeanName\">\n" +
            "<jca-adapter activation-spec-binding-name=\"jcaAdapterActivationSpecBindingName\"" +
                  " destination-binding-name=\"jcaAdapterDestinationSpecBindingName\"" +
                  " activation-spec-auth-alias=\"\"/>\n" +
        "</message-driven>  \n";

    @Test
    public void testEJBNonUniqueName1() throws Exception {
        parseEJBJarBndXML(ejbJarBnd10(sessionXML + sessionXML),
                          "found.duplicate.ejbname",
                          "CWWKC2269", "CommonBeanName", "ibm-ejb-jar-bnd.xml");        
    }

    @Test
    public void testEJBNonUniqueName2() throws Exception {
        parseEJBJarBndXML(ejbJarBnd10(messageDrivenXML + messageDrivenXML),
                          "found.duplicate.ejbname",
                          "CWWKC2269", "CommonBeanName", "ibm-ejb-jar-bnd.xml");        
    }

    @Test
    public void testEJBNonUniqueName3() throws Exception {
        parseEJBJarBndXML(ejbJarBnd10(sessionXML + messageDrivenXML),
                          "found.duplicate.ejbname",
                          "CWWKC2269", "CommonBeanName", "ibm-ejb-jar-bnd.xml");        
    }

    @Test
    public void testInterceptorNonUniqueClassName() throws Exception {
        parseEJBJarBndXML(ejbJarBnd10(interceptorXML1 + interceptorXML1),
                          "found.duplicate.attribute.value",
                          "CWWKC2270", "interceptor", "com.ibm.test.Interceptor1", "ibm-ejb-jar-bnd.xml");
    }

    @Test
    public void testMessageDestinationNonUniqueName() throws Exception {
        parseEJBJarBndXML(ejbJarBnd10(messageDetinationXML1 + messageDetinationXML1),
                          "found.duplicate.attribute.value",
                          "CWWKC2270", "message-destination", "messageDestName1", "ibm-ejb-jar-bnd.xml");            
    }
}
