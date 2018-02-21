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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callbacks;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.links.Link;
import org.eclipse.microprofile.openapi.annotations.links.LinkParameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlow;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlows;
import org.eclipse.microprofile.openapi.annotations.security.OAuthScope;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.servers.ServerVariable;
import org.eclipse.microprofile.openapi.annotations.servers.Servers;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.tags.Tags;

import app.web.airlines.model.Airline;
import app.web.airlines.model.Review;
import app.web.airlines.model.User;

@Path("/reviews")
@Servers({ @Server(description = "Secure server", url = "https://gigantic-server.com:443"),
           @Server(description = "Unsecure server", url = "http://gigantic-server.com:80") })
@Server(
    url = "{protocol}://test-server.com",
    description = "The production API server",
    variables = {
                  @ServerVariable(
                      name = "protocol",
                      enumeration = { "http",
                                      "https" },
                      defaultValue = "https") })
@SecurityScheme(
    securitySchemeName = "reviewoauth2",
    type = SecuritySchemeType.OAUTH2,
    description = "authentication needed to create and delete reviews",
    flows = @OAuthFlows(
        implicit = @OAuthFlow(authorizationUrl = "https://example.com/api/oauth/dialog", scopes = @OAuthScope(name = "write:reviews", description = "create a review")),
        authorizationCode = @OAuthFlow(
            authorizationUrl = "https://example.com/api/oauth/dialog",
            tokenUrl = "https://example.com/api/oauth/token",
            scopes = @OAuthScope(name = "write:reviews", description = "create a review")),
        password = @OAuthFlow(
            refreshUrl = "https://example.com/api/oauth/refresh",
            tokenUrl = "https://example.com/api/oauth/token",
            scopes = @OAuthScope(name = "write:reviews", description = "create a review")),
        clientCredentials = @OAuthFlow(tokenUrl = "https://example.com/api/oauth/token", scopes = @OAuthScope(name = "read:reviews", description = "search for a review"))))
@Tags(value = { @Tag(name = "Reviews", description = "All the review methods"),
                @Tag(name = "Ratings", description = "All the ratings methods") })
public class ReviewResource {

    private static Map<Integer, Review> reviews = new ConcurrentHashMap<Integer, Review>();
    private volatile int currentId = 0;

    static {
        reviews.put(1,
                    new Review("1", new User(3456, "userOne", "passwd", "Charlie", "Smith", "male", 46, "charlie@mail.com", "11-11-11", 1), new Airline("Acme Air", "1-888-1234-567"), 8, "great!"));
        reviews.put(2,
                    new Review("2", new User(7896, "userTwo", "Passwd", "Nora", "Parkings", "female", 23, "nora@mail.com", "12-12-12", 1), new Airline("Acme Air", "1-888-1234-567"), 7, "good"));
    }

