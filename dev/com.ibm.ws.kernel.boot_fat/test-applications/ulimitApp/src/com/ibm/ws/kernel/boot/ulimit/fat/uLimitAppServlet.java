/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.ulimit.fat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
@SuppressWarnings("serial")
public class uLimitAppServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String input = request.getParameter("exit");
        StringBuffer sb = new StringBuffer();
       
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", "ulimit -n" });
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;

            while ((line = in.readLine()) != null) {
                sb.append("ulimit data = " + line);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.getOutputStream().println(sb.toString());
    }
}
