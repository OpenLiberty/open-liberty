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
package io.openliberty.microprofile.telemetry.logging.internal.fat.ffdc.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

@WebServlet(urlPatterns = "/FFDCInInitServlet", loadOnStartup = 1)
public class FFDCInInitServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        boolean ffdcEarly = Boolean.getBoolean("io.openliberty.microprofile.telemetry.ffdc.early");
        if (ffdcEarly) {
            throw new RuntimeException("FFDC_TEST_INIT");
        }
    }
}
