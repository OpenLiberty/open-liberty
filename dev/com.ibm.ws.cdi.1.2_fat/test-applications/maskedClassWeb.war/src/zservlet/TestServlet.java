/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package zservlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import test.TestBean;
import test.Type1;
import test.Type3;

@WebServlet("/TestServlet")
public class TestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Inject
    private Type3 type3Injected;

    /**
     * There are two implementations of TestBean, one in this war and another in maskedClassAppClient.jar.
     * <p>
     * The version in the app client jar should not be visible to the war so this should not cause an ambiguous bean exception.
     */
    @Inject
    private TestBean testBeanInjected;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public TestServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream os = response.getOutputStream();
        Type1 type1 = new Type1();
        os.println("Type1: " + type1.getMessage());
        os.println("Type3: " + type3Injected.getMessage());
        os.println("TestBean: " + testBeanInjected.getMessage());
    }

}
