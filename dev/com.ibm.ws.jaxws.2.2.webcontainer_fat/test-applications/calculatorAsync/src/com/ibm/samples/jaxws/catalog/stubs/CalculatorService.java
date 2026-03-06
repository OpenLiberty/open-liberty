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

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

/**
 * Calculator Web Service Interface
 * JAX-WS will automatically generate async methods for clients
 */
@WebService(name = "CalculatorService", targetNamespace = "http://service.example.com/")
public interface CalculatorService {

    /**
     * Add two numbers
     *
     * @param value1 first number
     * @param value2 second number
     * @return sum of value1 and value2
     */
    @WebMethod
    int add(@WebParam(name = "value1") int value1, @WebParam(name = "value2") int value2);

}
