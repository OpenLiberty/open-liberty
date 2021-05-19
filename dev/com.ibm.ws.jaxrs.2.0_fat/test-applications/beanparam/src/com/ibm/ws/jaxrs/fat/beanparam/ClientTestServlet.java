/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.beanparam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/ClientTestServlet")
public class ClientTestServlet extends FATServlet {

    private static final long serialVersionUID = 4563445389586844836L;

    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/beanparam/";

    private Client client;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @After
    private void teardown() {
        client.close();
    }

    @Test
    public void testFormParamOnField() throws Exception {

        Form form = new Form();
        form.param("form", "FIRST");
        form.param("innerForm", "SECOND");

        String content = "content=&form=FIRST&innerForm=SECOND";

        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("formparam")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .post(Entity.entity(content, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertEquals(200, response.getStatus());
        String actual = response.readEntity(String.class);

        System.out.println("content=" + content);
        System.out.println("actual=" + actual);
        //RESTEasy can reposition the form parameters since it puts it into a map and then pulls it back out
        // so the comparison must be that all form fields are present, rather than in a particular order:
        //assertEquals(content + "&FIRST&SECOND", actual);
        assertTrue("Missing content parameter", actual.contains("content"));
        assertTrue("Missing first bean param form parameter", actual.contains("form=FIRST"));
        assertTrue("Missing inner bean form parameter", actual.contains("innerForm=SECOND"));
        //if working correctly, the resource method always puts this at the end though:
        assertTrue("Bean param processing failed", actual.endsWith("&FIRST&SECOND"));
    }

    @Test
    public void testCookieParam() throws Exception {
        String content = "Whatever";
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("cookieparam")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .cookie("cookie", "Chocolate Chip")
                        .cookie("innerCookie", "Snickerdoodle")
                        .post(Entity.entity(content, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        assertEquals("Whatever&Chocolate Chip&Snickerdoodle", response.readEntity(String.class));
    }
}