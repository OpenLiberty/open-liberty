/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.client.servlet;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.servlet.annotation.WebServlet;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.WebServiceRef;

import org.junit.Test;

import com.ibm.ws.jaxws.client.AddNegativesException;
import com.ibm.ws.jaxws.client.AddNumbers;
import com.ibm.ws.jaxws.client.AddNumbersException;
import com.ibm.ws.jaxws.client.AddNumbersException_Exception;
import com.ibm.ws.jaxws.client.AddNumbers_Service;
import com.ibm.ws.jaxws.client.EqualNumbersException_Exception;
import com.ibm.ws.jaxws.client.LargeNumbersException_Exception;
import com.ibm.ws.jaxws.client.LocalName;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/WebFaultTestServlet")
public class WebFaultTestServlet extends FATServlet {

    @WebServiceRef
    private AddNumbers_Service serviceFromRef;

    private AddNumbers addNumbers = null;

    /*
     *
     * Negative test case that invokes the provider with one non negative number. This returns a SOAP Fault
     * with a custom exception which contains a FaultInfo field (LocalName). This test checks that the expected LocalName.getInfo() returns the correct
     * String
     *
     * It will fail if the AddNegativesException
     *
     * 1.) Does not contain expected FaultInfo (LocalName)
     * 2.) Does not contain the expected string LocalName.getInfo()
     * 3.) No AddNegativesException is thrown.
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testAddNegativesExceptionFaultInfo() throws Exception {

        if (addNumbers == null) {
            setEndpointAddressProperty();
        }

        try {
            String addedNumbers = addNumbers.addNegatives(-4, 12); // Invoking the service with negative numbers throws AddNumbersException
            fail("Calling addNegatives(-4, 12) should returned an exception, but it returned " + addedNumbers);
        } catch (AddNegativesException ane) {
            // Verify the exception contains the FaultInof has been unmarshalled
            assertNotNull("AddNumbersException does not contain expected FaultInfo element", ane.getFaultInfo());
            LocalName ln = ane.getFaultInfo();
            // Verify the message is the expected response
            assertTrue("FaultInfo, LocalName, does not contain expected info = " + ln.getInfo(), ln.getInfo().equals("Expected all negative numbers."));
        }
    }

    /*
     *
     * Negative test case that invokes the provider with large numbers. This returns a SOAP Fault
     * with a custom exception which contains a message field that override the message obtained from Throwable.getMessage(). This test checks that the expected
     * LargeNumberException.getMessage() returns the
     * correct
     * String
     *
     * It will fail if the LargeNumbersException
     *
     * 1.) Does not contain a value in the returned LargeNumbersException.getMessage()
     * 2.) Does not contain the expected string in LargeNumbersException.getMessage()
     * 3.) No LargeNumbersException is thrown.
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testLargeNumbersExceptionWithOverridenMessage() throws Exception {

        if (addNumbers == null) {
            setEndpointAddressProperty();
        }

        try {
            String addedNumbers = addNumbers.addNumbers(200000, 12); // Invoking the service with negative numbers throws AddNumbersException
            fail("Calling addNumbers(200000, 12) should returned an exception, but it returned " + addedNumbers);
        } catch (LargeNumbersException_Exception lne) {
            // Verify the exception contains the FaultInof has been unmarshalled
            assertNotNull("LargeNumbersException does not contain expected FaultInfo element", lne.getMessage());
            // Verify the message is the expected response
            assertTrue("LargeNumbersException does not contain expected message = " + lne.getMessage(), lne.getMessage().equals("Expected all numbers less than 10000"));
        }
    }

    /*
     *
     * Negative test case that invokes provider with equal numbers. This returns a SOAP Fault
     * with a custom exception which contains a <message> element inherited from the java.lang.Throwable superClass. Between the 22.0.0.8
     * and 23.0.0.10 releases of Liberty, this message element is missing due to a breaking change in CXF's
     * org.apache.cxf.jaxb.Utils class.
     *
     * It will fail if the EqualNumbersException:
     *
     * 1.) Does not contain a message element
     * 2.) Does not contain the expected message content
     * 3.) No EqualNumbersException is thrown.
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testEqualNumbersExceptionWithInhereitedMessageElementAndAdditionalFields() throws Exception {
        if (addNumbers == null) {
            setEndpointAddressProperty();
        }
        try {
            String addedNumbers = addNumbers.addNumbers(1, 1); // Invoking the service with negative numbers throws AddNumbersException
            fail("Calling addNumbers(1, 1) should have returned an exception, but it returned " + addedNumbers);
        } catch (EqualNumbersException_Exception ene) {
            // Verify the exception contains the message element has been unmarshalled
            assertNotNull("EqualNumbersException does not contain expected message element ", ene.getMessage());
            // Verify the message is the expected response
            assertTrue("EqualNumbersException does not contain expected message element was - " + ene.getMessage() + ", but expected - Expected all unequal numbers.",
                       ene.getMessage().equals("Expected all unequal numbers."));
        }
    }

    /*
     *
     * Negative test case that invokes provider with numbers that sum to less that zero. This returns a SOAP Fault
     * with a custom exception marshalled in the <detail> element of the soap fault, where the message field is present
     * but the method that returns it is getInfo() - not getMessage()
     *
     * It will fail if the AddNumbersException_Exception:
     *
     * 1.) Does not contain a message element
     * 2.) Does not contain the expected message content
     * 3.) No AddNumbersException_Exception is thrown.
     */
    @Test
    @Mode(Mode.TestMode.FULL)
    public void testAddNumbersExceptionWithGetInfo() throws Exception {
        if (addNumbers == null) {
            setEndpointAddressProperty();
        }
        try {
            String addedNumbers = addNumbers.addNumbers(1, -2);
            fail("Calling addNumnbers(1,-2) should returned an exception, but it returned " + addedNumbers);
        } catch (AddNumbersException_Exception ane) {

            // Verify the exception contains the FaultInof has been unmarshalled
            assertNotNull("AddNumbersException does not contain expected FaultInfo element", ane.getFaultInfo());
            AddNumbersException aneInfo = ane.getFaultInfo();
            // Verify the message is the expected response
            assertTrue("FaultInfo, LocalName, does not contain expected info = " + aneInfo.getInfo() + ", but expected - Sum is less than 0.",
                       aneInfo.getInfo().equals("Sum is less than 0."));
        }

    }

    /**
     *
     */
    private void setEndpointAddressProperty() {
        addNumbers = serviceFromRef.getAddNumbersPort();
        ((BindingProvider) addNumbers).getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                                                               new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/addNumbersProvider/AddNumbers").toString());
    }
}
