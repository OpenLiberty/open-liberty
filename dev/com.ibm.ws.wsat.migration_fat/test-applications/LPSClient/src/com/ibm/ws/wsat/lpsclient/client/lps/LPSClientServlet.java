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
package com.ibm.ws.wsat.lpsclient.client.lps;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.xml.ws.BindingProvider;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.OnePhaseXAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

@WebServlet({ "/LPSClientServlet" })
public class LPSClientServlet extends HttpServlet {
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

			int method = Integer.parseInt(request.getParameter("method").trim());
			System.out.println("==============Test Number: " + method
					+ "================");
			String BASE_URL = request.getParameter("baseurl");
			if (BASE_URL == null || BASE_URL.equals("")){
				BASE_URL = "http://localhost:9992";
			}
			String output = "";
			URL wsdlLocation = new URL(BASE_URL
					+ "/LPSServer/LastParticipantSupportService?wsdl");
			LastParticipantSupportService service = new LastParticipantSupportService(wsdlLocation);
			LastParticipantSupport proxy = service.getLastParticipantSupportPort();
			BindingProvider bind = (BindingProvider) proxy;
			bind.getRequestContext().put("javax.xml.ws.service.endpoint.address",
					BASE_URL + "/LPSServer/LastParticipantSupportService");
			
