/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package client;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.tx.jta.ut.util.XAResourceImpl;

@WebServlet({ "/RecoveryCheckServlet" })
public class RecoveryCheckServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final int DIRECTION_COMMIT = 0,
			DIRECTION_ROLLBACK = 1;

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {		
		int number = Integer.parseInt(request.getParameter("number").trim());
		System.out.println("==============RecoveryCheckServlet Test Number: " + number
				+ "================");
		String BASE_URL = request.getParameter("baseurl");
		if (BASE_URL == null || BASE_URL.equals("")){
			BASE_URL = "http://localhost:"+Integer.parseInt(System.getProperty("HTTP_secondary"));
		}
		String output = "get resource states successfully";

		XAResourceImpl.printState();
		
		// There should be no transactions running on the participant at this point
		final int txCount = XAResourceImpl.transactionCount();

		if (txCount > 0) {
			throw new ServletException("There are " + txCount + " global transactions still running!");
		} else {
			System.out.println("There are " + txCount + " global transactions still running!");
		}

		if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
			output = "Rec" + number + " failed: resources are not all recovered";
		}

		switch (number) {
		case 1:
		case 3:
		case 9:
			if (XAResourceImpl.resourceCount() != 2) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=2";
			}
			if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
				output = "Rec" + number + " failed: all states are not RECOVERED";
			}
			break;
		case 2:
			if (XAResourceImpl.resourceCount() != 2) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=2";
			}
			if (!XAResourceImpl.getXAResourceImpl(1).inState(XAResourceImpl.ROLLEDBACK)) {
				output = "Rec" + number + " failed: XAResourceImpl 1 is not ROLLEDBACK";
			}
			break;
		case 4:
			if (XAResourceImpl.resourceCount() != 3) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=3";
			}
			if (!XAResourceImpl.getXAResourceImpl(2).inState(XAResourceImpl.ROLLEDBACK)) {
				output = "Rec" + number + " failed: XAResourceImpl 2 is not ROLLEDBACK";
			}
			break;
		case 5:
			if (XAResourceImpl.resourceCount() != 3) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=3";
			}
			if (!XAResourceImpl.getXAResourceImpl(1).inState(XAResourceImpl.ROLLEDBACK)) {
				output = "Rec" + number + " failed: XAResourceImpl 1 is not ROLLEDBACK";
			}
			break;
		case 6:
			if (XAResourceImpl.resourceCount() != 3) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=3";
			}
			if (!XAResourceImpl.allInState(XAResourceImpl.RECOVERED)) {
				output = "Rec" + number + " failed: all states are not RECOVERED";
			}
			break;
		case 7:
			if (XAResourceImpl.resourceCount() != 2) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=2";
			}
			if (!XAResourceImpl.getXAResourceImpl(1).inState(XAResourceImpl.COMMITTED)) {
				output = "Rec" + number + " failed: XAResourceImpl 1 is not COMMITTED";
			}
			break;
		case 8:
			if (XAResourceImpl.resourceCount() != 2) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=2";
			}
			if (!XAResourceImpl.getXAResourceImpl(0).inState(XAResourceImpl.COMMITTED)) {
				output = "Rec" + number + " failed: XAResourceImpl 0 is not COMMITTED";
			}
			break;
		case 10:
			if (XAResourceImpl.resourceCount() != 2) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=2";
			}
			if (!XAResourceImpl.getXAResourceImpl(0).inState(XAResourceImpl.ROLLEDBACK)) {
				output = "Rec" + number + " failed: XAResourceImpl 0 is not ROLLEDBACK";
			}
			break;
		case 11:
		case 12:
		case 13:
		case 14:
			if (XAResourceImpl.resourceCount() != 2) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=2";
			}
			if (!XAResourceImpl.getXAResourceImpl(0).inState(XAResourceImpl.COMMITTED)) {
				output = "Rec" + number + " failed: XAResourceImpl 0 is not COMMITTED";
			}
			if (!XAResourceImpl.getXAResourceImpl(1).inState(XAResourceImpl.FORGOTTEN)) {
				output = "Rec" + number + " failed: XAResourceImpl 1 is not FORGOTTEN";
			}
			break;
		case 15:
		case 16:
		case 17:
		case 18:
			if (XAResourceImpl.resourceCount() != 3) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=3";
			}
			if (!XAResourceImpl.getXAResourceImpl(1).inState(XAResourceImpl.ROLLEDBACK)) {
				output = "Rec" + number + " failed: XAResourceImpl 1 is not ROLLEDBACK";
			}
			if (!XAResourceImpl.getXAResourceImpl(2).inState(XAResourceImpl.FORGOTTEN)) {
				output = "Rec" + number + " failed: XAResourceImpl 2 is not FORGOTTEN";
			}
			break;
		case 37:
		case 38:
		case 39:
		case 40:
		case 41:
			if (XAResourceImpl.resourceCount() != 2) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=2";
			}
			if (!XAResourceImpl.allInState(XAResourceImpl.COMMITTED)) {
				output = "Rec" + number + " failed: resources are not all committed";
			}
			break;
		case 42:
			if (XAResourceImpl.resourceCount() != 3) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=3";
			}
			for (int resource = 1; resource < 2; resource++) {
				if (!XAResourceImpl.getXAResourceImpl(resource).inState(XAResourceImpl.ROLLEDBACK)) {
					output = "Rec" + number + " failed: XAResourceImpl " + resource + " is not ROLLEDBACK";
				}
			}
			break;
		case 43:
			if (XAResourceImpl.resourceCount() != 4) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=4";
			}
			for (int resource = 1; resource < 3; resource++) {
				if (!XAResourceImpl.getXAResourceImpl(resource).inState(XAResourceImpl.ROLLEDBACK)) {
					output = "Rec" + number + " failed: XAResourceImpl " + resource + " is not ROLLEDBACK";
				}
			}
			break;
		case 44:
			if (XAResourceImpl.resourceCount() != 4) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=4";
			}
			if (!XAResourceImpl.allInState(XAResourceImpl.COMMITTED)) {
				output = "Rec" + number + " failed: resources are not all committed";
			}
			break;
		case 45:
			if (XAResourceImpl.resourceCount() != 3) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=3";
			}
			if (!XAResourceImpl.allInState(XAResourceImpl.COMMITTED)) {
				output = "Rec" + number + " failed: resources are not all committed";
			}
			break;
		case 46:
			if (XAResourceImpl.resourceCount() != 10) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=10";
			}
			for (int resource = 1; resource < 10; resource++) {
				if (!XAResourceImpl.getXAResourceImpl(resource).inState(XAResourceImpl.ROLLEDBACK)) {
					output = "Rec" + number + " failed: XAResourceImpl " + resource + " is not ROLLEDBACK";
				}
			}
			break;			
		case 47:
			if (XAResourceImpl.resourceCount() != 10) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=10";
			}
			if (!XAResourceImpl.allInState(XAResourceImpl.COMMITTED)) {
				output = "Rec" + number + " failed: resources are not all committed";
			}
			break;
		case 48:
			if (XAResourceImpl.resourceCount() != 3) {
				output = "Rec" + number + " failed: " + XAResourceImpl.resourceCount()
						+ " resources!=3";
			}
			for (int resource = 1; resource < 2; resource++) {
				if (!XAResourceImpl.getXAResourceImpl(resource).inState(XAResourceImpl.ROLLEDBACK)) {
					output = "Rec" + number + " failed: XAResourceImpl " + resource + " is not ROLLEDBACK";
				}
			}
			break;
		}

		response.getWriter().println(output);
		response.getWriter().flush();

		XAResourceImpl.printState();
		System.out.println("end dispatch: "+output);
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}
}
