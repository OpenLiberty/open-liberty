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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.withbeanname;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("oneLocalInterfaceWithBeanNameEJB")
public interface OneLocalInterfaceWithBeanNameView {

    @GET
    public String getCounter();

    @DELETE
    public void resetCounter();
}
