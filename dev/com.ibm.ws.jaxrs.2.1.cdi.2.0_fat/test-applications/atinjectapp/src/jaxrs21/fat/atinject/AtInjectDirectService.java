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

import static jaxrs21.fat.atinject.AtInjectApp.toResponse;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/direct")
@RequestScoped
public class AtInjectDirectService {

    private final AbstractInjectedObject ctor;
    @Inject
    private FieldInjectedObject field;
    private AbstractInjectedObject method;

    //TODO: determine why no injection occurs unless this default ctor exists
    public AtInjectDirectService() {
        // default constructor - should not be used
        ctor = null;
    }

    @Inject
    public AtInjectDirectService(ConstructorInjectedObject ctor) {
        super();
        this.ctor = ctor;
    }

    @Inject
    protected void setMethodInjectedObject(MethodInjectedObject method) {
        this.method = method;
    }

    @GET
    @Path("/ctor")
    public String ctor() {
        return toResponse(ctor);
    }

    @GET
    @Path("/field")
    public String field() {
        return toResponse(field);
    }

    @GET
    @Path("/method")
    public String method() {
        return toResponse(method);
    }
}
