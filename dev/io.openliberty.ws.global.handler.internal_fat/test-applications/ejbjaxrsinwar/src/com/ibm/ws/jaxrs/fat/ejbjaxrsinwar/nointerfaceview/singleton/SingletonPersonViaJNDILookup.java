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
