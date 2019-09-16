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

import javax.jws.WebService;

@WebService(serviceName = "Calculator",
            portName = "CalculatorPort",
            endpointInterface = "com.ibm.samples.jaxws.catalog.server.Calculator",
            targetNamespace = "http://catalog.jaxws.samples.ibm.com",
            wsdlLocation = "http://foo.org/calculator.wsdl")
public class CalculatorService implements Calculator {

    /**
     * @param value1
     * @param value2
     * @return returns int
     */
    @Override
    public int add(int value1, int value2) {
        return value1 + value2;
    }
}
