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
package mpRestClient10.collections;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/collections")
public interface CollectionsClient {
    @GET
    Set<Widget> getWidgets();

    @GET
    @Path("/byName/{search}")
    Map<String, Widget> getWidgetsByName(@PathParam("search") String search) throws UnknownWidgetException;

    @POST
    int createNewWidgets(List<Widget> widget) throws DuplicateWidgetException;


    @DELETE
    @Path("/byName")
    Map<String, Widget> removeWidgets(Set<String> names) throws UnknownWidgetException;
    
    @GET
    CompletionStage<Set<Widget>> getWidgetsAsync();

    @GET
    @Path("/byName/{search}")
    CompletionStage<Map<String, Widget>> getWidgetsByNameAsync(@PathParam("search") String search) 
                    throws UnknownWidgetException;

}