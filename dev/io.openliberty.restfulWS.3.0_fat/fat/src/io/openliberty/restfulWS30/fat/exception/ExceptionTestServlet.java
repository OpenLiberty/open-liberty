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
package io.openliberty.restfulWS30.fat.exception;

import static org.junit.Assert.assertEquals;

import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/ExceptionTestServlet")
public class ExceptionTestServlet extends FATServlet {

    @Test
    public void testThrowsEJBException() throws Exception {
        URI uri = URI.create("http://localhost:" + System.getProperty("bvt.prop.HTTP_default") + "/exception/app/path/exception");
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        assertEquals(201, conn.getResponseCode());
    }
}
