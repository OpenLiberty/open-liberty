/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
