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
import javax.jws.WebService;

import com.ibm.samples.jaxws.service.SayHelloService;

/**
 *
 */
@WebService(serviceName = "SayHelloService", portName = "SayHelloPort", targetNamespace = "http://jaxws.samples.ibm.com.handler/")
@HandlerChain(file = "handler/handler-test-provider.xml")
public class SayHelloServiceImpl implements SayHelloService {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.samples.jaxws.service.SayHelloService#sayHello(java.lang.String)
     */
    @Override
    public String sayHello(String name) {
        return "Hello," + name;
    }

}
