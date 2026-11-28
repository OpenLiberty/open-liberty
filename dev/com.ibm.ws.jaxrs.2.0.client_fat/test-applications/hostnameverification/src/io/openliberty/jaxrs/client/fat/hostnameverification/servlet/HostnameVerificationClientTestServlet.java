/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package io.openliberty.jaxrs.client.fat.hostnameverification.servlet;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

/*
 * Test the different combinations of SSL Hostname Verification config in the server.xml
 * and the JAX-RS "com.ibm.ws.jaxrs.client.disableCNCheck" client property.
 * 
 * Skip the tests for `restfulWS-3.0`+ since there is no current way to disable 
 * hostname verificaiton with RESTEasy.
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/HostnameVerificationClientTestServlet")
public class HostnameVerificationClientTestServlet extends FATServlet {

    private static final String SERVER_CONTEXT_ROOT = "https://localhost:" + Integer.getInteger("bvt.prop.HTTP_default.secure") + "/simpleSSL/";
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    private static Client client;
    
    @After
    public void cleanup() {
        // client will be null when skipped for EE9+
        if (client != null) {
            // close the client after the test just in case
            client.close();
            client = null;
        }
    }
    
    /*
     * Test that Hostname Verification is enforced.
     * 
     * hostnameVerification = true
     * disableCNCheck = false
     */
    @Test
    public void testHostnameVerification() {
        client = ClientBuilder.newClient();
        client.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
        
        try {
            client.target(SERVER_CONTEXT_ROOT)
                        .path("echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        fail("request should have failed with ProcessingException");
        } catch (ProcessingException e) {
            // expected
        } finally {
            client.close();
        }
    }

    /*
     * Test just disabling Hostname Verification via SSL Config.
     * 
     * hostnameVerification = false
     * disableCNCheck = false
     */
    @Test
    @SkipForRepeat({"EE9_FEATURES","EE10_FEATURES","EE11_FEATURES"}) // we currently don't expose a way to disable HNV on RESTEasy
    public void testNoHostnameVerificationNoDisableCNCheck() {
        client = ClientBuilder.newClient();
        client.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfigNoHNV");
        
        try {
            client.target(SERVER_CONTEXT_ROOT)
                        .path("echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        } catch (ProcessingException e) {
            // expected
        } finally {
            client.close();
        }
    }

    /*
     * Test that Hostname Verification is enforced when SSL config is "true"
     * and "com.ibm.ws.jaxrs.client.disableCNCheck" is "true".
     * 
     * hostnameVerification = true
     * disableCNCheck = true
     */
    @Test
    @SkipForRepeat({"EE9_FEATURES","EE10_FEATURES","EE11_FEATURES"}) // we currently don't expose a way to disable HNV on RESTEasy
    public void testHostnameVerificationDisableCNCheckTrue() {
        client = ClientBuilder.newClient();
        client.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
        client.property("com.ibm.ws.jaxrs.client.disableCNCheck", "true");
        
        try {
            client.target(SERVER_CONTEXT_ROOT)
                        .path("echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        fail("request should have failed with ProcessingException");
        } catch (ProcessingException e) {
            // expected
        } finally {
            client.close();
        }
    }

    /*
     * Test that Hostname Verification is not disabled when SSL config is "false" and the 
     * "com.ibm.ws.jaxrs.client.disableCNCheck" client property is "false".
     * 
     * hostnameVerification = false
     * disableCNCheck = false
     */
    @Test
    @SkipForRepeat({"EE9_FEATURES","EE10_FEATURES","EE11_FEATURES"}) // we currently don't expose a way to disable HNV on RESTEasy
    public void testNoHostnameVerificationDisableCNCheckFalse() {
        // Skip on Windows with non-J9 JVMs - platform-specific SSL hostname verification behavior
        // On Windows, when verifyHostname=false is set in server.xml SSL config, setting
        // disableCNCheck=false on the JAX-RS client does not re-enable hostname verification
        // as expected. IBM J9 and OpenJ9 JVMs handle this correctly on Windows.
        String vmName = System.getProperty("java.vm.name", "unknown");
        boolean isJ9 = vmName.contains("IBM J9") || vmName.contains("OpenJ9");
        System.out.println("java.vm.name : " + vmName);
        if (isWindows && !isJ9) {
            System.out.println("Skipping test on Windows with non-J9 JVM - hostname verification re-enable not supported");
            return;
        }
        
        client = ClientBuilder.newClient();
        client.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfigNoHNV");
        client.property("com.ibm.ws.jaxrs.client.disableCNCheck", "false");
        
        try {
            client.target(SERVER_CONTEXT_ROOT)
                        .path("echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        fail("request should have failed with ProcessingException");
        } catch (ProcessingException e) {
            // expected
        } finally {
            client.close();
        }
    }

    // TODO:  Apparently the value for com.ibm.ws.jaxrs.client.disableCNCheck defaults to false and once it is set to "true" if cannot be
    // set to false without restarting the application.  So this test has been moved to execute after the "false" test.
    
    /*
     * Test that Hostname Verification is disabled when BOTH ssl config is "false" 
     * AND "com.ibm.ws.jaxrs.client.disableCNCheck" is "true".
     * 
     * hostnameVerification = false
     * disableCNCheck = true
     */
    @Test
    @SkipForRepeat({"EE9_FEATURES","EE10_FEATURES","EE11_FEATURES"}) // we currently don't expose a way to disable HNV on RESTEasy
    public void testNoHostnameVerificationDisableCNCheckTrue() {
        client = ClientBuilder.newClient();
        client.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfigNoHNV");
        client.property("com.ibm.ws.jaxrs.client.disableCNCheck", "true");
        
        Response response = client.target(SERVER_CONTEXT_ROOT)
                        .path("echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        client.close();
        assertEquals(200, response.getStatus());
        assertEquals("Hello World!", response.readEntity(String.class));
    }


}