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

import java.sql.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path("/basic")
@ApplicationPath("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BasicService extends Application {
    private static Logger LOG = Logger.getLogger(BasicService.class.getName());
    private static Map<String, Widget> widgets = new HashMap<>();

    @GET
    public Set<String> getNames() {
        return widgets.keySet();
    }

    @GET
    @Path("/{name}")
    public Widget get(@PathParam("name") String name) {
        Widget w = widgets.get(name);
        if (w == null) {
            throw new NotFoundException();
        }
        return w;
    }

    @POST
    public void createNewWidget(Widget widget) {
        String name = widget.getName();
        if (widgets.containsKey(name)) {
            throw new WebApplicationException(409); // 409 Conflict
        }
        widgets.put(name, widget);
    }

    @PUT
    public Widget updateWidget(Widget widget) {
        return widgets.put(widget.getName(), widget);
    }

    @DELETE
    @Path("/{name}")
    public Widget delete(@PathParam("name") String name) {
        if (!widgets.containsKey(name)) {
            throw new NotFoundException();
        }
        return widgets.remove(name);
    }

    @POST
    @Path("/batch")
    public Response batch(Widget widget) {
        createNewWidget(widget);
        return Response.accepted().build();
    }

    @DELETE
    @Path("/batch")
    public Response unbatch(Widget widget) {
        return Response.accepted( delete(widget.getName()) ).build();
    }

    @GET
    @Path("/collections")
    public Set<Widget> getWidgets() {
        Set<Widget> set = new HashSet<>();
        for (Widget w : widgets.values()) {
            set.add(w);
        }
        return set;
    }

    @GET
    @Path("/collections/byName/{search}")
    public Map<String, Widget> getWidgetsByName(@PathParam("search") String search) {
        Map<String, Widget> matchingWidgets = new HashMap<>();
        widgets.keySet().stream()
                        .filter(s -> s.contains(search))
                        .forEach(s -> matchingWidgets.put(s, widgets.get(s)));
        return matchingWidgets;
    }

    @POST
    @Path("/collections")
    public int createNewWidgets(List<Widget> widget) {
        if (widget.stream().anyMatch(w -> widgets.containsKey(w.getName()))) {
            throw new WebApplicationException(409); // conflict
        }
        widget.stream().forEach(w -> widgets.put(w.getName(), w));
        return widget.size();
    }

    @DELETE
    @Path("/collections/byName")
    public Map<String, Widget> removeWidgets(Set<String> names) {
        Map<String, Widget> removedWidgets = new HashMap<>();
        names.stream().forEach(s -> removedWidgets.put(s, widgets.remove(s)));
        return removedWidgets;
    }

    @GET
    @Path("/date")
    public Date getCurrentDate() {
        Date d = new Date(System.currentTimeMillis());
        LOG.info("returning " + d);
        return d;
    }

    @POST
    @Path("/date")
    public Response echoDate(Date d) {
        Date d2 = new Date(d.getTime());
        LOG.info("given " + d + ", returning " + d2);
        return Response.status(202).entity(d2).build();
    }
}
