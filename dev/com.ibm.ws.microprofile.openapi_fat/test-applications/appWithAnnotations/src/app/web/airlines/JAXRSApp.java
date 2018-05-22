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

package app.web.airlines;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.links.Link;
import org.eclipse.microprofile.openapi.annotations.links.LinkParameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.servers.ServerVariable;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import app.web.airlines.model.Airline;
import app.web.airlines.model.Booking;
import app.web.airlines.model.Review;
import app.web.airlines.model.User;
import app.web.airlines.resources.AirlinesResource;
import app.web.airlines.resources.AvailabilityResource;
import app.web.airlines.resources.ReviewResource;
import app.web.airlines.resources.bookings.BookingResource;

@ApplicationPath("/")
@OpenAPIDefinition(
    tags = {
             @Tag(name = "user", description = "Operations about user"),
             @Tag(name = "create", description = "Operations about create"),
             @Tag(name = "Bookings", description = "All the bookings methods")
    },
    externalDocs = @ExternalDocumentation(
        description = "instructions for how to deploy this app",
        url = "https://github.com/microservices-api/oas3-airlines/blob/master/README.md"),
    info = @Info(
        title = "AirlinesRatingApp API",
        version = "1.0",
        termsOfService = "http://airlinesratingapp.com/terms",
        contact = @Contact(name = "AirlinesRatingApp API Support", url = "http://exampleurl.com/contact", email = "techsupport@airlinesratingapp.com"),
        license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0.html")),
    security = @SecurityRequirement(name = "airlinesRatingApp_auth"),
    servers = {
                @Server(
                    url = "https://{username}.gigantic-server.com:{port}/{basePath}",
                    description = "The production API server",
                    variables = {
                                  @ServerVariable(
                                      name = "username",
                                      description = "Reviews of the app by users",
                                      defaultValue = "user1",
                                      enumeration = { "user1",
                                                      "user2" }),
                                  @ServerVariable(
                                      name = "port",
                                      description = "Booking data",
                                      defaultValue = "8443"),
                                  @ServerVariable(
                                      name = "user",
                                      description = "User data",
                                      defaultValue = "user"),
                                  @ServerVariable(
                                      name = "basePath",
                                      defaultValue = "v2") }),
                @Server(url = "https://test-server.com:80/basePath", description = "The test API server") },
    components = @Components(
        schemas = {
                    @Schema(name = "Bookings", title = "Bookings", type = SchemaType.ARRAY, implementation = Booking.class),
                    @Schema(name = "Airlines", title = "Airlines", type = SchemaType.ARRAY, implementation = Airline.class),
                    @Schema(name = "id", type = SchemaType.INTEGER, format = "int32"),
                    @Schema(name = "AirlinesRef", ref = "#/components/schemas/Airlines"),
                    @Schema(name = "User", implementation = User.class) },
        responses = {
                      @APIResponse(
                          name = "FoundAirlines",
                          responseCode = "200",
                          description = "successfully found airlines",
                          content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.ARRAY, implementation = Airline.class))),
                      @APIResponse(
                          name = "FoundBookings",
                          responseCode = "200",
                          description = "Bookings retrieved",
                          content = @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = Booking.class))) },
        parameters = {
                       @Parameter(
                           name = "departureDate",
                           in = ParameterIn.QUERY,
                           required = true,
                           description = "Customer departure date",
                           schema = @Schema(implementation = String.class)),
                       @Parameter(
                           name = "username",
                           in = ParameterIn.PATH,
                           description = "The name of the user to be retrieved",
                           schema = @Schema(type = SchemaType.STRING),
                           required = true) },
        examples = {
                     @ExampleObject(
                         name = "review",
                         summary = "External review example",
                         description = "This example exemplifies the content on our site.",
                         externalValue = "http://foo.bar/examples/review-example.json"),
                     @ExampleObject(name = "user", summary = "External user example", externalValue = "http://foo.bar/examples/user-example.json") },
        requestBodies = {
                          @RequestBody(
                              name = "review",
                              content = @Content(mediaType = "application/json", schema = @Schema(implementation = Review.class)),
                              required = true,
                              description = "example review to add") },
        headers = {
                    @Header(
                        name = "Max-Rate",
                        description = "Maximum rate",
                        schema = @Schema(type = SchemaType.INTEGER),
                        required = true,
                        allowEmptyValue = true,
                        deprecated = true),
                    @Header(name = "Request-Limit", description = "The number of allowed requests in the current period", schema = @Schema(type = SchemaType.INTEGER)) },
        securitySchemes = {
                            @SecurityScheme(securitySchemeName = "httpTestScheme", description = "user security scheme", type = SecuritySchemeType.HTTP, scheme = "testScheme") },
        links = {
                  @Link(
                      name = "UserName",
                      description = "The username corresponding to provided user id",
                      operationId = "getUserByUserName",
                      parameters = @LinkParameter(name = "userId", expression = "$request.path.id")) },
        callbacks = {
                      @Callback(
                          name = "GetBookings",
                          callbackUrlExpression = "http://localhost:9080/airlines/bookings",
                          operations = @CallbackOperation(summary = "Retrieve all bookings for current user", responses = { @APIResponse(ref = "FoundBookings") }))
        }))
@SecurityScheme(
    securitySchemeName = "airlinesRatingApp_auth",
    description = "authentication needed to access Airlines app",
    type = SecuritySchemeType.APIKEY,
    apiKeyName = "api_key",
    in = SecuritySchemeIn.HEADER)
@Schema(
    name = "AirlinesRatingApp API",
    description = "APIs for booking and managing air flights",
    externalDocs = @ExternalDocumentation(description = "For more information, see the link.", url = "http://exampleurl.com/schema"))
public class JAXRSApp extends Application {
    @Override
    public Set<Object> getSingletons() {
        Set<Object> singletons = new HashSet<Object>();
        singletons.add(new AirlinesResource());
        singletons.add(new AvailabilityResource());
        singletons.add(new BookingResource());
        singletons.add(new ReviewResource());
        return singletons;
    }

    public static int getRandomNumber(int max, int min) {
        return (new Random()).nextInt(max - min + 1) + min;
    }
}
