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

import java.util.ArrayList;
import java.util.HashMap;
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
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.XML;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.servers.ServerVariables;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.junit.Test;

import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.headers.HeaderImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.EncodingImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.OAuthFlowsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.tags.TagImpl;
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

    @Test
    public void testXML() {
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getComponents());
        Map<String, Schema> schemas = result.getOpenAPI().getComponents().getSchemas();
        Schema airline = schemas.get("Airline");
        assertNotNull(airline);
        assertNotNull(airline.getProperties());
        Schema name = airline.getProperties().get("name");
        Schema contactPhone = airline.getProperties().get("contactPhone");
        assertNotNull(name);
        assertNotNull(contactPhone);
        XML xml = name.getXml();
        assertNotNull(xml);
        assertEquals(xml.getName(), "airlinename");
        xml = contactPhone.getXml();
        assertNotNull(xml);
        assertEquals(xml.getPrefix(), "number");
        assertEquals(xml.getNamespace(), "http://example.com/schema/sample");

    }
  
    public void testSchemaInResponseContent() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("202"));
        assertNotNull(path.getGET().getResponses().get("202").getContent());
        assertNotNull(path.getGET().getResponses().get("202").getContent().get("applictaion/json"));
        assertNotNull(path.getGET().getResponses().get("202").getContent().get("applictaion/json").getSchema());
    }

    @Test
    public void testSchemaInParameter() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/availability");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getParameters());
        assertNotNull(path.getGET().getParameters().get(0).getSchema());
        assertEquals(path.getGET().getParameters().get(0).getSchema().getType().toString(), "string");
    }

    @Test
    public void testSchemaInRequestBody() {
        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());
        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings");
        assertNotNull(path);
        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getRequestBody());
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json").getSchema());
        assertEquals(path.getPOST().getRequestBody().getContent().get("application/json").getSchema().getRef(), "#/components/schemas/Booking");
    }

    @Test
    public void testReviewoauth2Scope() {

        OAuthFlowsImpl flows = new OAuthFlowsImpl();

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());

        flows = (OAuthFlowsImpl) result.getOpenAPI().getComponents().getSecuritySchemes().get("reviewoauth2").getFlows();
        assertNotNull(flows);
        assertNotNull(flows.getImplicit());
        assertNotNull(flows.getImplicit().getScopes());
        assertEquals(flows.getImplicit().getScopes().get("write:reviews"), "create a review");

        flows = (OAuthFlowsImpl) result.getOpenAPI().getComponents().getSecuritySchemes().get("bookingoauth2").getFlows();
        assertNotNull(flows);
        assertNotNull(flows.getImplicit());
        assertNotNull(flows.getImplicit().getScopes());
        assertEquals(flows.getImplicit().getScopes().get("write:booking"), "edit a booking");
    }

    @Test
    public void testOpenAPITags() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        List<Tag> tags = new ArrayList<Tag>();

        TagImpl tagOne = new TagImpl();
        tagOne.name("Airlines").description("airlines app");
        TagImpl tagTwo = new TagImpl();
        tagTwo.name("airline").description("all the airlines methods");
        TagImpl tagThree = new TagImpl();
        tagThree.name("availability").description("all the availibility methods");
        TagImpl tagFour = new TagImpl();
        tagFour.name("bookings").description("all the bookings methods");
        TagImpl tagFive = new TagImpl();
        tagFive.name("reviews").description("all the review methods");

        tags.add(tagOne);
        tags.add(tagTwo);
        tags.add(tagThree);
        tags.add(tagFour);
        tags.add(tagFive);

        assertNotNull(result.getOpenAPI().getTags());
        for (Tag tag : result.getOpenAPI().getTags()) {
            assertNotNull(tag);
        }

        for (Tag tag : result.getOpenAPI().getTags()) {
            assertTrue(tags.contains(tag));
        }
    }

    @Test
    public void testOperationTags() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        List<Tag> tags = new ArrayList<Tag>();

        TagImpl tagOne = new TagImpl();
        tagOne.name("Airlines").description("airlines app");
        TagImpl tagTwo = new TagImpl();
        tagTwo.name("airline").description("all the airlines methods");
        TagImpl tagThree = new TagImpl();
        tagThree.name("availability").description("all the availibility methods");
        TagImpl tagFour = new TagImpl();
        tagFour.name("bookings").description("all the bookings methods");
        TagImpl tagFive = new TagImpl();
        tagFive.name("reviews").description("all the review methods");

        tags.add(tagOne);
        tags.add(tagTwo);
        tags.add(tagThree);
        tags.add(tagFour);
        tags.add(tagFive);

        PathItemImpl path = new PathItemImpl();

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(1).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/availability");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(2).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(3).getName()));

        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getTags());
        assertTrue(path.getPOST().getTags().contains(tags.get(3).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(3).getName()));

        assertNotNull(path.getPUT());
        assertNotNull(path.getPUT().getTags());
        assertTrue(path.getPUT().getTags().contains(tags.get(3).getName()));

        assertNotNull(path.getDELETE());
        assertNotNull(path.getDELETE().getTags());
        assertTrue(path.getDELETE().getTags().contains(tags.get(3).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(4).getName()));

        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getTags());
        assertTrue(path.getPOST().getTags().contains(tags.get(4).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(4).getName()));

        assertNotNull(path.getDELETE());
        assertNotNull(path.getDELETE().getTags());
        assertTrue(path.getDELETE().getTags().contains(tags.get(4).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(4).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{airline}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(4).getName()));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}/{airlines}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getTags());
        assertTrue(path.getGET().getTags().contains(tags.get(4).getName()));
    }

    @Test
    public void testLinkInComponents() {

        Map<String, Link> links = new HashMap<String, Link>();

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());

        links = result.getOpenAPI().getComponents().getLinks();
        assertNotNull(links);
        assertTrue(links.size() == 2);
        assertNotNull(links.get("UserName"));
        assertEquals(links.get("UserName").getDescription(), "The username corresponding to provided user id");
        assertEquals(links.get("UserName").getOperationId(), "getUserByUserName");
        assertNotNull(links.get("ReviewId"));
        assertEquals(links.get("ReviewId").getDescription(), "The id corresponding to a particular review");
        assertEquals(links.get("ReviewId").getOperationId(), "getReviewById");
    }

    @Test
    public void testLinkInResponse() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());

        PathItemImpl path = new PathItemImpl();
        APIResponseImpl response = new APIResponseImpl();

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response);
        assertNotNull(response.getLinks());
        assertTrue(response.getLinks().size() == 1);
        assertNotNull(response.getLinks().get("ReviewId"));
        assertEquals(response.getLinks().get("ReviewId").getDescription(), "The id corresponding to a particular review");
        assertEquals(response.getLinks().get("ReviewId").getOperationId(), "getReviewById");
    }

    @Test
    public void testHeaderInRequestBody() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);

        assertNotNull(path);
        assertNotNull(path.getPUT());
        assertNotNull(path.getPUT().getRequestBody());
        assertNotNull(path.getPUT().getRequestBody().getContent());
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json"));
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json").getEncoding());
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json").getEncoding().get("profileImage"));
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json").getEncoding().get("profileImage").getHeaders());
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json").getEncoding().get("profileImage").getHeaders().get("X-Rate-Limit-Limit"));

        HeaderImpl header = new HeaderImpl();
        header = (HeaderImpl) path.getPUT().getRequestBody().getContent().get("application/json").getEncoding().get("profileImage").getHeaders().get("X-Rate-Limit-Limit");

        assertEquals(header.getDescription(), "The number of allowed requests in the current period");
        assertNotNull(header.getSchema());
        assertEquals(header.getSchema().getType().toString(), "integer");
        assertEquals(path.getPUT().getRequestBody().getContent().get("application/json").getEncoding().get("profileImage").getHeaders().size(), 1);
    }

    @Test
    public void testExampleInParameter() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");

        assertNotNull(path);
        assertNotNull(path.getPUT().getParameters().get(0).getExample());
        assertEquals(path.getPUT().getParameters().get(0).getExample(), "1");
    }

    @Test
    public void testExampleInSchemaProperties() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);

        Object example = new Object();
        example = path.getPUT().getRequestBody().getContent().get("application/json").getSchema().getProperties().get("airMiles").getExample();
        assertNotNull(example);
        assertEquals(example, 32126319);

        example = path.getPUT().getRequestBody().getContent().get("application/json").getSchema().getProperties().get("seatPreference").getExample();
        assertNotNull(example);
        assertEquals(example, "window");
    }

    @Test
    public void testExampleInComponentsSchemaProperties() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        assertNotNull(result.getOpenAPI().getComponents());
        assertNotNull(result.getOpenAPI().getComponents().getSchemas());
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("User"));
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("User").getProperties());
        assertNotNull(result.getOpenAPI().getComponents().getSchemas().get("User").getProperties().get("password"));

        Object example = new Object();
        example = result.getOpenAPI().getComponents().getSchemas().get("User").getProperties().get("password").getExample();
        assertNotNull(example);
        assertEquals(example, "bobSm37");
    }

    @Test
    public void testEncoding() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        PathItemImpl path = new PathItemImpl();
        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);

        assertNotNull(path);
        assertNotNull(path.getPUT());
        assertNotNull(path.getPUT().getRequestBody());
        assertNotNull(path.getPUT().getRequestBody().getContent());
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json"));
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json").getEncoding());

        EncodingImpl encoding = new EncodingImpl();
        encoding = (EncodingImpl) path.getPUT().getRequestBody().getContent().get("application/json").getEncoding().get("profileImage");
        assertNotNull(encoding);
        assertEquals(encoding.getContentType(), "text/plain");
        assertEquals(encoding.getStyle().toString(), "form");
        assertEquals(encoding.getAllowReserved(), true);
        assertEquals(encoding.getExplode(), false);
        assertNotNull(encoding.getHeaders());
    }

    @Test
    public void testMediaTypeInAPIResponse() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        PathItemImpl path = new PathItemImpl();
        APIResponseImpl response = new APIResponseImpl();

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("202"));
        response = (APIResponseImpl) path.getGET().getResponses().get("202");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("applictaion/json"));
        assertNotNull(response.getContent().get("applictaion/json").getSchema());
        assertEquals(response.getContent().get("applictaion/json").getSchema().getRef(), "#/components/schemas/Flight");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/availability");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("202"));
        response = (APIResponseImpl) path.getGET().getResponses().get("202");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("applictaion/json"));
        assertNotNull(response.getContent().get("applictaion/json").getSchema());
        assertEquals(response.getContent().get("applictaion/json").getSchema().getRef(), "#/components/schemas/Flight");
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("n/a"));

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getType().toString(), "string");

        assertNotNull(path);
        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getResponses());
        assertNotNull(path.getPOST().getResponses().get("201"));
        response = (APIResponseImpl) path.getPOST().getResponses().get("201");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getType().toString(), "string");
        assertEquals(response.getContent().get("application/json").getSchema().getDescription(), "id of the new booking");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getType().toString(), "array");
        assertNotNull(response.getContent().get("application/json").getSchema().getItems());
        assertEquals(response.getContent().get("application/json").getSchema().getItems().getRef(), "#/components/schemas/Booking");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getType().toString(), "array");
        assertNotNull(response.getContent().get("application/json").getSchema().getItems());
        assertNotNull(response.getContent().get("application/json").getSchema().getItems().getOneOf());
        assertNotNull(response.getContent().get("application/json").getSchema().getItems().getOneOf().get(0));
        assertTrue(response.getContent().get("application/json").getSchema().getItems().getOneOf().size() == 1);
        assertEquals(response.getContent().get("application/json").getSchema().getItems().getOneOf().get(0).getRef(), "#/components/schemas/Review");

        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getResponses());
        assertNotNull(path.getPOST().getResponses().get("201"));
        response = (APIResponseImpl) path.getPOST().getResponses().get("201");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getType().toString(), "string");
        assertEquals(response.getContent().get("application/json").getSchema().getDescription(), "id of the new review");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getRef(), "#/components/schemas/Review");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertNotNull(response.getContent().get("application/json").getSchema().getOneOf());
        assertTrue(response.getContent().get("application/json").getSchema().getOneOf().size() == 2);
        assertNotNull(response.getContent().get("application/json").getSchema().getOneOf().get(0));
        assertNotNull(response.getContent().get("application/json").getSchema().getOneOf().get(1));
        assertEquals(response.getContent().get("application/json").getSchema().getOneOf().get(0).getRef(), "#/components/schemas/Review");
        assertEquals(response.getContent().get("application/json").getSchema().getOneOf().get(1).getRef(), "#/components/schemas/User");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{airline}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getRef(), "#/components/schemas/Review");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}/{airlines}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertNotNull(response.getContent());
        assertNotNull(response.getContent().get("application/json"));
        assertNotNull(response.getContent().get("application/json").getSchema());
        assertEquals(response.getContent().get("application/json").getSchema().getRef(), "#/components/schemas/Review");
    }

    @Test
    public void testMediaTypeInRequestBody() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        PathItemImpl path = new PathItemImpl();

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings");
        assertNotNull(path);
        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getRequestBody());
        assertNotNull(path.getPOST().getRequestBody().getContent());
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json"));
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json").getSchema());
        assertEquals(path.getPOST().getRequestBody().getContent().get("application/json").getSchema().getRef(), "#/components/schemas/Booking");
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json").getExamples());
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json").getExamples().get("booking"));
        assertEquals(path.getPOST().getRequestBody().getContent().get("application/json").getExamples().get("booking").getSummary(), "External booking example");
        assertEquals(path.getPOST().getRequestBody().getContent().get("application/json").getExamples().get("booking").getExternalValue(),
                     "http://foo.bar/examples/booking-example.json");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);
        assertNotNull(path.getPUT());
        assertNotNull(path.getPUT().getRequestBody());
        assertNotNull(path.getPUT().getRequestBody().getContent());
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json"));
        assertNotNull(path.getPUT().getRequestBody().getContent().get("application/json").getSchema());
        assertEquals(path.getPUT().getRequestBody().getContent().get("application/json").getSchema().getType().toString(), "object");
        Map<String, Schema> properties = new HashMap<String, Schema>();
        properties = path.getPUT().getRequestBody().getContent().get("application/json").getSchema().getProperties();
        assertNotNull(properties);
        for (String key : properties.keySet()) {
            assertNotNull(properties.get(key));
        }
        assertEquals(properties.get("departtureFlight").getRef(), "#/components/schemas/Flight");
        assertEquals(properties.get("returningFlight").getRef(), "#/components/schemas/Flight");
        assertEquals(properties.get("creditCard").getRef(), "#/components/schemas/CreditCard");
        assertEquals(properties.get("airMiles").getType().toString(), "string");
        assertEquals(properties.get("airMiles").getExample(), 32126319);
        assertEquals(properties.get("seatPreference").getType().toString(), "string");
        assertEquals(properties.get("seatPreference").getExample(), "window");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews");
        assertNotNull(path);
        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getRequestBody());
        assertNotNull(path.getPOST().getRequestBody().getContent());
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json"));
        assertNotNull(path.getPOST().getRequestBody().getContent().get("application/json").getSchema());
        assertEquals(path.getPOST().getRequestBody().getContent().get("application/json").getSchema().getRef(), "#/components/schemas/Review");

    }

    @Test
    public void testMediaTypeInParameter() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        PathItemImpl path = new PathItemImpl();

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getParameters());
        assertNotNull(path.getGET().getParameters().get(0));
        assertNotNull(path.getGET().getParameters().get(0).getContent());
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*"));
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*").getSchema());
        assertEquals(path.getGET().getParameters().get(0).getContent().get("*/*").getSchema().getType().toString(), "integer");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getParameters());
        assertNotNull(path.getGET().getParameters().get(0));
        assertNotNull(path.getGET().getParameters().get(0).getContent());
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*"));
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*").getSchema());
        assertEquals(path.getGET().getParameters().get(0).getContent().get("*/*").getSchema().getType().toString(), "string");
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*").getExamples());

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{airline}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getParameters());
        assertNotNull(path.getGET().getParameters().get(0));
        assertNotNull(path.getGET().getParameters().get(0).getContent());
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*"));
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*").getSchema());
        assertEquals(path.getGET().getParameters().get(0).getContent().get("*/*").getSchema().getType().toString(), "string");
        assertNotNull(path.getGET().getParameters().get(0).getContent().get("*/*").getExamples());
    }

    @Test
    public void testSecurityRequirement() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        PathItemImpl booking = new PathItemImpl();
        booking = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(booking);
        assertNotNull(booking.getPUT());
        assertNotNull(booking.getPUT().getSecurity());
        assertNotNull(booking.getPUT().getSecurity().get(0));
        assertNotNull(booking.getPUT().getSecurity().get(0).get("bookingoauth2"));

        PathItemImpl reviews = new PathItemImpl();
        reviews = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews");
        assertNotNull(reviews);
        assertNotNull(reviews.getPOST());
        assertNotNull(reviews.getPOST().getSecurity());
        assertNotNull(reviews.getPOST().getSecurity().get(0));
        assertNotNull(reviews.getPOST().getSecurity().get(0).get("reviewoauth2"));
    }

    @Test
    public void testSecurityScheme() {

        Map<String, SecurityScheme> schemes = new HashMap<String, SecurityScheme>();

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());

        schemes = result.getOpenAPI().getComponents().getSecuritySchemes();
        assertNotNull(schemes);
        assertNotNull(schemes.get("reviewoauth2"));
        assertNotNull(schemes.get("reviewoauth2").getFlows());
        assertEquals(schemes.get("reviewoauth2").getType().toString(), "oauth2");
        assertEquals(schemes.get("reviewoauth2").getDescription(), "authentication needed to create and delete reviews");

        schemes = result.getOpenAPI().getComponents().getSecuritySchemes();
        assertNotNull(schemes);
        assertNotNull(schemes.get("bookingoauth2"));
        assertNotNull(schemes.get("bookingoauth2").getFlows());
        assertEquals(schemes.get("bookingoauth2").getType().toString(), "oauth2");
        assertEquals(schemes.get("bookingoauth2").getDescription(), "authentication needed to edit bookings");
    }

    @Test
    public void testAPIResponses() {

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());
        assertNotNull(result.getOpenAPI().getPaths());

        PathItemImpl path = new PathItemImpl();
        APIResponseImpl response = new APIResponseImpl();

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 1);
        assertNotNull(path.getGET().getResponses().get("202"));
        response = (APIResponseImpl) path.getGET().getResponses().get("202");
        assertEquals(response.getDescription(), "failed operation");
        assertNotNull(response.getContent());

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/availability");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("202"));
        response = (APIResponseImpl) path.getGET().getResponses().get("202");
        assertEquals(response.getDescription(), "failed operation");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "No available flights found");
        assertNotNull(response.getContent());

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "Bookings retrieved");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "No bookings found for the user.");

        assertNotNull(path);
        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getResponses());
        assertTrue(path.getPOST().getResponses().size() == 1);
        assertNotNull(path.getPOST().getResponses().get("201"));
        response = (APIResponseImpl) path.getPOST().getResponses().get("201");
        assertEquals(response.getDescription(), "Booking created");
        assertNotNull(response.getContent());

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/bookings/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "booking retrieved");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "No bookings found for the user.");

        assertNotNull(path.getPUT());
        assertNotNull(path.getPUT().getResponses());
        assertTrue(path.getPUT().getResponses().size() == 2);
        assertNotNull(path.getPUT().getResponses().get("200"));
        response = (APIResponseImpl) path.getPUT().getResponses().get("200");
        assertEquals(response.getDescription(), "Booking updated");
        assertNotNull(path.getPUT().getResponses().get("404"));
        response = (APIResponseImpl) path.getPUT().getResponses().get("404");
        assertEquals(response.getDescription(), "Booking not found");

        assertNotNull(path.getDELETE());
        assertNotNull(path.getDELETE().getResponses());
        assertTrue(path.getDELETE().getResponses().size() == 2);
        assertNotNull(path.getDELETE().getResponses().get("200"));
        response = (APIResponseImpl) path.getDELETE().getResponses().get("200");
        assertEquals(response.getDescription(), "Booking deleted successfully.");
        assertNotNull(path.getDELETE().getResponses().get("404"));
        response = (APIResponseImpl) path.getDELETE().getResponses().get("404");
        assertEquals(response.getDescription(), "Booking not found.");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 1);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "successful operation");
        assertNotNull(response.getContent());

        assertNotNull(path.getPOST());
        assertNotNull(path.getPOST().getResponses());
        assertTrue(path.getPOST().getResponses().size() == 1);
        assertNotNull(path.getPOST().getResponses().get("201"));
        response = (APIResponseImpl) path.getPOST().getResponses().get("201");
        assertEquals(response.getDescription(), "review created");
        assertNotNull(response.getContent());

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{id}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "Review retrieved");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "Review not found");

        assertNotNull(path.getDELETE());
        assertNotNull(path.getDELETE().getResponses());
        assertTrue(path.getDELETE().getResponses().size() == 2);
        assertNotNull(path.getDELETE().getResponses().get("200"));
        response = (APIResponseImpl) path.getDELETE().getResponses().get("200");
        assertEquals(response.getDescription(), "Review deleted");
        assertNotNull(path.getDELETE().getResponses().get("404"));
        response = (APIResponseImpl) path.getDELETE().getResponses().get("404");
        assertEquals(response.getDescription(), "Review not found");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "Review(s) retrieved");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "Review(s) not found");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{airline}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "Review(s) retrieved");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "Review(s) not found");

        path = (PathItemImpl) result.getOpenAPI().getPaths().get("/reviews/{user}/{airlines}");
        assertNotNull(path);
        assertNotNull(path.getGET());
        assertNotNull(path.getGET().getResponses());
        assertTrue(path.getGET().getResponses().size() == 2);
        assertNotNull(path.getGET().getResponses().get("200"));
        response = (APIResponseImpl) path.getGET().getResponses().get("200");
        assertEquals(response.getDescription(), "Review(s) retrieved");
        assertNotNull(response.getContent());
        assertNotNull(path.getGET().getResponses().get("404"));
        response = (APIResponseImpl) path.getGET().getResponses().get("404");
        assertEquals(response.getDescription(), "Review(s) not found");
    }

    @Test
    public void testOAuthFlow() {

        OAuthFlowsImpl flows = new OAuthFlowsImpl();

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());

        flows = (OAuthFlowsImpl) result.getOpenAPI().getComponents().getSecuritySchemes().get("reviewoauth2").getFlows();
        assertNotNull(flows);
        assertNotNull(flows.getImplicit());
        assertEquals(flows.getImplicit().getAuthorizationUrl(), "https://example.com/api/oauth/dialog");

        flows = (OAuthFlowsImpl) result.getOpenAPI().getComponents().getSecuritySchemes().get("bookingoauth2").getFlows();
        assertNotNull(flows);
        assertNotNull(flows.getImplicit());
        assertEquals(flows.getImplicit().getAuthorizationUrl(), "https://example.com/api/oauth/dialog");
    }

    @Test
    public void testOAuthFlows() {

        OAuthFlowsImpl flows = new OAuthFlowsImpl();

        assertNotNull(result);
        assertNotNull(result.getOpenAPI());

        flows = (OAuthFlowsImpl) result.getOpenAPI().getComponents().getSecuritySchemes().get("reviewoauth2").getFlows();
        assertNotNull(flows);
        assertNotNull(flows.getImplicit());

        flows = (OAuthFlowsImpl) result.getOpenAPI().getComponents().getSecuritySchemes().get("bookingoauth2").getFlows();
        assertNotNull(flows);
        assertNotNull(flows.getImplicit());
    }
}
