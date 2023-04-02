// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.event.resources;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.openliberty.guides.event.dao.EventDao;
import io.openliberty.guides.event.models.Event;

// tag::RequestedScoped[]
@RequestScoped
// end::RequestedScoped[]
@Path("events")
// tag::DAO[]
// tag::EventResource[]
public class EventResource {

    @Inject
    private EventDao eventDAO;

    /**
     * This method creates a new event from the submitted data (name, time and
     * location) by the user.
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    // tag::Transactional[]
    @Transactional
    // end::Transactional[]
    public Response addNewEvent(@FormParam("name") String name,
                                @FormParam("time") String time, @FormParam("location") String location) {
        Event newEvent = new Event(name, location, time);
        if (!eventDAO.findEvent(name, location, time).isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Event already exists")
                            .build();
        }
        eventDAO.createEvent(newEvent);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * This method updates a new event from the submitted data (name, time and
     * location) by the user.
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response updateEvent(@FormParam("name") String name,
                                @FormParam("time") String time, @FormParam("location") String location,
                                @PathParam("id") int id) {
        Event prevEvent = eventDAO.readEvent(id);
        if (prevEvent == null) {
            return Response.status(Response.Status.NOT_FOUND)
                            .entity("Event does not exist")
                            .build();
        }
        if (!eventDAO.findEvent(name, location, time).isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Event already exists")
                            .build();
        }
        prevEvent.setName(name);
        prevEvent.setLocation(location);
        prevEvent.setTime(time);

        eventDAO.updateEvent(prevEvent);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * This method deletes a specific existing/stored event
     */
    @DELETE
    @Path("{id}")
    @Transactional
    public Response deleteEvent(@PathParam("id") int id) {
        Event event = eventDAO.readEvent(id);
        if (event == null) {
            return Response.status(Response.Status.NOT_FOUND)
                            .entity("Event does not exist")
                            .build();
        }
        eventDAO.deleteEvent(event);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * This method returns a specific existing/stored event in Json format
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public JsonObject getEvent(@PathParam("id") int eventId) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        Event event = eventDAO.readEvent(eventId);
        if (event != null) {
            builder.add("name", event.getName())
                            .add("time", event.getTime())
                            .add("location", event.getLocation())
                            .add("id", event.getId());
        }
        return builder.build();
    }

    /**
     * This method returns the existing/stored events in Json format
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public JsonArray getEvents() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        JsonArrayBuilder finalArray = Json.createArrayBuilder();
        for (Event event : eventDAO.readAllEvents()) {
            builder.add("name", event.getName())
                            .add("time", event.getTime())
                            .add("location", event.getLocation())
                            .add("id", event.getId());
            finalArray.add(builder.build());
        }
        return finalArray.build();
    }
}
// end::DAO[]
// end::EventResource[]
