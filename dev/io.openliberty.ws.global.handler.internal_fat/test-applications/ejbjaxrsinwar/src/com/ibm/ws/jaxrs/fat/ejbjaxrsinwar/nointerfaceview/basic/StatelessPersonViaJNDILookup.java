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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.basic;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;

/**
 * Tests that an EJB lookup of the {@link StatelessPersonAsEJB} does in fact
 * return an EJB which has the proper injections.
 */
@Path("/statelessPersonViaJNDILookup/{name}")
public class StatelessPersonViaJNDILookup {

    private StatelessPersonAsEJB getPersonViaJNDI() {
        InitialContext ic;
        try {
            ic = new InitialContext();
            return (StatelessPersonAsEJB) ic.lookup("java:module/" + StatelessPersonAsEJB.class
                            .getSimpleName());
        } catch (NamingException e) {
            throw new WebApplicationException(e);
        }
    }

    @GET
    public String getPersonInfo(@PathParam("name") String aName) {
        return getPersonViaJNDI().getPersonInfo(aName);
    }
}
