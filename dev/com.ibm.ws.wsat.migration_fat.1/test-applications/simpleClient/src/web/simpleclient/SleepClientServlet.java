/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package web.simpleclient;

import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.xml.ws.BindingProvider;

import org.junit.Test;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.TxTestUtils;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@WebServlet({ "/SleepClientServlet" })
public class SleepClientServlet extends FATServlet {
	private static final long serialVersionUID = 1L;
	
	private static final float DEFAULT_TIMEOUT = 40; // seconds
	
	private static final int NO_SLEEP = 0;
	private static final int LONG_SLEEP = 1;
	private static final int SHORT_SLEEP = 2;
	
	protected static final String BASE_URL = "http://localhost:" + System.getProperty("bvt.prop.HTTP_secondary");
	protected static final String BASE_URL2 = "http://localhost:" + System.getProperty("bvt.prop.HTTP_tertiary");

	private Instant tranEndTime;
	private int timeout = (int) DEFAULT_TIMEOUT;
	private float perfFactor;

	private static final String[] noXARes = new String[]{};
	private static final String[] TwoXARes = new String[]{"" , ""};
	@Resource
	UserTransaction ut;

	@Override
    protected void before(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
    	XAResourceImpl.clear();
    	
    	final String pf = request.getParameter("perfFactor");
    	if (pf != null) {
    		perfFactor = Float.parseFloat(pf);
    		
    		timeout = (int)((float)DEFAULT_TIMEOUT / perfFactor);
    		System.out.println("Transaction timeout will be "+timeout);
    	}
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.xa.XAException", "javax.transaction.RollbackException", "javax.transaction.SystemException" })
    public void testWSATRE094FVT() {
    	assertTrue(execute(BASE_URL, noXARes, noXARes, LONG_SLEEP, NO_SLEEP, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE095FVT() {
    	assertTrue(execute(BASE_URL, noXARes, noXARes, SHORT_SLEEP, NO_SLEEP, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.SystemException" })

    public void testWSATRE096FVT() {
    	assertTrue(execute(BASE_URL, noXARes, noXARes, LONG_SLEEP, NO_SLEEP, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE097FVT() {
    	assertTrue(execute(BASE_URL, noXARes, noXARes, SHORT_SLEEP, NO_SLEEP, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.SystemException" })
    public void testWSATRE098FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXARes, LONG_SLEEP, NO_SLEEP, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE099FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXARes, SHORT_SLEEP, NO_SLEEP, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    @AllowedFFDC(value = { "javax.transaction.SystemException" })
    public void testWSATRE100FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXARes, LONG_SLEEP, NO_SLEEP, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE101FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXARes, SHORT_SLEEP, NO_SLEEP, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE102FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXARes, NO_SLEEP, LONG_SLEEP, "commit", XAResourceImpl.DIRECTION_ROLLBACK, "RollbackException").contains("Test passed"));
    }

    @Test
    public void testWSATRE103FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXARes, NO_SLEEP, SHORT_SLEEP, "commit", XAResourceImpl.DIRECTION_COMMIT, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE0104FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXARes, NO_SLEEP, LONG_SLEEP, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    @Test
    public void testWSATRE105FVT() {
    	assertTrue(execute(BASE_URL, TwoXARes, TwoXARes, NO_SLEEP, SHORT_SLEEP, "rollback", XAResourceImpl.DIRECTION_ROLLBACK, "NoException").contains("Test passed"));
    }

    private String execute(String BASE_URL, String[] CoordinatorXAResouces, String[] ParticipantXAResouces,
    		int sleepTimeClient, int sleepTimeServer, String commitRollback, int expectedDirection, String expectResult) {
    	return execute(BASE_URL, "", CoordinatorXAResouces, ParticipantXAResouces, 
    			new String[]{}, new String[]{}, "", sleepTimeClient, sleepTimeServer, commitRollback, expectedDirection, expectResult);
    }

	private String execute(String BASE_URL, String BASE_URL2, String[] CoordinatorXAResouces, String[] ParticipantXAResouces,
			String[] ParticipantXAResoucesInSecondCall, String[] CoordinatorXAResoucesAfterWSCall, String setRollbackOnly, 
			int sleepTimeClient, int sleepTimeServer, String commitRollback, int expectedDirection, String expectResult) {
		int UTstatus = -1;
		int UTexpectedStatus = -1;
		String output = "";
		try {
			ut.setTransactionTimeout(timeout);
			ut.begin();

			tranEndTime = Instant.now().plusSeconds((long)timeout);
			System.out.println("Transaction is due to timeout at " + tranEndTime.toString());

			// Check User Transaction Status
			UTexpectedStatus = Status.STATUS_ACTIVE;
			UTstatus = ut.getStatus();
			if (UTstatus != UTexpectedStatus){
				String errorMessage = "1: UserTransaction Status not expected! Expected: "
						+ UTexpectedStatus + "  Actual: " + UTstatus;
				System.out.println(errorMessage);
				return errorMessage;
			}
			
			if(setRollbackOnly.equals("setRollbackOnlyBeforeWSCall")){
				ut.setRollbackOnly();
				UTexpectedStatus = Status.STATUS_MARKED_ROLLBACK;
				UTstatus = ut.getStatus();
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
			
			final Duration serverSleepTime = calculateSleepTime(sleepTimeServer);

			String output1 = "", output2 = "No second web service call.";
			output1 = " Get response: " + callWebservice(BASE_URL, ParticipantXAResouces, expectedDirection, (int)serverSleepTime.getSeconds(), true) + ".";
			System.out.println("execute callWebservice() 1: " + output1);
			
			if (BASE_URL2 != null && !BASE_URL2.equals("")){
				UTstatus = ut.getStatus();
				if (UTstatus != UTexpectedStatus) {
					String errorMessage = "3: UserTransaction Status not expected! Expected: "
							+ UTexpectedStatus + "  Actual: " + UTstatus;
					System.out.println(errorMessage);
					return errorMessage;
				}
				output2 = " Get response in the second call: "
						+ callWebservice(BASE_URL2, ParticipantXAResoucesInSecondCall, expectedDirection, (int)serverSleepTime.getSeconds(), !BASE_URL2.equals(BASE_URL)) + ".";
				System.out.println("execute callWebservice() 2: " + output2);
			}
			
			output += (output1 + output2); 
			
			if(setRollbackOnly.equals("setRollbackOnlyAfterWSCall")){
				ut.setRollbackOnly();
				UTexpectedStatus = Status.STATUS_MARKED_ROLLBACK;
				UTstatus = ut.getStatus();
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

			final long sleepTime = calculateSleepTime(sleepTimeClient).getSeconds();
			System.out.println(">>>>>>>>>Client thread is hanging for " + sleepTime + " seconds!");
			Thread.sleep(sleepTime * 1000);
		  	System.out.println(">>>>>>>>>Woken up!");

			// User Transaction Commit / Rollback
			System.out.println("execute commitRollback(): " + commitRollback);
			if (commitRollback.equals("commit")) {
				ut.commit();
			} else if (commitRollback.equals("rollback")) {
				ut.rollback();
			} else return output + " User transaction action error. Test failed.";
			
			if (expectResult.equals("NoException") &&  !output1.contains("failed") && !output2.contains("failed") &&
					output1.contains("Transaction Manager Status: ACTIVE")
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
	
	
	// Sleep tests are specified to either just timeout or just not timeout
	// In the former case we'll sleep for just a bit longer than the remaining time in the tran (min 10s)
	// In the latter we'll sleep for half the remaining time in the tran (max 10s)
	private Duration calculateSleepTime(int type) {
		Duration sleepTime;

		final Duration limit = Duration.ofSeconds(10);
		switch (type) {
		case LONG_SLEEP:
			// 1.5 times the time left in the tran
			sleepTime = Duration.between(Instant.now(), tranEndTime).multipliedBy(3).dividedBy(2);
			if (sleepTime.compareTo(limit) < 0) {
				sleepTime = limit;
			}
			break;
		case SHORT_SLEEP:
			// 0.5 times the time left in the tran
			sleepTime = Duration.between(Instant.now(), tranEndTime).multipliedBy(1).dividedBy(2);
			if (sleepTime.compareTo(limit) > 0) {
				sleepTime = limit;
			}
			break;
		default:
			sleepTime = Duration.ZERO;
			break;
		}
		
		return sleepTime;
	}

	private String callWebservice(String BASE_URL, String[] XAResouces, int expectedDirection, int sleepTimeServer, boolean clearXAResource)
			throws MalformedURLException {
		final int timeout = Math.round(300000f / perfFactor);
		URL wsdlLocation = new URL(BASE_URL
				+ "/simpleService/WSATSimpleService?wsdl");
		WSATSimpleService service = new WSATSimpleService(wsdlLocation);
		WSATSimple proxy = service.getWSATSimplePort();
		BindingProvider bind = (BindingProvider) proxy;
		bind.getRequestContext().put("javax.xml.ws.service.endpoint.address",
				BASE_URL + "/simpleService/WSATSimpleService");
		TxTestUtils.setTimeouts(bind.getRequestContext(), timeout);
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
				final int recoveryId = TM.registerResourceInfo(XAResourceInfoFactory.filter,
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
}
