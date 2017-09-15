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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.javaee.dd.ejbbnd.Session;
import com.ibm.ws.javaee.ddmodel.DDParser;

public class EJBJarBndErrorTest extends EJBJarBndTestBase {

    List<Session> sessionBeans;

    static final String ejbJarBndInvalidVersion =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                    "<ejb-jar-bnd  xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" \n" +
                                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n " +
                                    " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee  \n " +
                                    " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_3_0.xsd\" version=\"3.0\"> ";

    @Test
    public void testGetVersionError() throws Exception {
        try {
            //1.0 & 1.1 are the only valid versions
            parse(ejbJarBndInvalidVersion + "</ejb-jar-bnd>").getVersion();
            Assert.fail("An exception should have been thrown.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Should get a specific message. Got: " + msg,
                              msg.contains("CWWKC2263") &&
                                              msg.contains("3.0") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    @Test
    public void testEjbJarBndBadRoot() throws Exception {
        try {
            // Passing ejb-jar-ext to ejb-jar-bnd parser
            getEJBJarBnd(ejbJarExt10() + "</ejb-jar-ext>");
            Assert.fail("Parser should have thrown an exception for bad root.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Parse exception for bad root is not as expected. Got: " + e.getMessage(),
                              msg.contains("CWWKC2252") &&
                                              msg.contains("ejb-jar-ext") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    static final String ejbJarBndNoNameSpace =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                    "<ejb-jar-bnd  \n" +
                                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n " +
                                    " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee  \n " +
                                    " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_0.xsd\" version=\"1.0\"> ";

    @Test
    public void testEjbJarBndNoNamespaceError() throws Exception {
        try {
            getEJBJarBnd(ejbJarBndNoNameSpace + "</ejb-jar-bnd>");
            Assert.fail("Parser should have thrown an exception for no namespace.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Should get specific exception message for missing namespace. Got: " + e.getMessage(),
                              msg.contains("CWWKC2264") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    static final String ejbJarBndMissingVersion =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                    "<ejb-jar-bnd  xmlns=\"http://websphere.ibm.com/xml/ns/javaee\" \n" +
                                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n " +
                                    " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee  \n " +
                                    " http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-bnd_1_0.xsd\" > ";

    @Test
    public void testEjbJarBndMissingVersionError() throws Exception {
        try {
            getEJBJarBnd(ejbJarBndMissingVersion + "</ejb-jar-bnd>");
            Assert.fail("Parser should have thrown an exception for no version.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Should get specific exception message for missing version. Got: " + e.getMessage(),
                              msg.contains("CWWKC2265") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    @Test
    public void testSessionBeanNoName() throws Exception {
        try {
            //name is required in EnterpriseBean
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() +
                         "<session>" + //no name
                         "</session>" +
                         "</ejb-jar-bnd>");
            Assert.fail("An exception should be thrown for missing session name.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Should get specific message for missing session name. Got: " + msg,
                              msg.contains("CWWKC2251") &&
                                              msg.contains("session") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    @Test
    public void testMessageDrivenBeanNoName() throws Exception {
        try {
            //name is required in EnterpriseBean
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() +
                         "<message-driven>" + //no name
                         "</message-driven>" +
                         "</ejb-jar-bnd>");
            Assert.fail("An exception should be thrown for missing session name.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Should get specific message for missing session name. Got: " + e.getMessage(),
                              msg.contains("CWWKC2251") &&
                                              msg.contains("message-driven") &&
                                              msg.contains("name") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    static final String ejbJarExt10() {
        return "<?xml version=\"1.1\" encoding=\"UTF-8\"?>" + "\n" +
               " <ejb-jar-ext" +
               " xmlns=\"http://websphere.ibm.com/xml/ns/javaee\"" +
               " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + "\n" +
               " xsi:schemaLocation=\"http://websphere.ibm.com/xml/ns/javaee http://websphere.ibm.com/xml/ns/javaee/ibm-ejb-jar-ext_1_0.xsd\"" +
               " version=\"1.0\"" +
               ">";
    }

    String interfaceErrorBindingName = EJBJarBndTestBase.ejbJarBnd10() +
                                       "<session name=\"VersionedDriver\"> " +
                                       "<interface class=\"com.ibm.test.ClassBN\"/> \n " +
                                       "</session> " +
                                       "</ejb-jar-bnd>";

    @Test
    public void testInterfaceMissingBindingName() throws Exception {
        try {
            //interface requires binding name
            getEJBJarBnd(interfaceErrorBindingName);
            Assert.fail("Parse exception should have been thrown for missing interface binding name.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Missing Interface binding name exception not as expected. Got: " + e.getMessage(),
                              msg.contains("CWWKC2251") &&
                                              msg.contains("interface") &&
                                              msg.contains("binding-name") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    String interfaceClassError = EJBJarBndTestBase.ejbJarBnd10() +
                                 "<session name=\"SessionBean\"> " +
                                 "<interface binding-name=\"interfaceBindingName\"/>" +
                                 "</session> " +
                                 "</ejb-jar-bnd>";

    @Test
    public void testInterfaceMissingClassName() throws Exception {
        try {
            //interface requires class name
            getEJBJarBnd(interfaceClassError);
            Assert.fail("Parse exception should have been thrown for missing interface class name.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Missing Interface class name exception not as expected. Got: " + e.getMessage(),
                              msg.contains("CWWKC2251") &&
                                              msg.contains("interface") &&
                                              msg.contains("class") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    String listenerPortNoNameError = EJBJarBndTestBase.ejbJarBnd11() +
                                     "<message-driven name=\"MessageDrivenBean\"> \n" +
                                     "<listener-port></listener-port> \n " +
                                     "</message-driven>\n" +
                                     "</ejb-jar-bnd>";

    @Test
    public void testListenerPortMissingName() throws Exception {
        try {
            //listener port requires name
            getEJBJarBnd(listenerPortNoNameError);
            Assert.fail("Parse exception should have been thrown for missing listener port name.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Missing listener port name exception not as expected. Got: " + e.getLocalizedMessage(),
                              msg.contains("CWWKC2251") &&
                                              msg.contains("listener-port") &&
                                              msg.contains("name") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    String jcaAdapterXML2 = EJBJarBndTestBase.ejbJarBnd10() +
                            "<message-driven name=\"MessageDrivenBean\"> \n" +
                            "<jca-adapter destination-binding-name=\"jcaAdapterDestinationBindingName\" \n " +
                            "activation-spec-auth-alias=\"\"/> \n " +
                            "</message-driven>\n" +
                            "</ejb-jar-bnd>";

    @Test
    public void testJCAAdapterMissingActivationSpecBinding() throws Exception {
        try {
            //listener port requires activation-spec-binding-name
            getEJBJarBnd(jcaAdapterXML2);
            Assert.fail("Parse exception should have been thrown for missing actiation-spec-binding-name.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Parse exception for missing JCA Adapter actiation-spec-binding-name not as expected. Got: " + e.getLocalizedMessage(),
                              msg.contains("CWWKC2251") &&
                                              msg.contains("jca-adapter") &&
                                              msg.contains("activation-spec-binding-name") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    String jcaAdapterBothListenerPortAndJcaAdapterError = "<message-driven name=\"MessageDrivenBean\"> \n"
                                                          + "<listener-port name=\"lpName\"/> \n "
                                                          + "<jca-adapter activation-spec-binding-name=\"jcaAdapterActivationSpecBindingName\" destination-binding-name=\"jcaAdapterDestinationBindingName\" \n "
                                                          + "activation-spec-auth-alias=\"\"/> \n "
                                                          + "</message-driven>\n";

    @Test
    // no longer validating the jca-adapter and listener-port content.
    // Ignore if they are both defined.
    public void testMDBListenerPortandJCAAdapterError() throws Exception {
        getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd11() + jcaAdapterBothListenerPortAndJcaAdapterError + "</ejb-jar-bnd>");
    }

    String jcaMDBNeitherListenerPortNorJcaAdapterError = "<message-driven name=\"InterceptorMDB02Bean\"> \n" +
                                                         "</message-driven>\n";

    @Test
    // removed validation that either a jca-adapter or listener-port is included.
    public void testJCAAdapterNeitherLPandJCAAdapterError() throws Exception {
        getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd11() +
                     jcaMDBNeitherListenerPortNorJcaAdapterError + "</ejb-jar-bnd>");
    }

    String sessionXML = "<session name=\"CommonBeanName\"> \n" +
                        "<interface class=\"com.ibm.test.SessionBean2Local\" binding-name=\"interfaceBinding\"/> \n" +
                        "</session> \n";

    String messageDrivenXML = "<message-driven name=\"CommonBeanName\">  \n"
                              + "<jca-adapter activation-spec-binding-name=\"jcaAdapterActivationSpecBindingName\" destination-binding-name=\"jcaAdapterDestinationSpecBindingName\" activation-spec-auth-alias=\"\" />  \n"
                              + "</message-driven>  \n";

    @Test
    public void testEJBNonUniqueName1() throws Exception {
        try {
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() + sessionXML + sessionXML + "</ejb-jar-bnd>");
            Assert.fail("Parse exception should have been thrown non unique EJB name.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Parse exception for non-unique EJB name not as expected. Got: " + e.getMessage(),
                              msg.contains("CWWKC2269") &&
                                              msg.contains("CommonBeanName")
                                              && msg.contains("ibm-ejb-jar-bnd.xml"));
        }

    }

    @Test
    public void testEJBNonUniqueName2() throws Exception {
        try {
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() + messageDrivenXML + messageDrivenXML + "</ejb-jar-bnd>");
            Assert.fail("Parse exception should have been thrown non unique EJB name.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Parse exception for non-unique EJB name not as expected. Got: " + msg,
                              msg.contains("CWWKC2269") &&
                                              msg.contains("CommonBeanName")
                                              && msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    @Test
    public void testEJBNonUniqueName3() throws Exception {
        try {
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() + sessionXML + messageDrivenXML + "</ejb-jar-bnd>");
            Assert.fail("Parse exception should have been thrown non unique EJB name.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Parse exception for non-unique EJB name not as expected. Got: " + msg,
                              msg.contains("CWWKC2269") &&
                                              msg.contains("CommonBeanName") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    @Test
    public void testInterceptorNonUniqueClassName() throws Exception {
        try {
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() + interceptorXML1 + interceptorXML1 + "</ejb-jar-bnd>");
            Assert.fail("Parse exception should have been thrown non unique interceptor class name.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Parse exception for non-unique interceptor class name name not as expected. Got: " + msg,
                              msg.contains("CWWKC2270") &&
                                              msg.contains("interceptor") &&
                                              msg.contains("com.ibm.test.Interceptor1") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }

    @Test
    public void testMessageDestinationNonUniqueName() throws Exception {
        try {
            getEJBJarBnd(EJBJarBndTestBase.ejbJarBnd10() + messageDetinationXML1 + messageDetinationXML1 + "</ejb-jar-bnd>");
            Assert.fail("Parse exception should have been thrown non unique message destination name.");
        } catch (DDParser.ParseException e) {
            String msg = e.getMessage();
            Assert.assertTrue("Parse exception for non-unique message destination name not as expected. Got: " + msg,
                              msg.contains("CWWKC2270") &&
                                              msg.contains("message-destination") &&
                                              msg.contains("messageDestName1") &&
                                              msg.contains("ibm-ejb-jar-bnd.xml"));
        }
    }
}
