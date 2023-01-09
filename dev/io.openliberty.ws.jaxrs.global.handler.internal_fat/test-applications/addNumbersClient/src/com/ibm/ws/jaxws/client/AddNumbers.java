/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "AddNumbers", targetNamespace = "http://provider.jaxws.ws.ibm.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface AddNumbers {


    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws AddNegativesException
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "addNegatives", targetNamespace = "http://provider.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.client.AddNegatives")
    @ResponseWrapper(localName = "addNegativesResponse", targetNamespace = "http://provider.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.client.AddNegativesResponse")
    public String addNegatives(
        @WebParam(name = "arg0", targetNamespace = "")
        int arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        int arg1)
        throws AddNegativesException
    ;

    /**
     * 
     * @param arg1
     * @param arg0
     * @return
     *     returns java.lang.String
     * @throws AddNumbersException_Exception
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "addNumbers", targetNamespace = "http://provider.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.client.AddNumbers_Type")
    @ResponseWrapper(localName = "addNumbersResponse", targetNamespace = "http://provider.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.client.AddNumbersResponse")
    public String addNumbers(
        @WebParam(name = "arg0", targetNamespace = "")
        int arg0,
        @WebParam(name = "arg1", targetNamespace = "")
        int arg1)
        throws AddNumbersException_Exception
    ;

}
