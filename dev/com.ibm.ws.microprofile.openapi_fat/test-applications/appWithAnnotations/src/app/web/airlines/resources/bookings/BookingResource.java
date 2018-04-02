/**
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package app.web.airlines.resources.bookings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterStyle;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;

import app.web.airlines.model.Booking;

@Path("/bookings")
@Tag(ref = "Bookings")
@Tags(
    value = @Tag(
        name = "Reservations",
        description = "All the reservation methods"))
@SecurityScheme(
    securitySchemeName = "bookingSecurityScheme",
    type = SecuritySchemeType.OPENIDCONNECT,
    description = "Security Scheme for booking resource",
    openIdConnectUrl = "http://openidconnect.com/testurl")
@Server(description = "Secure server", url = "https://gigantic-server.com:443")
@Server(description = "Unsecure server", url = "http://gigantic-server.com:80")
public class BookingResource {
    private final Map<Integer, Booking> bookings = new ConcurrentHashMap<Integer, Booking>();
    private volatile int currentId = 0;

    @GET
    @Tag(ref = "bookings")
    @APIResponses(value = {
                            @APIResponse(
                                responseCode = "200",
                                description = "Bookings retrieved",
                                content = @Content(
                                    schema = @Schema(
                                        type = SchemaType.ARRAY,
                                        implementation = Booking.class))),
                            @APIResponse(
                                responseCode = "404",
                                description = "No bookings found for the user.")
    })
    @Operation(
        summary = "Retrieve all bookings for current user",
        operationId = "getAllBookings")
    @Produces("application/json")
    public Response getBookings() {
        return Response.ok().entity(bookings.values()).build();
    }

    @POST
    @SecurityRequirement(
        name = "bookingSecurityScheme",
        scopes = { "write:bookings", "read:bookings" })
    @Callback(
        name = "bookingCallback",
        callbackUrlExpression = "http://localhost:9080/airlines/bookings",
        operations = @CallbackOperation(
            method = "get",
            summary = "Retrieve all bookings for current user",
            responses = {
                          @APIResponse(
                              responseCode = "200",
                              description = "Bookings retrieved",
                              content = @Content(
                                  mediaType = "application/json",
                                  schema = @Schema(
                                      type = SchemaType.ARRAY,
                                      implementation = Booking.class))),
                          @APIResponse(
                              responseCode = "404",
                              description = "No bookings found for the user.")
            }))
    @APIResponse(
        responseCode = "201",
        description = "Booking created",
        content = @Content(
            schema = @Schema(
                name = "id",
                description = "id of the new booking",
                type = SchemaType.STRING)))
    @Operation(
        summary = "Create a booking",
        description = "Create a new booking record with the booking information provided.",
        operationId = "createBooking")
    @Consumes("application/json")
    @Produces("application/json")
    public Response createBooking(@RequestBody(
        description = "Create a new booking with the provided information.",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(ref = "Booking"),
            examples = @ExampleObject(
                name = "booking",
                summary = "External booking example",
                externalValue = "http://foo.bar/examples/booking-example.json"))) Booking task) {
        bookings.put(currentId, task);
        return Response.status(Status.CREATED).entity("{\"id\":" + currentId++ + "}").build();
    }

    @GET
    @Path("{id}")
    @Parameters({
                  @Parameter(
                      name = "id",
                      description = "ID of the booking",
                      required = true,
                      in = ParameterIn.PATH,
                      style = ParameterStyle.SIMPLE)
    })

    @Produces("application/json")
    @Operation(
        summary = "Get a booking with ID",
        operationId = "getBookingById")
    @APIResponses(value = {
                            @APIResponse(
                                responseCode = "200",
                                description = "Booking retrieved",
                                content = @Content(
                                    schema = @Schema(
                                        implementation = Booking.class))),
                            @APIResponse(
                                responseCode = "404",
                                description = "Booking not found")
    })
    public Response getBooking(
                               @PathParam("id") int id) {
        Booking booking = bookings.get(id);
        if (booking != null) {
            return Response.ok().entity(booking).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @PUT
    @Path("{id}")
    @Consumes("application/json")
    @Produces("text/plain")
    @APIResponse(
        responseCode = "200",
        description = "Booking updated")
    @APIResponse(
        responseCode = "404",
        description = "Booking not found")
    @Operation(
        summary = "Update a booking with ID",
        operationId = "updateBookingId")
    public Response updateBooking(
                                  @PathParam("id") int id, Booking booking) {
        if (bookings.get(id) != null) {
            bookings.put(id, booking);
            return Response.ok().build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @DELETE
    @Path("{id}")
    @Tag()
    @APIResponse(
        responseCode = "200",
        description = "Booking deleted successfully.")
    @APIResponse(
        responseCode = "404",
        description = "Booking not found.")
    @Operation(
        summary = "Delete a booking with ID",
        operationId = "deleteBookingById")
    @Produces("text/plain")
    public Response deleteBooking(
                                  @PathParam("id") int id) {
        if (bookings.get(id) != null) {
            bookings.remove(id);
            return Response.ok().build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }
}
