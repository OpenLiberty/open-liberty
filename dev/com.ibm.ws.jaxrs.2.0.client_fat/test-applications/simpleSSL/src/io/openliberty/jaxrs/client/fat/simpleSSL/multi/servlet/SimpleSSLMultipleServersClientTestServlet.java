/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package io.openliberty.jaxrs.client.fat.simpleSSL.multi.servlet;


import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;
import junit.framework.Assert;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SimpleSSLMultipleServersClientTestServlet")
public class SimpleSSLMultipleServersClientTestServlet extends FATServlet {

    private static final String SERVER_CONTEXT_ROOT = "https://localhost:" + Integer.getInteger("bvt.prop.HTTP_secondary.secure") + "/simpleSSL/";
    private static final String KEYSTORE = "resources/security/key.jks";
    private static final String TRUSTSTORE = "resources/security/trust.jks";

    private static Client client;

    @Override
    public void after() {
        if (client != null) {
            client.close();
        }       
    }

    @Test
    public void testSimpleSSLRequestToSecondServer() {
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
        client = cb.build();
        
        Response response = client.target(SERVER_CONTEXT_ROOT)
                        .path("echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello World!", response.readEntity(String.class));
    }
    
//    @Test
    public void testSimpleSSLRequestToSecondServerWebTarget() {
        client = ClientBuilder.newClient();
        Response response = client.target(SERVER_CONTEXT_ROOT)
                        .path("echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello World!", response.readEntity(String.class));
    }
    
    /*
     * https://jakarta.ee/specifications/restful-ws/3.1/apidocs/jakarta.ws.rs/jakarta/ws/rs/client/clientbuilder#trustStore(java.security.KeyStore)
     */
    @Test
    @AllowedFFDC({"com.ibm.websphere.ssl.SSLException", "java.security.PrivilegedActionException"})
    public void testSimpleSSLRequestWithClientAPITrustStore() throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {
        // configure a client with the ClientBuilder.trustStore() spec API
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(TRUSTSTORE)) {
            trustStore.load(fis, "passw0rd".toCharArray());
        }
        
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.trustStore(trustStore);
        client = cb.build();
        
        Response response = client.target(SERVER_CONTEXT_ROOT)
                        .path("echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello World!", response.readEntity(String.class));
    }
    
    @Test
    @AllowedFFDC({"com.ibm.websphere.ssl.SSLException", "java.security.PrivilegedActionException"})
    public void testSimpleSSLRequestWithClientAPIKeyStoreTrustStore() throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {
        // configure a client with the ClientBuilder.keyStore() and ClientBuilder.trustStore() spec APIs
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(KEYSTORE)) {
            keyStore.load(fis, "passw0rd".toCharArray());
        }
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (FileInputStream fis = new FileInputStream(TRUSTSTORE)) {
            trustStore.load(fis, "passw0rd".toCharArray());
        }
        
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.keyStore(keyStore, "passw0rd".toCharArray());
        cb.trustStore(trustStore);
        client = cb.build();
        
        Response response = client.target(SERVER_CONTEXT_ROOT)
                        .path("echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello World!", response.readEntity(String.class));
    }
    
    @Test
    @AllowedFFDC({"com.ibm.websphere.ssl.SSLException", "java.security.PrivilegedActionException"})
    public void testSimpleSSLRequestToSecondServerNoConfig() {
        try {
            ClientBuilder cb = ClientBuilder.newBuilder();
            client = cb.build();
            
            Response response = client.target(SERVER_CONTEXT_ROOT)
                            .path("echo")
                            .request(MediaType.TEXT_PLAIN_TYPE)
                            .get();
            Assert.fail();
        } catch (Exception e) {
            // expected, don't connect
        }
    }
    
}