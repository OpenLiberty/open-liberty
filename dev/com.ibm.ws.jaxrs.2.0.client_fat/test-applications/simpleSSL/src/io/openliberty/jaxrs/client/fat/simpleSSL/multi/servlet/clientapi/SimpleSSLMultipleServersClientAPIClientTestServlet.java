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
package io.openliberty.jaxrs.client.fat.simpleSSL.multi.servlet.clientapi;


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
import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import junit.framework.Assert;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SimpleSSLMultipleServersClientAPIClientTestServlet")
public class SimpleSSLMultipleServersClientAPIClientTestServlet extends FATServlet {

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
    
    /* 
     * Request should succeed because the Client API has the correct keystore/truststore to handshake with target server.
     * Client API should take priority over Liberty's default SSL config when the custom property is set.
     * 
     * -Dio.openliberty.restfulws.prioritizeClientBuilderSSLConfig=true
     * 
     */
    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, "JAXRS-2.1", SkipForRepeat.EE11_FEATURES }) 
    @AllowedFFDC({"com.ibm.websphere.ssl.SSLException", "java.security.PrivilegedActionException"})
    public void testSimpleSSLRequestWithClientAPISystemProperty() throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {
        
        
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
    
}