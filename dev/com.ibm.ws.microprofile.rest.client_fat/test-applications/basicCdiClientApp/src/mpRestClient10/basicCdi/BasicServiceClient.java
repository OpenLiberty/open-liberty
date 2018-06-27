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
package mpRestClient10.basicCdi;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RegisterProvider(DuplicateWidgetExceptionMapper.class)
@RegisterProvider(UnknownWidgetExceptionMapper.class)
@RegisterProvider(MyFilter.class)
@RegisterRestClient
@Path("/")
public interface BasicServiceClient {
    @GET
    Set<String> getWidgetNames();

    @GET
    @Path("/{name}")
    Widget getWidget(@PathParam("name") String name) throws UnknownWidgetException;

    @POST
    void createNewWidget(Widget widget) throws DuplicateWidgetException;

    @PUT
    Widget putWidget(Widget widget);

    @DELETE
    @Path("/{name}")
    Widget removeWidget(@PathParam("name") String name) throws UnknownWidgetException;

}