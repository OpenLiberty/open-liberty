/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.List;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.jaxws.endpoint.properties.client.HelloNoWSDLInterface;
import com.ibm.ws.jaxws.endpoint.properties.client.HelloNoWSDLService;
import com.ibm.ws.jaxws.fat.util.ExplodedShrinkHelper;
import com.ibm.ws.jaxws.fat.util.TestUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

/**
 * Test the Endpoint properties in ibm-ws-bnd.xml
 */
@RunWith(FATRunner.class)
public class EndpointPropertiesTest {

    @Server("EndpointPropertiesTestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        ExplodedShrinkHelper.explodedDropinApp(server, "testEndpointPropertiesWeb", "com.ibm.ws.jaxws.test.endpoint.properties.server");
    }

    @After
    public void tearDown() throws Exception {
        if (server == null) {
            return;
        }

        if (server.isStarted()) {
            server.stopServer();
        }

        server.deleteFileFromLibertyServerRoot("dropins/testEndpointPropertiesWeb.war/WEB-INF/ibm-ws-bnd.xml");

    }

    /**
     * TestDescription:
     * - Test the CXF message contextual property - publishedEndpointUrl
     * - Use webservice-endpoint/properties element in binding file to config this property
     * Condition:
     * - A testEndpointPropertiesWeb.war publishes HelloService
     * - Config the publishedEndpointUrl="http://www.ibm.com/services/hello" in binding file: WEB-INF/ibm-ws-bnd.xml
     * Result:
     * - the wsdl will use "http://www.ibm.com/services/hello" as the port address
     */
    @Test
    public void testPublishedEndpointUrlProperty() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "EndpointPropertiesTest", "ibm-ws-bnd_testPublishedEndpointUrlProperty.xml",
                                      "dropins/testEndpointPropertiesWeb.war/WEB-INF", "ibm-ws-bnd.xml");

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*testEndpointPropertiesWeb");

        String wsdl = TestUtils.getServletResponse(getBaseUrl() + "/testEndpointPropertiesWeb/HelloService?wsdl");
        assertTrue("Can not access the HelloService's wsdl, the return result is: " + wsdl,
                   wsdl.contains("soap:address location=\"http://www.ibm.com/services/hello\""));

    }

    /**
     * TestDescription:
     * - Test the CXF message contextual property - org.apache.cxf.wsdl.create.imports
     * - Use webservice-endpoint/properties element in binding file to config this property
     * Condition:
     * - A testEndpointPropertiesWeb.war publishes HelloService
     * - Config the org.apache.cxf.wsdl.create.imports="true" in binding file: WEB-INF/ibm-ws-bnd.xml
     * Result:
     * - the wsdl will import the HelloNoWSDLService_schema1.xsd instead of generating the schema in the wsdl
     */
    @Test
    public void testWSDLCreateImportsProperty() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "EndpointPropertiesTest", "ibm-ws-bnd_testWSDLCreateImportsProperty.xml",
                                      "dropins/testEndpointPropertiesWeb.war/WEB-INF", "ibm-ws-bnd.xml");

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*testEndpointPropertiesWeb");

        String wsdl = TestUtils.getServletResponse(getBaseUrl() + "/testEndpointPropertiesWeb/HelloNoWSDLService?wsdl");
        assertTrue("Can not access the HelloService's wsdl, the return result is: " + wsdl,
                   wsdl.contains("xsd=HelloNoWSDLService_schema1.xsd"));

    }

    /**
     * TestDescription:
     * - Test the CXF message contextual property - autoRewriteSoapAddress
     * - Use webservice-endpoint/properties element in binding file to config this property
     * Condition:
     * - A testEndpointPropertiesWeb.war publishes HelloService
     * - Config the autoRewriteSoapAddress="false" in binding file: WEB-INF/ibm-ws-bnd.xml
     * - Because in liberty code we by default set autoRewriteSoapAddressForAllServices as true, we also need config the autoRewriteSoapAddressForAllServices="false"
     * Result:
     * - the wsdl will use "/HelloService" as the port address
     */
    //@Test Comment out the case as AutoRewriteSoapAddress did not work in CXF
    public void testAutoRewriteSoapAddressProperty() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "EndpointPropertiesTest", "ibm-ws-bnd_testAutoRewriteSoapAddressProperty.xml",
                                      "dropins/testEndpointPropertiesWeb.war/WEB-INF", "ibm-ws-bnd.xml");

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*testEndpointPropertiesWeb");

        String wsdl = TestUtils.getServletResponse(getBaseUrl() + "/testEndpointPropertiesWeb/HelloService?wsdl");
        assertTrue("Can not access the HelloService's wsdl, the return result is: " + wsdl,
                   wsdl.contains("soap:address location=\"http://www.dragonwell.com/services/hello\""));

    }

    //Default properties tests
    /**
     * TestDescription:
     * - Test the CXF message contextual property - org.apache.cxf.wsdl.create.imports
     * - Use webservice-endpoint-properties element in binding file to config this property
     * Condition:
     * - A testEndpointPropertiesWeb.war publishes HelloService
     * - Config the org.apache.cxf.wsdl.create.imports="true" in binding file: WEB-INF/ibm-ws-bnd.xml
     * Result:
     * - the wsdl will import the HelloNoWSDLService_schema1.xsd instead of generating the schema in the wsdl
     */
    @Test
    public void testDefaultWSDLCreateImportsProperty() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "EndpointPropertiesTest", "ibm-ws-bnd_testDefaultWSDLCreateImportsProperty.xml",
                                      "dropins/testEndpointPropertiesWeb.war/WEB-INF", "ibm-ws-bnd.xml");

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*testEndpointPropertiesWeb");

        String wsdl = TestUtils.getServletResponse(getBaseUrl() + "/testEndpointPropertiesWeb/HelloNoWSDLService?wsdl");
        assertTrue("Can not access the HelloNoWSDLService wsdl, the return result is: " + wsdl,
                   wsdl.contains("xsd=HelloNoWSDLService_schema1.xsd"));

    }

    /**
     * TestDescription:
     * - Test the CXF message contextual property - autoRewriteSoapAddress
     * - Use webservice-endpoint-properties element in binding file to config this property
     * Condition:
     * - A testEndpointPropertiesWeb.war publishes HelloService
     * - Config the autoRewriteSoapAddress="false" in binding file: WEB-INF/ibm-ws-bnd.xml
     * - Because in liberty code we by default set autoRewriteSoapAddressForAllServices as true, we also need config the autoRewriteSoapAddressForAllServices="false"
     * Result:
     * - the wsdl will use "/HelloService" as the port address
     */
    //@Test Comment out the case as AutoRewriteSoapAddress did not work in CXF
    public void testDefaultAutoRewriteSoapAddressProperty() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "EndpointPropertiesTest", "ibm-ws-bnd_testDefaultAutoRewriteSoapAddressProperty.xml",
                                      "dropins/testEndpointPropertiesWeb.war/WEB-INF", "ibm-ws-bnd.xml");

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*testEndpointPropertiesWeb");

        String wsdl = TestUtils.getServletResponse(getBaseUrl() + "/testEndpointPropertiesWeb/HelloService?wsdl");
        assertTrue("Can not access the HelloService's wsdl, the return result is: " + wsdl,
                   wsdl.contains("soap:address location=\"/http://www.dragonwell.com/services/hello\""));

    }

    /**
     * TestDescription:
     * - Test the user defined property - enableLoggingInOutInterceptor
     * - Use webservice-endpoint-properties element in binding file to config this property
     * Condition:
     * - A testEndpointPropertiesWeb.war publishes HelloService
     * - Config the enableLoggingInOutInterceptor="true" in binding file: WEB-INF/ibm-ws-bnd.xml
     * Result:
     * - incoming/outcoming messages will be dumped into message.log. Caution: if user enabled this setting for
     * their applicaion, any credential in the messages will be output in the log files.
     * - LoggingInOutInterceptors are replaced by LoggingFeature for jaxws-2.3 and xmlWS-3.0. This test will be skipped
     */
    @Test
    @SkipForRepeat({ "jaxws-2.3", JakartaEE9Action.ID })
    public void testDefaultLoggingInOutInterceptorProperty() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "EndpointPropertiesTest", "ibm-ws-bnd_testDefaultLoggingInOutInterceptorProperty.xml",
                                      "dropins/testEndpointPropertiesWeb.war/WEB-INF", "ibm-ws-bnd.xml");

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*testEndpointPropertiesWeb");
        String wsdl = getBaseUrl() + "/testEndpointPropertiesWeb/HelloNoWSDLService?wsdl";
        URL url = new URL(wsdl);

        HelloNoWSDLService service = new HelloNoWSDLService(url);
        // QName serviceQName = new QName("http://server.properties.endpoint.test.jaxws.ws.ibm.com/", "HelloNoWSDLService");
        //Service service = null;
        //service = Service.create(url, serviceQName);
        String result = service.getPort(HelloNoWSDLInterface.class).sayHello("Hello");
        assertTrue("Can not get expected result, the return result is: " + result,
                   result.equalsIgnoreCase("Hello Hello"));
        List<String> dumpInMessages = server.findStringsInLogs("Inbound Message");
        List<String> dumpOutMessages = server.findStringsInLogs("Outbound Message");
        assertTrue("Can't find inBoundMessage, the return inboundmessage is: " + dumpInMessages.toString(), !dumpInMessages.isEmpty());
        assertTrue("Can't find outBoundMessage, the return outboundmessage is: " + dumpOutMessages.toString(), !dumpOutMessages.isEmpty());

    }

    //override properties tests
    /**
     * TestDescription:
     * - Test the CXF message contextual property - org.apache.cxf.wsdl.create.imports
     * - Use webservice-endpoint-properties and webservice-endpoint/properties elements in binding file to config this property
     * - webservice-endpoint.properties should override webservice-endpoint-properties
     * Condition:
     * - A testEndpointPropertiesWeb.war publishes HelloService
     * - Config the org.apache.cxf.wsdl.create.imports="true" in binding file: WEB-INF/ibm-ws-bnd.xml
     * Result:
     * - the wsdl will import the HelloNoWSDLService_schema1.xsd instead of generating the schema in the wsdl
     */
    @Test
    public void testOverrideWSDLCreateImportsProperty() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "EndpointPropertiesTest", "ibm-ws-bnd_testOverrideWSDLCreateImportsProperty.xml",
                                      "dropins/testEndpointPropertiesWeb.war/WEB-INF", "ibm-ws-bnd.xml");

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*testEndpointPropertiesWeb");

        String wsdl = TestUtils.getServletResponse(getBaseUrl() + "/testEndpointPropertiesWeb/HelloNoWSDLService?wsdl");
        assertTrue("Can not access the HelloService's wsdl, the return result is: " + wsdl,
                   wsdl.contains("xsd=HelloNoWSDLService_schema1.xsd"));

    }

    /**
     * TestDescription:
     * - Test the CXF message contextual property - autoRewriteSoapAddress
     * - Use webservice-endpoint-properties and webservice-endpoint/properties elements in binding file to config this property
     * - webservice-endpoint.properties should override webservice-endpoint-properties
     * Condition:
     * - A testEndpointPropertiesWeb.war publishes HelloService
     * - Config the autoRewriteSoapAddress="false" in binding file: WEB-INF/ibm-ws-bnd.xml
     * - Because in liberty code we by default set autoRewriteSoapAddressForAllServices as true, we also need config the autoRewriteSoapAddressForAllServices="false"
     * Result:
     * - the wsdl will use "/HelloService" as the port address
     */
    //@Test Comment out the case as AutoRewriteSoapAddress did not work in CXF
    public void testOverrideAutoRewriteSoapAddressProperty() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "EndpointPropertiesTest", "ibm-ws-bnd_testOverrideAutoRewriteSoapAddressProperty.xml",
                                      "dropins/testEndpointPropertiesWeb.war/WEB-INF", "ibm-ws-bnd.xml");

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*testEndpointPropertiesWeb");

        String wsdl = TestUtils.getServletResponse(getBaseUrl() + "/testEndpointPropertiesWeb/HelloService?wsdl");
        assertTrue("Can not access the HelloService's wsdl, the return result is: " + wsdl,
                   wsdl.contains("soap:address location=\"/http://www.dragonwell.com/services/hello\""));

    }

    /**
     * TestDescription:
     * - Test the user defined property - enableLoggingInOutInterceptor
     * - Use webservice-endpoint-properties and webservice-endpoint/properties elements in binding file to config this property
     * - webservice-endpoint.properties should override webservice-endpoint-properties
     * Condition:
     * - A testEndpointPropertiesWeb.war publishes HelloService
     * - Config the enableLoggingInOutInterceptor="true" in binding file: WEB-INF/ibm-ws-bnd.xml
     * Result:
     * - incoming/outcoming messages will be dumped into message.log. Caution: if user enabled this setting for
     * their applicaion, any credential in the messages will be output in the log files.
     * - LoggingInOutInterceptors are replaced by LoggingFeature for jaxws-2.3 and xmlWS-3.0. This test will be skipped
     */
    @Test
    @SkipForRepeat({ "jaxws-2.3", JakartaEE9Action.ID })
    public void testOverrideLogginInOutInterceptorProperty() throws Exception {
        TestUtils.publishFileToServer(server,
                                      "EndpointPropertiesTest", "ibm-ws-bnd_testOverrideLogginInOutInterceptorProperty.xml",
                                      "dropins/testEndpointPropertiesWeb.war/WEB-INF", "ibm-ws-bnd.xml");

        server.startServer();
        server.waitForStringInLog("CWWKZ0001I.*testEndpointPropertiesWeb");
        String wsdl = getBaseUrl() + "/testEndpointPropertiesWeb/HelloNoWSDLService?wsdl";
        URL url = new URL(wsdl);
        HelloNoWSDLService service = new HelloNoWSDLService(url);
        //QName serviceQName = new QName("http://server.properties.endpoint.test.jaxws.ws.ibm.com/", "HelloNoWSDLService");
        //Service service = null;
        //service = Service.create(url, serviceQName);
        String result = service.getPort(HelloNoWSDLInterface.class).sayHello("HelloOverride");
        assertTrue("Can not get expected result, the return result is: " + result,
                   result.equalsIgnoreCase("Hello HelloOverride"));
        List<String> dumpInMessages = server.findStringsInLogs("Inbound Message");
        List<String> dumpOutMessages = server.findStringsInLogs("Outbound Message");
        assertTrue("Can't find inBoundMessage, the return inboundmessage is: " + dumpInMessages.toString(), !dumpInMessages.isEmpty());
        assertTrue("Can't find outBoundMessage, the return outboundmessage is: " + dumpOutMessages.toString(), !dumpOutMessages.isEmpty());

    }

    protected String getBaseUrl() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
    }
}
