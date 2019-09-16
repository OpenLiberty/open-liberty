/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

@Path("/direct2")
//public no-arg constructors are not required when the scope is @Dependent, but a public no-arg constructor is required for any other "normal" scope as defined by the CDI spec.
@Dependent
public class AtInjectDirectService2 {

    private final AbstractInjectedObject ctor;

    @Inject
    public AtInjectDirectService2(ConstructorInjectedObject ctor) {
        super();
        this.ctor = ctor;
    }

    @GET
    @Path("/ctor")
    public String ctor() {
        return toResponse(ctor);
    }
}
