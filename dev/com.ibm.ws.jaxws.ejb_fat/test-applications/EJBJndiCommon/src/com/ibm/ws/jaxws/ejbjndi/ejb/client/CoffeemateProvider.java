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


package com.ibm.ws.jaxws.ejbjndi.ejb.client;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "CoffeemateProvider", targetNamespace = "http://common.ejbjndi.jaxws.ws.ibm.com/")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface CoffeemateProvider {


    /**
     * 
     * @param arg0
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "take", targetNamespace = "http://common.ejbjndi.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbjndi.ejb.client.Take")
    @ResponseWrapper(localName = "takeResponse", targetNamespace = "http://common.ejbjndi.jaxws.ws.ibm.com/", className = "com.ibm.ws.jaxws.ejbjndi.ejb.client.TakeResponse")
    public String take(
        @WebParam(name = "arg0", targetNamespace = "")
        String arg0);

}
