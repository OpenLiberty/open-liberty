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
package com.ibm.samples.jaxws.client;

import javax.jws.WebService;

/**
 *
 */
@WebService(name = "SayHello", targetNamespace = "http://jaxws.samples.ibm.com.handler/")
public interface SayHelloService {
    public String sayHello(String name);
}
