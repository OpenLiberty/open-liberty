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
package com.ibm.logs;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@WebServlet("/ExceptionURL")
public class ExceptionServlet extends HttpServlet {
    Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Exception exception = new IllegalArgumentException("bad");
        logger.log(Level.INFO, "exception message", exception);

        logger.logp(Level.INFO, this.getClass().getCanonicalName(), "doGet", "second exception message", exception);

        logger.logp(Level.INFO, this.getClass().getCanonicalName(), "doGet", exception, () -> "third exception message");

        res.getWriter().print(new Date());
    }
}