/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

import javax.jws.WebService;
import javax.transaction.Status;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.tx.jta.ut.util.TxTestUtils;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

@WebService(wsdlLocation = "WEB-INF/wsdl/RecoveryService.wsdl")
public class Recovery {

	//	These methods return KillProfile arrays which allow test resources to be setup to kill particular test servers 
	private KillProfile[] RESOURCE_2_KILLS_OTHER(String url) {return new KillProfile[] {null, new KillProfile(DoomedServer.OTHER, url)};};
	private KillProfile[] RESOURCE_2_KILLS_BOTH(String url) {return new KillProfile[] {null, new KillProfile(DoomedServer.BOTH, url)};};
	private KillProfile[] RESOURCE_1_KILLS_OTHER(String url) {return new KillProfile[] {new KillProfile(DoomedServer.OTHER, url)};};
	private KillProfile[] RESOURCE_1_KILLS_BOTH(String url) {return new KillProfile[] {new KillProfile(DoomedServer.BOTH, url)};};

	private static int EXPECT_ROLLBACK = XAResourceImpl.DIRECTION_ROLLBACK;
	private static int EXPECT_COMMIT = XAResourceImpl.DIRECTION_COMMIT;
	
	final ExtendedTransactionManager TM = TransactionManagerFactory.getTransactionManager();

