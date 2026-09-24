/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.restfulWS40.fat.rest40examples;

import static org.junit.Assert.*;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/Rest40ExamplesTestServlet")
public class Rest40ExamplesTestServlet extends FATServlet {

    private String getBaseUrl(HttpServletRequest request) {
        return "http://" + request.getServerName() + ":" + request.getServerPort() +
               "/rest40examples/rest";
    }

    @Test
    public void testProductCRUD(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Client client = ClientBuilder.newClient();
        String baseUrl = getBaseUrl(request);

        try {
            // Clear existing products
            WebTarget clearTarget = client.target(baseUrl + "/products/clear");
            Response clearResp = clearTarget.request().delete();
            assertEquals(204, clearResp.getStatus());
            clearResp.close();

            // Test POST - Create product
            Product product = new Product(1L, "Laptop", 999.99);
            WebTarget target = client.target(baseUrl + "/products");
            Response postResp = target.request(MediaType.APPLICATION_JSON)
                .post(Entity.json(product));
            assertEquals(201, postResp.getStatus());
            Product created = postResp.readEntity(Product.class);
            assertNotNull(created);
            assertEquals("Laptop", created.getName());
            postResp.close();

            // Test GET - Get single product
            WebTarget getTarget = client.target(baseUrl + "/products/1");
            Response getResp = getTarget.request(MediaType.APPLICATION_JSON).get();
            assertEquals(200, getResp.getStatus());
            Product retrieved = getResp.readEntity(Product.class);
            assertEquals("Laptop", retrieved.getName());
            assertEquals(Double.valueOf(999.99), retrieved.getPrice());
            getResp.close();

            // Test PUT - Update product
            Product updated = new Product(1L, "Gaming Laptop", 1299.99);
            WebTarget putTarget = client.target(baseUrl + "/products/1");
            Response putResp = putTarget.request(MediaType.APPLICATION_JSON)
                .put(Entity.json(updated));
            assertEquals(200, putResp.getStatus());
            Product updatedProduct = putResp.readEntity(Product.class);
            assertEquals("Gaming Laptop", updatedProduct.getName());
            assertEquals(Long.valueOf(1L), updatedProduct.getId());
            putResp.close();

            // Test DELETE - Delete product
            WebTarget deleteTarget = client.target(baseUrl + "/products/1");
            Response deleteResp = deleteTarget.request().delete();
            assertEquals(204, deleteResp.getStatus());
            deleteResp.close();

            // Verify deletion
            Response getAfterDelete = client.target(baseUrl + "/products/1")
                .request(MediaType.APPLICATION_JSON).get();
            assertEquals(404, getAfterDelete.getStatus());
            getAfterDelete.close();

        } finally {
            client.close();
        }
    }

    @Test
    public void testGetMatchedResourceTemplate(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Client client = ClientBuilder.newClient();
        String baseUrl = getBaseUrl(request);

        try {
            // Test getMatchedResourceTemplate - REST 4.0 feature
            WebTarget target = client.target(baseUrl + "/api/users/123/orders/456");
            Response resp = target.request(MediaType.APPLICATION_JSON).get();

            assertEquals(200, resp.getStatus());

            // Check the X-Resource-Template header
            String template = resp.getHeaderString("X-Resource-Template");
            assertNotNull("X-Resource-Template header should be present", template);
            assertTrue("Template should contain path parameters",
                       template.contains("{userId}") && template.contains("{orderId}"));

            // Verify response body
            Order order = resp.readEntity(Order.class);
            assertEquals(Long.valueOf(456L), order.getOrderId());
            assertEquals(Long.valueOf(123L), order.getUserId());

            resp.close();
        } finally {
            client.close();
        }
    }

    @Test
    public void testJsonMergePatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Client client = ClientBuilder.newClient();
        String baseUrl = getBaseUrl(request);

