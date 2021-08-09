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
package io.openliberty.restfulWS30.fat.validator;

import static org.junit.Assert.assertEquals;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import jakarta.servlet.annotation.WebServlet;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/ValidatorTestServlet")
public class ValidatorTestServlet extends FATServlet {
    
    @Test
    public void testMinValidation() throws Exception {       
        int zeroInt = 0;
        URI uri = URI.create("http://localhost:" + System.getProperty("bvt.prop.HTTP_default") + "/validator/app/path/" + zeroInt);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        assertEquals(400, conn.getResponseCode());
    }
    
    @Test
    public void testMinValidation2() throws Exception {       
        int oneInt = 1;
        URI uri = URI.create("http://localhost:" + System.getProperty("bvt.prop.HTTP_default") + "/validator/app/path/" + oneInt);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod("POST");
        assertEquals(200, conn.getResponseCode());        
        assertEquals("foo " + oneInt, readEntity(conn.getInputStream()));
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
