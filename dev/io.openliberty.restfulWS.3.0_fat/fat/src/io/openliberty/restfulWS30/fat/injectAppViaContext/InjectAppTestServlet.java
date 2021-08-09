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
package io.openliberty.restfulWS30.fat.injectAppViaContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import jakarta.servlet.annotation.WebServlet;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/InjectAppTestServlet")
public class InjectAppTestServlet extends FATServlet {
    private final static String BASE_URI = "http://localhost:" + System.getProperty("bvt.prop.HTTP_default") + "/injectApp/rest/myresource";

    @Test
    public void testOnlyOneAppInjectedIntoMultipleResourceInstances() throws Exception {
        ClientBuilder builder = ClientBuilder.newBuilder();
        Client client = builder.build();
        try {
            WebTarget appIdTarget = client.target(BASE_URI).path("/appID");
            int appId1 = appIdTarget.request(MediaType.TEXT_PLAIN).get(Integer.class);
            int appId2 = appIdTarget.request(MediaType.TEXT_PLAIN).get(Integer.class);
            assertEquals("Expected same app instance to be injected into resource, but it was not", appId1, appId2);

            WebTarget resourceIdTarget = client.target(BASE_URI).path("/resourceID");
            int resourceId1 = resourceIdTarget.request(MediaType.TEXT_PLAIN).get(Integer.class);
            int resourceId2 = resourceIdTarget.request(MediaType.TEXT_PLAIN).get(Integer.class);
            assertFalse("Expected different app instance to be invoked, but same instance was invoked twice", resourceId1 == resourceId2);
        } finally {
            client.close();
        }
    }

    @Test
    public void testContextInjectionInAppOccursBeforePostConstruct() throws Exception {
        ClientBuilder builder = ClientBuilder.newBuilder();
        Client client = builder.build();
        try {
            WebTarget target = client.target(BASE_URI).path("/providersInjectedInAppBeforePostConstruct");
            boolean b = target.request(MediaType.TEXT_PLAIN).get(Boolean.class);
            assertTrue("Expected @Context injection of Providers to occur before @PostConstruct method invoked, but was not", b);
        } finally {
            client.close();
        }
    }

    @Test
    public void testContextInjectionOfAppOccursBeforePostConstructInResource() throws Exception {
        ClientBuilder builder = ClientBuilder.newBuilder();
        Client client = builder.build();
        try {
            WebTarget target = client.target(BASE_URI).path("/appInjectedInResourceBeforePostConstruct");
            boolean b = target.request(MediaType.TEXT_PLAIN).get(Boolean.class);
            assertTrue("Expected @Context injection of Application to occur before @PostConstruct method invoked in resource, but was not", b);
        } finally {
            client.close();
        }
    }
}
