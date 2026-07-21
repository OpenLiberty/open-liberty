/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.ejbinjection.servlet;


import static org.junit.Assert.assertEquals;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/EjbInjectionClientTestServlet")
public class EjbInjectionClientTestServlet extends FATServlet {

    private static final String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/EjbInjection/";

    private Client client;

    @Override
    public void before() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @Override
    public void after() {
        client.close();
    }

    @Test
    public void testNoInterfaceInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("nointerface/greet")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello, World!", response.readEntity(String.class));
    }

    @Test
    public void testSingleInterfaceInjection() {
        String message = "Hello, World!";
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("singleinterface/echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .post(Entity.entity(message, MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(message, response.readEntity(String.class));
    }

    @Test
    public void testMultipleInterfacesInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("multipleinterfaces/greet")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello, World!", response.readEntity(String.class));

        Response response2 = client.target(URI_CONTEXT_ROOT)
                        .path("multipleinterfaces/farewell")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response2.getStatus());
        assertEquals("Goodbye, World!", response2.readEntity(String.class));
    }

    @Test
    public void testSingleAnnotatedInterfaceInjection() {
        String message = "Hello, World!";
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("singleannotatedinterface/echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .post(Entity.entity(message, MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(message, response.readEntity(String.class));
    }

    @Test
    @SkipForRepeat({SkipForRepeat.NO_MODIFICATION, "JAXRS-2.1"}) // Skip EE7 and EE8 - requires RESTEasy (EE9+) for EJB @Local interface resolution
    public void testSingleNonImplementedAnnotatedInterfaceInjection() {
        String message = "Hello, World!";
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("singlenonimplementedannotatedinterface/echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .post(Entity.entity(message, MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(message, response.readEntity(String.class));
    }

    @Test
    public void testMultipleAnnotatedInterfacesInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("multipleannotatedinterfaces/greet")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello, World!", response.readEntity(String.class));

        Response response2 = client.target(URI_CONTEXT_ROOT)
                        .path("multipleannotatedinterfaces/farewell")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response2.getStatus());
        assertEquals("Goodbye, World!", response2.readEntity(String.class));
    }

    @Test
    @SkipForRepeat({SkipForRepeat.NO_MODIFICATION, "JAXRS-2.1"}) // Skip EE7 and EE8 - requires RESTEasy (EE9+) for EJB @Local interface resolution
    public void testMultipleNonImplementedAnnotatedInterfacesInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("multiplenonimplementedannotatedinterfaces/greet")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello, World!", response.readEntity(String.class));

        Response response2 = client.target(URI_CONTEXT_ROOT)
                        .path("multiplenonimplementedannotatedinterfaces/farewell")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response2.getStatus());
        assertEquals("Goodbye, World!", response2.readEntity(String.class));
    }

    @Test
    public void testSingleImplementedMixedAnnotationInjection() {
        // Test method with annotations from interface
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("singleimplementedmixed/interfaceAnnotated")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Single Implemented Mixed - Interface Annotated Method", response.readEntity(String.class));

        // Test method with annotations from class
        Response response2 = client.target(URI_CONTEXT_ROOT)
                        .path("singleimplementedmixed/classmethod")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response2.getStatus());
        assertEquals("Single Implemented Mixed - Class Annotated Method", response2.readEntity(String.class));
    }

    @Test
    @SkipForRepeat({SkipForRepeat.NO_MODIFICATION, "JAXRS-2.1"}) // Skip EE7 and EE8 - requires RESTEasy (EE9+) for EJB @Local interface resolution
    public void testSingleNonImplementedMixedAnnotationInjection() {
        // Test method with annotations from interface
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("singlenonimplementedmixed/interfaceAnnotated")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Single Non-Implemented Mixed - Interface Annotated Method", response.readEntity(String.class));

        // Test method with annotations from class
        Response response2 = client.target(URI_CONTEXT_ROOT)
                        .path("singlenonimplementedmixed/classmethod")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response2.getStatus());
        assertEquals("Single Non-Implemented Mixed - Class Annotated Method", response2.readEntity(String.class));
    }

    @Test
    @SkipForRepeat({SkipForRepeat.NO_MODIFICATION, "JAXRS-2.1"}) // Skip EE7 and EE8 - requires RESTEasy (EE9+) for EJB @Local interface resolution
    public void testMultipleImplementedMixedAnnotationInjection() {
        // Test method with annotations from interface
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("multipleimplementedmixed/methodA")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Multiple Implemented Mixed - Method A (from interface)", response.readEntity(String.class));

        // Test method with annotations from class
        Response response2 = client.target(URI_CONTEXT_ROOT)
                        .path("multipleimplementedmixed/methodB")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response2.getStatus());
        assertEquals("Multiple Implemented Mixed - Method B (from class)", response2.readEntity(String.class));
    }

    @Test
    @SkipForRepeat({SkipForRepeat.NO_MODIFICATION, "JAXRS-2.1"}) // Skip EE7 and EE8 - requires RESTEasy (EE9+) for EJB @Local interface resolution
    public void testMultipleNonImplementedMixedAnnotationInjection() {
        // Test method with annotations from interface
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("multiplenonimplementedmixed/methodA")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Multiple Non-Implemented Mixed - Method A (from interface)", response.readEntity(String.class));

        // Test method with annotations from class
        Response response2 = client.target(URI_CONTEXT_ROOT)
                        .path("multiplenonimplementedmixed/methodB")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response2.getStatus());
        assertEquals("Multiple Non-Implemented Mixed - Method B (from class)", response2.readEntity(String.class));
    }

    /**
     * Scenario 1: Resource class defined as an EJB via annotation (@Stateless)
     * with EJB field injection using @Inject.
     * Tests that EJBs can be injected using @Inject in CDI-enabled environments.
     */
    @Test
    public void testEjbFieldInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("ejbfieldinjection/greet")
                        .queryParam("name", "TestUser")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello from EJB service, TestUser!", response.readEntity(String.class));
    }

    /**
     * Scenario 2: Resource class defined as an EJB via annotation (@Stateless)
     * with an injected method parameter.
     */
    @Test
    public void testEjbMethodParamInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("ejbmethodparam/greet")
                        .queryParam("name", "Bob")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello from EJB service, Bob!", response.readEntity(String.class));
    }

    /**
     * Scenario 3: Resource class defined as an EJB via ejb-jar.xml file
     * (with no EJB annotations) with an injected field.
     */
    @Test
    public void testEjbXmlFieldInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("ejbxmlfield/greet")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello from XML-defined EJB!", response.readEntity(String.class));
    }

    /**
     * Scenario 4: Resource class defined as an EJB via ejb-jar.xml file
     * (with no EJB annotations) with an injected method parameter.
     */
    @Test
    public void testEjbXmlMethodParamInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("ejbxmlmethodparam/greet")
                        .queryParam("name", "Alice")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello from XML-defined EJB, Alice!", response.readEntity(String.class));
    }

    /**
     * Scenario 5: Standard non-EJB JAX-RS resource class that injects an EJB
     * (defined via annotation) using field injection.
     */
    @Test
    public void testStandardResourceWithEjbFieldInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("standardejbfield/greet")
                        .queryParam("name", "Charlie")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello from EJB service, Charlie!", response.readEntity(String.class));
    }

    /**
     * Scenario 6: Standard non-EJB JAX-RS resource class that injects an EJB
     * (defined via annotation) using method parameter injection.
     */
    @Test
    public void testStandardResourceWithEjbMethodParamInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("standardejbmethodinjection/greet")
                        .queryParam("name", "Diana")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello from EJB service, Diana!", response.readEntity(String.class));
    }

    /**
     * Scenario 7: Standard non-EJB JAX-RS resource class that injects an EJB
     * (defined via ejb-jar.xml) using field injection.
     */
    @Test
    public void testStandardResourceWithXmlEjbFieldInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("standardxmlejbfield/message")
                        .queryParam("name", "Eve")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Message from XML-defined EJB service for Eve!", response.readEntity(String.class));
    }

    /**
     * Scenario 8: Standard non-EJB JAX-RS resource class that injects an EJB
     * (defined via ejb-jar.xml) using method parameter injection.
     */
    @Test
    public void testStandardResourceWithXmlEjbMethodParamInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("standardxmlejbmethodinjection/status")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("XML-defined EJB service is active", response.readEntity(String.class));
    }

}
