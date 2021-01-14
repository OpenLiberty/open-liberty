/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.fat.managedbeans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import jakarta.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ManagedBeansTestServlet")
public class ManagedBeansTestServlet extends FATServlet {

    @Test
    public void testProviderInjectedIntoAppBeforePostConstruct() throws Exception {
        URI uri = URI.create("http://localhost:" + System.getProperty("bvt.prop.HTTP_default") + "/managedbeans/app/resource/priorapp");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        assertEquals(200, conn.getResponseCode());
        assertEquals("true", readEntity(conn.getInputStream()));
    }

    @Test
    public void testAppInjectedIntoResourceBeforePostConstruct() throws Exception {
        URI uri = URI.create("http://localhost:" + System.getProperty("bvt.prop.HTTP_default") + "/managedbeans/app/resource/priorresource");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        assertEquals(200, conn.getResponseCode());
        assertEquals("true", readEntity(conn.getInputStream()));
    }

    @Test
    public void testResourceIsRequestScoped() throws Exception {
        URI uri = URI.create("http://localhost:" + System.getProperty("bvt.prop.HTTP_default") + "/managedbeans/app/resource/id");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        assertEquals(200, conn.getResponseCode());
        String firstInstanceId = readEntity(conn.getInputStream());
        
        conn = (HttpURLConnection) uri.toURL().openConnection();
        assertEquals(200, conn.getResponseCode());
        String secondInstanceId = readEntity(conn.getInputStream());
        assertFalse(firstInstanceId.equals(secondInstanceId));
    }

    private String readEntity(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        byte[] b = new byte[256];
        int i = is.read(b);
        while (i > 0) {
            sb.append(new String(b, 0, i));
            i = is.read(b);
        }
        return sb.toString().trim();
    }
}
