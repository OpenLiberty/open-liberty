/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.jaxws.transport.server.security;

import javax.ejb.Local;

/**
 * The local EJB interface
 */
@Local
public interface SayHelloLocal extends SayHelloService {
    String sayHelloFromOther(String name);
}
