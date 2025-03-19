/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.http.monitor.fat.servletApp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Simple Servlet
 */
@WebServlet("/simpleServlet")
public class SimpleServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /** {@inheritDoc} */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().append("Hello, DELETEing!");
    }

    /** {@inheritDoc} */
    @Override
    protected void doHead(HttpServletRequest arg0, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().append("Hello, I'm a HEAD!");
    }

    /** {@inheritDoc} */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().append("Hello, I'm GETting it!");
    }

    /** {@inheritDoc} */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().append("Hello, I'm POSting it!");
    }

    /** {@inheritDoc} */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().append("Hello, I'm PUTTING it!");
    }

    /** {@inheritDoc} */
    @Override
    protected void doOptions(HttpServletRequest arg0, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().append("Hello, I'm OPTIONS-ing it!");
    }

}
