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
package remoteApp.basic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;

/**
 *
 */
@Path("/basic")
@ApplicationPath("/")
public class BasicService extends Application {

    private static Map<String, Widget> widgets = new HashMap<>();

    @GET
    Set<String> getNames() {
        return widgets.keySet();
    }

    @GET
    @Path("/{name}")
    Widget get(@PathParam("name") String name) {
        Widget w = widgets.get(name);
        if (w == null) {
            throw new NotFoundException();
        }
        return w;
    }

    @POST
    void createNewWidget(Widget widget) {
        String name = widget.getName();
        if (widgets.containsKey(name)) {
            throw new WebApplicationException(409); // 409 Conflict
        }
        widgets.put(name, widget);
    }

    @PUT
    Widget updateWidget(Widget widget) {
        return widgets.put(widget.getName(), widget);
    }

    @DELETE
    @Path("/{name}")
    Widget delete(@PathParam("name") String name) {
        if (!widgets.containsKey(name)) {
            throw new NotFoundException();
        }
        return widgets.remove(name);
    }
}
