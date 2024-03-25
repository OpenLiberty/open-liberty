/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws22.fat;

import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import fat.cxf.basic.jaxws.annotations.bindingtype.checkdefaults.client.BindingTypeDefaultsTestServlet;
import componenttest.custom.junit.runner.FATRunner;

/**
 *
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class BindingTypeDefaultsTest extends FATServletClient {
    private static Class<BindingTypeDefaultsTest> thisClass = BindingTypeDefaultsTest.class;

    private static final String APP_NAME1 = "BT11AddNumbersImplService";
    private static final String APP_NAME2 = "BT12AddNumbersImplService";

    //file names of WSDLs and XSD files used by the BT11 and BT12 applications
    private static String bt11WsdlName = "/BT11AddNumbersImplService.wsdl";
    //private static String bt11XsdName = "/BT11AddNumbersImplService_schema1.xsd";
    private static String bt12WsdlName = "/BT12AddNumbersImplService.wsdl";

    @Server("com.ibm.ws.jaxws22.annotations_fat")
    @TestServlet(servlet = BindingTypeDefaultsTestServlet.class, contextRoot = APP_NAME1)
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app1 = ShrinkHelper.buildDefaultApp(APP_NAME1, "fat.cxf.basic.jaxws.annotations.bindingtype.checkdefaults.server11",
                                                       "fat.cxf.basic.jaxws.annotations.bindingtype.checkdefaults.server12",
                                                       "fat.cxf.basic.jaxws.annotations.bindingtype.checkdefaults.client");

        ShrinkHelper.exportDropinAppToServer(server, app1);

        server.startServer("HolderServer.log");
        System.out.println("Starting Server");

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME1));

        //move wsdls needed for testing
        //server.copyFileToLibertyServerRoot(bt11WsdlName);
        //server.copyFileToLibertyServerRoot(bt11XsdName);
        //server.copyFileToLibertyServerRoot(bt12WsdlName);
        //server.copyFileToLibertyServerRoot(bt12XsdName);

        return;

    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * Copied from tWAS based com.ibm.ws.jaxws_fat annotations/bindingtype/checkdefaults/** bucket
     * This test method will very file that the Liberty's wsgen tool generated the wsdl with the
     * correct bindingtype. In this case SOAP11.
     *
     * Note about the migration of tWAS to Liberty
     * 1.) This artifact was statically generated at the time the test was written
     * 2.) The artifact was generated using wsgen rather than java2wsdl in the original
     * tWAS tests
     *
     * This test method will verify that the published endpoint implementation
     * of a class contains the correct binding as specified by the BindingType annotation.
     *
     * @testStrategy This test case generates the JAX-WS artifacts from the
     * java file. The service endpoint implementation (the service class)
     * specifies Soap11Http binding type and the wsgen tool must set it accordingly.
     * This test case checks that the generated wsdl file has the correct binding type.
     */
    @Test
    public void testWsgenBindingTypeSoap11Http() throws Exception {

        String thisMethod = "testWsgenBindingTypeSoap11Http()";

        String wsdlLocation = server.pathToAutoFVTTestFiles + bt11WsdlName;
        File f = new File(wsdlLocation);
        boolean _fileExists = f.exists();
        NodeList nodes = null;

        if (f.exists()) {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new File(wsdlLocation));
            XPath xpath = XPathFactory.newInstance().newXPath();

            String expression = "definitions"
                                + "/service[@name='AddNumbersImplService']"
                                + "/port[@name='AddNumbersImplPort']" + "/*";

            nodes = (NodeList) xpath.evaluate(expression, document,
                                              XPathConstants.NODESET);
        } else {
            fail("WSDL file was not found at: " + f.getPath());
        }

        assertTrue("soap binding not found in wsdl: " + "(" + _fileExists + ")",
                   _fileExists &&
                                                                                 (nodes.getLength() > 0) &&
                                                                                 nodes.item(0).getNodeName().equals("soap:address"));

        Log.info(thisClass, thisMethod, "xpath found: " + nodes.getLength() + " "
                                        + nodes.item(0).getNodeName());
    }

    /*
     * Copied from tWAS based com.ibm.ws.jaxws_fat annotations/bindingtype/checkdefaults/** bucket
     * This test method will very file that the Liberty's wsgen tool generated the wsdl with the
     * correct bindingtype. In this case SOAP12.
     *
     * Note about the migration of tWAS to Liberty
     * 1.) This artifact was statically generated at the time the test was written
     * 2.) The artifact was generated using wsgen rather than java2wsdl in the original
     * tWAS tests
     *
     * This test method will verify that the published endpoint implementation
     * of a class contains the correct binding as specified by the BindingType annotation.
     *
     * @testStrategy This test case generates the JAX-WS artifacts from the
     * java file. The service endpoint implementation (the service class)
     * specifies Soap12Http binding type and the wsgen tool must set it accordingly.
     * This test case checks that the generated wsdl file has the correct binding type.
     */
    @Test
    public void testWsgenBindingTypeSoap12Http() throws Exception {

        String thisMethod = "testWsgenBindingTypeSoap12Http()";

        String wsdlLocation = server.pathToAutoFVTTestFiles + bt12WsdlName;

        Log.info(thisClass, thisMethod, "--- wsdlLocation: " + wsdlLocation);

        File f = new File(wsdlLocation);
        boolean _fileExists = f.exists();
        NodeList nodes = null;

        if (f.exists()) {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new File(wsdlLocation));
            XPath xpath = XPathFactory.newInstance().newXPath();

            String expression = "definitions"
                                + "/service[@name='AddNumbersImplService']"
                                + "/port[@name='AddNumbersImplPort']" + "/*";

            nodes = (NodeList) xpath.evaluate(expression, document,
                                              XPathConstants.NODESET);
        } else {
            fail("WSDL file was not found at: " + f.getPath());
        }

        assertTrue("soap binding not found in wsdl: " + "(" + _fileExists + ")",
                   _fileExists &&
                                                                                 (nodes.getLength() > 0) &&
                                                                                 nodes.item(0).getNodeName().equals("soap12:address"));

        Log.info(thisClass, thisMethod, "xpath found: " + nodes.getLength() + " "
                                        + nodes.item(0).getNodeName());
    }

}
