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
package com.ibm.ws.wsat.ejbcdi.client.ejb;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "EJBService", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface EJBService {


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
    @RequestWrapper(localName = "testEJBSayHelloToOther", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.ejb.TestEJBSayHelloToOther")
    @ResponseWrapper(localName = "testEJBSayHelloToOtherResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.ejb.TestEJBSayHelloToOtherResponse")
    public String testEJBSayHelloToOther(
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
    @RequestWrapper(localName = "testEJBSayHelloToOtherWithNever", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.ejb.TestEJBSayHelloToOtherWithNever")
    @ResponseWrapper(localName = "testEJBSayHelloToOtherWithNeverResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.ejb.TestEJBSayHelloToOtherWithNeverResponse")
    public String testEJBSayHelloToOtherWithNever(
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
    @RequestWrapper(localName = "testEJBSayHelloToOtherWithRequiresNew", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.ejb.TestEJBSayHelloToOtherWithRequiresNew")
    @ResponseWrapper(localName = "testEJBSayHelloToOtherWithRequiresNewResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.ejb.TestEJBSayHelloToOtherWithRequiresNewResponse")
    public String testEJBSayHelloToOtherWithRequiresNew(
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
    @RequestWrapper(localName = "testEJBSayHelloToOtherWithNotSupported", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.ejb.TestEJBSayHelloToOtherWithNotSupported")
    @ResponseWrapper(localName = "testEJBSayHelloToOtherWithNotSupportedResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.ejb.TestEJBSayHelloToOtherWithNotSupportedResponse")
    public String testEJBSayHelloToOtherWithNotSupported(
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
    @RequestWrapper(localName = "testEJBSayHelloToOtherWithMandatory", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.ejb.TestEJBSayHelloToOtherWithMandatory")
    @ResponseWrapper(localName = "testEJBSayHelloToOtherWithMandatoryResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.ejb.TestEJBSayHelloToOtherWithMandatoryResponse")
    public String testEJBSayHelloToOtherWithMandatory(
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
    @RequestWrapper(localName = "testEJBSayHelloToOtherWithSupports", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.ejb.TestEJBSayHelloToOtherWithSupports")
    @ResponseWrapper(localName = "testEJBSayHelloToOtherWithSupportsResponse", targetNamespace = "http://server.ejbcdi.wsat.ws.ibm.com/", className = "com.ibm.ws.wsat.ejbcdi.client.ejb.TestEJBSayHelloToOtherWithSupportsResponse")
    public String testEJBSayHelloToOtherWithSupports(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        String arg1)
        throws NamingException_Exception, SQLException_Exception
    ;

}
