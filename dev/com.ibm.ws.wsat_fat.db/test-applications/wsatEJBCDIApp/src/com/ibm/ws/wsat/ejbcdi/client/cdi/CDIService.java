/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.ejbcdi.client.cdi;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "CDIService", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface CDIService {


    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws NamingException_Exception
     * @throws SQLException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "testCDISayHelloToOtherWithMandatory", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.cdi.TestCDISayHelloToOtherWithMandatory")
    @ResponseWrapper(localName = "testCDISayHelloToOtherWithMandatoryResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.cdi.TestCDISayHelloToOtherWithMandatoryResponse")
    public String testCDISayHelloToOtherWithMandatory(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        String arg1)
        throws NamingException_Exception, SQLException_Exception
    ;

    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws NamingException_Exception
     * @throws SQLException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "testCDISayHelloToOtherWithNever", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.cdi.TestCDISayHelloToOtherWithNever")
    @ResponseWrapper(localName = "testCDISayHelloToOtherWithNeverResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.cdi.TestCDISayHelloToOtherWithNeverResponse")
    public String testCDISayHelloToOtherWithNever(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        String arg1)
        throws NamingException_Exception, SQLException_Exception
    ;

    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws NamingException_Exception
     * @throws SQLException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "testCDISayHelloToOtherWithNotSupported", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.cdi.TestCDISayHelloToOtherWithNotSupported")
    @ResponseWrapper(localName = "testCDISayHelloToOtherWithNotSupportedResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.cdi.TestCDISayHelloToOtherWithNotSupportedResponse")
    public String testCDISayHelloToOtherWithNotSupported(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        String arg1)
        throws NamingException_Exception, SQLException_Exception
    ;

    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws NamingException_Exception
     * @throws SQLException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "testCDISayHelloToOtherWithSupports", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.cdi.TestCDISayHelloToOtherWithSupports")
    @ResponseWrapper(localName = "testCDISayHelloToOtherWithSupportsResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.cdi.TestCDISayHelloToOtherWithSupportsResponse")
    public String testCDISayHelloToOtherWithSupports(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        String arg1)
        throws NamingException_Exception, SQLException_Exception
    ;

    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws NamingException_Exception
     * @throws SQLException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "testCDISayHelloToOther", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.cdi.TestCDISayHelloToOther")
    @ResponseWrapper(localName = "testCDISayHelloToOtherResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.cdi.TestCDISayHelloToOtherResponse")
    public String testCDISayHelloToOther(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        String arg1)
        throws NamingException_Exception, SQLException_Exception
    ;

    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws NamingException_Exception
     * @throws SQLException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "testCDISayHelloToOtherWithRequiresNew", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.cdi.TestCDISayHelloToOtherWithRequiresNew")
    @ResponseWrapper(localName = "testCDISayHelloToOtherWithRequiresNewResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.cdi.TestCDISayHelloToOtherWithRequiresNewResponse")
    public String testCDISayHelloToOtherWithRequiresNew(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        String arg1)
        throws NamingException_Exception, SQLException_Exception
    ;

}
