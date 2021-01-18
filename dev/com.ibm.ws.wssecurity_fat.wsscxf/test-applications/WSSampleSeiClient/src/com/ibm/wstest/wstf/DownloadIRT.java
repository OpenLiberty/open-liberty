/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wstest.wstf;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class DownloadIRT
 */
public class DownloadIRT extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String SEARCH = "HOSTANDPORTANDCONTEXT";
    private static final String SCENARIO = "SCENARIO";
    private static final int BUFSIZE = 512;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public DownloadIRT() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * processRequest
     * Reads the specified file and changes SEARCH to the
     * machine and port we are running on.
     * 
     * @param req
     *                 - HttpServletRequest
     * @param resp
     *                 - HttpServletResponse
     *                 <br>This method will create an output stream with
     *                 text/xml ContentType and force it to be an attachment
     * @throws ServletException
     * @throws IOException
     */
    private void processRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        byte[] ibuffer = new byte[BUFSIZE];
        String idata = "";

        // get the passed parameter
        String filename = req.getParameter("filename");

        // Set the return value attributes
        resp.setContentType("text/xml");
        resp.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        // File ops
        ServletOutputStream ostream = resp.getOutputStream();
        String basepath = getServletConfig().getServletContext().getRealPath("/");
        System.out.println(">> path = " + basepath);
        FileInputStream istream = new FileInputStream(basepath + "/" + filename);

        // Read until no more data
        while (istream.available() > 0) {
            int read = istream.read(ibuffer);
            String iread = new String(ibuffer, 0, read);
            idata += iread;
        }

        // Get the machine port and name and context
        InetAddress addr = InetAddress.getLocalHost();
        String contextString = getServletContext().getContextPath();
        String repwith = addr.getCanonicalHostName() + ":" + req.getLocalPort() + contextString;
        System.out.println(">> Replacing '" + SEARCH + "' with " + repwith);

        // Modify and send it back
        idata = idata.replaceAll(SEARCH, repwith);
        idata = idata.replaceAll(SCENARIO, contextString.substring(1));
        ostream.write(idata.getBytes());
    }
}
