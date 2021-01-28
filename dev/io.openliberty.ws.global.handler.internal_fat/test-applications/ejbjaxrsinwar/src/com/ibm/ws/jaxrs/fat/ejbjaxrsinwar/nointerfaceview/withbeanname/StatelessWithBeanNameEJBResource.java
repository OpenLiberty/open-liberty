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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.withbeanname;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Stateless(name = "MyStatelessWithBeanNameEJBResource")
@Path("/statelessWithBeanNameEJBResource")
public class StatelessWithBeanNameEJBResource {

    @Resource(name = "injectedString")
    private String injectedString;

    @GET
    public String get() {
        return StatelessWithBeanNameEJBResource.class.getName() + injectedString;
    }
}