	public String invoke(int testNumber, String url) throws IOException {
		String result = null;
		System.out.println("============RecoveryService test number "
				+ testNumber+" ("+decodeTestNumber(testNumber) + ")==========");

		TxTestUtils.setTestResourcesFile();

		switch (testNumber) {
		case 1:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.NORMAL, ResourceAction.NORMAL);
			XAResourceImpl.dumpState();
	        Runtime.getRuntime().halt(0);
	        break;
		case 2:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.KILL_ON_PREPARE, ResourceAction.NORMAL);
			break;
		case 3:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.NORMAL, ResourceAction.KILL_ON_PREPARE);
			break;
		case 4:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 5:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.NORMAL, ResourceAction.KILL_ON_ROLLBACK);
			break;
		case 6:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.NORMAL, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.ROLLBACK_ON_PREPARE);
			break;
		case 7:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.NORMAL);
			break;
		case 8:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.NORMAL, ResourceAction.KILL_ON_COMMIT);
			break;
		case 9:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 10:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.NORMAL, ResourceAction.KILL_ON_ROLLBACK);
			break;
		case 11:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.HEURISTIC_ROLLBACK_ON_COMMIT);
			break;
		case 12:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.HEURISTIC_MIXED_ON_COMMIT);
			break;
		case 13:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.HEURISTIC_COMMIT_ON_COMMIT);
			break;
		case 14:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.HEURISTIC_HAZARD_ON_COMMIT);
			break;
		case 15:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.HEURISTIC_ROLLBACK_ON_ROLLBACK);
			break;
		case 16:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.HEURISTIC_COMMIT_ON_ROLLBACK);
			break;
		case 17:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.HEURISTIC_MIXED_ON_ROLLBACK);
			break;
		case 18:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.HEURISTIC_HAZARD_ON_ROLLBACK);
			break;
		case 37:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.RMFAIL_ON_NTH_RECOVER);
			break;
		case 38:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.RMERR_ON_NTH_RECOVER);
			break;
		case 39:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.INVAL_ON_NTH_RECOVER);
			break;
		case 40:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.PROTO_ON_NTH_RECOVER);
			break;
		case 41:
	        result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.KILL_ON_RECOVERY);
			break;
		case 42:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK_AND_RECOVERY, ResourceAction.NORMAL);
			break;
		case 43:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.KILL_ON_PREPARE, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL);
			break;
		case 44:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL);
			break;
		case 45:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.RMFAIL_ON_NTH_RECOVER, ResourceAction.RMFAIL_ON_NTH_RECOVER);
			break;
		case 46:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.KILL_ON_PREPARE, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL);
			break;
		case 47:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL, ResourceAction.NORMAL);
			break;
		case 48:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.KILL_ON_PREPARE, ResourceAction.RUNTIME_EXCEPTION_ON_RECOVER_RETRY, ResourceAction.NORMAL);
			break;
		case 49:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_PREPARE, ResourceAction.KILL_ON_COMMIT);
			break;
		case 10101:
		case 10201:
		case 10301:
		case 20101:
		case 20102:
		case 20201:
		case 20202:
		case 20301:
		case 20302:
		case 30101:
		case 30201:
		case 30301:
		case 40101:
		case 40201:
		case 40301:
		case 50102:
		case 50202:
		case 50302:
		case 60102:
		case 60202:
		case 60302:
		case 110101:
		case 110201:
		case 110301:
		case 120102:
		case 120202:
		case 120302:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.NORMAL, ResourceAction.NORMAL);
			break;
		case 10102:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.NORMAL, ResourceAction.NORMAL);
			callServlet("SuicideServlet"+"01",url);
			break;
		case 10202:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.NORMAL, ResourceAction.NORMAL);
			XAResourceImpl.dumpState();
			Runtime.getRuntime().halt(0);
			break;
		case 10302:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.NORMAL, ResourceAction.NORMAL);
			System.out.println("First Kill the other server '" + url + "'");
			try{
				callServlet("SuicideServlet"+"01",url);
			}catch (SocketException e){
				System.out.println("Get expected exception " + e.toString() 
						+ ". Continue to kill myself.");
			}
			XAResourceImpl.dumpState();
			System.out.println("Then Kill myself.");
			Runtime.getRuntime().halt(0);
			break;
		case 30102:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_2_KILLS_OTHER(url), ResourceAction.NORMAL, ResourceAction.KILL_ON_PREPARE);
			break;
		case 30202:
		case 50101:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.NORMAL, ResourceAction.KILL_ON_PREPARE);
			break;
		case 30302:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_2_KILLS_BOTH(url), ResourceAction.NORMAL, ResourceAction.KILL_ON_PREPARE);
			break;
		case 40102:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_1_KILLS_OTHER(url), ResourceAction.KILL_ON_PREPARE, ResourceAction.NORMAL);
			break;
		case 40202:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.KILL_ON_PREPARE, ResourceAction.NORMAL);
			break;
		case 40302:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_1_KILLS_BOTH(url), ResourceAction.KILL_ON_PREPARE, ResourceAction.NORMAL);
			break;
		case 50201:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_2_KILLS_OTHER(url), ResourceAction.NORMAL, ResourceAction.KILL_ON_PREPARE);
			break;
		case 50301:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_2_KILLS_BOTH(url), ResourceAction.NORMAL, ResourceAction.KILL_ON_PREPARE);
			break;
		case 60101:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.KILL_ON_PREPARE, ResourceAction.NORMAL);
			break;
		case 60201:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_1_KILLS_OTHER(url), ResourceAction.KILL_ON_PREPARE, ResourceAction.NORMAL);
			break;
		case 60301:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_1_KILLS_BOTH(url), ResourceAction.KILL_ON_PREPARE, ResourceAction.NORMAL);
			break;
		case 70101:
		case 70201:
		case 70301:
		case 80101:
		case 80201:
		case 80301:
		case 90102:
		case 90202:
		case 90302:
		case 100102:
		case 100202:
		case 100302:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.NORMAL, ResourceAction.NORMAL);
			break;	
		case 70102:
			result = enlistResources(EXPECT_COMMIT, RESOURCE_2_KILLS_OTHER(url), ResourceAction.NORMAL, ResourceAction.KILL_ON_COMMIT);
			break;
		case 70202:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.NORMAL, ResourceAction.KILL_ON_COMMIT);
			break;
		case 70302:
			result = enlistResources(EXPECT_COMMIT, RESOURCE_2_KILLS_BOTH(url), ResourceAction.NORMAL, ResourceAction.KILL_ON_COMMIT);
			break;
		case 80102:
			result = enlistResources(EXPECT_COMMIT, RESOURCE_1_KILLS_OTHER(url), ResourceAction.KILL_ON_COMMIT, ResourceAction.NORMAL);
			break;
		case 80202:
		case 100101:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.NORMAL);
			break;
		case 80302:
			result = enlistResources(EXPECT_COMMIT, RESOURCE_1_KILLS_BOTH(url), ResourceAction.KILL_ON_COMMIT, ResourceAction.NORMAL);
			break;
		case 90101:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.NORMAL, ResourceAction.KILL_ON_COMMIT);
			break;
		case 90201:
			result = enlistResources(EXPECT_COMMIT, RESOURCE_2_KILLS_OTHER(url), ResourceAction.NORMAL, ResourceAction.KILL_ON_COMMIT);
			break;
		case 90301:
			result = enlistResources(EXPECT_COMMIT, RESOURCE_2_KILLS_BOTH(url), ResourceAction.NORMAL, ResourceAction.KILL_ON_COMMIT);
			break;
		case 100201:
			result = enlistResources(EXPECT_COMMIT, RESOURCE_1_KILLS_OTHER(url), ResourceAction.KILL_ON_COMMIT, ResourceAction.NORMAL);
			break;
		case 100301:
			result = enlistResources(EXPECT_COMMIT, RESOURCE_1_KILLS_BOTH(url), ResourceAction.KILL_ON_COMMIT, ResourceAction.NORMAL);
			break;
		case 110102:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_2_KILLS_OTHER(url), ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 110202:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 120101:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 110302:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_2_KILLS_BOTH(url), ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 120201:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_2_KILLS_OTHER(url), ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK);
			break;
		case 120301:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_2_KILLS_BOTH(url), ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK);
			break;
		case 130101:
		case 130201:
		case 130301:
		case 140101:
		case 140201:
		case 140301:
		case 150101:
		case 150201:
		case 150301:
		case 160101:
		case 160201:
		case 160301:
			result = "No resource to enlist";
			break;
		case 130102:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_1_KILLS_OTHER(url), ResourceAction.KILL_ON_PREPARE, ResourceAction.NORMAL);
			break;
		case 130202:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.KILL_ON_PREPARE, ResourceAction.NORMAL);
			break;
		case 130302:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_1_KILLS_BOTH(url), ResourceAction.KILL_ON_PREPARE, ResourceAction.NORMAL);
			break;
		case 140102:
			result = enlistResources(EXPECT_COMMIT, RESOURCE_1_KILLS_OTHER(url), ResourceAction.KILL_ON_COMMIT, ResourceAction.NORMAL);
			break;
		case 140202:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.KILL_ON_COMMIT, ResourceAction.NORMAL);
			break;
		case 140302:
			result = enlistResources(EXPECT_COMMIT, RESOURCE_1_KILLS_BOTH(url), ResourceAction.KILL_ON_COMMIT, ResourceAction.NORMAL);
			break;
		case 150102:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_1_KILLS_OTHER(url), ResourceAction.KILL_ON_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 150202:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 150302:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_1_KILLS_BOTH(url), ResourceAction.KILL_ON_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 160102:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_2_KILLS_OTHER(url), ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 160202:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 160302:
			result = enlistResources(EXPECT_ROLLBACK, RESOURCE_2_KILLS_BOTH(url), ResourceAction.ROLLBACK_ON_PREPARE, ResourceAction.KILL_ON_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 301101:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.NORMAL);
			callServlet("SuicideServlet"+"01",url);
			break;
		case 301201:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.NORMAL);
			XAResourceImpl.dumpState();
	        Runtime.getRuntime().halt(0);
			break;
		case 301301:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.NORMAL);
			try{
				callServlet("SuicideServlet"+"01",url);
			}catch (SocketException e){
				System.out.println("Get expected exception " + e.toString() 
						+ ". Continue to kill myself.");
			}
			XAResourceImpl.dumpState();
	        Runtime.getRuntime().halt(0);
			break;
		case 302101:
		case 303101:
			result = enlistResources(EXPECT_ROLLBACK, ResourceAction.NORMAL);
			break;
		case 302201:
		case 302301:
			result = enlistResources(EXPECT_COMMIT, ResourceAction.NORMAL);
			break;
		}

		return "Invoke completed. " + result;
	}
	

	public String callServlet(String method, String endpointUrl) throws IOException,MalformedURLException {
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

	private String enlistResources(int expectedDirection, ResourceAction... resourceActions) {
		return enlistResources(expectedDirection, null, resourceActions);
	}

	private String enlistResources(int expectedDirection, KillProfile[] killProfiles, ResourceAction... resourceActions) {

		int TMstatus = -1;
		int resourceCount = 0;

		final StringBuffer ret = new StringBuffer();
		try {
			TMstatus = TM.getStatus();

			for (ResourceAction ra : resourceActions) {
				final Serializable xaResInfo = XAResourceInfoFactory
						.getXAResourceInfo(resourceCount);

				final KillProfile kp;
				if (killProfiles == null || resourceCount >= killProfiles.length) {
					kp = null;
				} else {
					kp = killProfiles[resourceCount];
				}

				final XAResourceImpl xaRes = (XAResourceImpl) setupResource(xaResInfo, ra, kp, expectedDirection);

				final int recoveryId = TM.registerResourceInfo(XAResourceInfoFactory.filter, xaResInfo);

				final boolean result = TM.enlist(xaRes, recoveryId);
				
				ret.append("Enlist XAResource ").append(xaRes).append(" ").append(result ? "successful" : "failed").append("; ");
				
				resourceCount++;
			}
		} catch (Exception e) {
			return "Exception happens when enlisting XAResource: " + e.toString() 
					+ ". Please check the web service provider.";
		}

		ret.append("Transaction Manager Status: ").append(TMstatus == Status.STATUS_ACTIVE ? "ACTIVE." : TMstatus + ".");
		return ret.toString();
	}

	private XAResource setupResource(final Serializable xaResInfo, final ResourceAction ra, KillProfile kp, final int expectedDirection)
			throws XAResourceNotAvailableException, MalformedURLException {
		
		final XAResourceImpl xaRes = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo).setExpectedDirection(expectedDirection);

		final int action = ra.getAction();

		if (action != 0) {
			switch (ra.getStep()) {
			case PREPARE:
				xaRes.setPrepareAction(action);
				break;
			case COMMIT:
				xaRes.setCommitAction(action);
				break;
			case ROLLBACK:
				xaRes.setRollbackAction(action);
				break;
			case ROLLBACK_AND_RECOVERY:
				xaRes.setRollbackAction(action).setRecoverAction(action).setRecoverRepeatCount(1);
				break;
			case RECOVER_5_TIMES:
				xaRes.setRecoverRepeatCount(5).setRecoverAction(action);
				break;
			case RECOVER_RETRY:
				xaRes.setRecoverRepeatCount(1).setRecoverAction(action);
				break;
			case RECOVERY:
				xaRes.setRecoverAction(action).setRecoverRepeatCount(1);
				break;
			default:
				break;
			}

			// If this resource is the killer, tell it which servers to kill
			if (action == XAResourceImpl.DIE && kp != null) {
				if (kp.getWhoDies() == DoomedServer.BOTH || kp.getWhoDies() == DoomedServer.OTHER) {
					xaRes.setDoomedServer(new URL(kp.getUrl()+"/recoveryServer/SuicideServlet"));
				}

				xaRes.setCommitSuicide(kp.getWhoDies() == DoomedServer.BOTH || kp.getWhoDies() == DoomedServer.SELF);
			}
		}

		return xaRes;
	}

	private enum Step {
		NONE,
		PREPARE,
		COMMIT,
		ROLLBACK,
		ROLLBACK_AND_RECOVERY,
		RECOVER_5_TIMES,
		RECOVER_RETRY,
		RECOVERY
	}

	private enum ResourceAction {
		NORMAL(0, Step.NONE),
		KILL_ON_PREPARE(XAResourceImpl.DIE, Step.PREPARE),
		ROLLBACK_ON_PREPARE(XAException.XA_RBROLLBACK, Step.PREPARE),
		HEURISTIC_ROLLBACK_ON_COMMIT(XAException.XA_HEURRB, Step.COMMIT),
		HEURISTIC_MIXED_ON_COMMIT(XAException.XA_HEURMIX, Step.COMMIT),
		HEURISTIC_COMMIT_ON_COMMIT(XAException.XA_HEURCOM, Step.COMMIT),
		HEURISTIC_HAZARD_ON_COMMIT(XAException.XA_HEURHAZ, Step.COMMIT),
		HEURISTIC_ROLLBACK_ON_ROLLBACK(XAException.XA_HEURRB, Step.ROLLBACK),
		HEURISTIC_COMMIT_ON_ROLLBACK(XAException.XA_HEURCOM, Step.ROLLBACK),
		HEURISTIC_MIXED_ON_ROLLBACK(XAException.XA_HEURMIX, Step.ROLLBACK),
		HEURISTIC_HAZARD_ON_ROLLBACK(XAException.XA_HEURHAZ, Step.ROLLBACK),
		KILL_ON_COMMIT(XAResourceImpl.DIE, Step.COMMIT),
		
		RUNTIME_EXCEPTION_ON_RECOVER_RETRY(XAResourceImpl.RUNTIME_EXCEPTION, Step.RECOVER_RETRY),
		RMERR_ON_NTH_RECOVER(XAException.XAER_RMERR, Step.RECOVER_5_TIMES),
		INVAL_ON_NTH_RECOVER(XAException.XAER_INVAL, Step.RECOVER_5_TIMES),
		PROTO_ON_NTH_RECOVER(XAException.XAER_PROTO, Step.RECOVER_5_TIMES),
		RMFAIL_ON_NTH_RECOVER(XAException.XAER_RMFAIL, Step.RECOVER_5_TIMES),
		KILL_ON_RECOVERY(XAResourceImpl.DIE, Step.RECOVERY),
		KILL_ON_ROLLBACK(XAResourceImpl.DIE, Step.ROLLBACK),
		KILL_ON_ROLLBACK_AND_RECOVERY(XAResourceImpl.DIE, Step.ROLLBACK_AND_RECOVERY);

		private int _action;
		private Step _step;

		ResourceAction(int action, Step step) {
			_action = action;
			_step = step;
		}

		public int getAction() {
			return _action;
		}

		public Step getStep() {
			return _step;
		}
    }

	private enum DoomedServer {
		BOTH,
		SELF,
		OTHER
	}
	
	private class KillProfile {
		private DoomedServer _whoDies; // null = both, TRUE = me, FALSE = other guy
		private String _url; // other guy
		
		KillProfile(DoomedServer whoDies, String url) {
			_whoDies = whoDies;
			_url = url;
		}

		/**
		 * @return the _whoDies
		 */
		public DoomedServer getWhoDies() {
			return _whoDies;
		}

		/**
		 * @return the _url
		 */
		public String getUrl() {
			return _url;
		}
	}
	
	private String decodeTestNumber(int testNumber) {
		if (testNumber < 100) {
			return String.format("WSTXREC%03dFVT", testNumber);
		} else {
			int test = testNumber / 10000;
			int abc = (testNumber % 10000) / 100;
			int server = testNumber % 100;

			return String.format("WSTXMPR%03dFVT", test) + (abc == 1 ? "A" : abc == 2 ? "B" : "C") + " " + (server == 1 ? "root" : "subordinate") + " server";
		}
	}
}
