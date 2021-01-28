/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.basic;

import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.EJBWithJAXRSFieldInjectionResource;

/**
 * Tests defect 103091
 */
@Path("statelessEJBWithJAXRSSecurityContextResource")
@Stateless
public class StatelessSecurityContextResource extends EJBWithJAXRSFieldInjectionResource {

    @GET
    public String getUserPrincipal() {
        //defect 103091
        //context.getUserPrincipal() will always return null, if defect 103091 not fixed, there will be a ClassCastException.
        return (getSecurityContext().getUserPrincipal() == null) ? "NULL" : getSecurityContext().getUserPrincipal().toString();
    }
}
