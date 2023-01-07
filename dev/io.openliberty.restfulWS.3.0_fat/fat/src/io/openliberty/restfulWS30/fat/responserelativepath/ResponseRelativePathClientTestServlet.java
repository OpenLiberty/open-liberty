/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.restfulWS30.fat.responserelativepath;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/ResponseRelativePathClientTestServlet")
public class ResponseRelativePathClientTestServlet extends FATServlet {

    @Test
    public void testResourceIsRequestScoped() throws Exception {
        URI uri = URI.create("http://localhost:" + System.getProperty("bvt.prop.HTTP_default") + "/responserelativepath/testRelativePath");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        assertEquals(200, conn.getResponseCode());
        String uriValue = readEntity(conn.getInputStream());
        // The URI in the response should be built on the baseuri and not the requesturi.  
        // "testRelativePath" is only in the requesturi
        assertTrue(uriValue.contains("/responserelativepath/"));       
        assertFalse(uriValue.contains("/responserelativepath/testRelativePath/"));       
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