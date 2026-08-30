/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package com.ibm.test.spring;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;

/**
 * This servlet exists in order to ensure that _something_ exists to be initialized in
 * the app so that the web container will invoke the ServletContextInitializer.
 */
@WebServlet(urlPatterns = { "/MyServlet" }, loadOnStartup = 0)
public class MyServlet extends HttpServlet {
    private static final long serialVersionUID = -5130625352489910031L;

    @Override
    public void init(ServletConfig cfg) {
        System.out.println("MyServlet - init");
    }
}
