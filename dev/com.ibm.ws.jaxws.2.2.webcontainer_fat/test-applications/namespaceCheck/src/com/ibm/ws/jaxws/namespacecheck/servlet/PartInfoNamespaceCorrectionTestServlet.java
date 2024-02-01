/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.jaxws.namespacecheck.servlet;

import static org.junit.Assert.fail;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.Service;

import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.junit.Test;

import com.ibm.ws.jaxws.namespacecheck.wsdl.importedserviceschema.PersonType;
import com.ibm.ws.jaxws.namespacecheck.wsdl.mainservice.CustomFaultDetail;
import com.ibm.ws.jaxws.namespacecheck.wsdl.mainservice.NameSpaceCheckPortType;
import com.ibm.ws.jaxws.namespacecheck.wsdl.mainserviceschema.DummyObjectTYPE;
import com.ibm.ws.jaxws.namespacecheck.wsdl.mainserviceschema.NameSpaceCheckMessage;

import componenttest.app.FATServlet;

/**
 * This test suite covers some changes we've had to make to account for bugs introduced when we upgraded
 * to CXF 3.4, and subsequent fixes built on top of that initial fix. Specifically, we're testing the
 * changes we've introduced to the {@link org.apache.cxf.binding.soap.SoapBindingFactory#initializeMessage(SoapBindingInfo bi, BindingOperationInfo boi, BindingMessageInfo bmsg)}
 * method.
 *
 * TODO: Add Handler NPE Test
 *
 * @see com.ibm.ws.jaxws.fat.PartInfoNamespaceCorrectionTest - Fat Suite Class
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PartInfoNamespaceCorrectionTestServlet")
public class PartInfoNamespaceCorrectionTestServlet extends FATServlet {

    private static final Logger LOG = Logger.getLogger("NamespaceCheckTestServletLogger");

    // Single service client parameters
    private static URL WSDL_URL;
    private static QName qname;
    private static QName portName;
    private static Service service;
    private static NameSpaceCheckPortType proxy;

    // Construct a single instance of the service client
    static {
        try {
            WSDL_URL = new URL(new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/namespaceCheck/MainService?wsdl").toString());

            qname = new QName("http://com/ibm/services/wsdl/MainService.wsdl", "NameSpaceCheckPortType");
            portName = new QName("http://com/ibm/services/wsdl/MainService.wsdl", "NameSpaceCheckPortType");
            service = Service.create(qname);
            proxy = service.getPort(portName, NameSpaceCheckPortType.class);

            String newTarget = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/namespaceCheck/NameSpaceCheckService";
            BindingProvider bp = (BindingProvider) proxy;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, newTarget);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * This test ensures that our fix to correct the MessagePartInfo namespace mismatching in CXF doesn't cause
     * out-of-bound SOAP Faults, (faults that are described in a parent WSDL and imported under a different namespace by
     * a child WSDL), to unmarshall the fault as a generic {@link javax.xml.ws.soap.SOAPFaultException} rather than the
     * correct and expected custom fault. The SOAPFault returned by our provider should always cause a {@link CustomFaultDetail} to
     * be thrown. We will automatically fail this test for three reasons:
     *
     * 1.) No SOAPFault is returned by the Web Service Provider
     * 2.) The Web Service Client throws an exception when invoking the provider
     * 3.) A generic SOAPFault is returned by the Web Service Provider, rather than the expected {@link CustomFaultDetail}
     */
    @Test
    public void partInfoNamespaceCorrectionTest() {
        try {
            NameSpaceCheckMessage nameSpaceCheckMessage = new NameSpaceCheckMessage();
            DummyObjectTYPE dot = new DummyObjectTYPE();
            dot.setID(new BigInteger("1234567890"));
            dot.setDESCRIPTION("Description of dummy object");
            nameSpaceCheckMessage.setDummyObject(dot);

            Holder<PersonType> hpt = new Holder();
            PersonType pt = new PersonType();
            pt.setName("John Doe");
            pt.setEmail("john.doe@dummy.email.com");
            hpt.value = pt;
            proxy.nameSpaceCheck(nameSpaceCheckMessage, hpt);
            fail("CustomFaultDeatil wasn't caught, and no exception was returned by the provider");
        } catch (CustomFaultDetail e) {
            // If we catch the CustomFaultDetail
            e.printStackTrace();
            return;
        } catch (Exception e1) {
            fail("CustomFaultDeatil wasn't the caught exception");
        }
    }

}
