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
package com.ibm.ws.wsat.fat.client.recovery;

import java.io.IOException;
import java.net.URL;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;
import javax.xml.ws.BindingProvider;

import com.ibm.tx.jta.ut.util.XAResourceImpl;

@WebServlet({ "/RecoverySetupServlet" })
public class RecoverySetupServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final int DIRECTION_COMMIT = 0,
    		DIRECTION_ROLLBACK = 1;

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		    XAResourceImpl.clear();
			int number = Integer.parseInt(request.getParameter("number").trim());
			System.out.println("==============RecoverySetupServlet Test Number: " + number
					+ "================");
			String BASE_URL = request.getParameter("baseurl");
			if (BASE_URL == null || BASE_URL.equals("")){
				BASE_URL = "http://localhost:9992";
			}
			String output = "";
			URL wsdlLocation = new URL(BASE_URL
					+ "/recoveryServer/RecoveryService?wsdl");
			RecoveryService service = new RecoveryService(wsdlLocation);
			Recovery proxy = service.getRecoveryPort();
			BindingProvider bind = (BindingProvider) proxy;
			bind.getRequestContext().put("javax.xml.ws.service.endpoint.address",
					BASE_URL + "/recoveryServer/RecoveryService");
			switch (number) {
			case 1:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.invoke(number,"");
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					//CommitSuicide
					output += " Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace();
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			case 11:
			case 12:
			case 13:
			case 14:
			case 15:
			case 16:
			case 17:
			case 18:
			case 37:
			case 38:
			case 39:
			case 40:
			case 41:
			case 42:
			case 43:
			case 44:
			case 45:
			case 46:
			case 47:
			case 48:
			case 49:
				try{
					Context ctx = new InitialContext();
					String logKeyword = "Jordan said in setupServlet: ";
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					System.out.println(logKeyword + "userTransaction begin start");
					userTransaction.begin();
					System.out.println(logKeyword + "userTransaction begin end");
					System.out.println(logKeyword + "web service invoke start");
					output = proxy.invoke(number,"");
					System.out.println("get web service result: " + output);
					System.out.println(logKeyword + "web service invoke end");
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					System.out.println(logKeyword + "userTransaction commit start");
					userTransaction.commit();//commit
					System.out.println(logKeyword + "userTransaction commit end");
					output += " Test passed.";
					System.out.println("get final result: " + output);
				}catch(java.lang.Exception e){
					e.printStackTrace();
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 9:
			case 10:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.invoke(number,"");
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.rollback();//rollback
					output += " Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace();
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
				/*
				 * The following are multi server recovery tests
				 * */
			}
			response.getWriter().println(
					"<html><header></header><body>" + output + "</body></html>");
			response.getWriter().flush();
		System.out.println("end dispatch");
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}
}
