/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mpRestClient.fat.prototypeClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.rules.repeater.MicroProfileActions;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/BasicClientTestServlet")
public class BasicClientTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(BasicClientTestServlet.class.getName());

    private RestClientBuilder builder;


    static {
        //for localhost testing only
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
                new javax.net.ssl.HostnameVerifier() {

            @Override
            public boolean verify(String hostname,
                    javax.net.ssl.SSLSession sslSession) {
                if (hostname.contains("localhost")) {
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void init() throws ServletException {
        String baseUrlStr = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_secondary") + "/prototype";
        LOG.info("baseUrl = " + baseUrlStr);
        URL baseUrl;
        try {
            baseUrl = new URL(baseUrlStr);
        } catch (MalformedURLException ex) {
            throw new ServletException(ex);
        }
        builder = RestClientBuilder.newBuilder()
                        .baseUrl(baseUrl);
    }

    @Test
    public void testSimpleGet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        BasicServiceClient client = builder.build(BasicServiceClient.class);
        String output = client.hello();
        assertEquals("Hello World!", output);
    }
    
}