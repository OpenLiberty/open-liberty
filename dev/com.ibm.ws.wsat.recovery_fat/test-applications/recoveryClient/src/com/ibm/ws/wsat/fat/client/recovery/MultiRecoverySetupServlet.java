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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.HeuristicMixedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.xml.ws.BindingProvider;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.OnePhaseXAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;

@WebServlet({ "/MultiRecoverySetupServlet" })
public class MultiRecoverySetupServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		
		
		    String output = null;
			try {
				XAResourceImpl.clear();
				int number = Integer.parseInt(request.getParameter("number").trim());
				System.out.println("==============MultiRecoverySetupServlet Test Number: " + number
						+ "================");
				String BASE_URL = request.getParameter("baseurl");
				if (BASE_URL == null || BASE_URL.equals("")){
					BASE_URL = "http://localhost:8010";
				}
				String BASE_URL2 = request.getParameter("baseurl2");
				if (BASE_URL2 == null || BASE_URL2.equals("")){
					BASE_URL2 = "http://localhost:9992";
				}
				output = "";
				URL wsdlLocation = new URL(BASE_URL
						+ "/recoveryServer/RecoveryService?wsdl");
				RecoveryService service = new RecoveryService(wsdlLocation);
				Recovery proxy = service.getRecoveryPort();
				BindingProvider bind = (BindingProvider) proxy;
				bind.getRequestContext().put("javax.xml.ws.service.endpoint.address",
						BASE_URL + "/recoveryServer/RecoveryService");
				URL wsdlLocation2 = new URL(BASE_URL2
						+ "/recoveryServer/RecoveryService?wsdl");
				RecoveryService service2 = new RecoveryService(wsdlLocation2);
				Recovery proxy2 = service2.getRecoveryPort();
				BindingProvider bind2 = (BindingProvider) proxy2;
				bind2.getRequestContext().put("javax.xml.ws.service.endpoint.address",
						BASE_URL2 + "/recoveryServer/RecoveryService");

//			Defect 200252 arises because webservice calls hang sometimes; usually when the 
//			environment is running very slowly. We'll set some timeouts here so that we fail
//			fast under these circumstances
				final int timeout = 30000;

				//Set timeout until a connection is established
				// Set timeouts 2 ways to cater for different javas
				bind2.getRequestContext().put("com.sun.xml.ws.connect.timeout", timeout);
				bind2.getRequestContext().put("com.sun.xml.ws.request.timeout", timeout);

				bind2.getRequestContext().put("javax.xml.ws.client.connectionTimeout", timeout);
				bind2.getRequestContext().put("javax.xml.ws.client.receiveTimeout", timeout);

				switch (number) {
				case 101:
				case 103:
					try{
						Context ctx = new InitialContext();
						UserTransaction userTransaction = (UserTransaction) ctx
							.lookup("java:comp/UserTransaction");
						userTransaction.begin();
						//server1 call web service
						String res1 = proxy.invoke(Integer.parseInt(number+"01"),"");
						if (!output.contains("failed"))
							output += "Get response in the first call: " + res1 + ".";
						//server2 call web service
						String res2 = proxy2.invoke(Integer.parseInt(number+"02"),BASE_URL);
						if (!output.contains("failed"))
							output += "Get response in the second call: " + res2 + ".";
					}catch(java.lang.Exception e){
						e.printStackTrace();
						output += " Exception happens: " + e.toString() + ". Test failed.";
					}
					break;
				case 102:
					try{
						Context ctx = new InitialContext();
						UserTransaction userTransaction = (UserTransaction) ctx
							.lookup("java:comp/UserTransaction");
						userTransaction.begin();
						//server1 call web service
						String res1 = proxy.invoke(Integer.parseInt(number+"01"),"");
						if (!output.contains("failed"))
							output += "Get response in the first call: " + res1 + ".";
						System.out.println("output after first call = " + output);
						//server2 call web service
						
						String res2;
						try {
							res2 = proxy2.invoke(Integer.parseInt(number+"02"),"");
								output += "Get response in the second call: " + res2 + ". Test failed";
						} catch (Exception e) {
							// expected
							output += "Get response in the second call: " + e.toString() + ".";
						}
						System.out.println("output after second call = " + output);
						try {
							userTransaction.commit();//commit the transaction
						} catch (HeuristicMixedException e) {
							output += " Test passed.";
						}
					}catch(java.lang.Exception e){
						e.printStackTrace();
						output += " Exception happens: " + e.toString() + ". Test failed.";
					}
					break;
				case 201:
					try{
						Context ctx = new InitialContext();
						UserTransaction userTransaction = (UserTransaction) ctx
							.lookup("java:comp/UserTransaction");
						userTransaction.begin();
						//server1 call web service
						String res1 = proxy.invoke(Integer.parseInt(number+"01"),"");
						if (!output.contains("failed"))
							output += "Get response in the first call: " + res1 + ".";
						//server2 call web service
						String res2 = proxy2.invoke(Integer.parseInt(number+"02"),"");
						if (!output.contains("failed"))
							output += "Get response in the second call: " + res2 + ".";
						//kill server1
						Runtime.getRuntime().halt(0);
						output += " Test passed.";
					}catch(java.lang.Exception e){
						e.printStackTrace();
						output += " Exception happens: " + e.toString() + ". Test failed.";
					}
					break;
				case 202:
					try{
						Context ctx = new InitialContext();
						UserTransaction userTransaction = (UserTransaction) ctx
							.lookup("java:comp/UserTransaction");
						userTransaction.begin();
						//server1 call web service
						String res1 = proxy.invoke(Integer.parseInt(number+"01"),"");
						if (!output.contains("failed"))
							output += "Get response in the first call: " + res1 + ".";
						//server2 call web service
						String res2 = proxy2.invoke(Integer.parseInt(number+"02"),"");
						if (!output.contains("failed"))
							output += "Get response in the second call: " + res2 + ".";
						//kill server2
						try{
							callServlet("SuicideServlet"+"02",BASE_URL2);
						}catch (SocketException e){
							System.out.println("Get expected exception " + e.toString() 
									+ ".");
						}
						userTransaction.commit();
						output += " Test passed.";
					}catch(java.lang.Exception e){
						//e.printStackTrace();
						output += " Get expected Exception" + e.toString();
					}
					break;
				case 203:
					try{
						Context ctx = new InitialContext();
						UserTransaction userTransaction = (UserTransaction) ctx
							.lookup("java:comp/UserTransaction");
						userTransaction.begin();
						//server1 call web service
						String res1 = proxy.invoke(Integer.parseInt(number+"01"),"");
						if (!output.contains("failed"))
							output += "Get response in the first call: " + res1 + ".";
						//server2 call web service
						String res2 = proxy2.invoke(Integer.parseInt(number+"02"),"");
						if (!output.contains("failed"))
							output += "Get response in the second call: " + res2 + ".";
						//kill server2
						try{
							callServlet("SuicideServlet"+"02",BASE_URL2);
						}catch (SocketException e){
							System.out.println("Get expected exception " + e.toString() 
									+ ". Continue to kill myself.");
						}
						Runtime.getRuntime().halt(0);	
						output += " Test passed.";
					}catch(java.lang.Exception e){
						e.printStackTrace();
						output += " Exception happens: " + e.toString() + ". Test failed.";
					}
					break;
				case 301:
				case 302:
				case 303:
				case 401:
				case 402:
				case 403:
				case 701:
				case 702:
				case 703:
				case 801:
				case 802:
				case 803:
				case 1101:
				case 1102:
				case 1103:
				case 1301:
				case 1302:
				case 1303:
				case 1401:
				case 1402:
				case 1403:
				case 1601:
				case 1602:
				case 1603:
					try{
						Context ctx = new InitialContext();
						UserTransaction userTransaction = (UserTransaction) ctx
							.lookup("java:comp/UserTransaction");
						userTransaction.begin();
						//server1 call web service
						System.out.println("Call the first web service.");
						String res1 = proxy.invoke(Integer.parseInt(number+"01"),"");
						if (!output.contains("failed"))
							output += "Get response in the first call: " + res1 + ".";
						System.out.println("First web service call response:" + res1);
						//server2 call web service
						System.out.println("Call the second web service.");
						String res2 = proxy2.invoke(Integer.parseInt(number+"02"),BASE_URL);
						if (!output.contains("failed"))
							output += "Get response in the second call: " + res2 + ".";
						System.out.println("Second web service call response:" + res2);
						userTransaction.commit();
					}catch(java.lang.Exception e){
						e.printStackTrace();
						output += " Get exception: " + e.toString() + ".";
					}
					break;
				case 501:
				case 502:
				case 503:
				case 601:
				case 602:
				case 603:
				case 901:
				case 902:
				case 903:
				case 1001:
				case 1002:
				case 1003:
				case 1201:
				case 1202:
				case 1203:
					try{
						Context ctx = new InitialContext();
						UserTransaction userTransaction = (UserTransaction) ctx
							.lookup("java:comp/UserTransaction");
						userTransaction.begin();
						//server1 call web service
						String res1 = proxy.invoke(Integer.parseInt(number+"01"), BASE_URL2);
						if (!output.contains("failed"))
							output += "Get response in the first call: " + res1 + ".";
						System.out.println("First web service call response:" + res1);
						//server2 call web service
						String res2 = proxy2.invoke(Integer.parseInt(number+"02"),"");
						if (!output.contains("failed"))
							output += "Get response in the second call: " + res2 + ".";
						System.out.println("Second web service call response:" + res2);
						output += " Test passed.";
						userTransaction.commit();
					}catch(java.lang.Exception e){
						e.printStackTrace();
						output += "Get exception : " + e.toString() + ".";
					}
					break;
				case 1501:
				case 1502:
				case 1503:
					try{
						Context ctx = new InitialContext();
						UserTransaction userTransaction = (UserTransaction) ctx
							.lookup("java:comp/UserTransaction");
						userTransaction.begin();
						//server1 call web service
						String res1 = proxy.invoke(Integer.parseInt(number+"01"),"");
						if (!output.contains("failed"))
							output += "Get response in the first call: " + res1 + ".";
						//server2 call web service
						String res2 = proxy2.invoke(Integer.parseInt(number+"02"),BASE_URL);
						if (!output.contains("failed"))
							output += "Get response in the second call: " + res2 + ".";
						output += " Test passed.";
						userTransaction.rollback();
					}catch(java.lang.Exception e){
						e.printStackTrace();
						output += " Exception happens: " + e.toString() + ". Test failed.";
					}
					break;
				case 3011:
				case 3012:
				case 3013:
					try {
						Context ctx = new InitialContext();
						UserTransaction userTransaction = (UserTransaction) ctx
							.lookup("java:comp/UserTransaction");
						userTransaction.begin();
						final ExtendedTransactionManager TM = TransactionManagerFactory
								.getTransactionManager();
						XAResourceImpl.clear();
						boolean result = false;
						try {
							XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl().setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
							//onePhaseXAResource.setExpectedDirection(DIRECTION_COMMIT);
							result = TM.getTransaction().enlistResource(onePhaseXAResource);
						} catch (IllegalStateException e) {
							e.printStackTrace();
							output = "IllegalStateException happens: " + e.toString();
							System.out.println(output);
							return;
						} catch (RollbackException e) {
							e.printStackTrace();
							output = "RollbackException happens: " + e.toString();
							System.out.println(output);
							return;
						} catch (SystemException e) {
							e.printStackTrace();
							output = "SystemException happens: " + e.toString();
							System.out.println(output);
							return;
						}
						if (result == false){
							output += "Enlist onePhaseXAResource failed. ";
							return;
						}
						output = proxy2.invoke(Integer.parseInt(number+"01"), BASE_URL);
						if (!output.contains("failed"))
							output = "Get response: " + output + ".";
						output += " Test passed.";
						userTransaction.commit();
					}catch(java.lang.Exception e){
						e.printStackTrace();
						output += " Exception happens: " + e.toString() + ". Test failed.";
					}
					break;
				case 3021:
					try {
						Context ctx = new InitialContext();
						UserTransaction userTransaction = (UserTransaction) ctx
							.lookup("java:comp/UserTransaction");
						userTransaction.begin();
						final ExtendedTransactionManager TM = TransactionManagerFactory
								.getTransactionManager();
						XAResourceImpl.clear();
						boolean result = false;
						try {
							XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl().setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
							//onePhaseXAResource.setExpectedDirection(DIRECTION_COMMIT);
							result = TM.getTransaction().enlistResource(onePhaseXAResource);
						} catch (IllegalStateException e) {
							e.printStackTrace();
							output = "IllegalStateException happens: " + e.toString();
							System.out.println(output);
							return;
						} catch (RollbackException e) {
							e.printStackTrace();
							output = "RollbackException happens: " + e.toString();
							System.out.println(output);
							return;
						} catch (SystemException e) {
							e.printStackTrace();
							output = "SystemException happens: " + e.toString();
							System.out.println(output);
							return;
						}
						if (result == false){
							output += "Enlist onePhaseXAResource failed. ";
							return;
						}
						output = proxy2.invoke(Integer.parseInt(number+"01"),"");
						if (!output.contains("failed"))
							output = "Get response: " + output + ".";
						//Call the SuicideServlet to kill myself
						//In this way "Setting state from RECOVERING to ACTIVE"
						//will show up in the trace
						try{
							callServlet("SuicideServlet"+"01",BASE_URL);
						}catch (SocketException e){
							System.out.println("Get expected exception " + e.toString() 
									+ " when killing myself");
						}
						//Runtime.getRuntime().halt(0);
						output += " Test passed.";
						userTransaction.commit();
					}catch(java.lang.Exception e){
						e.printStackTrace();
						output += " Exception happens: " + e.toString() + ". Test failed.";
					}
					break;
				case 3022:
					try {
						Context ctx = new InitialContext();
						UserTransaction userTransaction = (UserTransaction) ctx
							.lookup("java:comp/UserTransaction");
						userTransaction.begin();
						final ExtendedTransactionManager TM = TransactionManagerFactory
								.getTransactionManager();
						XAResourceImpl.clear();
						boolean result = false;
						try {
							XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl().setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
							result = TM.getTransaction().enlistResource(onePhaseXAResource);
						} catch (IllegalStateException e) {
							e.printStackTrace();
							output = "IllegalStateException happens: " + e.toString();
							System.out.println(output);
							return;
						} catch (RollbackException e) {
							e.printStackTrace();
							output = "RollbackException happens: " + e.toString();
							System.out.println(output);
							return;
						} catch (SystemException e) {
							e.printStackTrace();
							output = "SystemException happens: " + e.toString();
							System.out.println(output);
							return;
						}
						if (result == false){
							output += "Enlist onePhaseXAResource failed. ";
							return;
						}
						output = proxy2.invoke(Integer.parseInt(number+"01"),"");
						if (!output.contains("failed"))
							output = "Get response: " + output + ".";
						//kill server2
						///try{
							callServlet("SuicideServlet"+"02", BASE_URL2);
						//}catch (SocketException e){
						//	System.out.println("Get expected exception " + e.toString() 
						//			+ " when killing server2");
						// }
						output += " Test passed.";
						userTransaction.commit();
					}catch(java.lang.Exception e){
						e.printStackTrace();
						output += " Exception happens: " + e.toString() + ". Test failed.";
					}
					break;
				case 3023:
					try {
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
							//onePhaseXAResource.setExpectedDirection(DIRECTION_COMMIT);
							result = TM.getTransaction().enlistResource(onePhaseXAResource);
						} catch (IllegalStateException e) {
							e.printStackTrace();
							output = "IllegalStateException happens: " + e.toString();
							System.out.println(output);
							return;
						} catch (RollbackException e) {
							e.printStackTrace();
							output = "RollbackException happens: " + e.toString();
							System.out.println(output);
							return;
						} catch (SystemException e) {
							e.printStackTrace();
							output = "SystemException happens: " + e.toString();
							System.out.println(output);
							return;
						}
						if (result == false){
							output += "Enlist onePhaseXAResource failed. ";
							return;
						}
						output = proxy2.invoke(Integer.parseInt(number+"01"),"");
						if (!output.contains("failed"))
							output = "Get response: " + output + ".";
						//kill server2
						try{
						callServlet("SuicideServlet"+"02",BASE_URL2);
						}catch (SocketException e){
							System.out.println("Get expected exception " + e.toString() 
									+ " when killing server2");
						}
						//kill server1
						//Call the SuicideServlet to kill myself
						//In this way "Setting state from RECOVERING to ACTIVE"
						//will show up in the trace
						try{
							callServlet("SuicideServlet"+"01",BASE_URL);
						}catch (SocketException e){
							System.out.println("Get expected exception " + e.toString() 
									+ " when killing myself");
						}
						//Runtime.getRuntime().halt(0);
						output += " Test passed.";
						userTransaction.commit();
					}catch(java.lang.Exception e){
						e.printStackTrace();
						output += " Exception happens: " + e.toString() + ". Test failed.";
					}
					break;
				case 3031:
					try {
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
							onePhaseXAResource.setCommitAction(XAResourceImpl.DIE);//die on commit
							//onePhaseXAResource.setExpectedDirection(DIRECTION_ROLLBACK);
							result = TM.getTransaction().enlistResource(onePhaseXAResource);
						} catch (IllegalStateException e) {
							e.printStackTrace();
							output = "IllegalStateException happens: " + e.toString();
							System.out.println(output);
							return;
						} catch (RollbackException e) {
							e.printStackTrace();
							output = "RollbackException happens: " + e.toString();
							System.out.println(output);
							return;
						} catch (SystemException e) {
							e.printStackTrace();
							output = "SystemException happens: " + e.toString();
							System.out.println(output);
							return;
						}
						if (result == false){
							output += "Enlist onePhaseXAResource failed. ";
							return;
						}
						output = proxy2.invoke(Integer.parseInt(number+"01"),"");
						if (!output.contains("failed"))
							output = "Get response: " + output + ".";
						output += " Test passed.";
						userTransaction.commit();
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
			} catch (Throwable t) {
				// TODO Auto-generated catch block
				t.printStackTrace();
			}
		System.out.println("end dispatch " + output);
	}

	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
	}
	
	
	public String callServlet(String method,String endpointUrl) throws IOException,MalformedURLException
	{
		URL url = new URL(endpointUrl+"/recoveryServer/SuicideServlet");
		InputStream is = url.openConnection().getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		String line = null;
		StringBuffer sb = new StringBuffer();
		
		while ((line = br.readLine()) != null) {
			sb.append(line);
		}
	
		return sb.toString();
	}
}
