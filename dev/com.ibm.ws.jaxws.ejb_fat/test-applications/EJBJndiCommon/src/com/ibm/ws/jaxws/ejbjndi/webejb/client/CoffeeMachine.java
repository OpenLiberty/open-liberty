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


package com.ibm.ws.jaxws.ejbjndi.webejb.client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "CoffeeMachine", targetNamespace = "http://common.ejbjndi.jaxws.ws.ibm.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface CoffeeMachine {


    /**
     * 
     * @param arg0
     * @return
     *     returns boolean
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "isSupported", targetNamespace = "http://common.ejbjndi.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbjndi.webejb.client.IsSupported")
    @ResponseWrapper(localName = "isSupportedResponse", targetNamespace = "http://common.ejbjndi.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbjndi.webejb.client.IsSupportedResponse")
    public boolean isSupported(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0);

    /**
     * 
     * @param arg0
     * @return
     *     returns com.ibm.ws.jaxws.ejbjndi.webejb.client.Coffee
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "make", targetNamespace = "http://common.ejbjndi.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbjndi.webejb.client.Make")
    @ResponseWrapper(localName = "makeResponse", targetNamespace = "http://common.ejbjndi.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbjndi.webejb.client.MakeResponse")
    public Coffee make(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0);

}
