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
