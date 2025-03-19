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
package io.openliberty.commons.logging.test.app;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.commons.logging.test.bundle.DoTest;

@SuppressWarnings("serial")
@WebServlet("/TestCommonsLogging")
public class TestCommonsLogging extends FATServlet {

    @Test
    public void testCommonsLoggingTCCL(HttpServletRequest request, HttpServletResponse resp) throws Exception {
        resp.getWriter().println("Running test method 'testCommonsLoggingTCCL'");
        DoTest.useCommonsLogging();
    }
}
