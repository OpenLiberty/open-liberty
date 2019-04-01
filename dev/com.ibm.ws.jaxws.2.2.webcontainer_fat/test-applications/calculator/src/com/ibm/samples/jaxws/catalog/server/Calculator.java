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
package com.ibm.samples.jaxws.catalog.server;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;

@WebService(name = "CalculatorPortType",
            targetNamespace = "http://catalog.jaxws.samples.ibm.com")
public interface Calculator {

    /**
     * @param value1
     * @param value2
     * @return returns int
     */
    @WebMethod(operationName = "add")
    @WebResult(name = "return", targetNamespace = "http://catalog.jaxws.samples.ibm.com")
    @RequestWrapper(localName = "add", targetNamespace = "http://catalog.jaxws.samples.ibm.com", className = "com.ibm.samples.jaxws.catalog.server.Add")
    @ResponseWrapper(localName = "addResponse", targetNamespace = "http://catalog.jaxws.samples.ibm.com", className = "com.ibm.samples.jaxws.catalog.server.AddResponse")
    public int add(@WebParam(name = "value1", targetNamespace = "http://catalog.jaxws.samples.ibm.com") int value1,
                   @WebParam(name = "value2", targetNamespace = "http://catalog.jaxws.samples.ibm.com") int value2);

}
