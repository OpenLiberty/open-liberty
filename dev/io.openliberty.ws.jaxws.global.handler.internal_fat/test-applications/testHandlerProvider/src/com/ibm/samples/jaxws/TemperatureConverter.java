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
package com.ibm.samples.jaxws;

import javax.jws.HandlerChain;
import javax.jws.WebParam;
import javax.jws.WebService;

/**
 *
 */
@WebService(name = "TemperatureConverter", serviceName = "TemperatureConverterService", portName = "TemperatureConverterPort", targetNamespace = "http://jaxws.samples.ibm.com/")
@HandlerChain(file = "handler/handler-test-provider.xml")
public class TemperatureConverter {
    public double celsiusToFahrenheit(@WebParam(name = "temperature") double temperature) {
        return (temperature * 9.0d / 5.0d + 32.0d);
    }
}
