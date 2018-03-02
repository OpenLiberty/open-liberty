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
package com.ibm.ws.microprofile.impl.parser.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.servers.ServerVariables;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

/**
 *
 */
public class OpenAPIParserTest {

    SwaggerParseResult result = new OpenAPIParser().readLocation("airlines_openapi.yaml", null, null);

    @Test
    public void testOpenAPI() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertEquals(result.getOpenAPI().getOpenapi(), "3.0.0");
    }

    @Test
    public void testInfo() {
        assertNotNull(result.getOpenAPI());
        Info info = result.getOpenAPI().getInfo();
        assertNotNull(info);
        assertEquals(info.getTitle(), "Validation App");
        assertEquals(info.getVersion(), "1.0");
        assertEquals(info.getTermsOfService(), "http://www.termsofservice.com");
    }

    @Test
    public void testContactAndLicense() {
        assertNotNull(result.getOpenAPI());
        Info info = result.getOpenAPI().getInfo();
        assertNotNull(info);

        Contact contact = info.getContact();
        License license = info.getLicense();

        assertNotNull(contact);
        assertEquals(contact.getName(), "AirlinesRatingApp API Support");
        assertEquals(contact.getUrl(), "http://www.contacts.com");
        assertEquals(contact.getEmail(), "airlines@gmail.com");
        assertNotNull(license);
        assertEquals(license.getName(), "Apache 2.0");
        assertEquals(license.getUrl(), "http://www.license.com");
    }

    @Test
    public void testDiscriminator() {
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
        PathItem reviewsUser = result.getOpenAPI().getPaths().get("/reviews/{user}");

        assertNotNull(reviewsUser);
        APIResponses responses = reviewsUser.getGET().getResponses();
        assertNotNull(responses);
        APIResponse response = responses.get("200");
        assertNotNull(response);
        Discriminator discriminator = response.getContent().get("application/json").getSchema().getDiscriminator();
        assertNotNull(discriminator);
        assertEquals(discriminator.getPropertyName(), "pet_type");
        Map<String, String> mappings = discriminator.getMapping();
        assertTrue(mappings.size() == 2);
        assertEquals(mappings.get("review"), "#/components/schemas/Review");
        assertEquals(mappings.get("user"), "#/components/schemas/User");

    }

    @Test
    public void testRequestBody() {
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
        PathItem bookingsId = result.getOpenAPI().getPaths().get("/bookings/{id}");

        assertNotNull(bookingsId);
        RequestBody requestBody = bookingsId.getPUT().getRequestBody();
        assertNotNull(requestBody);
        assertEquals(requestBody.getDescription(), "requestbody consists of a booking");
        assertTrue(requestBody.getRequired());
    }

    @Test
    public void testExternalDocumentation() {
        assertNotNull(result.getOpenAPI());
        ExternalDocumentation externalDocs = result.getOpenAPI().getExternalDocs();
        assertEquals(externalDocs.getDescription(), "instructions for how to deploy this app");
        assertEquals(externalDocs.getUrl(), "http://www.externaldocumentation.com");
    }

    @Test
    public void testServersAndServerVariables() {
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
        PathItem bookingsId = result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(bookingsId);
        Operation operation = bookingsId.getGET();
        List<Server> servers = operation.getServers();
        assertTrue(servers.size() == 1);
        Server server = servers.get(0);
        assertEquals(server.getUrl(), "localhost:9080/oas3-airlines/bookings/{id}");
        assertEquals(server.getDescription(), "view of all the bookings for this user");
        ServerVariables variables = server.getVariables();
        assertTrue(variables.size() == 1);
        ServerVariable variable = variables.get("id");
        assertEquals(variable.getDefaultValue(), "1");
        assertEquals(variable.getDescription(), "id of the booking");

    }

    @Test
    public void testOperation() {
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
        PathItem bookingsId = result.getOpenAPI().getPaths().get("/bookings/{id}");

        assertNotNull(bookingsId);
        Operation operation = bookingsId.getPUT();
        assertTrue(operation.getTags().size() == 1);
        assertEquals(operation.getSummary(), "Update a booking with ID");
        assertEquals(operation.getDescription(), "Updates the given booking with the given ID.");
        assertNotNull(operation.getExternalDocs());
        assertTrue(operation.getDeprecated());
        assertEquals(operation.getOperationId(), "updateBooking");
        assertNotNull(operation.getSecurity());
        assertNotNull(operation.getServers());
        assertNotNull(operation.getParameters());
        assertNotNull(operation.getRequestBody());
        assertNotNull(operation.getResponses());
        assertNotNull(operation.getCallbacks());
    }

    @Test
    public void testCallback() {
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
        PathItem review = result.getOpenAPI().getPaths().get("/reviews");

        assertNotNull(review);
        Operation operation = review.getPOST();
        Map<String, Callback> callbacks = operation.getCallbacks();
        assertTrue(callbacks.size() == 1);
        Callback callback = callbacks.get("testCallback");
        assertNotNull(callback);
        Operation callbackOperation = callback.get("http://localhost:9080/oas3-airlines/reviews").getGET();
        assertNotNull(callbackOperation);
        assertEquals(callbackOperation.getOperationId(), "getReviewCallback");

    }

    @Test
    public void testPathsAndPathItems() {
        assertNotNull(result.getOpenAPI());
        Paths paths = result.getOpenAPI().getPaths();
        assertTrue(paths.size() == 9);

        assertTrue(paths.containsKey("/"));
        PathItem pathItem = paths.get("/");
        assertNotNull(pathItem.getGET());

        assertTrue(paths.containsKey("/bookings"));
        pathItem = paths.get("/bookings");
        assertNotNull(pathItem.getGET());
        assertNotNull(pathItem.getPOST());

        assertTrue(paths.containsKey("/availability"));
        pathItem = paths.get("/availability");
        assertNotNull(pathItem.getGET());

        assertTrue(paths.containsKey("/bookings/{id}"));
        pathItem = paths.get("/bookings/{id}");
        assertNotNull(pathItem.getGET());
        assertNotNull(pathItem.getPUT());
        assertNotNull(pathItem.getDELETE());

        assertTrue(paths.containsKey("/reviews"));
        pathItem = paths.get("/reviews");
        assertNotNull(pathItem.getGET());
        assertNotNull(pathItem.getPOST());

        assertTrue(paths.containsKey("/reviews/{id}"));
        pathItem = paths.get("/reviews/{id}");
        assertNotNull(pathItem.getGET());
        assertNotNull(pathItem.getDELETE());

        assertTrue(paths.containsKey("/reviews/{user}"));
        pathItem = paths.get("/reviews/{user}");
        assertNotNull(pathItem.getGET());

        assertTrue(paths.containsKey("/reviews/{airline}"));
        pathItem = paths.get("/reviews/{airline}");
        assertNotNull(pathItem.getGET());

        assertTrue(paths.containsKey("/reviews/{user}/{airlines}"));
        pathItem = paths.get("/reviews/{user}/{airlines}");
        assertNotNull(pathItem.getGET());
    }

    @Test
    public void testComponent() {
        Components components = result.getOpenAPI().getComponents();
        assertNotNull(components);
        assertNotNull(components.getSchemas());
        assertTrue(components.getSchemas().size() == 6);
        assertTrue(components.getSchemas().containsKey("User"));
        assertTrue(components.getSchemas().containsKey("Airline"));
        assertTrue(components.getSchemas().containsKey("Booking"));
        assertTrue(components.getSchemas().containsKey("Review"));
        assertTrue(components.getSchemas().containsKey("CreditCard"));
        assertTrue(components.getSchemas().containsKey("Flight"));
        assertNotNull(components.getSecuritySchemes());
        assertTrue(components.getSecuritySchemes().size() == 2);
        assertTrue(components.getSecuritySchemes().containsKey("reviewoauth2"));
        assertTrue(components.getSecuritySchemes().containsKey("bookingoauth2"));
    }

    @Test
    public void testParameter() {
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
        PathItem bookingsId = result.getOpenAPI().getPaths().get("/bookings/{id}");

        assertNotNull(bookingsId);
        Operation operation = bookingsId.getPUT();
        List<Parameter> parameters = operation.getParameters();
        assertTrue(parameters.size() == 1);
        Parameter name = parameters.get(0);
        assertNotNull(name);
        assertEquals(name.getName(), "id");
        assertEquals(name.getIn().toString(), "path");
        assertEquals(name.getStyle().toString(), "simple");
        assertFalse(name.getDeprecated());
        assertFalse(name.getExplode());
        assertEquals(name.getDescription(), "ID of the booking");
        assertEquals(name.getExample(), "1");
        assertTrue(name.getRequired());
        assertNotNull(name.getSchema());
    }

}
