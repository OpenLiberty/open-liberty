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
package com.ibm.samples.jaxws.testhandlerprovider;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jws.HandlerChain;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService(name = "TemperatureConverter",
            serviceName = "TemperatureConverterService",
            portName = "TemperatureConverterPort",
            targetNamespace = "http://jaxws.samples.ibm.com/")
@HandlerChain(file = "handler/handler-test-provider.xml")
public class TemperatureConverter {

    @PostConstruct
    public void init() {
        System.out.println(this.getClass().getName() + ": postConstruct is invoked");
    }

    @PreDestroy
    public void shutdown() {
        System.out.println(this.getClass().getName() + ": PreDestroy is invoked");
    }

    public double celsiusToFahrenheit(@WebParam(name = "temperature") double temperature) {
        return (temperature * 9.0d / 5.0d + 32.0d);
    }
}
