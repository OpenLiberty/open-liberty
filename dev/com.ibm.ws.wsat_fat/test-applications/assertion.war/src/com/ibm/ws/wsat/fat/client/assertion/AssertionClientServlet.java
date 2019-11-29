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
package com.ibm.ws.wsat.fat.client.assertion;

import java.io.IOException;
import java.net.URL;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.UserTransaction;
import javax.xml.ws.BindingProvider;

import com.ibm.ws.wsat.ut.util.AbstractTestServlet;

@WebServlet({ "/AssertionClientServlet" })
public class AssertionClientServlet extends AbstractTestServlet {
	private static final long serialVersionUID = 1L;

	protected String get(HttpServletRequest request) throws ServletException, IOException {
		String output = "";
		String type = request.getParameter("testName");
		System.out.println("==============Test type: " + type
				+ "================");
		String BASE_URL = request.getParameter("baseurl");
		if (BASE_URL == null || BASE_URL.equals(""))
			BASE_URL = "http://localhost:8010";
		Hello proxy;
		String endpointAddress = "";
		if (type.contains("NoTrans")) {
			if (type.equals("testAssertionOptionalNoTransaction")) {
				URL wsdlLocation = new URL(BASE_URL
						+ "/assertion/HelloImplAssertionOptionalService?wsdl");
				HelloImplAssertionOptionalService service = new HelloImplAssertionOptionalService(
						wsdlLocation);
				endpointAddress = "HelloImplAssertionOptionalService";
				proxy = service.getHelloImplAssertionOptionalPort();
			} else if (type.equals("testNoPolicyAssertionNoTransaction")) {
				URL wsdlLocation = new URL(BASE_URL
						+ "/assertion/HelloImplNoAssertionService?wsdl");
				HelloImplNoAssertionService service = new HelloImplNoAssertionService(
						wsdlLocation);
				endpointAddress = "HelloImplNoAssertionService";
				proxy = service.getHelloImplNoAssertionPort();
			} else {
				return "<html><header></header><body>Test type error.</body></html>";
			}
			BindingProvider bind = (BindingProvider) proxy;
			bind.getRequestContext().put(
					"javax.xml.ws.service.endpoint.address",
					BASE_URL + "/assertion/" + endpointAddress);
			System.out.println("Reply from server: " + proxy.sayHello());
			output = "<html><header></header><body> Reply from server: "
							+ proxy.sayHello() + "</body></html>";
		} else {
			try {
				System.out.println("begin try-catch");
				Context ctx = new InitialContext();
				UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");

				userTransaction.begin();
				if (type.equals("testNoPolicyAssertion")) {
					URL wsdlLocation = new URL(BASE_URL
							+ "/assertion/HelloImplNoAssertionService?wsdl");
					HelloImplNoAssertionService service = new HelloImplNoAssertionService(
							wsdlLocation);
					endpointAddress = "HelloImplNoAssertionService";
					proxy = service.getHelloImplNoAssertionPort();
				} else if (type.equals("testAssertionOptional")) {
					URL wsdlLocation = new URL(
							BASE_URL
									+ "/assertion/HelloImplAssertionOptionalService?wsdl");
					HelloImplAssertionOptionalService service = new HelloImplAssertionOptionalService(
							wsdlLocation);
					proxy = service.getHelloImplAssertionOptionalPort();
					endpointAddress = "HelloImplAssertionOptionalService";
				} else if (type.equals("testAssertionIgnorable")) {
					URL wsdlLocation = new URL(
							BASE_URL
									+ "/assertion/HelloImplAssertionIngorableService?wsdl");
					HelloImplAssertionIngorableService service = new HelloImplAssertionIngorableService(
							wsdlLocation);
					proxy = service.getHelloImplAssertionIngorablePort();
					endpointAddress = "HelloImplAssertionIngorableService";
				} else {
					return "<html><header></header><body>Test type error.</body></html>";
				}
				BindingProvider bind = (BindingProvider) proxy;
				bind.getRequestContext().put(
						"javax.xml.ws.service.endpoint.address",
						BASE_URL + "/assertion/" + endpointAddress);
				System.out.println("Reply from server: " + proxy.sayHello());
				System.out.println("client user transaction commit");
				output = "<html><header></header><body> Reply from server: "
								+ proxy.sayHello() + "</body></html>";

				userTransaction.commit();
			} catch (Exception e) {
				output = "<html><header></header><body> Client catch exception: "
								+ e.toString() + "</body></html>";
				e.printStackTrace();
			}
		}

		System.out.println("end doGet");
		return output;
	}
}
