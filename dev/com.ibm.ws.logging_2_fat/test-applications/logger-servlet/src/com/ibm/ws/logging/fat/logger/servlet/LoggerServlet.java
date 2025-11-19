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
import java.util.logging.Logger;
import java.io.PrintWriter;

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
                int numMessages = 6;
                    
                String numMessagesString = request.getParameter("numMessages");
                try {
                    if (numMessagesString != null) {
                        numMessages = Integer.valueOf(numMessagesString);
                    }
                } catch (NumberFormatException e) {
                }
        
                for(int i = 0; i < numMessages; i++) {
                    logger.warning(warningMessage);
                    logger.warning(warningMessage2 + " -- " + i);
                }

              PrintWriter pw = response.getWriter();
              pw.print("Printed message to logs: " + warningMessage);
            }
}
