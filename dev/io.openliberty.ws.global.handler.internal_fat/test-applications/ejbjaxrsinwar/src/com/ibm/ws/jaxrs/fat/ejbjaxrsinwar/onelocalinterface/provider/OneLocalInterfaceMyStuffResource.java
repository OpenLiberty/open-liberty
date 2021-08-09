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
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.onelocalinterface.provider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("oneLocalInterfaceMyStuffResource")
public class OneLocalInterfaceMyStuffResource {

    @GET
    @Produces("my/stuff")
    public String hello() {
        return "Ignored string"; // see the provider
    }

    @GET
    @Produces("my/otherstuff")
    public String helloOther() {
        return "Ignored string"; // see the provider
    }
}
