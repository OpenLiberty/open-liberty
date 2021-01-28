/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
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
