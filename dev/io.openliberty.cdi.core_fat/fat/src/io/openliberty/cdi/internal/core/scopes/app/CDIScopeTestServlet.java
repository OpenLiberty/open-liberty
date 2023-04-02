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
package io.openliberty.cdi.internal.core.scopes.app;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet must be called with the scope parameter set to one of three values:
 * <ul>
 * <li>{@code app} - to request the value from {@link AppScopedBean}
 * <li>{@code request} - to request the value from {@link RequestScopedBean}
 * <li>{@code dependent} - to request the value from two instances of {@link DependentScopedBean}
 * </ul>
 * <p>
 * It will return the result of calling getAndIncrement on the relevant bean injected at two separate points as a comma separated list.
 */
@WebServlet("/scope")
public class CDIScopeTestServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    @Inject
    private AppScopedBean appScoped1;

    @Inject
    private AppScopedBean appScoped2;

    @Inject
    private RequestScopedBean requestScoped1;

    @Inject
    private RequestScopedBean requestScoped2;

    @Inject
    private DependentScopedBean dependentScoped1;

    @Inject
    private DependentScopedBean dependentScoped2;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String result = getValue(req.getParameter("scope"));
        resp.getWriter().println(result);
    }

    private String getValue(String scope) {
        switch (scope) {
            case "app":
                return appScoped1.getAndIncrement() + "," + appScoped2.getAndIncrement();
            case "request":
                return requestScoped1.getAndIncrement() + "," + requestScoped2.getAndIncrement();
            case "dependent":
                return dependentScoped1.getAndIncrement() + "," + dependentScoped2.getAndIncrement();
            default:
                throw new IllegalArgumentException("Invalid request: " + scope);
        }
    }
}
