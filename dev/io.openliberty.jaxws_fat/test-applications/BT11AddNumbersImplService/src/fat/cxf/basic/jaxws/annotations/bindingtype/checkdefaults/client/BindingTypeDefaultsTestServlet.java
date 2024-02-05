/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package fat.cxf.basic.jaxws.annotations.bindingtype.checkdefaults.client;

import static org.junit.Assert.assertTrue;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.annotation.WebServlet;
import org.junit.Test;
import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/BindingTypeDefaultsTestServlet")
public class BindingTypeDefaultsTestServlet extends FATServlet {

    private static final String SERVICE_NS = "http://jaxws.basic.cxf.fats";

    //file names of WSDLs and XSD files used by the BT11 and BT12 applications
    private static String bt11WsdlName = "/BT11AddNumbersImplService.wsdl";
    //private static String bt11XsdName = "/BT11AddNumbersImplService_schema1.xsd";
    private static String bt12WsdlName = "/BT12AddNumbersImplService.wsdl";
    //private static String bt12XsdName = "/BT11AddNumbersImplService_schema1.xsd";

    private static String serviceClientUrl = "";
    private static String httpPortNumber = "";
    private static final StringReader reqMsg = new StringReader("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body xmlns=\"http://jaxws.basic.cxf.fats/types\"><invoke>JAXWS FVT Version: 2.0</invoke></soapenv:Body></soapenv:Envelope>");

    private static URL wsdlURLBt11;
    private static URL wsdlURLBt12;
    // Construct a single instance of the service client
    static {
        try {
            wsdlURLBt11 = new URL(new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/BT11AddNumbersImplService/services/BT11AddNumbersImplService?wsdl").toString());
            wsdlURLBt12 = new URL(new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/BT11AddNumbersImplService/services/BT12AddNumbersImplService?wsdl").toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /*
     *
     * Copied from tWAS based com.ibm.ws.jaxws_fat annotations/bindingtype/checkdefaults/** bucket
     *
     * This test method will verify that a service annotated with Soap11Http
     * binding works correctly.
     *
     * @testStrategy This test case invokes a service that is annotated with
     * Soap11Http binding and verifies that the service works correctly.
     */
    @Test
    public void testRuntimeBindingTypeSoap11Http() throws Exception {

        System.out.println("WSDL Location of Service is = " + wsdlURLBt11);

        int result = 0;
        result = AddNumbersClient11.addNumbers(10, 20, wsdlURLBt11);
        System.out.println("After running addNumbers on the client the result = " + result);
        assertTrue("****Failed with an Unexpected result: " + result + " != 30", (result == 30));
        System.out.println(" service responded with the correct answer...(" + result + ")");
    }

    /*
     *
     * Copied from tWAS based com.ibm.ws.jaxws_fat annotations/bindingtype/checkdefaults/** bucket
     *
     * This test method will verify that a service annotated with Soap12Http
     * binding works correctly.
     *
     * @testStrategy This test case invokes a service that is annotated with
     * Soap12Http binding and verifies that the service works correctly.
     */
    @Test
    public void testRuntimeBindingTypeSoap12Http() throws Exception {

        System.out.println("WSDL Location of Service is = " + wsdlURLBt12);

        int result = 0;
        result = AddNumbersClient12.addNumbers(10, 20, wsdlURLBt12);
        System.out.println("After running addNumbers on the client the result = " + result);
        assertTrue("****Failed with an Unexpected result: " + result + " != 30", (result == 30));
        System.out.println("test_runtime_NoWsdl(): " +
                           "service responded with the correct answer...(" + result + ")");
    }

}
