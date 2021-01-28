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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("oneLocalInterfaceMyStuffResource")
public class OneLocalInterfaceMyStuffResource {

    @GET
    @Produces("my/stuff")
    public String hello() {
        return "Ignored string"; // see the provider
    }

    @GET
    @Produces("my/otherstuff")
    public String helloOther() {
        return "Ignored string"; // see the provider
    }
}
