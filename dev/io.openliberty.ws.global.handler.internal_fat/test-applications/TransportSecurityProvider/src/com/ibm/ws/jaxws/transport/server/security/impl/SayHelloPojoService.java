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
package com.ibm.ws.jaxws.transport.server.security.impl;

import javax.jws.WebService;

import com.ibm.ws.jaxws.transport.server.security.SayHelloService;

@WebService(serviceName = "SayHelloPojoService",
            portName = "SayHelloPojoPort",
            name = "SayHello",
            targetNamespace = "http://ibm.com/ws/jaxws/transport/security/")
public class SayHelloPojoService implements SayHelloService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name + " from " + getClass().getSimpleName();
    }

}
