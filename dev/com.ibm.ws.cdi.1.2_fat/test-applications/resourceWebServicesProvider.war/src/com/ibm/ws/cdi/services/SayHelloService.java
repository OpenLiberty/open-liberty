package com.ibm.ws.cdi.services;

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

import javax.jws.WebService;

@WebService(name = "SayHello", targetNamespace = "http://ibm.com/ws/jaxws/cdi/")
public interface SayHelloService {
    String sayHello(String name);
}