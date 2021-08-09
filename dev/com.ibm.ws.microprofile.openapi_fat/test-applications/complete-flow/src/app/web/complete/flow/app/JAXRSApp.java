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
package app.web.complete.flow.app;

import java.util.Collections;
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
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@ApplicationPath("/test-service")
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
        title = "Title from JAX-RS app",
        version = "1.0",
        termsOfService = "http://airlinesratingapp.com/terms",
        contact = @Contact(
            name = "AirlinesRatingApp API Support",
            url = "http://exampleurl.com/contact",
            email = "techsupport@airlinesratingapp.com"),
        license = @License(
            name = "Apache 2.0",
            url = "http://www.apache.org/licenses/LICENSE-2.0.html")),
    security = @SecurityRequirement(name = "airlinesRatingApp_auth"),
    components = @Components(
        parameters = {
                       @Parameter(
                           name = "departureDate",
                           in = ParameterIn.QUERY,
                           required = true,
                           description = "Customer departure date",
                           schema = @Schema(implementation = String.class)),
                       @Parameter(
                           name = "username",
                           in = ParameterIn.QUERY,
                           description = "The name that needs to be deleted",
                           schema = @Schema(type = SchemaType.STRING),
                           required = true) },
        examples = {
                     @ExampleObject(
                         name = "review",
                         summary = "External review example",
                         description = "This example exemplifies the content on our site.",
                         externalValue = "http://foo.bar/examples/review-example.json"),
                     @ExampleObject(
                         name = "user",
                         summary = "External user example",
                         externalValue = "http://foo.bar/examples/user-example.json") },
        headers = {
                    @Header(
                        name = "Max-Rate",
                        description = "Maximum rate",
                        schema = @Schema(type = SchemaType.INTEGER),
                        required = true,
                        allowEmptyValue = true,
                        deprecated = true),
                    @Header(
                        name = "Request-Limit",
                        description = "The number of allowed requests in the current period",
                        schema = @Schema(type = SchemaType.INTEGER)) },
        securitySchemes = {
                            @SecurityScheme(
                                securitySchemeName = "httpTestScheme",
                                description = "user security scheme",
                                type = SecuritySchemeType.HTTP,
                                scheme = "testScheme") },
        links = {
                  @Link(
                      name = "UserName",
                      description = "The username corresponding to provided user id",
                      operationId = "getUserByName",
                      parameters = @LinkParameter(name = "userId", expression = "$request.path.id")) },
        callbacks = {
                      @Callback(
                          name = "GetBookings",
                          callbackUrlExpression = "http://localhost:9080/airlines/bookings",
                          operations = @CallbackOperation(
                              summary = "Retrieve all bookings for current user",
                              responses = { @APIResponse(ref = "FoundBookings") }))
        }))
@SecurityScheme(
    securitySchemeName = "airlinesRatingApp_auth",
    description = "authentication needed to access Airlines app",
    type = SecuritySchemeType.APIKEY,
    apiKeyName = "api_key",
    in = SecuritySchemeIn.HEADER)
public class JAXRSApp extends Application {
    @Override
    public Set<Object> getSingletons() {
        return Collections.singleton(TestService.class);
    }
}
