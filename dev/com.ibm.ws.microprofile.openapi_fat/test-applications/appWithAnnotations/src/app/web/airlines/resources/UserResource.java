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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterStyle;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.links.Link;
import org.eclipse.microprofile.openapi.annotations.links.LinkParameter;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Encoding;
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

import app.web.airlines.data.UserData;
import app.web.airlines.exception.ApiException;
import app.web.airlines.exception.NotFoundException;
import app.web.airlines.model.User;

@Path("/user")
@Produces({ "application/json", "application/xml" })
@SecurityScheme(
                description = "user security scheme",
                type = SecuritySchemeType.HTTP,
                securitySchemeName = "httpSchemeForTest",
                scheme = "testScheme")
@SecurityRequirement(
                     name = "httpSchemeForTest")
public class UserResource {

    private static UserData userData = new UserData();

    public UserData getUserData() {
        return this.userData;
    }

    public void setUserData(UserData userData) {
        this.userData = userData;
    }

    @POST
    @Tags(refs = { "user", "create" })
    @APIResponses(value = {
                            @APIResponse(
                                         responseCode = "200",
                                         description = "New user record successfully created."),
                            @APIResponse(
                                         responseCode = "400",
                                         description = "Unable to create this user record.")
    })
    @Parameters(value = {
                          @Parameter(
                                     name = "id",
                                     in = ParameterIn.QUERY,
                                     description = "User id for the new user record to be created",
                                     required = true,
                                     allowReserved = true,
                                     style = ParameterStyle.FORM,
                                     schema = @Schema(type = SchemaType.INTEGER, format = "int32")),
                          @Parameter(
                                     name = "userName",
                                     in = ParameterIn.QUERY,
                                     description = "Username for the new user record to be created",
                                     required = true,
                                     schema = @Schema(
                                                      type = SchemaType.STRING,
                                                      externalDocs = @ExternalDocumentation(
                                                                                            description = "How to create good user names.",
                                                                                            url = "http://exampleurl.com/usernames"))),
                          @Parameter(
                                     name = "password",
                                     in = ParameterIn.QUERY,
                                     description = "User password for the new user record to be created",
                                     required = true,
                                     hidden = true,
                                     schema = @Schema(
                                                      type = SchemaType.STRING,
                                                      externalDocs = @ExternalDocumentation(
                                                                                            description = "How to create good passwords.",
                                                                                            url = "http://exampleurl.com/passwords"))),
                          @Parameter(
                                     name = "firstName",
                                     in = ParameterIn.QUERY,
                                     description = "User's first name for the new user record to be created",
                                     required = true,
                                     schema = @Schema(type = SchemaType.STRING)),
                          @Parameter(
                                     name = "lastName",
                                     in = ParameterIn.QUERY,
                                     description = "User's last name for the new user record to be created",
                                     style = ParameterStyle.FORM,
                                     required = true,
                                     schema = @Schema(type = SchemaType.STRING)),
                          @Parameter(
                                     name = "sex",
                                     in = ParameterIn.QUERY,
                                     description = "User's sex for the new user record to be created",
                                     required = true,
                                     style = ParameterStyle.FORM,
                                     schema = @Schema(type = SchemaType.STRING)),
                          @Parameter(
                                     name = "age",
                                     in = ParameterIn.QUERY,
                                     description = "User's age for the new user record to be created",
                                     required = true,
                                     schema = @Schema(type = SchemaType.INTEGER, format = "int64")),
                          @Parameter(
                                     name = "phone",
                                     in = ParameterIn.QUERY,
                                     description = "User phone number for the new user record to be created",
                                     required = true,
                                     schema = @Schema(type = SchemaType.STRING)),
                          @Parameter(
                                     name = "status",
                                     in = ParameterIn.QUERY,
                                     description = "User status for the new user record to be created",
                                     required = true,
                                     schema = @Schema(type = SchemaType.INTEGER))
    })
    @Operation(
               summary = "Create user",
               description = "This can only be done by the logged in user.",
               operationId = "createUser")
    public Response createUser(
                               @RequestBody(
                                            description = "Record of a new user to be created in the system.",
                                            required = true,
                                            content = @Content(
                                                               mediaType = "application/json",
                                                               schema = @Schema(
                                                                                name = "testUser",
                                                                                type = SchemaType.OBJECT,
                                                                                maxProperties = 1024,
                                                                                minProperties = 1,
                                                                                required = true,
                                                                                implementation = User.class),

                                                               examples = @ExampleObject(
                                                                                         name = "user",
                                                                                         summary = "External user example",
                                                                                         externalValue = "http://foo.bar/examples/user-example.json"),
                                                               encoding = @Encoding(
                                                                                    name = "email",
                                                                                    contentType = "text/plain",
                                                                                    style = "form",
                                                                                    allowReserved = true,
                                                                                    explode = true,
                                                                                    headers = @Header(
                                                                                                      name = "testHeader",
                                                                                                      description = "Minimum rate",
                                                                                                      schema = @Schema(type = SchemaType.INTEGER),
                                                                                                      required = true,
                                                                                                      allowEmptyValue = true,
                                                                                                      deprecated = true)))) User user) {
        userData.addUser(user);
        return Response.ok().entity("").build();
    }

