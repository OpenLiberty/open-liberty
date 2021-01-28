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
import javax.ejb.Stateless;
import javax.jws.WebService;

import com.ibm.ws.jaxws.transport.server.security.SayHelloLocal;

@WebService(serviceName = "SayHelloStatelessService",
            portName = "SayHelloStatelessPort",
            name = "SayHello",
            targetNamespace = "http://ibm.com/ws/jaxws/transport/security/")
@Stateless(name = "SayHelloSessionBean")
public class SayHelloStatelessService implements SayHelloLocal {
    @EJB(name = "ejb/statelessdef/singleton", beanName = "SayHelloSingleBean")
    SayHelloLocal singleton;

    @Override
    public String sayHelloFromOther(String name) {
        return "From other bean: Hello, " + name + " from " + getClass().getSimpleName();
    }

    @Override
    public String sayHello(String name) {
        return singleton.sayHelloFromOther(name);
    }

}
