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
package com.ibm.ws.wsat.simpleserver.server;

import java.io.Serializable;

import javax.jws.WebService;
import javax.transaction.Status;
import javax.transaction.xa.XAException;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

@WebService(wsdlLocation = "WEB-INF/wsdl/WSATSimpleService.wsdl")
public class WSATSimple {

	public String echo(String s) {
		System.out.println("========== echo("+ s +") Start... ==========");
		return "Hi " + s + ". This is WSATSimpleService.";
	}

	public String getStatus() {
		System.out.println("========== getStatus() Start... ==========");
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();
		int TMstatus = -1;
		try {
			TMstatus = TM.getStatus();
			// System.out.println("WSATSimple Status: " + TMstatus);
			if (TMstatus != Status.STATUS_ACTIVE)
				System.out.println("Status not expected! Expected: "
						+ Status.STATUS_ACTIVE + " Actual: " + TMstatus + ".");
		} catch (Exception e) {
			return "Exception happens in getStatus: " + e.toString() +". Please check the web service provider.";
		}
		return "Transaction Manager Status: "
				+ (TMstatus == Status.STATUS_ACTIVE ? "ACTIVE." : TMstatus + ".");
	}

	public String enlistOneXAResource(String vote, int expectedDirection, boolean clearXAResource) {
		System.out.println("========== enlistOneXAResource(" + vote + ", " + expectedDirection + ") Start... ==========");
		if (clearXAResource){
			System.out.println("Clear XAResource in the second call.");
			XAResourceImpl.clear();
		}else {
			System.out.println("Do not clear XAResource in the second call.");
		}
		boolean result = false;
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();
		
		int TMstatus = -1;
		try {
			TMstatus = TM.getStatus();
			if (TMstatus != Status.STATUS_ACTIVE){
				System.out.println("Status not expected! Expected: "
						+ Status.STATUS_ACTIVE + "  Actual: " + TMstatus + ".");
			} else {
				System.out.println("Status is expected! Expected: " + TMstatus);
			}
		} catch (Exception e) {
			return "Exception happens when checking the status of Transaction Manager: "
					+ e.toString() +". Please check the web service provider.";
		}

		int prepareAction = 0;
		if (vote.equals("rollback")) {
			prepareAction = XAException.XA_RBROLLBACK;
		} else if (vote.equals("readonly")) {
			prepareAction = XAException.XA_RDONLY;
		}
		System.out.println("Vote in prepareAction: " + vote);
		System.out.println("prepareAction is set: " + prepareAction);

		final Serializable xaResInfo = XAResourceInfoFactory
				.getXAResourceInfo(0);
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
			result = TM.enlist(xaRes, recoveryId);
		} catch (Exception e) {
			return "Exception happens when enlisting XAResource: " + e.toString() 
					+ ". Please check the web service provider.";
		}
		return "Enlist XAResource voting '" + vote + (result ? "' successful" : " failed")
				+ "; Transaction Manager Status: "
				+ (TMstatus == Status.STATUS_ACTIVE ? "ACTIVE." : TMstatus  + ".");
	}

	public String enlistTwoXAResources(String vote1, String vote2, int expectedDirection, boolean clearXAResource) {
		System.out.println("========== enlistTwoXAResources(" + vote1 + ", " + vote2 + ", " + expectedDirection + ") Starting... ==========");
		if (clearXAResource){
			System.out.println("Clear XAResource in the second call.");
			XAResourceImpl.clear();
		}else {
			System.out.println("Do not clear XAResource in the second call.");
		}
		boolean result1 = false, result2 = false;
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();		
		int TMstatus = -1;
		try {
			TMstatus = TM.getStatus();
			if (TMstatus != Status.STATUS_ACTIVE) {
				System.out.println("Status not expected! Expected: "
						+ Status.STATUS_ACTIVE + "  Actual: " + TMstatus + ".");
			} else {
				System.out.println("Status is expected! Expected: " + TMstatus);
			}
		} catch (Exception e) {
			return "Exception happens when checking the status of Transaction Manager: "
					+ e.toString() +". Please check the web service provider.";
		}

		int prepareAction1 = 0;
		if (vote1.equals("rollback")) {
			prepareAction1 = XAException.XA_RBROLLBACK;
			
		} else if (vote1.equals("readonly")) {
			prepareAction1 = XAException.XA_RDONLY;
		}
		System.out.println("Vote1 in prepareAction: " + vote1);
		System.out.println("prepareAction1 is set: " + prepareAction1);

		int prepareAction2 = 0;
		if (vote2.equals("rollback")) {
			prepareAction2 = XAException.XA_RBROLLBACK;
		} else if (vote2.equals("readonly")) {
			prepareAction2 = XAException.XA_RDONLY;
		}
		System.out.println("Vote2 in prepareAction: " + vote2);
		System.out.println("prepareAction2 is set: " + prepareAction2);

		final Serializable xaResInfo1 = XAResourceInfoFactory
				.getXAResourceInfo(0);
		final Serializable xaResInfo2 = XAResourceInfoFactory
				.getXAResourceInfo(1);
		XAResourceImpl xaRes1, xaRes2;
		try {
			if (prepareAction1 == 0) {
				xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo1);
			} else {
				xaRes1 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo1)
						.setPrepareAction(prepareAction1);
			}

			if (prepareAction2 == 0) {
				xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo2);
			} else {
				xaRes2 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo2)
						.setPrepareAction(prepareAction2);
			}

			final int recoveryId1 = TM.registerResourceInfo("xaResInfo1",
					xaResInfo1);
			final int recoveryId2 = TM.registerResourceInfo("xaResInfo2",
					xaResInfo2);
			xaRes1.setExpectedDirection(expectedDirection);
			xaRes2.setExpectedDirection(expectedDirection);
			result1 = TM.enlist(xaRes1, recoveryId1);
			result2 = TM.enlist(xaRes2, recoveryId2);
		} catch (Exception e) {
			return "Exception happens when enlisting XAResource: " + e.toString() 
					+ ". Please check the web service provider.";
		}
		return "Enlist XAResource1 voting '" + vote1 + (result1 ? "' successful" : " failed")
				+ "; Enlist XAResource2 voting '" + vote2 + (result2 ? "' successful" : " failed")
				+ "; Transaction Manager Status: "
				+ (TMstatus == Status.STATUS_ACTIVE ? "ACTIVE." : TMstatus + ".");
	}
	
	public String sleep(int seconds, int expectedDirection) {
		System.out.println("========== enlistTwoXAResources(" + seconds + ", " + expectedDirection + ") Starting... ==========");
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();
		int TMstatus = -1;
		try {
			TMstatus = TM.getStatus();
			if (TMstatus != Status.STATUS_ACTIVE)
				System.out.println("Status not expected! Expected: "
						+ Status.STATUS_ACTIVE + "  Actual: " + TMstatus + ".");
		} catch (Exception e) {
			return "Exception happens when checking the status of Transaction Manager: "
					+ e.toString() +". Please check the web service provider.";
		}
		boolean result1 = false, result2 = false;
		final Serializable xaResInfo1 = XAResourceInfoFactory
				.getXAResourceInfo(0);
		final Serializable xaResInfo2 = XAResourceInfoFactory
				.getXAResourceInfo(1);
		XAResourceImpl xaRes1, xaRes2;
		try {
				xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo1);
				xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo2);

			final int recoveryId1 = TM.registerResourceInfo("xaResInfo1",
					xaResInfo1);
			final int recoveryId2 = TM.registerResourceInfo("xaResInfo2",
					xaResInfo2);
			xaRes1.setExpectedDirection(expectedDirection);
			xaRes2.setExpectedDirection(expectedDirection);
			result1 = TM.enlist(xaRes1, recoveryId1);
			result2 = TM.enlist(xaRes2, recoveryId2);
			System.out.println(">>>>>>>>>Thread is hanging for " + seconds + "seconds!");
		  	Thread.sleep(seconds * 1000);
		  	System.out.println(">>>>>>>>>Woken up!");
		} catch (Exception e) {
			System.out.println("Exception happens in sleep : " + e.toString());
			return "Exception happens in sleep : " + e.toString();
		}
		return "Response from Sleep method. Enlist XAResource1" + (result1 ? " successful" : " failed")
				+ "; Enlist XAResource2" + (result2 ? " successful" : " failed")
				+ "; Transaction Manager Status: "
				+ (TMstatus == Status.STATUS_ACTIVE ? "ACTIVE." : TMstatus + ".");
	}
}