    @POST
    @Path("/createWithArray")
    @Tag(ref = "user")
    @Tag(ref = "create")
    @APIResponse(
                 responseCode = "200",
                 description = "Successfully created list of users.")
    @APIResponse(
                 responseCode = "400",
                 description = "Unable to create list of users.")
    @Operation(
               summary = "Creates list of users with given input array", //Array of User objects
               operationId = "createUsersFromArray"
    /* tags = {"user"}, //this operation intentionally doesn't have tags attribute, since above Tag ref should apply */
    )
    public Response createUsersWithArrayInput(
                                              @RequestBody(
                                                           description = "Array of user object",
                                                           required = true,
                                                           content = @Content(
                                                                              mediaType = "application/json",
                                                                              schema = @Schema(
                                                                                               type = SchemaType.ARRAY,
                                                                                               implementation = User.class,
                                                                                               nullable = true,
                                                                                               writeOnly = true,
                                                                                               minItems = 2,
                                                                                               maxItems = 20,
                                                                                               uniqueItems = true),
                                                                              encoding = @Encoding(
                                                                                                   name = "firstName",
                                                                                                   contentType = "text/plain",
                                                                                                   style = "form",
                                                                                                   allowReserved = true,
                                                                                                   explode = true))) User[] users) {
        for (User user : users) {
            userData.addUser(user);
        }
        return Response.ok().entity("").build();
    }

    @POST
    @Path("/createWithList")
    @Tag(ref = "user")
    @Tags(refs = "create")
    @APIResponse(
                 responseCode = "200",
                 description = "Successfully created list of users.")
    @APIResponse(
                 responseCode = "400",
                 description = "Unable to create list of users.")
    @Operation(
               summary = "Creates list of users with given input list", //List of User objects
               operationId = "createUsersFromList")
    public Response createUsersWithListInput(
                                             @RequestBody(
                                                          description = "List of user object",
                                                          required = true) java.util.List<User> users) {
        for (User user : users) {
            userData.addUser(user);
        }
        return Response.ok().entity("").build();
    }

