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
package jaxrs21.fat.contextandclient;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ContextAndClientTestServlet")
public class ContextAndClientTestServlet extends FATServlet {

    private static final int HTTP_PORT = Integer.getInteger("bvt.prop.HTTP_default", 8010);
    private Client client;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newBuilder().build();
    }

    @Override
    public void destroy() {
        client.close();
    }

    @Test
    public void testInjectedContextWorksAfterClientInvocation(HttpServletRequest req, HttpServletResponse resp) throws Exception {
         String response = invoke(req, "contextandclient/resource/invokeClient");
         assertTrue("Expected response: " + response, response.equals("PASS"));
    }

    private String invoke(HttpServletRequest request, String path) {

        String base = "http://" + request.getServerName() + ':' + HTTP_PORT + '/';
        WebTarget target = client.target(base + path);
        CompletableFuture<String> completableFuture = target.request().rx().get(String.class).toCompletableFuture();
        String response = null;
        try {
            response = completableFuture.get().trim();            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println(this.getClass() + " Response = " + response);
        return response;
    }

}