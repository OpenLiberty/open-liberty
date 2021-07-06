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
package com.ibm.ws.samples.jaxws.sharedlib;

import javax.jws.WebService;

@WebService(serviceName = "CalculatorSvcName", portName = "CalculatorPort", endpointInterface = "com.ibm.ws.samples.jaxws.sharedlib.Calculator",
            targetNamespace = "http://sharedlib.jaxws.samples.ibm.com")
public class CalculatorService implements Calculator {
    public int add(int a, int b) {
        return a + b;
    }
}
