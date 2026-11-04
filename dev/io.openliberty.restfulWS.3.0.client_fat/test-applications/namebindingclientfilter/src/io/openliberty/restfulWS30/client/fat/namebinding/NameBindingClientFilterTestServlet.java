/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.restfulWS30.client.fat.namebinding;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Servlet that tests the behavior of client filters with @NameBinding annotations
 * in RESTful WS 3.0 (RESTEasy implementation).
 *
 * This test reproduces the issue reported by the customer in TS020587370:
 * Client filters with @NameBinding annotations worked in JAX-RS 2.1 (Apache CXF)
 * but do not work in RESTful WS 3.0 (RESTEasy).
 */
@SuppressWarnings("serial")
@WebServlet("/NameBindingClientFilterTestServlet")
public class NameBindingClientFilterTestServlet extends FATServlet {

    private static int PORT = Integer.getInteger("bvt.prop.HTTP_default", 8010);

    /**
     * Test that reproduces the customer's issue in TS020587370.
     *
     * Customer expectation (based on JAX-RS 2.1 behavior):
     * - Client filter with @NameBinding annotation should be applied
     * - The X-Annotated-Filter header should be present in the request
     *
     * Actual behavior in RESTful WS 3.0 (RESTEasy):
     * - Client filter with @NameBinding annotation is NOT applied
     * - The X-Annotated-Filter header is NOT present in the request
     * - This test demonstrates the customer's issue by logging the behavior
     */
    @Test
    public void testNameBindingClientFilter() throws Exception {
        Client client = ClientBuilder.newClient();
        
        // Register the client filter with @NameBinding annotation
        // Customer expects this to work like it did in JAX-RS 2.1 (Apache CXF)
        client.register(new AnnotatedClientFilter());
        
        // Make a request to the test resource
        WebTarget target = client.target("http://localhost:" + PORT + "/namebindingclientfilter/resource");
        Response response = target.request().get();
        
        // Verify the response
        assertEquals(200, response.getStatus());
        
        // Check if the header is present
        String headerValue = response.getHeaderString("X-Annotated-Filter");
        boolean headerPresent = headerValue != null;
        
        System.out.println("=== TS020587370 Reproduction Test ===");
        System.out.println("X-Annotated-Filter header present: " + headerPresent);
        System.out.println("Expected in JAX-RS 2.1 (Apache CXF): true");
        System.out.println("Actual in RESTful WS 3.0 (RESTEasy): " + headerPresent);
        
        if (!headerPresent) {
            System.out.println("REPRODUCTION SUCCESSFUL: Client filter with @NameBinding is silently ignored in RESTful WS 3.0");
            System.out.println("This matches the customer's reported issue - the filter does not execute");
        } else {
            System.out.println("UNEXPECTED: Filter was applied - this does not match the customer's issue");
        }
        
        // Assert that the header is NOT present (this is the expected behavior in RESTful WS 3.0)
        // This demonstrates the issue without causing a test failure
        assertNull("Client filter with @NameBinding should be ignored in RESTful WS 3.0 per Jakarta spec",
                   response.getHeaderString("X-Annotated-Filter"));
        
        response.close();
        client.close();
    }
    
    /**
     * Test that demonstrates that client filters without @NameBinding annotations
     * are applied in RESTful WS 3.0 (RESTEasy implementation).
     *
     * This test shows that the issue is specifically with @NameBinding annotations:
     * - Client filters without @NameBinding work correctly in both JAX-RS 2.1 and RESTful WS 3.0
     * - Only client filters WITH @NameBinding are affected by the change in behavior
     */
    @Test
    public void testNonAnnotatedClientFilter() throws Exception {
        Client client = ClientBuilder.newClient();
        
        // Register the client filter without @NameBinding annotation
        // This filter works in both JAX-RS 2.1 (Apache CXF) and RESTful WS 3.0 (RESTEasy)
        client.register(new NonAnnotatedClientFilter());
        
        // Make a request to the test resource
        WebTarget target = client.target("http://localhost:" + PORT + "/namebindingclientfilter/resource");
        Response response = target.request().get();
        
        // Verify the response
        assertEquals(200, response.getStatus());
        
        // The header should be present because the filter without @NameBinding is applied
        // in both JAX-RS 2.1 (Apache CXF) and RESTful WS 3.0 (RESTEasy)
        System.out.println("Checking if X-Non-Annotated-Filter header is present: " +
                          (response.getHeaderString("X-Non-Annotated-Filter") != null));
        assertNotNull("The X-Non-Annotated-Filter header should be present",
                     response.getHeaderString("X-Non-Annotated-Filter"));
        assertEquals("true", response.getHeaderString("X-Non-Annotated-Filter"));
        
        response.close();
        client.close();
    }
}