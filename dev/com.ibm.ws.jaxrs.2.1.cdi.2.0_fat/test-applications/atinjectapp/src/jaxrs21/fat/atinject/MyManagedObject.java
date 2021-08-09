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

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Dependent
public class MyManagedObject {

    private final AbstractInjectedObject ctor;
    @Inject
    private FieldInjectedObject field;
    private AbstractInjectedObject method;

    @Inject
    public MyManagedObject(ConstructorInjectedObject ctor) {
        super();
        this.ctor = ctor;
    }

    @Inject
    protected void setMethodInjectedObject(MethodInjectedObject method) {
        this.method = method;
    }

    @GET
    @Path("/ctor")
    public String getCtor() {
        return toResponse(ctor);
    }

    @GET
    @Path("/field")
    public String getField() {
        return toResponse(field);
    }

    @GET
    @Path("/method")
    public String getMethod() {
        return toResponse(method);
    }

}
