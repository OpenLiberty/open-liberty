/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.testhttpsessionattrlistener.war.sessionListener;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class TestAddListener
 */
@WebServlet("/TestAddListener")
public class TestAddListenerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public TestAddListenerServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doWork(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doWork(request, response);
    }

    private void doWork(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // Add one attribute in session

        HttpSession sess = request.getSession();
        sess.setAttribute("Att1", "ValueAtt1");
        sess.setAttribute("Att2", "ValeAtt2");

        // testcase requires invalidate to implicitly call removeAttributes , so we will not call them below.
        // remove attribute in session
//		sess.removeAttribute("Att2");
//		sess.removeAttribute("Att1");

        sess.invalidate();

        //sess.removeAttribute("Att2"); , this shud throw IllegalStateException if uncommented
        int i = CalculateListenerInvoke.getI();
        Collection<Object> values = CalculateListenerInvoke.getAttrValuesOnDestroy();
        String result = "TEST_FAILED: " + i + ": " + values;
        ServletOutputStream sos = response.getOutputStream();
        if (i == 0 && values.size() == 6 && values.containsAll(Arrays.asList("Att2", "Att2", "Att2", "Att1", "Att1", "Att1"))) {
            result = "TEST_PASSED";
        }
        System.out.println("i -->" + i + " , values size-->" + values.size());
        System.out.println("the result is " + result);
        sos.println("the result is " + result);

        CalculateListenerInvoke.reset();

    }

}
