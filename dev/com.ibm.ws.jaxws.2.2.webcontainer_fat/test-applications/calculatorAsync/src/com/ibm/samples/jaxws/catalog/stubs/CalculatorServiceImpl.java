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

import javax.jws.WebService;

/**
 * Implementation of the Calculator web service
 */
@WebService(serviceName = "Calculator",
            portName = "CalculatorPort",
            targetNamespace = "http://catalog.jaxws.samples.ibm.com",
            endpointInterface = "com.ibm.samples.jaxws.catalog.stubs.CalculatorPortType")
public class CalculatorServiceImpl implements CalculatorService {

    /**
     * Implementation of the add operation
     *
     * @param value1 First integer value
     * @param value2 Second integer value
     * @return The sum of value1 and value2
     */
    @Override
    public int add(int value1, int value2) {
        return value1 + value2;
    }

}
