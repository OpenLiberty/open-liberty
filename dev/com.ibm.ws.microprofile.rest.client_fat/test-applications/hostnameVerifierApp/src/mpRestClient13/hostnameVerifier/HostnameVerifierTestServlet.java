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

package mpRestClient13.hostnameVerifier;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/HostnameVerifierTestServlet")
public class HostnameVerifierTestServlet extends FATServlet {

    private final static int HTTPS_PORT = Integer.getInteger("bvt.prop.HTTP_default.secure");
    private final static File trustStoreFile = new File(System.getProperty("server.config.dir"), "resources/security/trust.jks");

    private static KeyStore getKeyStore(File keystoreFile) throws Exception {
        KeyStore keystore = KeyStore.getInstance("jks");
        try (FileInputStream input = new FileInputStream(keystoreFile)) {
            keystore.load(input, "passw0rd".toCharArray());
        }
        return keystore;
    }
    @Test
    public void testHostnameVerifer(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "testHostnameVerifer";
        Client client = RestClientBuilder.newBuilder()
                                         .baseUrl(new URL("https://127.0.0.1:" + HTTPS_PORT + "/hostnameVerifierApp"))
                                         .hostnameVerifier(new MyHostnameVerifier())
                                         .trustStore(getKeyStore(trustStoreFile))
                                         .build(Client.class);
        
        assertEquals("you made it here!", client.attemptAccessDespiteWrongHostname());
        assertEquals(1, MyHostnameVerifier.INVOCATION_COUNT.get());
    }
}