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
package mpRestClient12.headerPropagation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/resource")
@ClientHeaderParam(name="InterfaceHeader", value="abc")
@RegisterRestClient
public interface ClientHeaderParamClient {

    @GET
    @Path("/normal")
    @ClientHeaderParam(name="MethodHeader", value="def")
    String normalMethod();
}
