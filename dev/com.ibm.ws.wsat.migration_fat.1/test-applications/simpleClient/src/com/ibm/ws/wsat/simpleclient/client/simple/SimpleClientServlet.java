/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.simpleclient.client.simple;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.xml.ws.BindingProvider;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

@WebServlet({ "/SimpleClientServlet" })
public class SimpleClientServlet extends HttpServlet {
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
		String output = "";
		try {
			int method = Integer.parseInt(request.getParameter("method").trim());
			System.out.println("==============Test Number: " + method
					+ "================");
			XAResourceImpl.clear();
			String BASE_URL = request.getParameter("baseurl");
			String BASE_URL2 = request.getParameter("baseurl2");
			if (BASE_URL == null || BASE_URL.equals("")){
				BASE_URL = "http://localhost:9992";
			}
			String[] noXARes = new String[]{};
			String[] OneXARes = new String[]{""}; 
			String[] OneXAResVoteRollback = new String[]{"rollback"}; 
			String[] OneXAResVoteReadonly = new String[]{"readonly"}; 
			String[] TwoXARes = new String[]{"" , ""};
			String[] TwoXAResVoteRollback = new String[]{"rollback" , "rollback"};
			String[] TwoXAResVoteReadonlyCommit = new String[]{"readonly" , ""};
			String[] TwoXAResVoteReadonly = new String[]{"readonly" , "readonly"};
			switch (method) {
			case 0:
				//The first test often fails because the transaction timeouts.
				//Call JAX-WS providers without transaction so that JAXBContext
				//can be ready before the WS-AT tests.
				output = init(BASE_URL, BASE_URL2);
				break;
			case 1:
				output = execute(BASE_URL, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 2:
				output = execute(BASE_URL, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 3:
				output = execute(BASE_URL, noXARes, OneXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 4:
				output = execute(BASE_URL, noXARes, OneXAResVoteRollback,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 5:
				output = execute(BASE_URL, noXARes, OneXAResVoteReadonly,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 6:
				output = execute(BASE_URL, noXARes, OneXARes,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 7:
				output = execute(BASE_URL, noXARes, TwoXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 8:
				output = execute(BASE_URL, noXARes, TwoXAResVoteRollback,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 9:
				output = execute(BASE_URL, noXARes, TwoXAResVoteReadonlyCommit,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 10:
				output = execute(BASE_URL, noXARes, TwoXAResVoteReadonly,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 11:
				output = execute(BASE_URL, noXARes, TwoXARes,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 12:
				output = execute(BASE_URL, OneXARes, noXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 13:
				output = execute(BASE_URL, OneXAResVoteRollback, noXARes,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 14:
				output = execute(BASE_URL, OneXAResVoteReadonly, noXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 15:
				output = execute(BASE_URL, OneXARes, noXARes,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 16:
				output = execute(BASE_URL, OneXARes, OneXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 17:
				output = execute(BASE_URL, OneXAResVoteRollback, OneXARes,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 18:
				output = execute(BASE_URL, OneXARes, OneXAResVoteRollback,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 19:
				output = execute(BASE_URL, OneXAResVoteReadonly, OneXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 20:
				output = execute(BASE_URL, OneXARes, OneXAResVoteReadonly,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 21:
				output = execute(BASE_URL, OneXAResVoteReadonly, OneXAResVoteReadonly,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 22:
				output = execute(BASE_URL, OneXARes, OneXARes, OneXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 23:
				output = execute(BASE_URL, OneXARes, OneXARes,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 24:
				output = execute(BASE_URL, TwoXARes, OneXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 25:
				output = execute(BASE_URL, TwoXAResVoteRollback, OneXARes,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 26:
				output = execute(BASE_URL, TwoXARes, OneXAResVoteRollback,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 27:
				output = execute(BASE_URL, TwoXAResVoteReadonly, OneXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 28:
				output = execute(BASE_URL, TwoXARes, OneXAResVoteReadonly,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 29:
				output = execute(BASE_URL, TwoXAResVoteReadonly, OneXAResVoteReadonly,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 30:
				output = execute(BASE_URL, TwoXARes, OneXARes, OneXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 31:
				output = execute(BASE_URL, TwoXARes, OneXARes,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 32:
				output = execute(BASE_URL, OneXARes, TwoXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 33:
				output = execute(BASE_URL, OneXAResVoteRollback, TwoXARes,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 34:
				output = execute(BASE_URL, OneXARes, TwoXAResVoteRollback,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 35:
				output = execute(BASE_URL, OneXAResVoteReadonly, TwoXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 36:
				output = execute(BASE_URL, OneXARes, TwoXAResVoteReadonly,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 37:
				output = execute(BASE_URL, OneXAResVoteReadonly, TwoXAResVoteReadonly,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 38:
				output = execute(BASE_URL, OneXARes, TwoXARes, OneXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 39:
				output = execute(BASE_URL, OneXARes, TwoXARes,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 40:
				output = execute(BASE_URL, TwoXARes, TwoXARes, 
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 41:
				output = execute(BASE_URL, TwoXAResVoteRollback, TwoXARes, 
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 42:
				output = execute(BASE_URL, TwoXARes, TwoXAResVoteRollback, 
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 43:
				output = execute(BASE_URL, TwoXAResVoteReadonly, TwoXARes, 
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 44:
				output = execute(BASE_URL, TwoXARes, TwoXAResVoteReadonly, 
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 45:
				output = execute(BASE_URL, TwoXAResVoteReadonly, TwoXAResVoteReadonly, 
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 46:
				output = execute(BASE_URL, TwoXARes, TwoXARes, OneXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 47:
				output = execute(BASE_URL, TwoXARes, TwoXARes,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 48:
				output = execute(BASE_URL, noXARes, OneXARes, "setRollbackOnlyBeforeWSCall",
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 49:
				output = execute(BASE_URL, noXARes, OneXARes, "setRollbackOnlyBeforeWSCall",
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 50:
				output = execute(BASE_URL, noXARes, OneXARes, "setRollbackOnlyAfterWSCall",
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 51:
				output = execute(BASE_URL, noXARes, OneXARes, "setRollbackOnlyAfterWSCall",
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 52:
				output = execute(BASE_URL, OneXARes, OneXARes, "setRollbackOnlyAfterWSCall",
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 53:
				output = execute(BASE_URL, OneXARes, OneXARes, "setRollbackOnlyAfterWSCall",
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 54:
				output = execute(BASE_URL, BASE_URL, noXARes, OneXARes, OneXARes, noXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 55:
				output = execute(BASE_URL, BASE_URL, noXARes, OneXARes, OneXARes, OneXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 56:
				output = execute(BASE_URL, BASE_URL, noXARes, OneXARes, OneXARes, OneXAResVoteRollback,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 57:
				output = execute(BASE_URL, BASE_URL, noXARes, OneXAResVoteRollback, OneXARes, OneXARes,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 58:
				output = execute(BASE_URL, BASE_URL, noXARes, OneXARes, OneXAResVoteRollback, OneXARes,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 59:
				output = execute(BASE_URL, BASE_URL, noXARes, TwoXARes, TwoXARes, noXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 60:
				output = execute(BASE_URL, BASE_URL, noXARes, TwoXARes, TwoXARes, TwoXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 61:
				output = execute(BASE_URL, BASE_URL, noXARes, TwoXARes, TwoXARes, TwoXAResVoteRollback,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 62:
				output = execute(BASE_URL, BASE_URL, noXARes, TwoXAResVoteRollback, TwoXARes, TwoXARes,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 63:
				output = execute(BASE_URL, BASE_URL, noXARes, TwoXARes, TwoXAResVoteRollback, TwoXARes, 
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 64:
				output = execute(BASE_URL, BASE_URL2, noXARes, OneXARes, OneXARes, noXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 65:
				output = execute(BASE_URL, BASE_URL2, noXARes, OneXARes, OneXARes, OneXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 66:
				output = execute(BASE_URL, BASE_URL2, noXARes, OneXARes, OneXARes, OneXAResVoteRollback,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 67:
				output = execute(BASE_URL, BASE_URL2, noXARes, OneXAResVoteRollback, OneXARes, OneXARes,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 68:
				output = execute(BASE_URL, BASE_URL2, noXARes, OneXARes, OneXAResVoteRollback, OneXARes,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 69:
				output = execute(BASE_URL, BASE_URL2, noXARes, TwoXARes, TwoXARes, noXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 70:
				output = execute(BASE_URL, BASE_URL2, noXARes, TwoXARes, TwoXARes, TwoXARes,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 71:
				output = execute(BASE_URL, BASE_URL2, noXARes, TwoXARes, TwoXARes, TwoXAResVoteRollback,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 72:
				output = execute(BASE_URL, BASE_URL2, noXARes, TwoXAResVoteRollback, TwoXARes, TwoXARes,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 73:
				output = execute(BASE_URL, BASE_URL2, noXARes, TwoXARes, TwoXAResVoteRollback, TwoXARes, 
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 94:
				output = execute(BASE_URL, noXARes, noXARes, 45, 0,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 95:
				output = execute(BASE_URL, noXARes, noXARes, 10, 0,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 96:
				output = execute(BASE_URL, noXARes, noXARes, 45, 0,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 97:
				output = execute(BASE_URL, noXARes, noXARes, 20, 0,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 98:
				output = execute(BASE_URL, TwoXARes, TwoXARes, 45, 0,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 99:
				output = execute(BASE_URL, TwoXARes, TwoXARes, 20, 0,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 100:
				output = execute(BASE_URL, TwoXARes, TwoXARes, 45, 0,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 101:
				output = execute(BASE_URL, TwoXARes, TwoXARes, 20, 0,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 102:
				output = execute(BASE_URL, TwoXARes, TwoXARes, 0, 45,
						"commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException");
				break;
			case 103:
				output = execute(BASE_URL, TwoXARes, TwoXARes, 0, 20,
						"commit", XAResourceImpl.DIRECTION_COMMIT, "NoException");
				break;
			case 104:
				output = execute(BASE_URL, TwoXARes, TwoXARes, 0, 45,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			case 105:
				output = execute(BASE_URL, TwoXARes, TwoXARes, 0, 20,
						"rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException");
				break;
			}
			
			output = "<html><header></header><body>" + output + "</body></html>";
		} catch (Exception e) {
			output = "<html><header></header><body> Client catch exception: "
							+ e.toString() + "</body></html>";
			e.printStackTrace();
		} finally{
			XAResourceImpl.clear();
		}
		
		System.out.println("end dispatch");
		return output;
	}
	
	private String execute(String BASE_URL, String commitRollback, int expectedDirection, String expectResult) {
		return execute(BASE_URL, new String[]{}, new String[]{}, commitRollback, expectedDirection, expectResult);
	}
	
	private String execute(String BASE_URL, String[] CoordinatorXAResouces, String[] ParticipantXAResouces,
			String commitRollback, int expectedDirection, String expectResult) {
		return execute(BASE_URL, CoordinatorXAResouces, ParticipantXAResouces, 
				new String[]{}, commitRollback,expectedDirection, expectResult);
	}
	
	
	private String execute(String BASE_URL, String[] CoordinatorXAResouces, String[] ParticipantXAResouces,
			int sleepTimeClient, int sleepTimeServer, String commitRollback, int expectedDirection, String expectResult) {
		return execute(BASE_URL, "", CoordinatorXAResouces, ParticipantXAResouces, 
				new String[]{}, new String[]{}, "", sleepTimeClient, sleepTimeServer, commitRollback, expectedDirection, expectResult);
	}
	
	private String execute(String BASE_URL, String[] CoordinatorXAResouces, String[] ParticipantXAResouces,
			String setRollbackOnly, String commitRollback, int expectedDirection, String expectResult) {
		return execute(BASE_URL, CoordinatorXAResouces, ParticipantXAResouces, 
				new String[]{}, setRollbackOnly, commitRollback, expectedDirection,expectResult);
	}

	private String execute(String BASE_URL, String[] CoordinatorXAResouces, String[] ParticipantXAResouces,
			String[] CoordinatorXAResoucesAfterWSCall, String commitRollback, int expectedDirection, String expectResult) {
		return execute(BASE_URL, CoordinatorXAResouces, ParticipantXAResouces, 
				CoordinatorXAResoucesAfterWSCall, "", commitRollback, expectedDirection, expectResult);
	}
	
	
	private String execute(String BASE_URL, String[] CoordinatorXAResouces, String[] ParticipantXAResouces,
			String[] CoordinatorXAResoucesAfterWSCall, String setRollbackOnly, 
			String commitRollback, int expectedDirection, String expectResult) {
		return execute(BASE_URL, "", CoordinatorXAResouces, ParticipantXAResouces, new String[]{},
				CoordinatorXAResoucesAfterWSCall, setRollbackOnly, commitRollback, expectedDirection, expectResult);
	}
	
	private String execute(String BASE_URL, String BASE_URL2, String[] CoordinatorXAResouces, String[] ParticipantXAResouces,
			String[] ParticipantXAResoucesInSecondCall, String[] CoordinatorXAResoucesAfterWSCall,
			String commitRollback, int expectedDirection, String expectResult) {
		return execute(BASE_URL, BASE_URL2, CoordinatorXAResouces, ParticipantXAResouces, ParticipantXAResoucesInSecondCall,
				CoordinatorXAResoucesAfterWSCall, "", commitRollback, expectedDirection, expectResult);
	}
	
	private String execute(String BASE_URL, String BASE_URL2, String[] CoordinatorXAResouces, String[] ParticipantXAResouces,
			String[] ParticipantXAResoucesInSecondCall, String[] CoordinatorXAResoucesAfterWSCall, String setRollbackOnly, 
			String commitRollback, int expectedDirection, String expectResult) {
		return execute(BASE_URL, BASE_URL2, CoordinatorXAResouces, ParticipantXAResouces, ParticipantXAResoucesInSecondCall,
				CoordinatorXAResoucesAfterWSCall, setRollbackOnly, 0, 0, commitRollback, expectedDirection, expectResult);
	}
	
	private String execute(String BASE_URL, String BASE_URL2, String[] CoordinatorXAResouces, String[] ParticipantXAResouces,
			String[] ParticipantXAResoucesInSecondCall, String[] CoordinatorXAResoucesAfterWSCall, String setRollbackOnly, 
			int sleepTimeClient, int sleepTimeServer, String commitRollback, int expectedDirection, String expectResult) {
		int UTstatus = -1;
		int UTexpectedStatus = -1;
		String output = "";
		try {
			// User Transaction Initialization
			Context ctx = new InitialContext();
			UserTransaction userTransaction = (UserTransaction) ctx
					.lookup("java:comp/UserTransaction");
			userTransaction.begin();
			System.out.println("execute userTransaction.begin()");
			// Check User Transaction Status
			UTexpectedStatus = Status.STATUS_ACTIVE;
			UTstatus = userTransaction.getStatus();
			if (UTstatus != UTexpectedStatus){
				String errorMessage = "1: UserTransaction Status not expected! Expected: "
						+ UTexpectedStatus + "  Actual: " + UTstatus;
				System.out.println(errorMessage);
				return errorMessage;
			}
			
			if(setRollbackOnly.equals("setRollbackOnlyBeforeWSCall")){
				userTransaction.setRollbackOnly();
				UTexpectedStatus = Status.STATUS_MARKED_ROLLBACK;
				UTstatus = userTransaction.getStatus();
				if (UTstatus != UTexpectedStatus){
					String errorMessage = "2: UserTransaction Status not expected! Expected: "
							+ UTexpectedStatus + "  Actual: " + UTstatus;
					System.out.println(errorMessage);
					return errorMessage;
				}
			}

			if (CoordinatorXAResouces.length > 0) {
				boolean result = enlistXAResouces(CoordinatorXAResouces, expectedDirection);
				if (result == false){
					output += " Enlist XAResource fails before calling web service.";
					return output;
				}
				System.out.println("execute enlistXAResouces(): " + result);
			}
			
			String output1 = "", output2 = "No second web service call.";
			output1 = " Get response: " + callWebservice(BASE_URL, ParticipantXAResouces, expectedDirection, sleepTimeServer, true) + ".";
			System.out.println("execute callWebservice() 1: " + output1);
			
			if (BASE_URL2 != null && !BASE_URL2.equals("")){
				UTstatus = userTransaction.getStatus();
				if (UTstatus != UTexpectedStatus) {
					String errorMessage = "3: UserTransaction Status not expected! Expected: "
							+ UTexpectedStatus + "  Actual: " + UTstatus;
					System.out.println(errorMessage);
					return errorMessage;
				}
				output2 = " Get response in the second call: "
						+ callWebservice(BASE_URL2, ParticipantXAResoucesInSecondCall, expectedDirection, sleepTimeServer, !BASE_URL2.equals(BASE_URL)) + ".";
				System.out.println("execute callWebservice() 2: " + output2);
			}
			
			output += (output1 + output2); 
			
			if(setRollbackOnly.equals("setRollbackOnlyAfterWSCall")){
				userTransaction.setRollbackOnly();
				UTexpectedStatus = Status.STATUS_MARKED_ROLLBACK;
				UTstatus = userTransaction.getStatus();
				if (UTstatus != UTexpectedStatus){
					String errorMessage = "4: UserTransaction Status not expected! Expected: "
							+ UTexpectedStatus + "  Actual: " + UTstatus;
					System.out.println(errorMessage);
					return errorMessage;
				}
			} else {
			// Check User Transaction Status
				if (UTstatus != UTexpectedStatus){
					String errorMessage = "5: UserTransaction Status not expected! Expected: "
							+ UTexpectedStatus + "  Actual: " + UTstatus;
					System.out.println(errorMessage);
					return errorMessage;
				}
			}

			if (CoordinatorXAResoucesAfterWSCall.length > 0) {
				boolean result = enlistXAResouces(CoordinatorXAResoucesAfterWSCall, expectedDirection);
				if (result == false){
					output += " Enlist XAResource fails before calling web service.";
					return output;
				}
				System.out.println("execute enlistXAResouces(): " + result);
			}
			
			if (sleepTimeClient > 0)
			{
				System.out.println(">>>>>>>>>Thread is hanging for " + sleepTimeClient + "seconds!");
			  	Thread.sleep(sleepTimeClient * 1000);
			  	System.out.println(">>>>>>>>>Woken up!");
			}

			// User Transaction Commit / Rollback
			System.out.println("execute commitRollback(): " + commitRollback);
			if (commitRollback.equals("commit")) {
				userTransaction.commit();
			} else if (commitRollback.equals("rollback")) {
				userTransaction.rollback();
			} else return output + " User transaction action error. Test failed.";
			
			if (expectResult.equals("NoException") &&  !output1.contains("failed") && !output2.contains("failed") &&
					output1.contains("Transaction Manager Status: ACTIVE")
					// || (sleepTimeServer > 0) && output1.contains("Sleep method successfully returns."))
					&& (output2.contains("Transaction Manager Status: ACTIVE") || output2.contains("No second web service call."))){
				return output + " Test passed.";
			}else {
				return output + " Cannot get the expected exception " + expectResult + ". Test failed.";
			}
		} catch (RollbackException e) {
			System.out.println("execute catch RollbackException!" + e.toString());
			e.printStackTrace();
			if(expectResult.equals("RollbackException"))
			{
				return output + " Get Excepted RollbackException. Test passed.";
			}
			else return output + " RollbackException happens: " + e.toString() + ". Test failed.";
		} catch (Exception e) {
			System.out.println("execute catch Exception: " + e.getMessage());
			e.printStackTrace();
			return output + " Exception happens: " + e.toString() + ". Test failed.";
		}
	}

	private String callWebservice(String BASE_URL, String[] XAResouces, int expectedDirection, int sleepTimeServer, boolean clearXAResource)
			throws MalformedURLException {
		final int timeout = 300 * 1000;
		URL wsdlLocation = new URL(BASE_URL
				+ "/simpleServer/WSATSimpleService?wsdl");
		WSATSimpleService service = new WSATSimpleService(wsdlLocation);
		WSATSimple proxy = service.getWSATSimplePort();
		BindingProvider bind = (BindingProvider) proxy;
		bind.getRequestContext().put("javax.xml.ws.service.endpoint.address",
				BASE_URL + "/simpleServer/WSATSimpleService");
		bind.getRequestContext().put("com.sun.xml.ws.connect.timeout", timeout);
		bind.getRequestContext().put("com.sun.xml.ws.request.timeout", timeout);
		bind.getRequestContext().put("javax.xml.ws.client.connectionTimeout", timeout);
		bind.getRequestContext().put("javax.xml.ws.client.receiveTimeout", timeout);
		String response = "";
		System.out.println("Set expectedDirection in callWebservice: " + expectedDirection);
		System.out.println("XAResouces.length in callWebservice: " + XAResouces.length);
		for(int i=0; i<XAResouces.length; i++){
			System.out.println("Vote of XAResouces[" + i + "] in callWebservice: " + XAResouces[i].toString());
		}
		switch (XAResouces.length) {
		case 0:
			response = proxy.getStatus();
			break;
		case 1:
			response = proxy.enlistOneXAResource(XAResouces[0], expectedDirection, clearXAResource);
			break;
		case 2:
			if (sleepTimeServer > 0){
				System.out.println(">>>>>>>>>>Server will sleep " + sleepTimeServer + " seconds.");
				response = proxy.sleep(sleepTimeServer, expectedDirection);
			}
			else {
				response = proxy.enlistTwoXAResources(XAResouces[0], XAResouces[1], expectedDirection, clearXAResource);
			}
			break;
		}
		System.out.println("Reply from server: " + response);
		return response;

	}

	private boolean enlistXAResouces(String[] CoordinatorXAResouces, int expectedDirection) {
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();
		/*try{
			TM.begin();
		}catch (Exception e) {
			System.out.println("Get exception in enlistXAResouces when "
					+ "beginning TransactionManager:" + e.toString());
			return false;
		}*/
		for (int i = 0; i < CoordinatorXAResouces.length; i++) {
			int prepareAction = 0;
			if (CoordinatorXAResouces[i].equals("rollback")) {
				prepareAction = XAException.XA_RBROLLBACK;
			} else if (CoordinatorXAResouces[i].equals("readonly")) {
				prepareAction = XAException.XA_RDONLY;
			}
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(i);
			XAResourceImpl xaRes;
			try {
				if (prepareAction == 0) {
					xaRes = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo);
				} else {
					xaRes = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo)
							.setPrepareAction(prepareAction);
				}
				final int recoveryId = TM.registerResourceInfo("xaResInfo",
						xaResInfo);
				xaRes.setExpectedDirection(expectedDirection);
				boolean result = TM.enlist(xaRes, recoveryId);
				if (result == false) {
					System.out.println("Enlist XAResource voting "
							+ CoordinatorXAResouces[i] + " failed.");
					return false;
				}
			} catch (Exception e) {
				System.out.println("Get exception in enlistXAResouces :" + e.toString());
				return false;
			}
		}
		return true;
	}
	
	private String init(String BASE_URL, String BASE_URL2) throws MalformedURLException{
		String res = "First reply : '" + callWebservice(BASE_URL) + "'";
		if(BASE_URL2 != null && !BASE_URL2.equals("")){
			res += "; Second reply : " + callWebservice(BASE_URL2) + "'";
		}
		return res;
	}
	
	private String callWebservice(String BASE_URL) throws MalformedURLException{
		URL wsdlLocation = new URL(BASE_URL
				+ "/simpleServer/WSATSimpleService?wsdl");
		WSATSimpleService service = new WSATSimpleService(wsdlLocation);
		WSATSimple proxy = service.getWSATSimplePort();
		BindingProvider bind = (BindingProvider) proxy;
		bind.getRequestContext().put("javax.xml.ws.service.endpoint.address",
				BASE_URL + "/simpleServer/WSATSimpleService");
		return proxy.echo("Init " + BASE_URL);
	}
}