			switch (method) {
			case 1:
			case 101:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps101FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.commit();
					output += " Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 2:
			case 102:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps102FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output;
					userTransaction.commit();
				} catch (RollbackException e) {
					output +="Get expected RollbackException: " + e.toString() + ". Test passed.";
				} catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 3:
			case 103:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps103FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output;
					userTransaction.commit();
				} catch (RollbackException e) {
					output += "Get expected RollbackException: " + e.toString() + ". Test passed.";
				} catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 4:
				System.out.println("========== LPS Disabled WSTXLPS004FVT from LPSClientServlet start ==========");
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps004FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.commit();
				}catch (RollbackException e) {
					output += "Get expected RollbackException: " + e.toString() + ". Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 104:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps104FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.commit();
					output += " Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 5:
				System.out.println("========== LPS Disabled WSTXLPS005FVT from LPSClientServlet start ==========");
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps005FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.commit();
				}catch (RollbackException e) {
					output += "Get expected RollbackException: " + e.toString() + ". Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 105:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps105FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.commit();
					output += " Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 6:
			case 106:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps106FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.commit();
					output += " Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 7:
			case 107:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps107FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.commit();
					output += " Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 8:
			case 108:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps108FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.commit();
					output += " Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 9:
			case 109:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps109FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.commit();
					output += " Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 10:
			case 110:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps110FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.commit();
					output += " Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 11:
				System.out.println("========== LPS Disabled WSTXLPS011FVT from LPSClientServlet start ==========");
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps011FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.commit();
				}catch (RollbackException e) {
					output += "Get expected RollbackException: " + e.toString() + ". Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 111:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps111FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output;
					userTransaction.commit();
				} catch (RollbackException e) {
					output +="Get expected RollbackException: " + e.toString() + ". Test passed.";
				} catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 112:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps112FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output;
					userTransaction.commit();
				} catch (RollbackException e) {
					output +="Get expected RollbackException: " + e.toString() + ". Test passed.";
				} catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 113:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps113FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output;
					userTransaction.commit();
				} catch (RollbackException e) {
					output +="Get expected RollbackException: " + e.toString() + ". Test passed.";
				} catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 114:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps114FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output;
					userTransaction.commit();
				} catch (RollbackException e) {
					output +="Get expected RollbackException: " + e.toString() + ". Test passed.";
				} catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 201:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					final ExtendedTransactionManager TM = TransactionManagerFactory
							.getTransactionManager();
					XAResourceImpl.clear();
					boolean result = false;
					try {
						XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
						onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_COMMIT);
						result = TM.getTransaction().enlistResource(onePhaseXAResource);
					} catch (IllegalStateException e) {
						e.printStackTrace(System.out);
						output = "IllegalStateException happens: " + e.toString();
						System.out.println(output);
						return output;
					} catch (RollbackException e) {
						e.printStackTrace(System.out);
						output = "RollbackException happens: " + e.toString();
						System.out.println(output);
						return output;
					} catch (SystemException e) {
						e.printStackTrace(System.out);
						output = "SystemException happens: " + e.toString();
						System.out.println(output);
						return output;
					}
					if (result == false){
						output += "Enlist onePhaseXAResource failed. ";
						return output;
					}
					output = proxy.wstxlps201FVT();
					if (!output.contains("failed"))
						output += "Get response: " + output;
					userTransaction.commit();
					output += " Test passed.";
				} catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 202:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					output = proxy.wstxlps202FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output;
					userTransaction.commit();
				} catch (RollbackException e) {
					output +="Get expected RollbackException: " + e.toString() + ". Test passed.";
				} catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 203:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					final ExtendedTransactionManager TM = TransactionManagerFactory
							.getTransactionManager();
					XAResourceImpl.clear();
					boolean result = false;
					try {
						XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
						onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_COMMIT);
						result = TM.getTransaction().enlistResource(onePhaseXAResource);
					} catch (IllegalStateException e) {
						e.printStackTrace(System.out);
						output = "IllegalStateException happens: " + e.toString();
						System.out.println(output);
						return output;
					} catch (RollbackException e) {
						e.printStackTrace(System.out);
						output = "RollbackException happens: " + e.toString();
						System.out.println(output);
						return output;
					} catch (SystemException e) {
						e.printStackTrace(System.out);
						output = "SystemException happens: " + e.toString();
						System.out.println(output);
						return output;
					}
					if (result == false){
						output += "Enlist onePhaseXAResource failed. ";
						return output;
					}
					output = proxy.wstxlps203FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output + ".";
					userTransaction.commit();
					output += " Test passed.";
				}catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 204:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					
					final ExtendedTransactionManager TM = TransactionManagerFactory
							.getTransactionManager();
					XAResourceImpl.clear();
					boolean result = false;
					try {
						XAResourceImpl onePhaseXAResource = 
								new OnePhaseXAResourceImpl().setCommitAction(XAException.XA_RBROLLBACK);
						onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
						result = TM.getTransaction().enlistResource(onePhaseXAResource);
					} catch (IllegalStateException e) {
						e.printStackTrace(System.out);
						output = "IllegalStateException happens: " + e.toString();
						System.out.println(output);
						return output;
					} catch (RollbackException e) {
						e.printStackTrace(System.out);
						output = "RollbackException happens: " + e.toString();
						System.out.println(output);
						return output;
					} catch (SystemException e) {
						e.printStackTrace(System.out);
						output = "SystemException happens: " + e.toString();
						System.out.println(output);
						return output;
					}
					if (result == false){
						output += "Enlist onePhaseXAResource failed. ";
						return output;
					}
					output = proxy.wstxlps204FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output;
					userTransaction.commit();
				} catch (RollbackException e) {
					output +="Get expected RollbackException: " + e.toString() + ". Test passed.";
				} catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 205:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					
					final ExtendedTransactionManager TM = TransactionManagerFactory
							.getTransactionManager();
					XAResourceImpl.clear();
					boolean result = false;
					try {
						XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
						onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
						result = TM.getTransaction().enlistResource(onePhaseXAResource);
					} catch (IllegalStateException e) {
						e.printStackTrace(System.out);
						output = "IllegalStateException happens: " + e.toString();
						System.out.println(output);
						return output;
					} catch (RollbackException e) {
						e.printStackTrace(System.out);
						output = "RollbackException happens: " + e.toString();
						System.out.println(output);
						return output;
					} catch (SystemException e) {
						e.printStackTrace(System.out);
						output = "SystemException happens: " + e.toString();
						System.out.println(output);
						return output;
					}
					
					if (result == false){
						output += "Enlist onePhaseXAResource failed. ";
						return output;
					}
					output = proxy.wstxlps205FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output;
					userTransaction.commit();
				} catch (RollbackException e) {
					output +="Get expected RollbackException: " + e.toString() + ". Test passed.";
				} catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 206:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					
					final ExtendedTransactionManager TM = TransactionManagerFactory
							.getTransactionManager();
					XAResourceImpl.clear();
					boolean result = false;
					try {
						XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
						onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
						result = TM.getTransaction().enlistResource(onePhaseXAResource);
					} catch (IllegalStateException e) {
						e.printStackTrace(System.out);
						output = "IllegalStateException happens: " + e.toString();
						System.out.println(output);
						return output;
					} catch (RollbackException e) {
						e.printStackTrace(System.out);
						output = "RollbackException happens: " + e.toString();
						System.out.println(output);
						return output;
					} catch (SystemException e) {
						e.printStackTrace(System.out);
						output = "SystemException happens: " + e.toString();
						System.out.println(output);
						return output;
					}
					
					boolean result2 = enlistXAResouces(new String[]{"rollback"}, XAResourceImpl.DIRECTION_ROLLBACK);
					if (result == false){
						output += "Enlist onePhaseXAResource failed. ";
						return output;
					}
					if (result2 == false){
						output = "Failed when enlistXAResouces. Test failed.";
						return output;
					}
					output = proxy.wstxlps206FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output;
					userTransaction.commit();
				} catch (RollbackException e) {
					output +="Get expected RollbackException: " + e.toString() + ". Test passed.";
				} catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			case 207:
				try{
					Context ctx = new InitialContext();
					UserTransaction userTransaction = (UserTransaction) ctx
						.lookup("java:comp/UserTransaction");
					userTransaction.begin();
					
					final ExtendedTransactionManager TM = TransactionManagerFactory
							.getTransactionManager();
					XAResourceImpl.clear();
					boolean result = false;
					try {
						XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
						result = TM.getTransaction().enlistResource(onePhaseXAResource);
					} catch (IllegalStateException e) {
						e.printStackTrace(System.out);
						output = "IllegalStateException happens: " + e.toString();
						System.out.println(output);
						return output;
					} catch (RollbackException e) {
						e.printStackTrace(System.out);
						output = "RollbackException happens: " + e.toString();
						System.out.println(output);
						return output;
					} catch (SystemException e) {
						e.printStackTrace(System.out);
						output = "SystemException happens: " + e.toString();
						System.out.println(output);
						return output;
					}
					
					boolean result2 = enlistXAResouces(new String[]{"readonly"}, XAResourceImpl.DIRECTION_COMMIT);
					if (result == false){
						output += "Enlist onePhaseXAResource failed. ";
						return output;
					}
					if (result2 == false){
						output = "Failed when enlistXAResouces. Test failed.";
						return output;
					}
					
					output = proxy.wstxlps207FVT();
					if (!output.contains("failed"))
						output = "Get response: " + output;
					userTransaction.commit();
					output += " Test passed.";
				} catch(java.lang.Exception e){
					e.printStackTrace(System.out);
					output += " Exception happens: " + e.toString() + ". Test failed.";
				}
				break;
			}
			
		System.out.println("end dispatch");

		return "<html><header></header><body>" + output + "</body></html>";
	}

	private boolean enlistXAResouces(String[] CoordinatorXAResouces, int expectedDirection) {
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();
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
			} catch (java.lang.Exception e) {
				System.out.println("Get exception in enlistXAResouces :" + e.toString());
				return false;
			}
		}
		return true;
	}
}
