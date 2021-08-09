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
package io.openliberty.restfulWS30.fat.cdiInjectIntoApp;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/CDIInjectIntoAppTestServlet")
public class CDIInjectIntoAppTestServlet extends FATServlet {

    @Test
    public void testCanInjectIntoAppAndAppIntoResource() throws Exception {
        assertEquals("SUCCESS", testResource("checkAppInjection"));
    }

    @Test
    public void testInjectedBeansHaveCorrectScopes() throws Exception {
        assertEquals("1 - 1", testResource("1"));
        assertEquals("2 - 2", testResource("1"));
        assertEquals("1 - 3", testResource("2"));
        assertEquals("3 - 4", testResource("1"));
        assertEquals("2 - 5", testResource("2"));
    }

    private String testResource(String resourcePath) throws Exception {
        URI uri = URI.create("http://localhost:" + System.getProperty("bvt.prop.HTTP_default") + "/cdiinjectintoapp/app/resource/" + resourcePath);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        assertEquals(200, conn.getResponseCode());
        return readEntity(conn.getInputStream());
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
