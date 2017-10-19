/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.providerPriority;

import static org.junit.Assert.assertEquals;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ProviderPriorityTestServlet")
public class ProviderPriorityTestServlet extends FATServlet {

    private Client client;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newBuilder().build().register(ClientSideMBRW.class);
    }

    @Override
    public void destroy() {
        client.close();
    }

    @Test
    public void testProviderPriorities(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        MyObject requestObject = new MyObject();
        requestObject.setMyString("hello");
        requestObject.setMyInt(5);
        Response response = target(req, "providerPriorityApp/rest/test")
                        .request(MediaType.TEXT_PLAIN)
                        .put(Entity.text(requestObject));
        response.bufferEntity();
        System.out.println("testProviderPriorities responseEntity = " + response.readEntity(String.class));
        MyObject responseObject = response.readEntity(MyObject.class);

        // check that the response object was not altered during request
        assertEquals("Response object contains unexpected values",
                     requestObject.getMyString(), responseObject.getMyString());
        assertEquals("Response object contains unexpected values",
                     requestObject.getMyInt(), responseObject.getMyInt());

        // check that the expected providers processed the response object
        assertEquals("The higher priority MessageBodyReader was not selected", 2,
                     responseObject.getMbrVersion());
        assertEquals("The higher priority ContextResolver in the MessageBodyReader was not selected",
                     2, responseObject.getContextResolverVersionFromReader());
        assertEquals("The higher priority MessageBodyWriter was not selected", 2,
                     responseObject.getMbwVersion());
        assertEquals("The higher priority ContextResolver in the MessageBodyWriter was not selected",
                     2, responseObject.getContextResolverVersionFromWriter());

    }

/*
 * @Test
 * public void testSubResourcePatchOptions(HttpServletRequest req, HttpServletResponse resp) throws Exception {
 * SubResource.resources.put(1, new SubResource("resource 1 data"));
 *
 * String allowedHeaders = target(req, "patchapp/rest/test/SubResource/1")
 * .request()
 * .options()
 * .getHeaderString("Allow");
 * System.out.println("Allowed headers of subresource are: " + allowedHeaders);
 * assertTrue(allowedHeaders, allowedHeaders.contains("PATCH"));
 * }
 *
 * @Test
 * public void testPatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
 * // TODO use the `patch(...)` isntead if the JAXRS 2.1 EG includes it in spec
 * // otherwise, continue to use `method("PATCH", ...)`
 * String result = target(req, "patchapp/rest/test")
 * .request()
 * .method("PATCH", String.class);
 * assertEquals("patch-success", result);
 * }
 *
 * @Test
 * public void testSubResourcePatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
 * SubResource.resources.put(2, new SubResource("resource 2 data"));
 *
 * // TODO use the `patch(...)` isntead if the JAXRS 2.1 EG includes it in spec
 * // otherwise, continue to use `method("PATCH", ...)`
 * String result = target(req, "patchapp/rest/test/SubResource/2")
 * .request()
 * .method("PATCH", String.class);
 * assertEquals("PATCH: resource 2 data", result);
 * }
 */
    private WebTarget target(HttpServletRequest request, String path) {
        String base = "http://" + request.getServerName() + ':' + request.getServerPort() + '/';
        return client.target(base + path);
    }

}