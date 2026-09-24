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

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.json.Json;
import jakarta.json.JsonMergePatch;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {

    // Using thread-safe map for storage
    private static Map<Long, Customer> customers = new ConcurrentHashMap<>();

    static {
        // Initialize with a test customer
        customers.put(1L, new Customer(1L, "John Doe", "john@example.com"));
    }

    @GET
    @Path("/{id}")
    public Response getCustomer(@PathParam("id") Long id) {
        Customer customer = customers.get(id);
        if (customer == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(customer).build();
    }

    @POST
    public Response createCustomer(Customer customer) {
        customers.put(customer.getId(), customer);
        return Response.status(Response.Status.CREATED)
            .entity(customer)
            .build();
    }

    // NEW in REST 4.0: JSON Merge Patch support (RFC 7396)
    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_MERGE_PATCH_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response patchCustomer(
            @PathParam("id") Long id,
            JsonValue patchJson) {

        Customer customer = customers.get(id);
        if (customer == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try (Jsonb jsonb = JsonbBuilder.create()) {
            // Convert customer to JsonValue
            JsonValue customerJson = jsonb.fromJson(
                jsonb.toJson(customer), JsonValue.class);

            // Create merge patch manually from the incoming JSON
            JsonMergePatch mergePatch = Json.createMergePatch(patchJson);

            // Apply the merge patch
            JsonValue patchedJson = mergePatch.apply(customerJson);

            // Convert back to Customer object
            Customer patchedCustomer = jsonb.fromJson(
                patchedJson.toString(), Customer.class);

            customers.put(id, patchedCustomer);

            return Response.ok(patchedCustomer).build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Invalid patch: " + e.getMessage())
                .build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCustomer(@PathParam("id") Long id) {
        Customer removed = customers.remove(id);
        if (removed == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.noContent().build();
    }

    // Helper method for testing - clear all customers
    @DELETE
    @Path("/clear")
    public Response clearAllCustomers() {
        customers.clear();
        return Response.noContent().build();
    }
}

// Made with Bob
