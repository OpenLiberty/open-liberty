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
package com.ibm.ws.cdi.services.impl;

import javax.annotation.Resource;
import javax.jws.WebService;

import com.ibm.ws.cdi.services.SayHelloService;

@WebService(serviceName = "SayHelloPojoService", portName = "SayHelloPojoPort",
            endpointInterface = "com.ibm.ws.cdi.services.SayHelloService")
public class SayHelloPojoService implements SayHelloService {

    @Resource(name = "secondName")
    String mySecondName;

    @Override
    public String sayHello(String name) {
        return "Hello, " + name + " from " + mySecondName + " in " + getClass().getSimpleName();
    }

}