        try {
            // Clear and create a customer
            WebTarget clearTarget = client.target(baseUrl + "/customers/clear");
            clearTarget.request().delete().close();

            Customer customer = new Customer(1L, "John Doe", "john@example.com");
            WebTarget createTarget = client.target(baseUrl + "/customers");
            Response createResp = createTarget.request(MediaType.APPLICATION_JSON)
                .post(Entity.json(customer));
            assertEquals(201, createResp.getStatus());
            createResp.close();

            // Test JSON Merge Patch - REST 4.0 feature
            String patchJson = "{\"email\":\"john.doe@newdomain.com\"}";
            WebTarget patchTarget = client.target(baseUrl + "/customers/1");
            Response patchResp = patchTarget.request(MediaType.APPLICATION_JSON)
                .method("PATCH", Entity.entity(patchJson, MediaType.APPLICATION_MERGE_PATCH_JSON));

            assertEquals(200, patchResp.getStatus());

            Customer patched = patchResp.readEntity(Customer.class);
            assertEquals("John Doe", patched.getName()); // Name unchanged
            assertEquals("john.doe@newdomain.com", patched.getEmail()); // Email updated

            patchResp.close();
        } finally {
            client.close();
        }
    }

    @Test
    public void testProductNotFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Client client = ClientBuilder.newClient();
        String baseUrl = getBaseUrl(request);

        try {
            WebTarget target = client.target(baseUrl + "/products/99999");
            Response resp = target.request(MediaType.APPLICATION_JSON).get();
            assertEquals(404, resp.getStatus());
            resp.close();
        } finally {
            client.close();
        }
    }

    @Test
    public void testCustomerNotFound(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Client client = ClientBuilder.newClient();
        String baseUrl = getBaseUrl(request);

        try {
            WebTarget target = client.target(baseUrl + "/customers/99999");
            Response resp = target.request(MediaType.APPLICATION_JSON).get();
            assertEquals(404, resp.getStatus());
            resp.close();
        } finally {
            client.close();
        }
    }

    @Test
    public void testInvalidJsonMergePatch(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Client client = ClientBuilder.newClient();
        String baseUrl = getBaseUrl(request);

        try {
            // Try to patch a non-existent customer
            String patchJson = "{\"email\":\"test@example.com\"}";
            WebTarget patchTarget = client.target(baseUrl + "/customers/99999");
            Response patchResp = patchTarget.request(MediaType.APPLICATION_JSON)
                .method("PATCH", Entity.entity(patchJson, "application/merge-patch+json"));

            assertEquals(404, patchResp.getStatus());
            patchResp.close();
        } finally {
            client.close();
        }
    }

    @Test
    public void testFileUpload(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Client client = ClientBuilder.newClient();
        String baseUrl = getBaseUrl(request);

        try {
            // Create a simple text file content
            String fileContent = "This is a test file for multipart upload";
            byte[] fileBytes = fileContent.getBytes();

            // Build multipart form data using EntityPart (REST 4.0 API)
            EntityPart filePart = EntityPart.withName("file")
                .fileName("test.txt")
                .content(new java.io.ByteArrayInputStream(fileBytes))
                .mediaType(MediaType.TEXT_PLAIN_TYPE)
                .build();

            EntityPart descriptionPart = EntityPart.withName("description")
                .content("Test file upload")
                .mediaType(MediaType.TEXT_PLAIN_TYPE)
                .build();

            GenericEntity<List<EntityPart>> parts = new GenericEntity<List<EntityPart>>(
                java.util.Arrays.asList(filePart, descriptionPart)) {};

            // Upload the file
            WebTarget uploadTarget = client.target(baseUrl + "/upload");
            Response uploadResp = uploadTarget.request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(parts, MediaType.MULTIPART_FORM_DATA));

            assertEquals(200, uploadResp.getStatus());

            // Verify response
            UploadResponse uploadResponse = uploadResp.readEntity(UploadResponse.class);
            assertNotNull(uploadResponse);
            assertEquals("test.txt", uploadResponse.getFileName());
            assertEquals(fileBytes.length, uploadResponse.getFileSize());
            assertEquals("Upload successful", uploadResponse.getMessage());

            uploadResp.close();
        } finally {
            client.close();
        }
    }
}

// Made with Bob
