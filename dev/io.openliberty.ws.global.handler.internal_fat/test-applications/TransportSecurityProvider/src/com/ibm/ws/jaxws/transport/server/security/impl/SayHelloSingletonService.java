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

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.jws.WebService;

import com.ibm.ws.jaxws.transport.server.security.SayHelloLocal;

@WebService(serviceName = "SayHelloSingletonService",
            portName = "SayHelloSingletonPort",
            name = "SayHello",
            targetNamespace = "http://ibm.com/ws/jaxws/transport/security/")
@Singleton(name = "SayHelloSingleBean")
@Startup
public class SayHelloSingletonService implements SayHelloLocal {
    @EJB(name = "ejb/singletondef/stateless", beanName = "SayHelloSessionBean")
    SayHelloLocal stateless;

    @Override
    public String sayHelloFromOther(String name) {
        return "From other bean: Hello, " + name + " from " + getClass().getSimpleName();
    }

    @Override
    public String sayHello(String name) {
        return stateless.sayHelloFromOther(name);
    }

}
