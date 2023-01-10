/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package jaxrs21.fat.acceptlanguage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/CDITestServlet")
public class AcceptLanguageTestServlet extends FATServlet {
    private final static String FROM_HTTP_HEADERS = "fromHttpHeaders";
    private final static String FROM_REQUEST_FILTER = "fromContainerRequestFilter";
    private final static int HTTP_PORT = Integer.getInteger("bvt.prop.HTTP_default", 8010);

    
    @Test
    public void testUnorderedButQualifiedList_httpHeaders() throws Exception {
        assertThatURI(FROM_HTTP_HEADERS)
            .withAcceptLanguageHeader("en-US;q=0.5, de;q=0.4, en;q=0.6, fr-CA;q=0.9, en-CA;q=0.7")
            .returns("fr-CA,en-CA,en,en-US,de");
    }
    @Test
    public void testUnorderedButQualifiedList_filterContext() throws Exception {
        assertThatURI(FROM_HTTP_HEADERS)
            .withAcceptLanguageHeader("en-US;q=0.5, de;q=0.4, en;q=0.6, fr-CA;q=0.9, en-CA;q=0.7")
            .returns("fr-CA,en-CA,en,en-US,de");
    }

    @Test
    public void testEmptyList_httpHeaders() throws Exception {
        assertThatURI(FROM_HTTP_HEADERS)
            .withAcceptLanguageHeader("")
            .returns("und"); 
        // per spec, should return Locale with * language (undefined) if unable to find valid language from header
    }
    @Test
    public void testEmptyList_filterContext() throws Exception {
        assertThatURI(FROM_REQUEST_FILTER)
            .withAcceptLanguageHeader("")
            .returns("und");
        // per spec, should return Locale with * language (undefined) if unable to find valid language from header
    }

    @Test
    public void testListWithEmptyEntry_httpHeaders() throws Exception {
        assertThatURI(FROM_HTTP_HEADERS)
            .withAcceptLanguageHeader("en;q=0.9, , es;q=0.7, fr;q=0.6")
            .returns("und,en,es,fr");
    }
    @Test
    public void testListWithEmptyEntry_filterContext() throws Exception {
        assertThatURI(FROM_REQUEST_FILTER)
            .withAcceptLanguageHeader("zh;q=0.7, jp;q=0.8, , ro;q=0.9, , en;q=0.1")
            .returns("und,und,ro,jp,zh,en");
    }

    @Test
    public void testListWithClearlyMadeUpEntries_httpHeaders() throws Exception {
        assertThatURI(FROM_HTTP_HEADERS)
            .withAcceptLanguageHeader("blah, boo, js, ewok")
            .returns("blah,boo,js,ewok");
    }
    @Test
    public void testListWithClearlyMadeUpEntries_filterContext() throws Exception {
        assertThatURI(FROM_REQUEST_FILTER)
            .withAcceptLanguageHeader("blah, boo, js, ewok")
            .returns("blah,boo,js,ewok");
    }

    private static Tester assertThatURI(String path) throws Exception {
        return new Tester(path);
    }
    
    private static class Tester {
        private String fullUri;
        private String returnedValue;
        
        Tester(String path) {
            fullUri = "http://localhost:" + HTTP_PORT + "/acceptlanguage/app/resource/" + path;
        }
        Tester withAcceptLanguageHeader(String headerValue) {
            Client c = ClientBuilder.newClient();
            try {
                returnedValue = c.target(fullUri).request()
                                 .header(HttpHeaders.ACCEPT_LANGUAGE, headerValue)
                                 .get(String.class);
            } catch (Throwable t) {
                t.printStackTrace();
                returnedValue = t.toString();
            } finally {
                c.close();
            }
            return this;
        }
        void returns(String expected) {
            assertEquals(expected, returnedValue);
        }
    }
}
