/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.client.fat.clientfeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


import jakarta.servlet.annotation.WebServlet;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;


import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ClientFeatureTestServlet")
public class ClientFeatureTestServlet extends FATServlet {

    private static int PORT = Integer.getInteger("bvt.prop.HTTP_default", 8010);


    @Test
    public void testCanGETUsingOnlyClientFeature() throws Exception {
        ClientBuilder builder = ClientBuilder.newBuilder();
        Client client = builder.build();
        try {
            WebTarget target = client.target("http://localhost:" + PORT + "/clientfeature/endpoint");
            Response response = target.request().header("x-name", "Robert").get();
            assertEquals(200, response.getStatus());
            assertEquals("Hello Robert", response.readEntity(String.class));
        } finally {
            client.close();
        }
    }
}
