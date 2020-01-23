/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.web;

import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class AuthorizationServlet
 */
@WebServlet("/acme-challenge/AcmeAuthorization")
public class AcmeAuthorization extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public AcmeAuthorization() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see Servlet#getServletConfig()
     */
    public ServletConfig getServletConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see Servlet#getServletInfo()
     */
    public String getServletInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        System.out.println("******** AcmeAuthoriation: Entered doGet() in AuthorizationServlet ...");
        String keyFileName = request.getQueryString();
        System.out.println("Keyfile name passed = " + keyFileName);

        String current = new java.io.File(".").getCanonicalPath();
        System.out.println("******** AcmeAuthoriation: Current dir:" + current);

        Scanner in = new Scanner(new FileReader(keyFileName));
        StringBuilder sb = new StringBuilder();
        while (in.hasNext()) {
            sb.append(in.nextLine());
        }
        in.close();
        String outString = sb.toString();
        System.out.println("******** AcmeAuthoriation: Return contents of file as a string: " + outString);
        response.resetBuffer();
        response.getWriter().write(outString);
        response.getWriter().close();
        response.getWriter().flush();

        String keyFileNamePath = current + "/" + keyFileName;
        System.out.println("******** AcmeAuthoriation: Keyfile name path: " + keyFileNamePath);
        // File file = new File(keyFileNamePath);

        // if(file.delete())
        // {
        //    System.out.println("******** AcmeAuthoriation: Key File deleted successfully.");
        // }
        // else
        // {
        //    System.out.println("******** AcmeAuthoriation: Failed to delete the key file!");
        // }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
        doGet(request, response);
    }

    /**
     * @see HttpServlet#doHead(HttpServletRequest, HttpServletResponse)
     */
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
    }

    /**
     * @see HttpServlet#doOptions(HttpServletRequest, HttpServletResponse)
     */
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // TODO Auto-generated method stub
    }

}
