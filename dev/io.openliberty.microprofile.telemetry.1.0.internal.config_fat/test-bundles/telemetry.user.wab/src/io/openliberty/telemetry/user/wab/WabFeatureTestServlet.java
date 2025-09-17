/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.telemetry.user.wab;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import componenttest.app.FATServlet;

public class WabFeatureTestServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
        //we're just testing that a servlet inside a userfeature doesn't make the telemetry servlet filter crash
        //And we'll check if open telemetry is enabled or not based on wheather we're in app mode or not.
        System.out.println("Servlet inside user feature");
        sos.println("telemetry enabled: " + request.getAttribute("telemetryEnabled"));
    }
}
