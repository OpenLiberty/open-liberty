/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.microprofile.health.file.healthcheck.app;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class HealthAppServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    static Logger logger = Logger.getLogger("HealthAppServlet");

    static boolean isLive = true;
    static boolean isReady = true;

    static long startTime;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public HealthAppServlet() {
        super();
    }

    @Override
    public void init() {
        startTime = System.currentTimeMillis();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String liveInput;
        if ((liveInput = request.getParameter("live")) != null) {
            liveInput = liveInput.trim();
            if (liveInput.equalsIgnoreCase("true")) {
                isLive = true;
            } else if (liveInput.equalsIgnoreCase("false")) {
                isLive = false;
            }
        }

        String readyInput;
        if ((readyInput = request.getParameter("ready")) != null) {
            readyInput = readyInput.trim();
            if (readyInput.equalsIgnoreCase("true")) {
                isReady = true;
            } else if (readyInput.equalsIgnoreCase("false")) {
                isReady = false;
            }
        }

        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().append("Served at: ").append(request.getContextPath()).append("\n");
        response.getWriter().println(String.format("Current status of flags: liveFlag[%b] readyFlag[%b]", isLive, isReady));
        logger.info(String.format("Current status of flags: liveFlag[%b] readyFlag[%b]", isLive, isReady));
        System.out.println((String.format("Current status of flags: liveFlag[%b] readyFlag[%b]", isLive, isReady)));
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        doGet(request, response);
    }

}
