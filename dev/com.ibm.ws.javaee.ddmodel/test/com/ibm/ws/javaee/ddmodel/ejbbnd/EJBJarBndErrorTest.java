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

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.ddmodel.DDParser;

public class EJBJarBndErrorTest extends EJBJarBndTestBase {

    protected static final String ejbJarBndInvalidVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
             "<ejb-jar-bnd  xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"\n" +
                 " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                 " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee\n" +
                     " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_3_0.xsd\"" +
                 " version=\"3.0\"> ";

    // 1.0 & 1.1 are the only valid versions
    @Test
    public void testGetVersionError() throws Exception {
        try {
            parse(ejbJarBndInvalidVersion + "</ejb-jar-bnd>");
            Assert.fail("An exception should have been thrown.");
        } catch (DDParser.ParseException e) {
            verifyMessage(e,
                    "invalid.deployment.descriptor.version",
                    "CWWKC2263", "3.0", "ibm-ejb-jar-bnd.xml"); 
        }
    }

    @Test
    public void testEjbJarBndBadRoot() throws Exception {
        try {
            // Passing ejb-jar-ext to ejb-jar-bnd parser
            getEJBJarBnd(ejbJarExt10 + "</ejb-jar-ext>");
            Assert.fail("Parser should have thrown an exception for bad root.");
        } catch (DDParser.ParseException e) {
            verifyMessage(e,
                    "invalid.root.element",
                    "CWWKC2252", "ejb-jar-ext", "ibm-ejb-jar-bnd.xml");
        }
    }

    protected static final String ejbJarBndNoNameSpace =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
        "<ejb-jar-bnd\n" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee\n" +
                  " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_0.xsd\"" +
            " version=\"1.0\"> ";

    @Test
    public void testEjbJarBndNoNamespaceError() throws Exception {
        try {
            getEJBJarBnd(ejbJarBndNoNameSpace + "</ejb-jar-bnd>");
            Assert.fail("Parser should have thrown an exception for no namespace.");
        } catch (DDParser.ParseException e) {
            verifyMessage(e,
                    "missing.deployment.descriptor.namespace",
                    "CWWKC2264", "ibm-ejb-jar-bnd.xml");
        }
    }

    protected static final String ejbJarBndMissingVersion =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<ejb-jar-bnd  xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"\n" +
            " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee\n" +
                " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_0.xsd\">";

    @Test
    public void testEjbJarBndMissingVersionError() throws Exception {
        try {
            getEJBJarBnd(ejbJarBndMissingVersion + "</ejb-jar-bnd>");
            Assert.fail("Parser should have thrown an exception for no version.");
        } catch (DDParser.ParseException e) {
            verifyMessage(e,
                    "missing.deployment.descriptor.version",
                    "CWWKC2265", "ibm-ejb-jar-bnd.xml");
        }
    }

