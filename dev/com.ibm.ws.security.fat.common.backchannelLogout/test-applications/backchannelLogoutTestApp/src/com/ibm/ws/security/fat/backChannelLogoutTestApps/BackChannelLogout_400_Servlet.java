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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.backChannelLogoutTestApps;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BackChannelLogout_400_Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    int count = 0;
    PrintWriter writer = null;
    private final Lock lock = new ReentrantLock(true);

    public BackChannelLogout_400_Servlet() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Resetting 400 servlet counter");
        count = 0;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handleRequest(resp);
    }

    private void handleRequest(HttpServletResponse resp) throws ServletException, IOException {
        lock.lock();
        try {
            writer = resp.getWriter();

            count = count + 1;

            System.out.println("BackChannelLogout_400_Servlet - " + count + " returning status code of 400 ");
            writer.println("BackChannelLogout_400_Servlet - " + count + " returning status code of 400 ");

            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

        } catch (Exception e) {
            System.out.println("Post exception: " + e.getMessage());
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
            lock.unlock();
        }
    }
}
