/*******************************************************************************
 * Copyright (c) 2020,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.fat.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.tx.jta.ut.util.XAResourceImpl;

@WebServlet({ "/MultiRecoveryCheckServlet" })
public class MultiRecoveryCheckServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final int DIRECTION_COMMIT = 0,
    		DIRECTION_ROLLBACK = 1;

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {		
			int number = Integer.parseInt(request.getParameter("number").trim());
			System.out.println("==============MultiRecoveryCheckServlet Test Number: " + number
					+ "================");
			String output = "";
			int maxAttempts = 300;
			int attempt = 0;

			XAResourceImpl.printState();
			
			int	txCount = XAResourceImpl.transactionCount();

			while (txCount != 0 && attempt++ < maxAttempts) {
				try {
					System.out.println("Waiting for " + txCount + " transaction" + (txCount == 1 ? "" : "s") + " to finish.");
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				txCount = XAResourceImpl.transactionCount();
			}
			
			if (0 != XAResourceImpl.transactionCount()) {
				System.out.println("Transactions are still running. This test has probably failed.");
			}

			switch (number) {
			case 110102:
				if (XAResourceImpl.resourceCount() != 3) {
					System.out.println("!!!!!!!!!!!" + 
							"XARerouce number is not as expected."
							+ " Exoepted 3 but get " + 
							XAResourceImpl.resourceCount()
							+ " !!!!!!!!!!!!!");
				}
				output = XAResourceImpl.checkAtomicity();
				break;

			
			
			case 10101:
			case 10102:
			case 10201:
			case 10202:
			case 10301:
			case 10302:
			case 20101:
			case 20102:
			case 20201:
			case 20202:
			case 20301:
			case 20302:
			case 30101:
			case 30102:
			case 30201:
			case 30202:
			case 30301:
			case 30302:
			case 40101:
			case 40102:
			case 40201:
			case 40202:
			case 40301:
			case 40302:
			case 50101:
			case 50102:
			case 50201:
			case 50202:
			case 50301:
			case 50302:
			case 60101:
			case 60102:
			case 60201:
			case 60202:
			case 60301:
			case 60302:
			case 70101:
			case 70102:
			case 70201:
			case 70202:
			case 70301:
			case 70302:
			case 80101:
			case 80102:
			case 80201:
			case 80202:
			case 80301:
			case 80302:
			case 90101:
			case 90102:
			case 90201:
			case 90202:
			case 90301:
			case 90302:
			case 100101:
			case 100102:
			case 100201:
			case 100202:
			case 100301:
			case 100302:
			case 110101:
			case 110201:
			case 110202:
			case 110301:
			case 110302:
			case 120101:
			case 120102:
			case 120201:
			case 120202:
			case 120301:
			case 120302:
			case 130102:
			case 130202:
			case 130302:
			case 140102:
			case 140202:
			case 140302:
			case 150102:
			case 150202:
			case 150302:
			case 160102:
			case 160202:
			case 160302:
			case 301102: //WSTXLPS301AFVT subordinate
			case 301202: //WSTXLPS301BFVT subordinate
			case 301302: //WSTXLPS301CFVT subordinate
			case 302102: //WSTXLPS302AFVT subordinate
			case 302202: //WSTXLPS302BFVT subordinate
			case 303102: //WSTXLPS303AFVT subordinate
			case 302302: //WSTXLPS302CFVT subordinate
//				if (XAResourceImpl.resourceCount() != 2) {
//					System.out.println("!!!!!!!!!!!" + 
//							"XARerouce number is not as expected."
//							+ " Expected Two but get " + 
//							XAResourceImpl.resourceCount()
//							+ " !!!!!!!!!!!!!");
//					}
//				if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
//					System.out.println("!!!!!!!!!!!" + 
//							"There are XAResources not recovered!!!!!!!!!!!!!");
//					}
				output = XAResourceImpl.checkAtomicity();
//				boolean allCommitted = XAResourceImpl.allInState(XAResourceImpl.COMMITTED);
//				boolean allRollback = XAResourceImpl.allInState(XAResourceImpl.ROLLEDBACK);
//				if (allCommitted == true)
//					output = "allCommitted";
//				else if (allRollback == true)
//					output = "allRollback";
//				else output = "Unatomic";
				break;
			case 301101://WSTXLPS301AFVT root
			case 301301://WSTXLPS301CFVT root
			case 302101://WSTXLPS302AFVT root
			case 302301://WSTXLPS302CFVT root
				//One Phase XAResource cannot recover, so XAResourceImpl.resourceCount() is 0?
				/*if (XAResourceImpl.resourceCount() == 1) {
					output += "Get expected one XAResource.";
				} else {
					output += "XAResource number is: " + XAResourceImpl.resourceCount();
				}*/
				if (XAResourceImpl.allInState(XAResourceImpl.STARTED)) {
					output += " The One Phase XAResource is in STARTED state.";
					}
				else {
					output += " XAResource state is not as expected.";
				}
				break;
//			case 301102:
//			case 302102:
//			case 303102:
//				if (XAResourceImpl.resourceCount() == 1) {
//					output += "Get expected one XAResource.";
//				} else {
//					output += "XAResource number is: " + XAResourceImpl.resourceCount();
//				}
//				if (XAResourceImpl.allInState(XAResourceImpl.ROLLEDBACK)) {
//					output += " The XAResource is in ROLLEDBACK state.";
//					}
//				else {
//					output += " XAResource state is not as expected.";
//				}
//				if (number == 303102){
//				int waitForRollback = 0;
//				while (waitForRollback <= 10) {
//					waitForRollback++;
//					output = "Wait " + waitForRollback * 15
//							+ " seconds and check again. ";
//					try {
//						Thread.sleep(waitForRollback * 1000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//					if (XAResourceImpl.resourceCount() == 1) {
//						output += "Get expected one XAResource.";
//					} else {
//						output += "XAResource number is: "
//								+ XAResourceImpl.resourceCount();
//					}
//					if (XAResourceImpl.allInState(XAResourceImpl.ROLLEDBACK)) {
//						output += " The XAResource is in ROLLEDBACK state.";
//					} else {
//						output += " XAResource state is not as expected.";
//					}
//					if (output.contains("The XAResource is in ROLLEDBACK state"))
//						break;
//				}
//				}
//				//XAResourceImpl.printState();
//				break;
			case 301201: //WSTXLPS301BFVT root
			case 302201: //WSTXLPS302BFVT root
			case 303101: //WSTXLPS303AFVT root
				if (XAResourceImpl.allInState(XAResourceImpl.ROLLEDBACK)) {
					output += " The One Phase XAResource is in ROLLEDBACK state.";
					}
				else {
					output += " XAResource state is not as expected.";
				}
				break;
//			case 301202:
//			case 302202:
//			case 301302:
//			case 302302:
//				if (XAResourceImpl.allInState(XAResourceImpl.ROLLEDBACK)) {
//					output += " The XAResource is in ROLLEDBACK state.";
//					}
//				else {
//					output += " XAResource state is not as expected.";
//				}
//				output += XAResourceImpl.checkAtomicity();
				//XAResourceImpl.printState();
//				break;
//				if (XAResourceImpl.resourceCount() != 1) {
//					output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
//							+ " resources";
//					}
//				if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
//					output = "Rec" + number + " failed";
//					}
//				break;
				
			}
			response.getWriter().println(
					"<html><header></header><body>" + output + "</body></html>");
			response.getWriter().flush();
		System.out.println("end dispatch: " + output);
		XAResourceImpl.clear();
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}
}
