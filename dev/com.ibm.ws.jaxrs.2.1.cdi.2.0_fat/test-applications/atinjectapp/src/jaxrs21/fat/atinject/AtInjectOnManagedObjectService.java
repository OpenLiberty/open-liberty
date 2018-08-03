/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.atinject;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/viaManagedObject")
@RequestScoped
public class AtInjectOnManagedObjectService {

    @Inject
    MyManagedObject mo;

    @GET
    @Path("/ctor")
    public String ctor() {
        return mo.getCtor();
    }

    @GET
    @Path("/field")
    public String field() {
        return mo.getField();
    }

    @GET
    @Path("/method")
    public String method() {
        return mo.getMethod();
    }

}
