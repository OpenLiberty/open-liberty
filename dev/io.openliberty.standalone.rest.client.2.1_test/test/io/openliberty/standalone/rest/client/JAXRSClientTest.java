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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;

import io.openliberty.standalone.rest.client.entities.Person;

@ExtendWith(MockServerExtension.class)
public class JAXRSClientTest {
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
    public void testPlainText(MockServerClient mockServer) {
        System.out.println("testPlainText");
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
        
        System.out.println("port = " + mockServer.getPort());
        String uri = "http://localhost:" + mockServer.getPort() + "/abc";
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(uri);
        Response response = target.request().get();
        System.out.println("response = " + response);
        assertEquals(200, response.getStatus());
        assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()));
        assertEquals("https://www.mock-server.com", response.getHeaderString("X-ServerURL"));
        assertEquals("ABC", response.readEntity(String.class));
    }

    @Test
    public void testJsonb(MockServerClient mockServer) {
        mockServer.when(
                    request()
                        .withMethod("GET")
                        .withPath("/person")
                    ).respond(
                    response()
                        .withStatusCode(200)
                        .withHeader("Content-type", "application/json")
                        .withBody("{\"firstName\": \"John\", \"lastName\": \"Doe\", \"birthDate\": \"2000-01-01\"}")
        );
        mockServer.getPort();
        String uri = "http://localhost:" + mockServer.getPort() + "/person";
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(uri);
        Response response = target.request().get();
        assertEquals(200, response.getStatus());
        assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()));
        assertEquals(new Person("John", "Doe", LocalDate.of(2000, 1, 1)), response.readEntity(Person.class));
    }

    @Test
    public void testJsonbList(MockServerClient mockServer) {
        mockServer.when(
                    request()
                        .withMethod("GET")
                        .withPath("/person")
                    ).respond(
                    response()
                        .withStatusCode(200)
                        .withHeader("Content-type", "application/json")
                        .withBody("[{\"firstName\": \"John\", \"lastName\": \"Doe\", \"birthDate\": \"2000-01-01\"},"
                                 + "{\"firstName\": \"Jane\", \"lastName\": \"Doe\", \"birthDate\": \"2001-02-14\"},"
                                 + "{\"firstName\": \"Skip\", \"lastName\": \"Doe\", \"birthDate\": \"2020-12-24\"}]")
        );
        mockServer.getPort();
        String uri = "http://localhost:" + mockServer.getPort() + "/person";
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(uri);
        Response response = target.request().get();
        assertEquals(200, response.getStatus());
        assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()));
        List<Person> people = response.readEntity(new GenericType<ArrayList<Person>>() {});
        assertNotNull(people);
        assertEquals(3, people.size());
        assertEquals(new Person("John", "Doe", LocalDate.of(2000, 1, 1)).toString(), people.get(0).toString());
        assertEquals(new Person("Jane", "Doe", LocalDate.of(2001, 2, 14)).toString(), people.get(1).toString());
        assertEquals(new Person("Skip", "Doe", LocalDate.of(2020, 12, 24)).toString(), people.get(2).toString());
    }

    @Test
    public void testJsonp_array(MockServerClient mockServer) {
        mockServer.when(
                    request()
                        .withMethod("GET")
                        .withPath("/person")
                    ).respond(
                    response()
                        .withStatusCode(200)
                        .withHeader("Content-type", "application/json")
                        .withBody("[{\"firstName\": \"John\", \"lastName\": \"Doe\", \"age\": 21},"
                                 + "{\"firstName\": \"Jane\", \"lastName\": \"Doe\", \"age\": 20},"
                                 + "{\"firstName\": \"Skip\", \"lastName\": \"Doe\", \"age\": 1}]")
        );
        mockServer.getPort();
        String uri = "http://localhost:" + mockServer.getPort() + "/person";
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(uri);
        Response response = target.request().get();
        assertEquals(200, response.getStatus());
        assertTrue(MediaType.APPLICATION_JSON_TYPE.isCompatible(response.getMediaType()));
        JsonArray jsonArray = response.readEntity(JsonArray.class);
        assertNotNull(jsonArray);
        assertEquals(3, jsonArray.size());
        assertEquals("John", jsonArray.getJsonObject(0).getString("firstName"));
        assertEquals("Jane", jsonArray.getJsonObject(1).getString("firstName"));
        assertEquals("Skip", jsonArray.getJsonObject(2).getString("firstName"));
        assertEquals(21, jsonArray.getJsonObject(0).getInt("age"));
        assertEquals(20, jsonArray.getJsonObject(1).getInt("age"));
        assertEquals(1, jsonArray.getJsonObject(2).getInt("age"));
    }
}