    @Path("/{username}")
    @PUT
    @RequestBody(
                 name = "user",
                 description = "Record of a new user to be created in the system.",
                 content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = User.class),
                                    examples = @ExampleObject(
                                                              name = "user",
                                                              summary = "Example user properties to update",
                                                              value = "Properties to update in JSON format here")))
    @Operation(
               summary = "Update user",
               description = "This can only be done by the logged in user.",
               operationId = "updateUser")
    @APIResponses(value = {
                            @APIResponse(
                                         responseCode = "200",
                                         description = "User updated successfully",
                                         content = @Content(
                                                            schema = @Schema(ref = "User"),
                                                            encoding = @Encoding(
                                                                                 name = "password",
                                                                                 contentType = "text/plain",
                                                                                 style = "form",
                                                                                 allowReserved = true,
                                                                                 explode = true,
                                                                                 headers = @Header(ref = "Max-Rate")))),
                            @APIResponse(
                                         responseCode = "400",
                                         description = "Invalid user supplied"),
                            @APIResponse(
                                         responseCode = "404",
                                         description = "User not found")
    })
    @Tag(ref = "user")
    public Response updateUser(
                               @Parameter(
                                          name = "username",
                                          description = "User that needs to be updated",
                                          schema = @Schema(type = SchemaType.STRING),
                                          required = true) @PathParam("username") String username,
                               User user) {
        userData.addUser(user);
        return Response.ok().entity("").build();
    }

    @DELETE
    @Path("/{username}")
    @Tag(ref = "user")
    @APIResponse(
                 responseCode = "200",
                 description = "User deleted successfully")
    @APIResponse(
                 responseCode = "400",
                 description = "Invalid username supplied")
    @APIResponse(
                 responseCode = "404",
                 description = "User not found")
    @Operation(
               summary = "Delete user",
               description = "This can only be done by the logged in user.",
               operationId = "deleteUser")
    public Response deleteUser(
                               @Parameter(
                                          name = "username",
                                          description = "The name that needs to be deleted",
                                          schema = @Schema(type = SchemaType.STRING),
                                          required = true) @PathParam("username") String userName) {
        if (userData.removeUser(userName)) {
            return Response.ok().entity("").build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/{username}")
    @Tag(ref = "user")
    @APIResponses(value = {
                            @APIResponse(
                                         responseCode = "200",
                                         description = "Successfully retrieved user by user name.",
                                         content = @Content(
                                                            schema = @Schema(implementation = User.class))),
                            @APIResponse(
                                         responseCode = "400",
                                         description = "Invalid username supplied",
                                         content = @Content(
                                                            schema = @Schema(implementation = User.class))),
                            @APIResponse(
                                         responseCode = "404",
                                         description = "User not found",
                                         content = @Content(
                                                            schema = @Schema(implementation = User.class)))
    })
    @Operation(
               summary = "Get user by user name",
               operationId = "getUserByUserName")
    public Response getUserByName(
                                  @Parameter(
                                             ref = "username") @PathParam("username") String userName) throws ApiException {
        User user = userData.findUserByName(userName);
        if (null != user) {
            return Response.ok().entity(user).build();
        } else {
            throw new NotFoundException(404, "User not found");
        }
    }

    @GET
    @Path("/{id}")
    @Tag(ref = "user")
    @APIResponse(
                 responseCode = "200",
                 description = "Successfully retrieved user by id.",
                 content = @Content(
                                    schema = @Schema(implementation = User.class)),
                 links = {
                           @Link(
                                 name = "User name",
                                 description = "The username corresponding to provided user id",
                                 operationId = "getUserByName",
                                 parameters = @LinkParameter(
                                                             name = "userId",
                                                             expression = "$request.path.id")),
                           @Link(
                                 name = "Review",
                                 description = "The reviews provided by user",
                                 operationRef = "/db/reviews/{userName}",
                                 parameters = @LinkParameter(
                                                             name = "path.userName",
                                                             expression = "$response.body#userName"),
                                 requestBody = "$request.path.id",
                                 server = @Server(url = "http://example.com"))
                 })
    @APIResponse(
                 responseCode = "400",
                 description = "Invalid id supplied",
                 content = @Content(
                                    schema = @Schema(implementation = User.class)))
    @APIResponse(
                 responseCode = "404",
                 description = "User not found",
                 content = @Content(
                                    schema = @Schema(implementation = User.class)))
    @Operation(
               summary = "Get user by id",
               operationId = "getUserById")
    public Response getUserById(
                                @Parameter(
                                           name = "id",
                                           description = "The name that needs to be fetched. Use 1 for testing.",
                                           schema = @Schema(type = SchemaType.INTEGER),
                                           required = true) @PathParam("id") int id) throws ApiException {
        User user = userData.findUserById(id);
        if (null != user) {
            return Response.ok().entity(user).build();
        } else {
            throw new NotFoundException(404, "User not found");
        }
    }

    @GET
    @Path("/login")
    @Tag()
    @ExternalDocumentation(
                           description = "Policy on user security.",
                           url = "http://exampleurl.com/policy")
    @APIResponse(
                 responseCode = "200",
                 description = "Successful user login.",
                 content = @Content(
                                    schema = @Schema(implementation = User.class)))
    @APIResponse(
                 responseCode = "400",
                 description = "Invalid username/password supplied")
    @Operation(
               summary = "Logs user into the system",
               operationId = "logInUser")

    @SecurityScheme(
                    ref = "#/components/securitySchemes/httpTestScheme")
    @SecurityRequirement(
                         name = "httpTestScheme")
    public Response loginUser(
                              @Parameter(
                                         name = "username",
                                         description = "The user name for login",
                                         schema = @Schema(type = SchemaType.STRING),
                                         required = true) @QueryParam("username") String username,
                              @Parameter(
                                         name = "password",
                                         description = "The password for login in clear text",
                                         schema = @Schema(type = SchemaType.STRING),
                                         required = true) @QueryParam("password") String password) {
        return Response.ok()
                        .entity("logged in user session:" + System.currentTimeMillis())
                        .build();
    }

    @GET
    @Path("/logout")
    @APIResponse(
                 responseCode = "200",
                 description = "Successful user logout.")
    @ExternalDocumentation(
                           description = "Policy on user security.",
                           url = "http://exampleurl.com/policy")
    @Operation(
               summary = "Logs out current logged in user session",
               operationId = "logOutUser"
    /* tags = {"user"}, // intentionally removed to have a method with no tags */)
    public Response logoutUser() {
        return Response.ok().entity("").build();
    }
}
