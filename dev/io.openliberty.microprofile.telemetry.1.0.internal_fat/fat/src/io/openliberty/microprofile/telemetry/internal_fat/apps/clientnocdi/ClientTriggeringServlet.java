/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat.apps.clientnocdi;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

/**
 * Tests MP Telemetry class TelemetryClientFilter on an app that is not CDI enabled
 */
@WebServlet("/ClientTriggeringServlet")
public class ClientTriggeringServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(ClientTriggeringServlet.class.getName());

    private Client client;

    @PostConstruct
    private void openClient() {
        LOGGER.info("Creating JAX-RS client");
        client = ClientBuilder.newClient();
    }

    @PreDestroy
    private void closeClient() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LOGGER.info(">>> testClientWithNoCDIJaxClient");

        //Verify that CDI is disabled.
        try {
            CDI.current().getBeanManager();
            fail("We found a bean manager. CDI is enabled");
        } catch (Exception e) {
            //ignore
        }

        String url = request.getRequestURL().toString();
        url = url.replace("ClientTriggeringServlet", "ClientInvokedServlet"); //The jaxrsclient will use the URL as given so it needs the final part to be provided.

        String result = client.target(url).request(MediaType.TEXT_PLAIN).get(String.class);

        PrintWriter pw = response.getWriter();
        pw.write(result);
        pw.flush();
        pw.close();

        LOGGER.info("<<< testClientWithNoCDIJaxClient");
    }
}
