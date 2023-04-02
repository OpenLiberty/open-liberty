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
package startupcode;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.openliberty.checkpoint.fat.SlowAppStartTest;

@WebServlet(urlPatterns = "/request", loadOnStartup = 1)
public class ServletA extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Override
    public void init() throws ServletException {
        try {
            System.out.println(SlowAppStartTest.TEST_INIT_SLEEPING);
            Thread.sleep(20000);
            System.out.println(SlowAppStartTest.TEST_INIT_DONE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServletException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.getOutputStream().println("TEST - Slow start servlet");
    }
}
