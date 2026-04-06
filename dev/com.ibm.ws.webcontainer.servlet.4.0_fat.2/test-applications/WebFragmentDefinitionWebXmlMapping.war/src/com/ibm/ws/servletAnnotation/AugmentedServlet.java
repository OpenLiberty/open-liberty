/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.servletAnnotation;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@WebServlet(name="AugmentedServlet", urlPatterns={"/AugmentedServletIgnore"},
		initParams={@WebInitParam(name="ccc", value="333")}, asyncSupported=true)
public class AugmentedServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public AugmentedServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	ServletOutputStream os = response.getOutputStream();
    	os.println("AugmentedServlet successful");
    	os.println("isAsyncSupported->"+request.isAsyncSupported());
    	Enumeration<String> initParamNames = getInitParameterNames();
    	while (initParamNames.hasMoreElements()){
    		String initParamName = initParamNames.nextElement();
    		os.println(initParamName+","+getInitParameter(initParamName));
    	}
    	
    }

}
