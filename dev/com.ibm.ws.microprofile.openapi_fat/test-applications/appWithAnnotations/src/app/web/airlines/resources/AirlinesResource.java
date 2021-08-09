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

package app.web.airlines.resources;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;

import app.web.airlines.JAXRSApp;
import app.web.airlines.model.Airline;
import app.web.airlines.model.Flight;

@Path("")
@Schema(name = "Airline Booking API")
@Tags(
    value = @Tag(
        name = "Airlines",
        description = "All the airlines methods"))
@Callback(
    name = "availabilityCallback",
    callbackUrlExpression = "http://localhost:9080/oas3-airlines/availability",
    operations = @CallbackOperation(
        method = "get",
        summary = "Retrieve available flights.",
        responses = {
                      @APIResponse(
                          responseCode = "200",
                          description = "successful operation",
                          content = @Content(
                              mediaType = "applictaion/json",
                              schema = @Schema(
                                  type = SchemaType.ARRAY,
                                  implementation = Flight.class))),
                      @APIResponse(
                          responseCode = "404",
                          description = "No available flights found",
                          content = @Content(
                              mediaType = "n/a"))
        }))
public class AirlinesResource {
    private static Map<Integer, Airline> airlines = new ConcurrentHashMap<Integer, Airline>();

    static {
        airlines.put(1, new Airline("Acme Air", "1-888-1234-567"));
        airlines.put(2, new Airline("Acme Air Partner", "1-855-1284-563"));
        airlines.put(3, new Airline("PanAm 5000", "1-855-1267-561"));
    }

    public static Airline getRandomAirline() {
        return airlines.get(JAXRSApp.getRandomNumber(2, 1));
    }

    @GET
    @APIResponse(
        ref = "FoundAirlines")
    @APIResponse(
        responseCode = "404",
        description = "No airlines found",
        content = @Content(
            mediaType = "n/a"))
    @Operation(
        summary = "Retrieve all available airlines",
        operationId = "getAirlines")
    @Produces("application/json")
    public Response getAirlines() {
        return Response.ok().entity(airlines.values()).build();
    }

}
