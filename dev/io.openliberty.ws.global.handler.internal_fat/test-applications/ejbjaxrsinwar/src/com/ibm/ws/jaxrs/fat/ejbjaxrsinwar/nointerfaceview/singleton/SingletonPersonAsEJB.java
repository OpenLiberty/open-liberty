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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.singleton;

import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * This JAX-RS resource is a simple singleton session EJB with a no-interface
 * view. The test is with multiple invocations to make sure that the counter is
 * correct. Note that this doesn't actually guarantee this is a singleton (it
 * could be the same stateless session bean every single time). However, since
 * there isn't a {@link Stateless} or EJB deployment descriptor in this test
 * that makes this a session bean, let's assume this works.
 */
@Path("singletonPersonAsEJB")
@Singleton
public class SingletonPersonAsEJB {

    private int counter = 0;

    @GET
    public String getCounter() {
        ++counter;
        return String.valueOf(counter);
    }

    @DELETE
    public void resetCounter() {
        counter = 0;
    }

}
