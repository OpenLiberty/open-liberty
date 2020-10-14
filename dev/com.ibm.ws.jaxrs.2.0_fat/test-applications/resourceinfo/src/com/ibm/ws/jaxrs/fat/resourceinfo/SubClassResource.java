/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.resourceinfo;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("subclass")
@Produces(MediaType.TEXT_PLAIN)
public class SubClassResource extends SimpleResource {

    @Context
    ResourceInfo resourceInfo;

    @Override
    public Response get() {
        return App.process(resourceInfo.getResourceClass(), resourceInfo.getResourceMethod());
    }
}
