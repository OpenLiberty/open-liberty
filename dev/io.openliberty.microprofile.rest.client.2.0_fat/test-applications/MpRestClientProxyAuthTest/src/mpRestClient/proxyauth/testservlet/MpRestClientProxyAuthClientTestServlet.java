/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpRestClient.proxyauth.testservlet;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;
import componenttest.app.FATServlet;
import mpRestClient.proxyauth.restclient.MpRestClientProxyAuthClient;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MpRestClientProxyAuthClientTestServlet")
public class MpRestClientProxyAuthClientTestServlet extends FATServlet {

    private static final String URI_CONTEXT_ROOT = "https://localhost:" + Integer.getInteger("bvt.prop.HTTP_default.secure") + "/MpRestClientProxyAuth/";

    @Test
    public void testProxyAuthHelloWorldProperty() throws MalformedURLException {
        RestClientBuilder builder = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig")
                        .property("com.ibm.ws.jaxrs.client.disableCNCheck", "true")
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.proxy.username", "mpRestClientUser")
                        .property("com.ibm.ws.jaxrs.client.proxy.password", "myPa$$word")
                        .property("com.ibm.ws.jaxrs.client.proxy.host", "localhost")
                        .property("com.ibm.ws.jaxrs.client.proxy.port", 8085)
                        .baseUrl(new URL(URI_CONTEXT_ROOT));

        MpRestClientProxyAuthClient client = builder.build(MpRestClientProxyAuthClient.class);
        String response = client.hello();
        assertEquals("Hello World!", response);
    }

    @Test
    public void testProxyAuthHelloWorldProxyAddress() throws MalformedURLException {

        RestClientBuilder builder = RestClientBuilder.newBuilder()
                        .proxyAddress("localhost", 8085)
                        .property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig")
                        .property("com.ibm.ws.jaxrs.client.disableCNCheck", "true")
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.proxy.username", "mpRestClientUser")
                        .property("com.ibm.ws.jaxrs.client.proxy.password", "myPa$$word")
                        .baseUrl(new URL(URI_CONTEXT_ROOT));

        MpRestClientProxyAuthClient client = builder.build(MpRestClientProxyAuthClient.class);
        String response = client.hello();
        assertEquals("Hello World!", response);
    }
}