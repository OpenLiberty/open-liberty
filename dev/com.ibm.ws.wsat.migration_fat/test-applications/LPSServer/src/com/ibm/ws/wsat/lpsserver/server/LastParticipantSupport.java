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
package com.ibm.ws.wsat.lpsserver.server;

import java.io.Serializable;

import javax.jws.WebService;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.tx.jta.ut.util.OnePhaseXAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

@WebService(wsdlLocation = "WEB-INF/wsdl/LPSService.wsdl")
public class LastParticipantSupport {

	public String echo(String s) {
		return "Hi " + s + ". This is LastParticipantSupportService.";
	}

	public String WSTXLPS01FVT(String test) {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result = false;
		try {
			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_COMMIT);
			result = TM.getTransaction().enlistResource(onePhaseXAResource);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS" + test + "FVT: Enlist OnePhaseResource"
				+ (result ? " successful" : " failed");
	}
	
	public String WSTXLPS001FVT() {
		return WSTXLPS01FVT("001");
	}
	
	public String WSTXLPS101FVT() {
		return WSTXLPS01FVT("101");
	}

	
	public String WSTXLPS02FVT(String test) {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result = false;
		try {
			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl()
					.setCommitAction(XAException.XA_RBROLLBACK);
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_COMMIT);
			result = TM.getTransaction().enlistResource(onePhaseXAResource);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS" + test + "FVT: Enlist OnePhaseResource"
				+ (result ? " successful" : " failed");
	}
	
	public String WSTXLPS002FVT() {
		return WSTXLPS02FVT("002");
	}
	
	public String WSTXLPS102FVT() {
		return WSTXLPS02FVT("102");
	}
	

