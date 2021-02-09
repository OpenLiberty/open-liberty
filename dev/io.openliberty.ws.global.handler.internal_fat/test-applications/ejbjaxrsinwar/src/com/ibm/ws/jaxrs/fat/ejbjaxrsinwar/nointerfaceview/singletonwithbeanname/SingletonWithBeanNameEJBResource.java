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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview.singletonwithbeanname;

import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * This JAX-RS resource is a simple singleton session EJB with a no-interface
 * view with a customized bean name. The test is with multiple invocations to
 * make sure that the counter is correct. Note that this doesn't actually
 * guarantee this is a singleton (it could be the same stateless session bean
 * every single time). However, since there isn't a {@link Stateless} or EJB
 * deployment descriptor in this test that makes this a session bean, let's
 * assume this works.
 */
@Path("singletonWithBeanNameEJBResource")
@Singleton(name = "MySingletonWithBeanNameEJBResource")
public class SingletonWithBeanNameEJBResource {

    private int counter = 0;

    @GET
    public String getCounter() {
        ++counter;
        return String.valueOf(counter);
    }

    @DELETE
    public void resetCounter() {
        counter = 0;
    }

}
