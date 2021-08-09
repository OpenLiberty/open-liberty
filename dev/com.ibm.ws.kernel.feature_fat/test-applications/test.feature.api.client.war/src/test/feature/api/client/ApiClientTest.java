/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.feature.api.client;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/apiClient")
public class ApiClientTest extends HttpServlet {
    /**  */
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Class<?> someAPIClass = Class.forName("test.feature.api.SomeAPI");
            response.getOutputStream().println(getClass().getSimpleName() + ":" + someAPIClass.newInstance().toString());
        } catch (ClassNotFoundException e) {
            response.getOutputStream().println("FAILED");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

}
