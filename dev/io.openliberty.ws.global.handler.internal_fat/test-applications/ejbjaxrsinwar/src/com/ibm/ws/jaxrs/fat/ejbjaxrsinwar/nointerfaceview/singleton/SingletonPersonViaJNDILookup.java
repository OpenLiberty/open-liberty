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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;

/**
 * Tests that an EJB JNDI lookup does indeed get the {@link SingletonPersonAsEJB} EJB singleton and can increment the counter in
 * the singleton.
 */
@Path("singletonPersonViaJNDILookup")
public class SingletonPersonViaJNDILookup {

    private SingletonPersonAsEJB getSingletonPersonViaJNDI() {
        InitialContext ic;
        try {
            ic = new InitialContext();
            return (SingletonPersonAsEJB) ic.lookup("java:module/" + SingletonPersonAsEJB.class
                            .getSimpleName());
        } catch (NamingException e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    public String getCounter() {
        return getSingletonPersonViaJNDI().getCounter();
    }

    @DELETE
    public void resetCounter() {
        getSingletonPersonViaJNDI().resetCounter();
    }
}
