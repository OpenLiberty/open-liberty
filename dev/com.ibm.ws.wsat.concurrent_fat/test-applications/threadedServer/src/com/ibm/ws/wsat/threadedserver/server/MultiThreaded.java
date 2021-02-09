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
package com.ibm.ws.wsat.threadedserver.server;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jws.WebService;

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

@WebService(wsdlLocation = "WEB-INF/wsdl/MultiThreadedService.wsdl")
public class MultiThreaded {
	public boolean clearXAResource() {
		XAResourceImpl.printState();
		return true;
	}
	
	private static AtomicInteger xaresourceindex = new AtomicInteger(0);
	
	public String invoke() {
		boolean result1 = false;

		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();		
		final Serializable xaResInfo1 = XAResourceInfoFactory
				.getXAResourceInfo(xaresourceindex.getAndIncrement());
		XAResourceImpl xaRes1;
		try {
			xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo1);
			final int recoveryId1 = TM.registerResourceInfo("xaResInfo1",
					xaResInfo1);
			result1 = TM.enlist(xaRes1, recoveryId1);
		} catch (Exception e) {
			return "Exception happens when enlisting XAResource: " + e.toString() 
					+ ". Please check the web service provider.";
		}
		return "Enlist XAResource1 " + (result1 ? " successful" : " failed")
				+ ".";
	}
}
