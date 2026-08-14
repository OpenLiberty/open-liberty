/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class PushLargeMessageToEngine
 */

@WebServlet("/MessageURL")
public class MessageServlet extends HttpServlet {
    Logger logger = Logger.getLogger(this.getClass().getCanonicalName());
    String loggerName = "com.ibm.logs.MessageServlet";
    String logMessage = "Test Logstash Message Servlet";
    private Thread loggerThread;
    private volatile boolean running = false;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        if (loggerThread == null || !loggerThread.isAlive()) {
            running = true;
            loggerThread = new Thread(() -> {
                int counter = 0;
                logger.info("Test logger started");
                while (running) {
                    logger.log(Level.INFO, this.getClass().getCanonicalName(), "Counter: " + counter);

                    counter++;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        logger.warning("Logger thread interrupted: " + e.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                logger.info("Test logger stopped");
            });
            loggerThread.setDaemon(false);
            loggerThread.start();
        }
    }
}