    @GET
    @APIResponse(
        responseCode = "200",
        description = "successful operation",
        content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.ARRAY, implementation = Review.class)),
        headers = @Header(ref = "#/components/headers/Request-Limit"))
    @Operation(operationId = "getAllReviews", summary = "get all the reviews")
    @Produces("application/json")
    public Response getAllReviews() {
        return Response.ok().entity(reviews.values()).build();
    }

    @GET
    @Path("{id}")
    @APIResponse(responseCode = "200", description = "Review retrieved", content = @Content(schema = @Schema(implementation = Review.class)), headers = {
                                                                                                                                                          @Header(
                                                                                                                                                              name = "responseHeader1",
                                                                                                                                                              description = "Max rate",
                                                                                                                                                              schema = @Schema(
                                                                                                                                                                  type = SchemaType.INTEGER),
                                                                                                                                                              required = true,
                                                                                                                                                              allowEmptyValue = true,
                                                                                                                                                              deprecated = true),
                                                                                                                                                          @Header(
                                                                                                                                                              name = "responseHeader2",
                                                                                                                                                              description = "Input value",
                                                                                                                                                              schema = @Schema(
                                                                                                                                                                  type = SchemaType.STRING),
                                                                                                                                                              required = true,
                                                                                                                                                              allowEmptyValue = true,
                                                                                                                                                              deprecated = true) })
    @APIResponse(responseCode = "404", description = "Review not found")
    @Operation(operationId = "getReviewById", summary = "Get a review with ID")
    @Produces("application/json")
    public Response getReviewById(
                                  @Parameter(
                                      name = "id",
                                      description = "ID of the booking",
                                      required = true,
                                      in = ParameterIn.PATH,
                                      content = @Content(examples = @ExampleObject(name = "example", value = "1"))) @PathParam("id") int id) {
        Review review = reviews.get(id);
        if (review != null) {
            return Response.ok().entity(review).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("{user}")
    @Operation(operationId = "getReviewByUser", summary = "Get all reviews by user")
    @APIResponses(value = {
                            @APIResponse(responseCode = "200", description = "Review(s) retrieved", content = @Content(schema = @Schema(implementation = Review.class))),
                            @APIResponse(responseCode = "404", description = "Review(s) not found") })
    @Produces("application/json")
    public Response getReviewByUser(
                                    @Parameter(
                                        name = "user",
                                        description = "username of the user for the reviews",
                                        required = true,
                                        in = ParameterIn.PATH,
                                        content = @Content(examples = @ExampleObject(name = "example", value = "bsmith")),
                                        examples = {
                                                     @ExampleObject(name = "example1", value = "bsmith"),
                                                     @ExampleObject(name = "example2", value = "pat@example.com") }) @PathParam("user") String user) {

        List<Review> reviewsByUser = new ArrayList<Review>();
        for (Review review : reviews.values()) {
            User currentUser = review.getUser();
            if (currentUser.getUsername() == user) {
                reviewsByUser.add(review);
            }
        }
        if (!reviewsByUser.isEmpty()) {
            return Response.ok().entity(reviewsByUser).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("{airline}")
    @Operation(operationId = "getReviewByAirline", summary = "Get all reviews by airlines")
    @Parameter(
        name = "airline",
        description = "name of the airlines for the reviews",
        required = true,
        in = ParameterIn.PATH,
        content = @Content(examples = @ExampleObject(name = "example", value = "Acme Air")),
        example = "Acme Air")
    @APIResponse(responseCode = "200", description = "Review(s) retrieved", content = @Content(schema = @Schema(implementation = Review.class)))
    @APIResponse(responseCode = "404", description = "Review(s) not found")
    @Produces("application/json")
    public Response getReviewByAirline(
                                       @Parameter(
                                           name = "airline",
                                           description = "name of the airlines for the reviews",
                                           required = true,
                                           in = ParameterIn.PATH,
                                           content = @Content(examples = @ExampleObject(value = "Acme Air"))) @PathParam("airline") String airlines) {

        List<Review> reviewsByAirlines = new ArrayList<Review>();
        for (Review review : reviews.values()) {
            Airline currentAirline = review.getAirlines();
            if (currentAirline.getName() == airlines) {
                reviewsByAirlines.add(review);
            }
        }
        if (!reviewsByAirlines.isEmpty()) {
            return Response.ok().entity(reviewsByAirlines).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("{user}/{airlines}")
    @APIResponse(responseCode = "200", description = "Review(s) retrieved", content = @Content(schema = @Schema(implementation = Review.class)))
    @APIResponse(responseCode = "404", description = "Review(s) not found")
    @Operation(operationId = "getReviewByAirlineAndUser", summary = "Get all reviews for an airline by User")
    @Produces("application/json")
    public Response getReviewByAirlineAndUser(
                                              @Parameters(value = {
                                                                    @Parameter(
                                                                        name = "airlines",
                                                                        description = "name of the airlines for the reviews",
                                                                        required = true,
                                                                        in = ParameterIn.PATH,
                                                                        content = @Content(examples = @ExampleObject(name = "example", value = "Acme Air"))),
                                                                    @Parameter(
                                                                        name = "user",
                                                                        description = "sername of the user for the reviews",
                                                                        required = true,
                                                                        in = ParameterIn.PATH,
                                                                        content = @Content(
                                                                            examples = @ExampleObject(name = "example", value = "bsmith"))) }) @PathParam("user") String user,
                                              @PathParam("airlines") String airlines) {
        List<Review> reviewsByAirlinesUser = new ArrayList<Review>();
        for (Review review : reviews.values()) {
            Airline currentAirline = review.getAirlines();
            User currentUser = review.getUser();

            if (currentAirline.getName() == airlines && currentUser.getUsername() == user) {
                reviewsByAirlinesUser.add(review);
            }
        }
        if (!reviewsByAirlinesUser.isEmpty()) {
            return Response.ok().entity(reviewsByAirlinesUser).build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @POST
    @Callbacks({
                 @Callback(
                     name = "testCallback",
                     callbackUrlExpression = "http://localhost:9080/oas3-airlines/reviews",
                     operations = @CallbackOperation(
                         summary = "Get all reviews",
                         method = "get",
                         responses = @APIResponse(
                             responseCode = "200",
                             description = "successful operation",
                             content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.ARRAY, implementation = Review.class))))) })
    @Tag(ref = "Reviews")
    @Servers(value = {
                       @Server(url = "localhost:9080/{proxyPath}/reviews/id", description = "view of all the reviews", variables = {
                                                                                                                                     @ServerVariable(
                                                                                                                                         name = "proxyPath",
                                                                                                                                         description = "Base path of the proxy",
                                                                                                                                         defaultValue = "proxy") }),
                       @Server(url = "http://random.url/reviews", description = "random text") })
    @SecurityRequirement(name = "reviewoauth2", scopes = "write:reviews")
    @APIResponse(
        responseCode = "201",
        description = "review created",
        content = @Content(schema = @Schema(name = "id", description = "id of the new review", type = SchemaType.STRING)),
        links = {
                  @Link(
                      name = "Review",
                      description = "get the review that was added",
                      operationId = "getReviewById",
                      server = @Server(description = "endpoint for all the review related methods", url = "http://localhost:9080/airlines/reviews/"),
                      parameters = @LinkParameter(name = "reviewId", expression = "$request.path.id")) })
    @RequestBody(ref = "#/components/requestBodies/review")
    @Operation(summary = "Create a Review", operationId = "createReview")
    @Consumes("application/json")
    @Produces("application/json")
    public Response createReview(Review review) {
        reviews.put(currentId, review);
        return Response.status(Status.CREATED).entity("{\"id\":" + currentId++ + "}").build();
    }

    @DELETE
    @Path("{id}")
    @APIResponse(responseCode = "200", description = "Review deleted")
    @APIResponse(responseCode = "404", description = "Review not found")
    @Operation(summary = "Delete a Review with ID", operationId = "deleteReview")
    @Produces("text/plain")
    public Response deleteReview(@PathParam("id") int id) {
        if (reviews.get(id) != null) {
            reviews.remove(id);
            return Response.ok().build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }
}
