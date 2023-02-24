/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxrs.ejbsubresource;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

@Singleton
@Path("/root")
public class SingletonRootResource {

    // For some reason this line is needed for @Context injection to work in LocalSingletonBean
    @Context private UriInfo uri;

    @EJB
    ILocalSingletonBean bean;

    @Path("/sub")
    public ILocalSingletonBean getSubResource() {
        return bean;
    }
}