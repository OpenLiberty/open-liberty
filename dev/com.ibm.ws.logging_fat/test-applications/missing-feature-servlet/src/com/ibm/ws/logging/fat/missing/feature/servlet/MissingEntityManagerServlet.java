/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.fat.missing.feature.servlet;

import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet with a dependency on JPA, running in a server with no jpa feature loaded. What could
 * possibly go wrong?
 */
@WebServlet("/MissingEntityManagerServlet")
public class MissingEntityManagerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @PersistenceContext(unitName = "thiswontworkpu")
    private EntityManager em;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public MissingEntityManagerServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request,
                            HttpServletResponse response) throws ServletException, IOException {
        response.getWriter().println("About to try and run a query on the entity manager we don't have.");
        String query = "SELECT f FROM ChocolateOrder f";
        Query q = em.createQuery(query);

        List<?> list = q.getResultList();
        response.getWriter().println("How did that work out?" + list);

    }

}
