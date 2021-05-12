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
package io.openliberty.restfulWS30.fat.jsonb;

import static org.junit.Assert.assertEquals;

import jakarta.servlet.annotation.WebServlet;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/JsonbTestServlet")
public class JsonbTestServlet extends FATServlet {
    private final static String BASE_URI = "http://localhost:" + System.getProperty("bvt.prop.HTTP_default") + "/jsonb/app/widget";

    @Test
    public void testUseJsonBProviderToSendAndReceiveWidgetClass() throws Exception {
        ClientBuilder builder = ClientBuilder.newBuilder();
        Client client = builder.build();
        try {
            WebTarget baseTarget = client.target(BASE_URI);
            WebTarget target0 = baseTarget.path("/0");
            Widget widget0 = target0.request().get(Widget.class);
            assertEquals(App.WIDGETS[0], widget0);
            WebTarget target1 = baseTarget.path("/1");
            Widget widget1 = target1.request().get(Widget.class);
            assertEquals(App.WIDGETS[1], widget1);
        } finally {
            client.close();
        }
    }
}
