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
package com.ibm.ws.wsat.oneway.client.oneway;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.io.IOException;
import java.net.URL;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.UserTransaction;
import javax.xml.ws.BindingProvider;

@WebServlet({ "/OnewayClientServlet" })
public class OnewayClientServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;


    private static String TEST_NAME_PARAM = "testName";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        System.out.println("Servlet: " + request.getRequestURI());
        System.out.println("Test: " + request.getParameter(TEST_NAME_PARAM));

        final Enumeration<?> params = request.getParameterNames();

        while (params.hasMoreElements()) {
            final String param = (String) params.nextElement();

            if (!TEST_NAME_PARAM.equals(param)) {
                System.out.println(param + ": " + request.getParameter(param));
            }
        }

        final String result = get(request);
        
        response.getWriter().println(result);
    }

	protected String get(HttpServletRequest request) throws ServletException, IOException {
		try {
			System.out.println("begin try-catch");
			Context ctx = new InitialContext();
			UserTransaction userTransaction = (UserTransaction) ctx
					.lookup("java:comp/UserTransaction");

			userTransaction.begin();

			String type = request.getParameter(TEST_NAME_PARAM);
			System.out.println("==============Test type: " + type
					+ "================");
			String BASE_URL = request.getParameter("baseurl");
			if (BASE_URL == null || BASE_URL.equals(""))
				BASE_URL = "http://localhost:8010";
				URL wsdlLocation = new URL(BASE_URL
						+ "/oneway/HelloImplOnewayService?wsdl");
				HelloImplOnewayService service = new HelloImplOnewayService(
						wsdlLocation);
				HelloImplOneway proxy = service.getHelloImplOnewayPort();
				BindingProvider bind = (BindingProvider) proxy;
				bind.getRequestContext().put(
						"javax.xml.ws.service.endpoint.address",
						BASE_URL + "/oneway/HelloImplOnewayService");
				proxy.sayHello();
				userTransaction.commit();
				System.out.println("client user transaction commit");
				return "<html><header></header><body>Finish Oneway message</body></html>";
		} catch (Exception e) {
			e.printStackTrace();
			return "<html><header></header><body> Client catch exception: "
							+ e.toString() + "</body></html>";
		} finally {
			System.out.println("end dispatch");
		}
	}
}
