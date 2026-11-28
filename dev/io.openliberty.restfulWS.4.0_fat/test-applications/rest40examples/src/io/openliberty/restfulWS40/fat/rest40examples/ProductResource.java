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
import java.util.List;
import java.util.ArrayList;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    private static List<Product> products = new ArrayList<>();

    @GET
    public Response getAllProducts() {
        return Response.ok(products).build();
    }

    @GET
    @Path("/{id}")
    public Response getProduct(@PathParam("id") Long id) {
        Product product = products.stream()
            .filter(p -> p.getId().equals(id))
            .findFirst()
            .orElse(null);

        if (product == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(product).build();
    }

    @POST
    public Response createProduct(Product product) {
        products.add(product);
        return Response.status(Response.Status.CREATED)
            .entity(product)
            .build();
    }

    @PUT
    @Path("/{id}")
    public Response updateProduct(@PathParam("id") Long id, Product updatedProduct) {
        Product product = products.stream()
            .filter(p -> p.getId().equals(id))
            .findFirst()
            .orElse(null);

        if (product == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Preserve the ID from path parameter
        updatedProduct.setId(id);
        products.remove(product);
        products.add(updatedProduct);

        return Response.ok(updatedProduct).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteProduct(@PathParam("id") Long id) {
        boolean removed = products.removeIf(p -> p.getId().equals(id));

        if (!removed) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.noContent().build();
    }

    // Helper method for testing - clear all products
    @DELETE
    @Path("/clear")
    public Response clearAllProducts() {
        products.clear();
        return Response.noContent().build();
    }
}

// Made with Bob
