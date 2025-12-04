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
package com.ibm.ws.logging.fat.logger.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
@SuppressWarnings("serial")
public class LoggerServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(LoggerServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String warningMessage = "TESTA0001W: Warning log test";
        String warningMessage2 = "TESTA0002W: Warning log test 2";
        String warningMessage3 = "TESTA0003W: Warning log test 3 - println";
        String warningMessage4 = "TESTA0004W: Warning log test 4 - print";
        String warningMessage5 = "TESTA0005W: Warning log test 5 - logger.log";
        String warningMessage6 = "TESTA0006W: Warning log test 6 - system.err.println";
        String warningMessage7 = "TESTA0007W: Warning log test 7 - system.err.println";

        int numMessages = 6;

        String numMessagesString = request.getParameter("numMessages");
        try {
            if (numMessagesString != null) {
                numMessages = Integer.valueOf(numMessagesString);
            }
        } catch (NumberFormatException e) {
        }

        for (int i = 0; i < numMessages; i++) {
            logger.warning(warningMessage);
            logger.warning(warningMessage2 + " -- " + i);
            System.out.println(warningMessage3);
            System.out.print(warningMessage4);
            logger.log(Level.INFO, warningMessage5);
            System.err.println(warningMessage6);
            System.err.print(warningMessage7);

        }

        PrintWriter pw = response.getWriter();
        pw.print("Printed message to logs: " + warningMessage);
    }
}
