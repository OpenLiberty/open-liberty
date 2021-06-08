/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.standalone.rest.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.JsonBody.json;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.matchers.MatchType;

import io.openliberty.standalone.rest.client.clientinterfaces.JsonPClient;
import io.openliberty.standalone.rest.client.clientinterfaces.PersonService;
import io.openliberty.standalone.rest.client.entities.Animal;
import io.openliberty.standalone.rest.client.entities.GetterClient;
import io.openliberty.standalone.rest.client.entities.Person;
import io.openliberty.standalone.rest.client.entities.Pet;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;;

/**
 * Unit test for simple App.
 */
@ExtendWith(MockServerExtension.class)
class MPRestClientTest {
    private static final String LS = System.lineSeparator();
    @BeforeAll
    public static void beforeClass(TestInfo testInfo) {
        System.out.println("TEST CLASS STARTING: " + testInfo.getDisplayName());
    }

    @AfterAll
    public static void afterClass(TestInfo testInfo) {
        System.out.println("TEST CLASS FINISHED: " + testInfo.getDisplayName());
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        System.out.println("TEST STARTING: " + testInfo.getDisplayName());
    }

    @AfterEach
    void tearDown(TestInfo testInfo, MockServerClient mockServer) {
        System.out.println("TEST FINISHED: " + testInfo.getDisplayName());
        mockServer.reset();
    }

    @Test
    void testPlainText(MockServerClient mockServer) {
        mockServer.when(
                    request()
                        .withMethod("GET")
                        .withPath("/abc")
                    ).respond(
                    response()
                        .withStatusCode(200)
                        .withHeader("X-ServerURL", "https://www.mock-server.com")
                        .withHeader("Content-type", "text/plain")
                        .withBody("ABC")
        );

        String uri = "http://localhost:" + mockServer.getPort() + "/abc";
        GetterClient client = RestClientBuilder.newBuilder().baseUri(URI.create(uri)).build(GetterClient.class);
        Response response = client.get();
        assertEquals(200, response.getStatus());
        assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()));
        assertEquals("https://www.mock-server.com", response.getHeaderString("X-ServerURL"));
        assertEquals("ABC", response.readEntity(String.class));
    }

    @Test
    void testJsonb(MockServerClient mockServer) {
        mockServer.when(
                    request()
                        .withMethod("GET")
                        .withPath("/person/1")
                    ).respond(
                    response()
                        .withStatusCode(200)
                        .withHeader("Content-type", "application/json")
                        .withBody("{\"firstName\": \"John\", \"lastName\": \"Doe\", \"birthDate\": \"2000-01-01\"}")
        );
        mockServer.when(
                    request()
                        .withMethod("GET")
                        .withPath("/person/2")
                    ).respond(
                    response()
                        .withStatusCode(200)
                        .withHeader("Content-type", "application/json")
                        .withBody("{\"firstName\": \"Jane\", \"lastName\": \"Doe\", \"birthDate\": \"2001-02-14\"}")
        );
        mockServer.when(
                    request()
                        .withMethod("POST")
                        .withPath("/person")
                        .withBody(
                            json("{"+LS+"\"firstName\": \"Skip\","+LS+"\"lastName\": \"Doe\","+LS+"\"birthDate\": \"2020-12-24\""+LS+"}", MatchType.STRICT))
                    ).respond(
                    response()
                        .withStatusCode(200)
                        .withHeader("Content-type", "text/plain")
                        .withBody("3")
        );

        String uri = "http://localhost:" + mockServer.getPort();
        PersonService client = RestClientBuilder.newBuilder().baseUri(URI.create(uri)).build(PersonService.class);
        assertEquals(new Person("John", "Doe", LocalDate.of(2000, 1, 1)), client.get("1"));
        assertEquals(new Person("Jane", "Doe", LocalDate.of(2001, 2, 14)), client.get("2"));
        assertEquals("3", client.post(new Person("Skip", "Doe", LocalDate.of(2020, 12, 24))));
    }

    @Test
    void testSubResource(MockServerClient mockServer) {
        mockServer.when(
                    request()
                        .withMethod("GET")
                        .withPath("/person/1/pets")
                    ).respond(
                    response()
                        .withStatusCode(200)
                        .withHeader("Content-type", "application/json")
                        .withBody("[{\"name\": \"Fluffy\", \"type\": \"CAT\", \"birthDate\": \"2020-05-05\"}]")
        );

        String uri = "http://localhost:" + mockServer.getPort();
        PersonService client = RestClientBuilder.newBuilder().baseUri(URI.create(uri)).build(PersonService.class);
        List<Pet> pets = client.petsByOwner("1").allPets();
        assertEquals(1, pets.size());
        assertEquals(new Pet("Fluffy", Animal.CAT, LocalDate.of(2020, 5, 5)), pets.get(0));
    }

    @Test
    void testJsonP(MockServerClient mockServer) {
        mockServer.when(
                    request()
                        .withMethod("GET")
                        .withPath("/1")
                    ).respond(
                    response()
                        .withStatusCode(200)
                        .withHeader("Content-type", "application/json")
                        .withBody("{\"name\": \"Fluffy\", \"type\": \"CAT\", \"numFeet\": 4}")
        );

        String uri = "http://localhost:" + mockServer.getPort();
        JsonPClient client = RestClientBuilder.newBuilder().baseUri(URI.create(uri)).build(JsonPClient.class);
        JsonObject jsonObject = client.getSpecific(1);
        assertNotNull(jsonObject);
        assertEquals("Fluffy", jsonObject.getString("name"));
        assertEquals("CAT", jsonObject.getString("type"));
        assertEquals(4, jsonObject.getInt("numFeet"));
    }
}