    //name is required in EnterpriseBean
    @Test
    public void testSessionBeanNoName() throws Exception {
        try {
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() +
                         "<session>" + //no name
                         "</session>" +
                         "</ejb-jar-bnd>");
            Assert.fail("An exception should be thrown for missing session name.");
        } catch (DDParser.ParseException e) {
            verifyMissingAttribute(e, "session", "name");
        }
    }

    //name is required in EnterpriseBean
    @Test
    public void testMessageDrivenBeanNoName() throws Exception {
        try {
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() +
                         "<message-driven>" + //no name
                         "</message-driven>" +
                         "</ejb-jar-bnd>");
            Assert.fail("An exception should be thrown for missing session name.");
        } catch (DDParser.ParseException e) {
            verifyMissingAttribute(e, "message-driven", "name");
        }
    }

    protected static final String ejbJarExt10 =
            "<?xml version=\"1.1\" encoding=\"UTF-8\"?>" + "\n" +
            "<ejb-jar-ext" +
                " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
                " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
                " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_0.xsd\"" +
                " version=\"1.0\"" +
            ">";
    
    protected static final String interfaceErrorBindingName =
        EJBJarBndTestBase.ejbJarBnd10() +
            "<session name=\"VersionedDriver\"> " +
                "<interface class=\"com.ibm.test.ClassBN\"/> \n " +
            "</session> " +
        "</ejb-jar-bnd>";

    //interface requires binding name
    @Test
    public void testInterfaceMissingBindingName() throws Exception {
        try {
            getEJBJarBnd(interfaceErrorBindingName);
            Assert.fail("Parse exception should have been thrown for missing interface binding name.");
        } catch (DDParser.ParseException e) {
            verifyMissingAttribute(e, "interface", "binding-name");
        }
    }

    protected static final String interfaceClassError =
            EJBJarBndTestBase.ejbJarBnd10() +
                "<session name=\"SessionBean\"> " +
                    "<interface binding-name=\"interfaceBindingName\"/>" +
                "</session> " +
            "</ejb-jar-bnd>";

    //interface requires class name
    @Test
    public void testInterfaceMissingClassName() throws Exception {
        try {
            getEJBJarBnd(interfaceClassError);
            Assert.fail("Parse exception should have been thrown for missing interface class name.");
        } catch (DDParser.ParseException e) {
            verifyMissingAttribute(e, "interface", "class");
        }
    }

    protected static final String listenerPortNoNameError =
            EJBJarBndTestBase.ejbJarBnd11() +
                "<message-driven name=\"MessageDrivenBean\"> \n" +
                    "<listener-port></listener-port> \n " +
                "</message-driven>\n" +
            "</ejb-jar-bnd>";

    //listener port requires name
    @Test
    public void testListenerPortMissingName() throws Exception {
        try {
            getEJBJarBnd(listenerPortNoNameError);
            Assert.fail("Parse exception should have been thrown for missing listener port name.");
        } catch (DDParser.ParseException e) {
            verifyMissingAttribute(e, "listener-port", "name");            
        }
    }

    protected static final String jcaAdapterXML2 =
        EJBJarBndTestBase.ejbJarBnd10() +
            "<message-driven name=\"MessageDrivenBean\">\n" +
                "<jca-adapter destination-binding-name=\"jcaAdapterDestinationBindingName\"\n" +
                    " activation-spec-auth-alias=\"\"/>\n" +
            "</message-driven>\n" +
        "</ejb-jar-bnd>";

    //listener port requires activation-spec-binding-name
    @Test
    public void testJCAAdapterMissingActivationSpecBinding() throws Exception {
        try {
            getEJBJarBnd(jcaAdapterXML2);
            Assert.fail("Parse exception should have been thrown for missing actiation-spec-binding-name.");
        } catch (DDParser.ParseException e) {
            verifyMissingAttribute(e, "jca-adapter", "activation-spec-binding-name");
        }
    }

    protected static final String jcaAdapterBothListenerPortAndJcaAdapterError =
            "<message-driven name=\"MessageDrivenBean\">\n" +
                "<listener-port name=\"lpName\"/>\n" +
                "<jca-adapter activation-spec-binding-name=\"jcaAdapterActivationSpecBindingName\" destination-binding-name=\"jcaAdapterDestinationBindingName\"\n" +
                    " activation-spec-auth-alias=\"\"/>\n" +
            "</message-driven>\n";

    // no longer validating the jca-adapter and listener-port content.
    // Ignore if they are both defined.
    @Test
    public void testMDBListenerPortandJCAAdapterError() throws Exception {
        getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd11() +
                     jcaAdapterBothListenerPortAndJcaAdapterError +
                     "</ejb-jar-bnd>");
    }

    protected static final String jcaMDBNeitherListenerPortNorJcaAdapterError =
            "<message-driven name=\"InterceptorMDB02Bean\">\n" +
            "</message-driven>\n";

    // removed validation that either a jca-adapter or listener-port is included.
    @Test
    public void testJCAAdapterNeitherLPandJCAAdapterError() throws Exception {
        getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd11() +
                     jcaMDBNeitherListenerPortNorJcaAdapterError +
                     "</ejb-jar-bnd>");
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
        try {
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() +
                             sessionXML +
                             sessionXML +
                         "</ejb-jar-bnd>");
            Assert.fail("Parse exception should have been thrown non unique EJB name.");
        } catch (DDParser.ParseException e) {
            verifyDuplicateEJBName(e, "CommonBeanName");
        }
    }

    @Test
    public void testEJBNonUniqueName2() throws Exception {
        try {
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() +
                             messageDrivenXML +
                             messageDrivenXML +
                         "</ejb-jar-bnd>");
            Assert.fail("Parse exception should have been thrown non unique EJB name.");
        } catch (DDParser.ParseException e) {
            verifyDuplicateEJBName(e, "CommonBeanName");            
        }
    }

    @Test
    public void testEJBNonUniqueName3() throws Exception {
        try {
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() +
                             sessionXML +
                             messageDrivenXML +
                         "</ejb-jar-bnd>");
            Assert.fail("Parse exception should have been thrown non unique EJB name.");
        } catch (DDParser.ParseException e) {
            verifyDuplicateEJBName(e, "CommonBeanName");                        
        }
    }

    @Test
    public void testInterceptorNonUniqueClassName() throws Exception {
        try {
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() + interceptorXML1 + interceptorXML1 + "</ejb-jar-bnd>");
            Assert.fail("Parse exception should have been thrown non unique interceptor class name.");
        } catch (DDParser.ParseException e) {
            verifyMessage(e,
                    "found.duplicate.attribute.value",
                    "CWWKC2270", "interceptor", "com.ibm.test.Interceptor1", "ibm-ejb-jar-bnd.xml");
        }
    }

    @Test
    public void testMessageDestinationNonUniqueName() throws Exception {
        try {
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() + messageDetinationXML1 + messageDetinationXML1 + "</ejb-jar-bnd>");
            Assert.fail("Parse exception should have been thrown non unique message destination name.");
        } catch (DDParser.ParseException e) {
            verifyMessage(e,
                    "found.duplicate.attribute.value",
                    "CWWKC2270", "message-destination", "messageDestName1", "ibm-ejb-jar-bnd.xml");            
        }
    }
}
