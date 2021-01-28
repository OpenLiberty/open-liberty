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

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * This JAX-RS resource is a simple stateless session EJB with a no-interface
 * view. In order for it to work, the {@link Resource} injections must happen
 * (so unless magic happens, it must be retrieved as an EJB).
 */
@Path("statelessPersonAsEJB/{name}")
@Stateless
public class StatelessPersonAsEJB {
    /*
     * the @Resource values come from the deployment descriptor.
     */

    @Resource(name = "namePrefix")
    private String namePrefix;

    private String suffix;

    @Resource
    public void setNameSuffix(String nameSuffix) {
        this.suffix = nameSuffix;
    }

    public String getSuffix() {
        return suffix;
    }

    @GET
    public String getPersonInfo(@PathParam("name") String aName) {
        return namePrefix + aName + getSuffix();
    }
}