	public String WSTXLPS03FVT(String test) throws Exception {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();
		try {
			XAResourceImpl onePhaseXAResource1 = new OnePhaseXAResourceImpl();
			onePhaseXAResource1.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			TM.getTransaction().enlistResource(onePhaseXAResource1);

			XAResourceImpl onePhaseXAResource2 = new OnePhaseXAResourceImpl();
			onePhaseXAResource2.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			TM.getTransaction().enlistResource(onePhaseXAResource2);
		} catch (IllegalStateException e) {
			System.out.println("Get expected IllegalStateException: " + e.toString() + ".");
			TM.setRollbackOnly();
			//throw e;
			//return "Get expected IllegalStateException: " + e.toString() + ".";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "Do not receive the expected IllegalStateException. Test failed.";
	}
	
	public String WSTXLPS003FVT() throws Exception {
		return WSTXLPS03FVT("003");
	}
	
	public String WSTXLPS103FVT() throws Exception {
		return WSTXLPS03FVT("103");
	}
	
	public String WSTXLPS004FVT() {
		System.out.println("========== LPS Disabled WSTXLPS004FVT start ==========");
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false;
		try {
			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result1 = TM.getTransaction().enlistResource(onePhaseXAResource);

			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			xaRes.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result2 = TM.enlist(xaRes, recoveryId);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS004FVT: Enlist OnePhaseResource"
				+ (result1 ? " successful" : " failed") + "; Enlist XAResource"
				+ (result2 ? " successful" : " failed");
	}

	
	public String WSTXLPS104FVT() {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false;
		try {
			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_COMMIT);
			result1 = TM.getTransaction().enlistResource(onePhaseXAResource);

			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			result2 = TM.enlist(xaRes, recoveryId);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS104FVT: Enlist OnePhaseResource"
				+ (result1 ? " successful" : " failed") + "; Enlist XAResource"
				+ (result2 ? " successful" : " failed");
	}
	
	public String WSTXLPS005FVT() {
		System.out.println("========== LPS Disabled WSTXLPS005FVT start ==========");
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false;
		try {
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			xaRes.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result1 = TM.enlist(xaRes, recoveryId);

			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result2 = TM.getTransaction().enlistResource(onePhaseXAResource);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS005FVT: Enlist XAResource"
				+ (result1 ? " successful" : " failed")
				+ "; Enlist OnePhaseResource"
				+ (result2 ? " successful" : " failed");
	}

	public String WSTXLPS105FVT() {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false;
		try {
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			result1 = TM.enlist(xaRes, recoveryId);

			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_COMMIT);
			result2 = TM.getTransaction().enlistResource(onePhaseXAResource);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS105FVT: Enlist XAResource"
				+ (result1 ? " successful" : " failed")
				+ "; Enlist OnePhaseResource"
				+ (result2 ? " successful" : " failed");
	}

	public String WSTXLPS06FVT(String test) {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false;
		try {
			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_COMMIT);
			result1 = TM.getTransaction().enlistResource(onePhaseXAResource);

			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo)
					.setPrepareAction(XAException.XA_RDONLY);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			xaRes.setExpectedDirection(XAResourceImpl.DIRECTION_COMMIT);
			result2 = TM.enlist(xaRes, recoveryId);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS" + test + "FVT: Enlist OnePhaseResource"
				+ (result1 ? " successful" : " failed")
				+ "; Enlist XAResource voting readonly"
				+ (result2 ? " successful" : " failed");
	}
	
	public String WSTXLPS006FVT() {
		return WSTXLPS06FVT("006");
	}
	
	public String WSTXLPS106FVT() {
		return WSTXLPS06FVT("106");
	}
	

	public String WSTXLPS07FVT(String test) {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false;
		try {
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo)
					.setPrepareAction(XAException.XA_RDONLY);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			result1 = TM.enlist(xaRes, recoveryId);

			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_COMMIT);
			result2 = TM.getTransaction().enlistResource(onePhaseXAResource);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS" + test + "FVT: Enlist XAResource voting readonly"
				+ (result1 ? " successful" : " failed")
				+ "; Enlist OnePhaseResource"
				+ (result2 ? " successful" : " failed");
	}
	
	public String WSTXLPS007FVT() {
		return WSTXLPS07FVT("007");
	}
	
	public String WSTXLPS107FVT() {
		return WSTXLPS07FVT("107");
	}
	

	public String WSTXLPS08FVT(String test) {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false, result3 = false, result4 = false, result5 = false;
		try {
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0), xaResInfo2 = XAResourceInfoFactory
					.getXAResourceInfo(1), xaResInfo3 = XAResourceInfoFactory
					.getXAResourceInfo(2), xaResInfo4 = XAResourceInfoFactory
					.getXAResourceInfo(3);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo)
					.setPrepareAction(XAException.XA_RDONLY);
			XAResourceImpl xaRes2 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo2)
					.setPrepareAction(XAException.XA_RDONLY);
			XAResourceImpl xaRes3 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo3)
					.setPrepareAction(XAException.XA_RDONLY);
			XAResourceImpl xaRes4 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo4)
					.setPrepareAction(XAException.XA_RDONLY);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			final int recoveryId2 = TM.registerResourceInfo("xaResInfo2",
					xaResInfo2);
			final int recoveryId3 = TM.registerResourceInfo("xaResInfo3",
					xaResInfo3);
			final int recoveryId4 = TM.registerResourceInfo("xaResInfo4",
					xaResInfo4);
			result1 = TM.enlist(xaRes, recoveryId);
			result2 = TM.enlist(xaRes2, recoveryId2);
			result3 = TM.enlist(xaRes3, recoveryId3);
			result4 = TM.enlist(xaRes4, recoveryId4);

			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_COMMIT);
			result5 = TM.getTransaction().enlistResource(onePhaseXAResource);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS" + test + "FVT: Enlist XAResource voting readonly"
				+ (result1 ? " successful" : " failed")
				+ "; Enlist XAResource voting readonly"
				+ (result2 ? " successful" : " failed")
				+ "; Enlist XAResource voting readonly"
				+ (result3 ? " successful" : " failed")
				+ "; Enlist XAResource voting readonly"
				+ (result4 ? " successful" : " failed")
				+ "; Enlist OnePhaseResource"
				+ (result5 ? " successful" : " failed");
	}
	
	public String WSTXLPS008FVT() {
		return WSTXLPS08FVT("008");
	}
	
	public String WSTXLPS108FVT() {
		return WSTXLPS08FVT("108");
	}
	

	public String WSTXLPS09FVT(String test) {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false, result3 = false, result4 = false, result5 = false;
		try {
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			final Serializable xaResInfo2 = XAResourceInfoFactory
					.getXAResourceInfo(1);
			final Serializable xaResInfo3 = XAResourceInfoFactory
					.getXAResourceInfo(2);
			final Serializable xaResInfo4 = XAResourceInfoFactory
					.getXAResourceInfo(3);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo)
					.setPrepareAction(XAException.XA_RDONLY);
			XAResourceImpl xaRes2 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo2)
					.setPrepareAction(XAException.XA_RDONLY);
			XAResourceImpl xaRes3 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo3)
					.setPrepareAction(XAException.XA_RDONLY);
			XAResourceImpl xaRes4 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo4)
					.setPrepareAction(XAException.XA_RDONLY);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			final int recoveryId2 = TM.registerResourceInfo("xaResInfo2",
					xaResInfo2);
			final int recoveryId3 = TM.registerResourceInfo("xaResInfo3",
					xaResInfo3);
			final int recoveryId4 = TM.registerResourceInfo("xaResInfo4",
					xaResInfo4);
			result1 = TM.enlist(xaRes, recoveryId);
			result2 = TM.enlist(xaRes2, recoveryId2);

			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_COMMIT);
			result3 = TM.getTransaction().enlistResource(onePhaseXAResource);

			result4 = TM.enlist(xaRes3, recoveryId3);
			result5 = TM.enlist(xaRes4, recoveryId4);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS" + test + "FVT: Enlist XAResource voting readonly"
				+ (result1 ? " successful" : " failed")
				+ "; Enlist XAResource voting readonly"
				+ (result2 ? " successful" : " failed")
				+ "; Enlist OnePhaseResource"
				+ (result3 ? " successful" : " failed")
				+ "; Enlist XAResource voting readonly"
				+ (result4 ? " successful" : " failed")
				+ "; Enlist XAResource voting readonly"
				+ (result5 ? " successful" : " failed");
	}
	
	public String WSTXLPS009FVT() {
		return WSTXLPS09FVT("009");
	}
	
	public String WSTXLPS109FVT() {
		return WSTXLPS09FVT("109");
	}
	

	public String WSTXLPS10FVT(String test) {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false, result3 = false, result4 = false, result5 = false;
		try {
			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_COMMIT);
			result1 = TM.getTransaction().enlistResource(onePhaseXAResource);

			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0), xaResInfo2 = XAResourceInfoFactory
					.getXAResourceInfo(1), xaResInfo3 = XAResourceInfoFactory
					.getXAResourceInfo(2), xaResInfo4 = XAResourceInfoFactory
					.getXAResourceInfo(3);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo)
					.setPrepareAction(XAException.XA_RDONLY);
			XAResourceImpl xaRes2 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo2)
					.setPrepareAction(XAException.XA_RDONLY);
			XAResourceImpl xaRes3 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo3)
					.setPrepareAction(XAException.XA_RDONLY);
			XAResourceImpl xaRes4 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo4)
					.setPrepareAction(XAException.XA_RDONLY);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			final int recoveryId2 = TM.registerResourceInfo("xaResInfo2",
					xaResInfo2);
			final int recoveryId3 = TM.registerResourceInfo("xaResInfo3",
					xaResInfo3);
			final int recoveryId4 = TM.registerResourceInfo("xaResInfo4",
					xaResInfo4);
			result2 = TM.enlist(xaRes, recoveryId);
			result3 = TM.enlist(xaRes2, recoveryId2);
			result4 = TM.enlist(xaRes3, recoveryId3);
			result5 = TM.enlist(xaRes4, recoveryId4);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS" + test + "FVT: Enlist OnePhaseResource"
				+ (result1 ? " successful" : " failed")
				+ "; Enlist XAResource voting readonly"
				+ (result2 ? " successful" : " failed")
				+ "; Enlist XAResource voting readonly"
				+ (result3 ? " successful" : " failed")
				+ "; Enlist XAResource voting readonly"
				+ (result4 ? " successful" : " failed")
				+ "; Enlist XAResource voting readonly"
				+ (result5 ? " successful" : " failed");
	}
	
	public String WSTXLPS010FVT() {
		return WSTXLPS10FVT("010");
	}
	
	public String WSTXLPS110FVT() {
		return WSTXLPS10FVT("110");
	}
	
	public String WSTXLPS011FVT() {
		System.out.println("========== LPS Disabled WSTXLPS011FVT start ==========");
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false, result3 = false, result4 = false;
		try {
			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result1 = TM.getTransaction().enlistResource(onePhaseXAResource);

			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			final Serializable xaResInfo2 = XAResourceInfoFactory
					.getXAResourceInfo(1);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo);
			XAResourceImpl xaRes2 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo2);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			final int recoveryId2 = TM.registerResourceInfo("xaResInfo2",
					xaResInfo2);
			xaRes.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			xaRes2.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result2 = TM.enlist(xaRes, recoveryId);
			result3 = TM.enlist(xaRes2, recoveryId2);
			
			XAResourceImpl onePhaseXAResource2 = new OnePhaseXAResourceImpl();
			onePhaseXAResource2.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result4 = TM.getTransaction().enlistResource(onePhaseXAResource2);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS011FVT: Enlist OnePhaseResource"
				+ (result1 ? " successful" : " failed")
				+ "; Enlist XAResource voting rollback"
				+ (result2 ? " successful" : " failed")
				+ "; Enlist XAResource "
				+ (result3 ? " successful" : " failed")
				+("Enlist OnePhaseResource2"
				+ (result4 ? " successful" : " failed"));
	}

	public String WSTXLPS111FVT() {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false, result3 = false;
		try {
			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result1 = TM.getTransaction().enlistResource(onePhaseXAResource);

			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			final Serializable xaResInfo2 = XAResourceInfoFactory
					.getXAResourceInfo(1);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo)
					.setPrepareAction(XAException.XA_RBROLLBACK);
			XAResourceImpl xaRes2 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo2);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			final int recoveryId2 = TM.registerResourceInfo("xaResInfo2",
					xaResInfo2);
			xaRes.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			xaRes2.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result2 = TM.enlist(xaRes, recoveryId);
			result3 = TM.enlist(xaRes2, recoveryId2);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS111FVT: Enlist OnePhaseResource"
				+ (result1 ? " successful" : " failed")
				+ "; Enlist XAResource voting rollback"
				+ (result2 ? " successful" : " failed")
				+ "; Enlist XAResource "
				+ (result3 ? " successful" : " failed");
	}
	

	public String WSTXLPS112FVT() {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false, result3 = false;
		try {
			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result1 = TM.getTransaction().enlistResource(onePhaseXAResource);

			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			final Serializable xaResInfo2 = XAResourceInfoFactory
					.getXAResourceInfo(1);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo);
			XAResourceImpl xaRes2 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo2)
					.setPrepareAction(XAException.XA_RBROLLBACK);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			final int recoveryId2 = TM.registerResourceInfo("xaResInfo2",
					xaResInfo2);
			xaRes.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			xaRes2.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result2 = TM.enlist(xaRes, recoveryId);
			result3 = TM.enlist(xaRes2, recoveryId2);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS112FVT: Enlist OnePhaseResource"
				+ (result1 ? " successful" : " failed")
				+ "; Enlist XAResource "
				+ (result2 ? " successful" : " failed")
				+ "; Enlist XAResource voting rollback"
				+ (result3 ? " successful" : " failed");
	}

	public String WSTXLPS113FVT() {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false, result2 = false, result3 = false;
		try {
			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			onePhaseXAResource.setCommitAction(XAException.XA_RBROLLBACK);
			result1 = TM.getTransaction().enlistResource(onePhaseXAResource);

			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			final Serializable xaResInfo2 = XAResourceInfoFactory
					.getXAResourceInfo(1);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo);
			XAResourceImpl xaRes2 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo2);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			final int recoveryId2 = TM.registerResourceInfo("xaResInfo2",
					xaResInfo2);
			xaRes.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			xaRes2.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result2 = TM.enlist(xaRes, recoveryId);
			result3 = TM.enlist(xaRes2, recoveryId2);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS113FVT: Enlist OnePhaseResource"
				+ (result1 ? " successful" : " failed")
				+ "; Enlist XAResource"
				+ (result2 ? " successful" : " failed")
				+ "; Enlist XAResource"
				+ (result3 ? " successful" : " failed");
	}

	public String WSTXLPS114FVT() throws IllegalStateException, SystemException{
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();
		try {
			XAResourceImpl onePhaseXAResource1 = new OnePhaseXAResourceImpl();
			onePhaseXAResource1.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			TM.getTransaction().enlistResource(onePhaseXAResource1);

			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			final Serializable xaResInfo2 = XAResourceInfoFactory
					.getXAResourceInfo(1);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo);
			XAResourceImpl xaRes2 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo2);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			final int recoveryId2 = TM.registerResourceInfo("xaResInfo2",
					xaResInfo2);
			xaRes.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			xaRes2.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			TM.enlist(xaRes, recoveryId);
			TM.enlist(xaRes2, recoveryId2);

			XAResourceImpl onePhaseXAResource2 = new OnePhaseXAResourceImpl();
			onePhaseXAResource2.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			TM.getTransaction().enlistResource(onePhaseXAResource2);
		} catch (IllegalStateException e) {
			System.out.println("Get expected IllegalStateException: " + e.toString() + ".");
			TM.setRollbackOnly();
			//throw e;
			//return "Get expected IllegalStateException: " + e.toString() + ".";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		}catch(XAResourceNotAvailableException e){
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "Do not receive the expected IllegalStateException. Test failed.";
	}

	public String WSTXLPS201FVT() {
		XAResourceImpl.clear();
		return "WSTXLPS201FVT";
	}

	public String WSTXLPS202FVT() {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result = false;
		try {
			XAResourceImpl onePhaseXAResource = new OnePhaseXAResourceImpl();
			onePhaseXAResource.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result = TM.getTransaction().enlistResource(onePhaseXAResource);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS202FVT: Enlist OnePhaseResource"
				+ (result ? " successful." : " failed.");
	}

	public String WSTXLPS203FVT() {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false;
		try {
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			result1 = TM.enlist(xaRes, recoveryId);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS203FVT: Enlist XAResource"
				+ (result1 ? " successful" : " failed");
	}

	public String WSTXLPS204FVT() {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false;
		try {
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			xaRes.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result1 = TM.enlist(xaRes, recoveryId);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS204FVT: Enlist XAResource"
				+ (result1 ? " successful" : " failed");
	}

	public String WSTXLPS205FVT() {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false;
		try {
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo)
					.setPrepareAction(XAException.XA_RBROLLBACK);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			xaRes.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result1 = TM.enlist(xaRes, recoveryId);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS205FVT: Enlist XAResource voting rollback "
				+ (result1 ? " successful" : " failed");
	}

	public String WSTXLPS206FVT() {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false;
		try {
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			xaRes.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);
			result1 = TM.enlist(xaRes, recoveryId);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS206FVT: Enlist XAResource voting rollback "
				+ (result1 ? " successful" : " failed");
	}

	public String WSTXLPS207FVT() {
		XAResourceImpl.clear();
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		boolean result1 = false;
		try {
			final Serializable xaResInfo = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo)
					.setPrepareAction(XAException.XA_RDONLY);
			final int recoveryId = TM.registerResourceInfo("xaResInfo",
					xaResInfo);
			result1 = TM.enlist(xaRes, recoveryId);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "IllegalStateException happens: " + e.toString()
					+ " Operation failed.";
		} catch (RollbackException e) {
			e.printStackTrace();
			return "RollbackException happens: " + e.toString()
					+ " Operation failed.";
		} catch (SystemException e) {
			e.printStackTrace();
			return "SystemException happens: " + e.toString()
					+ " Operation failed.";
		} catch (XAResourceNotAvailableException e) {
			e.printStackTrace();
			return "XAResourceNotAvailableException happens: " + e.toString()
					+ " Operation failed.";
		}
		return "WSTXLPS207FVT: Enlist XAResource voting rollback "
				+ (result1 ? " successful" : " failed");
	}
}
