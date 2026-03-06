/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.samples.jaxws.catalog.stubs;

import java.util.concurrent.Future;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.Response;
import javax.xml.ws.ResponseWrapper;

/**
 * Service Interface for Calculator Web Service
 */
@WebService(name = "CalculatorPortType", targetNamespace = "http://catalog.jaxws.samples.ibm.com")
@XmlSeeAlso({
              ObjectFactory.class
})
public interface CalculatorPortType {

    /**
     * Add two integer values
     *
     * @param value1 First integer value
     * @param value2 Second integer value
     * @return The sum of value1 and value2
     */
    @WebMethod
    @WebResult(targetNamespace = "http://catalog.jaxws.samples.ibm.com")
    @RequestWrapper(localName = "add", targetNamespace = "http://catalog.jaxws.samples.ibm.com", className = "com.ibm.samples.jaxws.catalog.stubs.Add")
    @ResponseWrapper(localName = "addResponse", targetNamespace = "http://catalog.jaxws.samples.ibm.com", className = "com.ibm.samples.jaxws.catalog.stubs.AddResponse")
    public int add(
                   @WebParam(name = "value1", targetNamespace = "http://catalog.jaxws.samples.ibm.com") int value1,
                   @WebParam(name = "value2", targetNamespace = "http://catalog.jaxws.samples.ibm.com") int value2);

    /**
     * Add two integer values
     *
     * @param value1 First integer value
     * @param value2 Second integer value
     * @return The sum of value1 and value2
     */
    @WebMethod(operationName = "add")
    @WebResult(targetNamespace = "http://catalog.jaxws.samples.ibm.com")
    @RequestWrapper(localName = "add", targetNamespace = "http://catalog.jaxws.samples.ibm.com", className = "com.ibm.samples.jaxws.catalog.stubs.Add")
    @ResponseWrapper(localName = "addResponse", targetNamespace = "http://catalog.jaxws.samples.ibm.com", className = "com.ibm.samples.jaxws.catalog.stubs.AddResponse")
    public Response<AddResponse> addAsync(
                                          @WebParam(name = "value1", targetNamespace = "http://catalog.jaxws.samples.ibm.com") int value1,
                                          @WebParam(name = "value2", targetNamespace = "http://catalog.jaxws.samples.ibm.com") int value2);

    /**
     * Add two integer values
     *
     * @param value1 First integer value
     * @param value2 Second integer value
     * @return The sum of value1 and value2
     */
    @WebMethod(operationName = "add")
    @WebResult(targetNamespace = "http://catalog.jaxws.samples.ibm.com")
    @RequestWrapper(localName = "add", targetNamespace = "http://catalog.jaxws.samples.ibm.com", className = "com.ibm.samples.jaxws.catalog.stubs.Add")
    @ResponseWrapper(localName = "addResponse", targetNamespace = "http://catalog.jaxws.samples.ibm.com", className = "com.ibm.samples.jaxws.catalog.stubs.AddResponse")
    public Future<?> addAsync(
                              @WebParam(name = "value1", targetNamespace = "http://catalog.jaxws.samples.ibm.com") int value1,
                              @WebParam(name = "value2", targetNamespace = "http://catalog.jaxws.samples.ibm.com") int value2,
                              @WebParam(name = "asyncHandler", targetNamespace = "http://catalog.jaxws.samples.ibm.com") AsyncHandler<AddResponse> asyncHandler);
}
