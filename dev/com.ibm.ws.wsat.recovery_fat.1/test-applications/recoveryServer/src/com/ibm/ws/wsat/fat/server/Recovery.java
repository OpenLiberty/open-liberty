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
package com.ibm.ws.wsat.fat.server;

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

import com.ibm.tx.jta.ExtendedTransactionManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.ut.util.XAResourceFactoryImpl;
import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.XAResourceInfoFactory;

@WebService(wsdlLocation = "WEB-INF/wsdl/RecoveryService.wsdl")
public class Recovery {

	public static final int DIE_ON_COMMIT = 1;
	public static final int DIE_ON_ROLLBACK = 2;
	public static final int DIE_ON_PREPARE = 3;
	
	private static final String VOTE_NONE = "";
	private static final String VOTE_RECOVER_RETRY = "recover retry";
	private static final String VOTE_DIE = "die";
	private static final String VOTE_ROLLBACK = "rollback";
	private static final String VOTE_KILL_OTHER_SERVER = "kill other server";	

	private static final String STEP_NONE = "";
	private static final String STEP_RUNTIME_EXCEPTION = "runtime exception";
	private static final String STEP_PREPARE = "prepare";
	private static final String STEP_COMMIT = "commit";
	private static final String STEP_ROLLBACK = "rollback";
	private static final String STEP_RECOVER_5_TIMES = "recover 5 times";
	
	private static String METHOD_KILL_OTHER = "kill other";
	private static String METHOD_DUAL_KILLER = "dual killer";

	private static final String filter = "(testfilter=jon)";
	
	public String invoke(int testNumber,String url) throws IOException {
		String result = "";
		System.out.println("============RecoveryService test number "
				+ testNumber + "==========");
		switch (testNumber) {
		case 1:
			result = enlistTwoXAResources(VOTE_NONE,STEP_NONE, VOTE_NONE,STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			XAResourceImpl.dumpState();
	        Runtime.getRuntime().halt(0);
	        break;
		case 2:
			result = enlistTwoXAResources(VOTE_DIE,STEP_PREPARE,VOTE_NONE,STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 3:
			result = enlistTwoXAResources(VOTE_NONE,STEP_NONE,VOTE_DIE,STEP_PREPARE, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 4:
			result = enlistThreeXAResources(VOTE_ROLLBACK,STEP_PREPARE,VOTE_DIE,STEP_ROLLBACK,VOTE_NONE,STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistThreeXAResources: " + result;
			break;
		case 5:
			result = enlistThreeXAResources(VOTE_ROLLBACK,STEP_PREPARE,VOTE_NONE,STEP_NONE,VOTE_DIE,STEP_ROLLBACK, XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistThreeXAResources: " + result;
			break;
		case 6:
			result = enlistThreeXAResources(VOTE_NONE,STEP_NONE,VOTE_DIE,STEP_ROLLBACK,VOTE_ROLLBACK,STEP_PREPARE, XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistThreeXAResources: " + result;
			break;
		case 7:
			result = enlistTwoXAResources(VOTE_DIE,STEP_COMMIT,VOTE_NONE,STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 8:
			result = enlistTwoXAResources(VOTE_NONE,STEP_NONE,VOTE_DIE,STEP_COMMIT, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 9:
			result = enlistTwoXAResources(VOTE_DIE,STEP_ROLLBACK,VOTE_NONE,STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 10:
			result = enlistTwoXAResources(VOTE_NONE,STEP_NONE,VOTE_DIE,STEP_ROLLBACK, XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 11:
			result = enlistTwoXAResources(VOTE_DIE,STEP_COMMIT,"HRB",STEP_COMMIT, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 12:
			result = enlistTwoXAResources(VOTE_DIE,STEP_COMMIT,"HM",STEP_COMMIT, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 13:
			result = enlistTwoXAResources(VOTE_DIE,STEP_COMMIT,"HC",STEP_COMMIT, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 14:
			result = enlistTwoXAResources(VOTE_DIE,STEP_COMMIT,"HH",STEP_COMMIT, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 15:
			result = enlistThreeXAResources("RB",STEP_PREPARE,VOTE_DIE,STEP_ROLLBACK, "HRB",STEP_ROLLBACK, XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistThreeXAResources: " + result;
			break;
		case 16:
			result = enlistThreeXAResources("RB",STEP_PREPARE,VOTE_DIE,STEP_ROLLBACK, "HC",STEP_ROLLBACK, XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistThreeXAResources: " + result;
			break;
		case 17:
			result = enlistThreeXAResources("RB",STEP_PREPARE,VOTE_DIE,STEP_ROLLBACK, "HM",STEP_ROLLBACK, XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistThreeXAResources: " + result;
			break;
		case 18:
			result = enlistThreeXAResources("RB",STEP_PREPARE,VOTE_DIE,STEP_ROLLBACK, "HH",STEP_ROLLBACK, XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistThreeXAResources: " + result;
			break;
			
		case 37:
			result = enlistTwoXAResources(VOTE_DIE,STEP_COMMIT,"XAER_RMFAIL",STEP_RECOVER_5_TIMES, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 38:
			result = enlistTwoXAResources(VOTE_DIE,STEP_COMMIT,"XAER_RMERR",STEP_RECOVER_5_TIMES, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 39:
			result = enlistTwoXAResources(VOTE_DIE,STEP_COMMIT,"XAER_INVAL",STEP_RECOVER_5_TIMES, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 40:
			result = enlistTwoXAResources(VOTE_DIE,STEP_COMMIT,"XAER_PROTO",STEP_RECOVER_5_TIMES, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTwoXAResources: " + result;
			break;
		case 41:
	        result = "EnlistTwoXAResources: " + enlistTwoXAResources(VOTE_DIE,STEP_COMMIT,VOTE_DIE,"recovery", XAResourceImpl.DIRECTION_COMMIT);
			break;
		case 42:
			result = "EnlistThreeXAResources2: " + enlistThreeXAResources2(VOTE_ROLLBACK,STEP_PREPARE,VOTE_DIE,STEP_ROLLBACK,VOTE_DIE,"recovery","","", XAResourceImpl.DIRECTION_ROLLBACK);
			break;
		case 43:
			result = enlistFourXAResources(VOTE_DIE,STEP_PREPARE,VOTE_NONE,STEP_NONE,VOTE_NONE,STEP_NONE,VOTE_NONE,STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistFourXAResources: " + result;
			break;
		case 44:
			result = enlistFourXAResources(VOTE_DIE,STEP_COMMIT,VOTE_NONE,STEP_NONE,VOTE_NONE,STEP_NONE,VOTE_NONE,STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistFourXAResources: " + result;
			break;
		case 45:
			result = enlistThreeXAResources(VOTE_DIE,STEP_COMMIT,"XAER_RMFAIL",STEP_RECOVER_5_TIMES,"XAER_RMFAIL",STEP_RECOVER_5_TIMES, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistThreeXAResources: " + result;
			break;
		case 46:
			result = enlistTenXAResources(VOTE_DIE,STEP_PREPARE, XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistTenXAResources: " + result;
			break;
		case 47:
			result = enlistTenXAResources(VOTE_DIE,STEP_COMMIT, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTenXAResources: " + result;
			break;
		case 48:
			result = enlistThreeXAResources(VOTE_DIE, STEP_PREPARE, STEP_RUNTIME_EXCEPTION, VOTE_RECOVER_RETRY, STEP_NONE, "", XAResourceImpl.DIRECTION_ROLLBACK);
	        result = "EnlistThreeXAResources: " + result;
			break;
		case 49:
			result = enlistTwoXAResources(VOTE_DIE,STEP_PREPARE,VOTE_DIE,STEP_COMMIT, XAResourceImpl.DIRECTION_COMMIT);
	        result = "EnlistTenXAResources: " + result;
			break;
		case 10101:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 10102:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			callServlet("SuicideServlet"+"01",url);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 10201:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 10202:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			XAResourceImpl.dumpState();
			Runtime.getRuntime().halt(0);
			break;
		case 10301:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 10302:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
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
		case 20101:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 20102:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 20201:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 20202:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 20301:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 20302:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 30101:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;	
		case 30102:
			result = enlistTwoXAResources_killer(VOTE_NONE, STEP_NONE, VOTE_KILL_OTHER_SERVER, STEP_PREPARE,METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 30201:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 30202:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_DIE, STEP_PREPARE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 30301:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;	
		case 30302:
			result = enlistTwoXAResources_killer(VOTE_NONE, STEP_NONE, VOTE_KILL_OTHER_SERVER, STEP_PREPARE,METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 40101:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;	
		case 40102:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_PREPARE,VOTE_NONE,STEP_NONE,METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 40201:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 40202:
			result = enlistTwoXAResources(VOTE_DIE, STEP_PREPARE,VOTE_NONE,STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 40301:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 40302:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_PREPARE,VOTE_NONE,STEP_NONE,METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 50101:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_DIE, STEP_PREPARE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 50102:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 50201:
			result = enlistTwoXAResources_killer(VOTE_NONE,STEP_NONE,VOTE_KILL_OTHER_SERVER, STEP_PREPARE,METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 50202:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 50301:
			result = enlistTwoXAResources_killer(VOTE_NONE,STEP_NONE,VOTE_KILL_OTHER_SERVER, STEP_PREPARE,METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 50302:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 60101:
			result = enlistTwoXAResources(VOTE_DIE, STEP_PREPARE, VOTE_NONE,STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 60102:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 60201:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_PREPARE,VOTE_NONE,STEP_NONE,METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 60202:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 60301:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_PREPARE,VOTE_NONE,STEP_NONE,METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 60302:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 70101:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;	
		case 70102:
			result = enlistTwoXAResources_killer(VOTE_NONE, STEP_NONE, VOTE_KILL_OTHER_SERVER, STEP_COMMIT,METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 70201:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 70202:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_DIE, STEP_COMMIT, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 70301:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;	
		case 70302:
			result = enlistTwoXAResources_killer(VOTE_NONE, STEP_NONE, VOTE_KILL_OTHER_SERVER, STEP_COMMIT,METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 80101:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;	
		case 80102:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_COMMIT,VOTE_NONE,STEP_NONE,METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 80201:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 80202:
			result = enlistTwoXAResources(VOTE_DIE, STEP_COMMIT,VOTE_NONE,STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 80301:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 80302:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_COMMIT,VOTE_NONE,STEP_NONE,METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 90101:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_DIE, STEP_COMMIT, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 90102:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 90201:
			result = enlistTwoXAResources_killer(VOTE_NONE,STEP_NONE,VOTE_KILL_OTHER_SERVER, STEP_COMMIT,METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 90202:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 90301:
			result = enlistTwoXAResources_killer(VOTE_NONE,STEP_NONE,VOTE_KILL_OTHER_SERVER, STEP_COMMIT,METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 90302:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 100101:
			result = enlistTwoXAResources(VOTE_DIE, STEP_COMMIT, VOTE_NONE,STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 100102:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 100201:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_COMMIT,VOTE_NONE,STEP_NONE,METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 100202:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 100301:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_COMMIT,VOTE_NONE,STEP_NONE,METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources_killother: " + result;
			break;
		case 100302:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 110101:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 110102:
			result = enlistThreeXAResources_killer(VOTE_ROLLBACK, STEP_PREPARE, VOTE_KILL_OTHER_SERVER, STEP_ROLLBACK, VOTE_NONE,STEP_NONE,METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistThreeXAResources: " + result;
			break;
		case 110201:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 110202:
			result = mpr011bParticipant();
			result = "EnlistTwoXAResources: " + result;
			break;
		case 110301:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 110302:
			result = mpr011cParticipant(url);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 120101:
			result = enlistTwoXAResources(VOTE_ROLLBACK, STEP_PREPARE, VOTE_DIE, STEP_ROLLBACK, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 120102:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 120201:
			result = enlistTwoXAResources_killer(VOTE_ROLLBACK, STEP_PREPARE, VOTE_KILL_OTHER_SERVER, STEP_ROLLBACK, METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 120202:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 120301:
			result = enlistTwoXAResources_killer(VOTE_ROLLBACK, STEP_PREPARE, VOTE_KILL_OTHER_SERVER, STEP_ROLLBACK, METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 120302:
			result = enlistTwoXAResources(VOTE_NONE, STEP_NONE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
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
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_PREPARE, VOTE_NONE, STEP_NONE, METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 130202:
			result = enlistTwoXAResources(VOTE_DIE, STEP_PREPARE, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 130302:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_PREPARE, VOTE_NONE, STEP_NONE, METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 140102:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_COMMIT, VOTE_NONE, STEP_NONE, METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 140202:
			result = enlistTwoXAResources(VOTE_DIE, STEP_COMMIT, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 140302:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_COMMIT, VOTE_NONE, STEP_NONE, METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_COMMIT);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 150102:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_ROLLBACK, VOTE_NONE, STEP_NONE, METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 150202:
			result = enlistTwoXAResources(VOTE_DIE, STEP_ROLLBACK, VOTE_NONE, STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 150302:
			result = enlistTwoXAResources_killer(VOTE_KILL_OTHER_SERVER, STEP_ROLLBACK, VOTE_NONE, STEP_NONE, METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlistTwoXAResources: " + result;
			break;
		case 160102:
			result = enlistThreeXAResources_killer(VOTE_ROLLBACK, STEP_PREPARE, VOTE_KILL_OTHER_SERVER, STEP_ROLLBACK, VOTE_NONE,STEP_NONE,METHOD_KILL_OTHER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlisthreeXAResources: " + result;
			break;
		case 160202:
			result = enlistThreeXAResources(VOTE_ROLLBACK, STEP_PREPARE, VOTE_DIE, STEP_ROLLBACK,VOTE_NONE,STEP_NONE, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlisthreeXAResources: " + result;
			break;
		case 160302:
			result = enlistThreeXAResources_killer(VOTE_ROLLBACK, STEP_PREPARE, VOTE_KILL_OTHER_SERVER, STEP_ROLLBACK,VOTE_NONE,STEP_NONE, METHOD_DUAL_KILLER, url, XAResourceImpl.DIRECTION_ROLLBACK);
			result = "EnlisthreeXAResources: " + result;
			break;
		case 301101:
			result = enlistOneXAResources(XAResourceImpl.DIRECTION_ROLLBACK);
			callServlet("SuicideServlet"+"01",url);
			break;
		case 301201:
			result = enlistOneXAResources();
			XAResourceImpl.dumpState();
	        Runtime.getRuntime().halt(0);
			break;
		case 301301:
			result = enlistOneXAResources();
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
			result = enlistOneXAResources(XAResourceImpl.DIRECTION_ROLLBACK);
			break;
		case 302201:
			result = enlistOneXAResources();
			break;
		case 302301:
			result = enlistOneXAResources();
			break;
		case 303101:
			result = enlistOneXAResources(XAResourceImpl.DIRECTION_ROLLBACK);
			break;
		}
		return "Invoke completed. " + result;
	}
	

	public String callServlet(String method, String endpointUrl) throws IOException,MalformedURLException
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
	
	private String enlistOneXAResources() {
		return enlistOneXAResources(XAResourceImpl.DIRECTION_COMMIT);
	}
	
	private String enlistOneXAResources(int expectedDirection) {
		boolean result1 = false;
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
		
		final Serializable xaResInfo = XAResourceInfoFactory
				.getXAResourceInfo(0);
		XAResourceImpl xaRes;
		try {
				xaRes = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo).setExpectedDirection(expectedDirection);

			final int recoveryId = TM.registerResourceInfo(filter,
					xaResInfo);
			
			result1 = TM.enlist(xaRes, recoveryId);
		} catch (Exception e) {
			return "Exception happens when enlisting XAResource: " + e.toString() 
					+ ". Please check the web service provider.";
		}
		return "Enlist XAResource1 " + (result1 ? " successful" : " failed")
				+ "; Transaction Manager Status: "
				+ (TMstatus == Status.STATUS_ACTIVE ? "ACTIVE." : TMstatus + ".");
	}
	
	private String enlistTwoXAResources(String vote1, String step1, String vote2, String step2, int expectedDirection) {
		boolean result1 = false, result2 = false;
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

		int action1 = 0;
		if (vote1.equals(VOTE_ROLLBACK)) {
			action1 = XAException.XA_RBROLLBACK;
		} else if (vote1.equals("readonly")) {
			action1 = XAException.XA_RDONLY;
		} else if (vote1.equals(VOTE_DIE)) {
			action1 = XAResourceImpl.DIE;
		} else if (vote1.equals("HRB")){
			action1 = XAException.XA_HEURRB;
		} else if (vote1.equals("HM")){
			action1 = XAException.XA_HEURMIX;
		} else if (vote1.equals("HC")){
			action1 = XAException.XA_HEURCOM;
		} else if (vote1.equals("HH")){
			action1 = XAException.XA_HEURHAZ;
		} else if (vote1.equals("RB")){
			action1 = XAException.XA_RBROLLBACK;
		} else if (vote1.equals("XAER_RMFAIL")){
			action1 = XAException.XAER_RMFAIL;
		} else if (vote1.equals("XAER_RMERR")){
			action1 = XAException.XAER_RMERR;
		} else if (vote1.equals("XAER_INVAL")){
			action1 = XAException.XAER_INVAL;
		} else if (vote1.equals("XAER_PROTO")){
			action1 = XAException.XAER_PROTO;
		} 
		
		int action2 = 0;
		if (vote2.equals(VOTE_ROLLBACK)) {
			action2 = XAException.XA_RBROLLBACK;
		} else if (vote2.equals("readonly")) {
			action2 = XAException.XA_RDONLY;
		} else if (vote2.equals(VOTE_DIE)) {
			action2 = XAResourceImpl.DIE;
		} else if (vote2.equals("HRB")){
			action2 = XAException.XA_HEURRB;
		} else if (vote2.equals("HM")){
			action2 = XAException.XA_HEURMIX;
		} else if (vote2.equals("HC")){
			action2 = XAException.XA_HEURCOM;
		} else if (vote2.equals("HH")){
			action2 = XAException.XA_HEURHAZ;
		} else if (vote2.equals("RB")){
			action2 = XAException.XA_RBROLLBACK;
		} else if (vote2.equals("XAER_RMFAIL")){
			action2 = XAException.XAER_RMFAIL;
		} else if (vote2.equals("XAER_RMERR")){
			action2 = XAException.XAER_RMERR;
		} else if (vote2.equals("XAER_INVAL")){
			action2 = XAException.XAER_INVAL;
		} else if (vote2.equals("XAER_PROTO")){
			action2 = XAException.XAER_PROTO;
		} 

		final Serializable xaResInfo1 = XAResourceInfoFactory
				.getXAResourceInfo(0);
		final Serializable xaResInfo2 = XAResourceInfoFactory
				.getXAResourceInfo(1);
		XAResourceImpl xaRes1, xaRes2;
		try {
			if (action1 == 0) {
				xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo1);
			} else {
				if (step1.equals(STEP_PREPARE)){
					xaRes1 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo1)
						.setPrepareAction(action1);
				}else if (step1.equals(STEP_COMMIT)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setCommitAction(action1);
				}else if (step1.equals(STEP_ROLLBACK)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRollbackAction(action1);
				}else if (step1.equals(STEP_RECOVER_5_TIMES)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRecoverRepeatCount(5)
							.setRecoverAction(action1);
				}else if (step1.equals("recovery")){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRecoverAction(action1);
				}else {
					xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo1);
				}
			}

			if (action2 == 0) {
				xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo2);
			} else {
				if (step2.equals(STEP_PREPARE)){
					xaRes2 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo2)
						.setPrepareAction(action2);
				}else if (step2.equals(STEP_COMMIT)){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setCommitAction(action2);
				}else if (step2.equals(STEP_ROLLBACK)){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setRollbackAction(action2);
				}else if (step2.equals(STEP_RECOVER_5_TIMES)){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setRecoverRepeatCount(5)
							.setRecoverAction(action2);
				}else if (step2.equals("recovery")){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setRecoverAction(action2);
				}else {
					xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo2);
				}
			}

			final int recoveryId1 = TM.registerResourceInfo(filter,
					xaResInfo1);
			final int recoveryId2 = TM.registerResourceInfo(filter,
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
	
	private String enlistThreeXAResources(String vote1, String step1, String vote2, String step2,String vote3,String step3, int expectedDirection) {
		
		System.out.println("enlistThreeXAResources("+vote1+", "+step1+", "+vote2+", "+step2+", "+vote3+", "+step3+", "+(expectedDirection==XAResourceImpl.DIRECTION_COMMIT?"commit":"rollback")+")");

		boolean result1 = false, result2 = false, result3 = false;
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

		int action1 = 0;
		if (vote1.equals(VOTE_ROLLBACK)) {
			action1 = XAException.XA_RBROLLBACK;
		} else if (vote1.equals("readonly")) {
			action1 = XAException.XA_RDONLY;
		} else if (vote1.equals(VOTE_DIE)) {
			action1 = XAResourceImpl.DIE;
		} else if (vote1.equals("HRB")){
			action1 = XAException.XA_HEURRB;
		} else if (vote1.equals("HM")){
			action1 = XAException.XA_HEURMIX;
		} else if (vote1.equals("HC")){
			action1 = XAException.XA_HEURCOM;
		} else if (vote1.equals("HH")){
			action1 = XAException.XA_HEURHAZ;
		} else if (vote1.equals("RB")){
			action1 = XAException.XA_RBROLLBACK;
		} else if (vote1.equals("XAER_RMFAIL")){
			action1 = XAException.XAER_RMFAIL;
		} else if (vote1.equals("XAER_RMERR")){
			action1 = XAException.XAER_RMERR;
		} else if (vote1.equals("XAER_INVAL")){
			action1 = XAException.XAER_INVAL;
		} else if (vote1.equals("XAER_PROTO")){
			action1 = XAException.XAER_PROTO;
		} else if (vote1.equals(STEP_RUNTIME_EXCEPTION)){
			action1 = XAResourceImpl.RUNTIME_EXCEPTION;
		}

		int action2 = 0;
		if (vote2.equals(VOTE_ROLLBACK)) {
			action2 = XAException.XA_RBROLLBACK;
		} else if (vote2.equals("readonly")) {
			action2 = XAException.XA_RDONLY;
		} else if (vote2.equals(VOTE_DIE)) {
			action2 = XAResourceImpl.DIE;
		} else if (vote2.equals("HRB")){
			action2 = XAException.XA_HEURRB;
		} else if (vote2.equals("HM")){
			action2 = XAException.XA_HEURMIX;
		} else if (vote2.equals("HC")){
			action2 = XAException.XA_HEURCOM;
		} else if (vote2.equals("HH")){
			action2 = XAException.XA_HEURHAZ;
		} else if (vote2.equals("RB")){
			action2 = XAException.XA_RBROLLBACK;
		} else if (vote2.equals("XAER_RMFAIL")){
			action2 = XAException.XAER_RMFAIL;
		} else if (vote2.equals("XAER_RMERR")){
			action2 = XAException.XAER_RMERR;
		} else if (vote2.equals("XAER_INVAL")){
			action2 = XAException.XAER_INVAL;
		} else if (vote2.equals("XAER_PROTO")){
			action2 = XAException.XAER_PROTO;
		} else if (vote2.equals(STEP_RUNTIME_EXCEPTION)){
			action2 = XAResourceImpl.RUNTIME_EXCEPTION;
		}
		
		System.out.println("action2="+action2);
		
		int action3 = 0;
		if (vote3.equals(VOTE_ROLLBACK)) {
			action3 = XAException.XA_RBROLLBACK;
		} else if (vote3.equals("readonly")) {
			action3 = XAException.XA_RDONLY;
		} else if (vote3.equals(VOTE_DIE)) {
			action3 = XAResourceImpl.DIE;
		} else if (vote3.equals("HRB")){
			action3 = XAException.XA_HEURRB;
		} else if (vote3.equals("HM")){
			action3 = XAException.XA_HEURMIX;
		} else if (vote3.equals("HC")){
			action3 = XAException.XA_HEURCOM;
		} else if (vote3.equals("HH")){
			action3 = XAException.XA_HEURHAZ;
		} else if (vote3.equals("RB")){
			action3 = XAException.XA_RBROLLBACK;
		} else if (vote3.equals("XAER_RMFAIL")){
			action3 = XAException.XAER_RMFAIL;
		} else if (vote3.equals("XAER_RMERR")){
			action3 = XAException.XAER_RMERR;
		} else if (vote3.equals("XAER_INVAL")){
			action3 = XAException.XAER_INVAL;
		} else if (vote3.equals("XAER_PROTO")){
			action3 = XAException.XAER_PROTO;
		} else if (vote3.equals(STEP_RUNTIME_EXCEPTION)){
			action3 = XAResourceImpl.RUNTIME_EXCEPTION;
		}

		final Serializable xaResInfo1 = XAResourceInfoFactory
				.getXAResourceInfo(0);
		final Serializable xaResInfo2 = XAResourceInfoFactory
				.getXAResourceInfo(1);
		final Serializable xaResInfo3 = XAResourceInfoFactory
				.getXAResourceInfo(2);
		XAResourceImpl xaRes1, xaRes2, xaRes3;
		try {
			if (action1 == 0) {
				xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo1);
			} else {
				if (step1.equals(STEP_PREPARE)){
					xaRes1 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo1)
						.setPrepareAction(action1);
				}else if (step1.equals(STEP_COMMIT)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setCommitAction(action1);
				}else if (step1.equals(STEP_ROLLBACK)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRollbackAction(action1);
				}else if (step1.equals(STEP_RECOVER_5_TIMES)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRecoverRepeatCount(5)
							.setRecoverAction(action1);
				}else if (step1.equals(VOTE_RECOVER_RETRY)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRecoverRepeatCount(1)
							.setRecoverAction(action1);
				}else if (step1.equals("recovery")){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRecoverAction(action1);
				}else{
					xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo1);
				}
			}

			if (action2 == 0) {
				xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo2);
			} else {
				if (step2.equals(STEP_PREPARE)){
					xaRes2 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo2)
						.setPrepareAction(action2);
				}else if (step2.equals(STEP_COMMIT)){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setCommitAction(action2);
				}else if (step2.equals(STEP_ROLLBACK)){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setRollbackAction(action2);
				}else if (step2.equals(STEP_RECOVER_5_TIMES)){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setRecoverRepeatCount(5)
							.setRecoverAction(action2);
				}else if (step2.equals(VOTE_RECOVER_RETRY)){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setRecoverRepeatCount(1)
							.setRecoverAction(action2);
				}else if (step2.equals("recovery")){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setRecoverAction(action2);
				}else {
					xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo2);
				}
			}
			
			if (action3 == 0) {
				xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo3);
			} else {
				if (step3.equals(STEP_PREPARE)){
					xaRes3 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo3)
						.setPrepareAction(action3);
				}else if (step3.equals(STEP_COMMIT)){
					xaRes3 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo3)
							.setCommitAction(action3);
				}else if (step3.equals(STEP_ROLLBACK)){
					xaRes3 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo3)
							.setRollbackAction(action3);
				}else if (step3.equals(STEP_RECOVER_5_TIMES)){
					xaRes3 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo3)
							.setRecoverRepeatCount(5)
							.setRecoverAction(action3);
				}else if (step3.equals("recover retry")){
					xaRes3 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo3)
							.setRecoverRepeatCount(1)
							.setRecoverAction(action3);
				}else if (step3.equals("recovery")){
					xaRes3 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo3)
							.setRecoverAction(action3);
				}else {
					xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo3);
				}
			}

			final int recoveryId1 = TM.registerResourceInfo(filter,
					xaResInfo1);
			final int recoveryId2 = TM.registerResourceInfo(filter,
					xaResInfo2);
			final int recoveryId3 = TM.registerResourceInfo(filter,
					xaResInfo3);
			xaRes1.setExpectedDirection(expectedDirection);
			xaRes2.setExpectedDirection(expectedDirection);
			xaRes3.setExpectedDirection(expectedDirection);
			
			result1 = TM.enlist(xaRes1, recoveryId1);
			result2 = TM.enlist(xaRes2, recoveryId2);
			result3 = TM.enlist(xaRes3, recoveryId3);
		} catch (Exception e) {
			return "Exception happens when enlisting XAResource: " + e.toString() 
					+ ". Please check the web service provider.";
		}
		return "Enlist XAResource1 voting '" + vote1 + (result1 ? "' successful" : " failed")
				+ "; Enlist XAResource2 voting '" + vote2 + (result2 ? "' successful" : " failed")
				+ "; Enlist XAResource3 voting '" + vote3 + (result3 ? "' successful" : " failed")
				+ "; Transaction Manager Status: "
				+ (TMstatus == Status.STATUS_ACTIVE ? "ACTIVE." : TMstatus + ".");
	}
	
	//test case WSTXREC042FVT
	private String enlistThreeXAResources2(String vote1, String step1, String vote21, String step21,String vote22,String step22,String vote3,String step3, int expectedDirection) {
		boolean result1 = false, result2 = false, result3 = false;
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

		int action1 = 0;
		if (vote1.equals(VOTE_ROLLBACK)) {
			action1 = XAException.XA_RBROLLBACK;
		} 
		
		int action21 = 0;
		if (vote21.equals(VOTE_DIE)) {
			action21 = XAResourceImpl.DIE;
		} 
		
		int action22 = 0;
		if (vote22.equals(VOTE_DIE)) {
			action22 = XAResourceImpl.DIE;
		} 
	
		final Serializable xaResInfo1 = XAResourceInfoFactory
				.getXAResourceInfo(0);
		final Serializable xaResInfo2 = XAResourceInfoFactory
				.getXAResourceInfo(1);
		final Serializable xaResInfo3 = XAResourceInfoFactory
				.getXAResourceInfo(2);
		XAResourceImpl xaRes1, xaRes2, xaRes3;
		try {
			if (action1 == 0) {
				xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo1);
			} else {
				if (step1.equals(STEP_PREPARE)){
					xaRes1 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo1)
						.setPrepareAction(action1);
				}
				else
				{
					xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo1);
				}
			}

			if (action21 == 0) {
				xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo2);
			} else {
				if (step21.equals(STEP_ROLLBACK)){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setRollbackAction(action21);
				}
				else
				{
					xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo2);
				}
			}
			
			if (action22 == 0) {
				xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo2);
			} else {
				if (step22.equals("recovery")){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setRecoverAction(action22);
				}
				else
				{
					xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo2);
				}
			}
			
			
			xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo3);

			final int recoveryId1 = TM.registerResourceInfo(filter,
					xaResInfo1);
			final int recoveryId2 = TM.registerResourceInfo(filter,
					xaResInfo2);
			final int recoveryId3 = TM.registerResourceInfo(filter,
					xaResInfo3);
			xaRes1.setExpectedDirection(expectedDirection);
			xaRes2.setExpectedDirection(expectedDirection);
			xaRes3.setExpectedDirection(expectedDirection);
			
			result1 = TM.enlist(xaRes1, recoveryId1);
			result2 = TM.enlist(xaRes2, recoveryId2);
			result3 = TM.enlist(xaRes3, recoveryId3);
		} catch (Exception e) {
			return "Exception happens when enlisting XAResource: " + e.toString() 
					+ ". Please check the web service provider.";
		}
		return "Enlist XAResource1 voting '" + vote1 + (result1 ? "' successful" : " failed")
				+ "; Enlist XAResource2 voting '" + vote21 + (result2 ? "' successful" : " failed")
				+ "; Enlist XAResource3 voting '" + vote3 + (result3 ? "' successful" : " failed")
				+ "; Transaction Manager Status: "
				+ (TMstatus == Status.STATUS_ACTIVE ? "ACTIVE." : TMstatus + ".");
	}
	
	
	private String enlistFourXAResources(String vote1, String step1, String vote2, String step2,String vote3,String step3,String vote4,String step4, int expectedDirection) {
		boolean result1 = false, result2 = false, result3 = false, result4 = false;
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

		int action1 = 0;
		if (vote1.equals(VOTE_ROLLBACK)) {
			action1 = XAException.XA_RBROLLBACK;
		} else if (vote1.equals("readonly")) {
			action1 = XAException.XA_RDONLY;
		} else if (vote1.equals(VOTE_DIE)) {
			action1 = XAResourceImpl.DIE;
		} else if (vote1.equals("HRB")){
			action1 = XAException.XA_HEURRB;
		} else if (vote1.equals("HM")){
			action1 = XAException.XA_HEURMIX;
		} else if (vote1.equals("HC")){
			action1 = XAException.XA_HEURCOM;
		} else if (vote1.equals("HH")){
			action1 = XAException.XA_HEURHAZ;
		} else if (vote1.equals("RB")){
			action1 = XAException.XA_RBROLLBACK;
		} else if (vote1.equals("XAER_RMFAIL")){
			action1 = XAException.XAER_RMFAIL;
		} else if (vote1.equals("XAER_RMERR")){
			action1 = XAException.XAER_RMERR;
		} else if (vote1.equals("XAER_INVAL")){
			action1 = XAException.XAER_INVAL;
		} else if (vote1.equals("XAER_PROTO")){
			action1 = XAException.XAER_PROTO;
		} 

		int action2 = 0;
		if (vote2.equals(VOTE_ROLLBACK)) {
			action2 = XAException.XA_RBROLLBACK;
		} else if (vote2.equals("readonly")) {
			action2 = XAException.XA_RDONLY;
		} else if (vote2.equals(VOTE_DIE)) {
			action2 = XAResourceImpl.DIE;
		} else if (vote2.equals("HRB")){
			action2 = XAException.XA_HEURRB;
		} else if (vote2.equals("HM")){
			action2 = XAException.XA_HEURMIX;
		} else if (vote2.equals("HC")){
			action2 = XAException.XA_HEURCOM;
		} else if (vote2.equals("HH")){
			action2 = XAException.XA_HEURHAZ;
		} else if (vote2.equals("RB")){
			action2 = XAException.XA_RBROLLBACK;
		} else if (vote2.equals("XAER_RMFAIL")){
			action2 = XAException.XAER_RMFAIL;
		} else if (vote2.equals("XAER_RMERR")){
			action2 = XAException.XAER_RMERR;
		} else if (vote2.equals("XAER_INVAL")){
			action2 = XAException.XAER_INVAL;
		} else if (vote2.equals("XAER_PROTO")){
			action2 = XAException.XAER_PROTO;
		} 
		
		int action3 = 0;
		if (vote3.equals(VOTE_ROLLBACK)) {
			action3 = XAException.XA_RBROLLBACK;
		} else if (vote3.equals("readonly")) {
			action3 = XAException.XA_RDONLY;
		} else if (vote3.equals(VOTE_DIE)) {
			action3 = XAResourceImpl.DIE;
		} else if (vote3.equals("HRB")){
			action3 = XAException.XA_HEURRB;
		} else if (vote3.equals("HM")){
			action3 = XAException.XA_HEURMIX;
		} else if (vote3.equals("HC")){
			action3 = XAException.XA_HEURCOM;
		} else if (vote3.equals("HH")){
			action3 = XAException.XA_HEURHAZ;
		} else if (vote3.equals("RB")){
			action3 = XAException.XA_RBROLLBACK;
		} else if (vote3.equals("XAER_RMFAIL")){
			action3 = XAException.XAER_RMFAIL;
		} else if (vote3.equals("XAER_RMERR")){
			action3 = XAException.XAER_RMERR;
		} else if (vote3.equals("XAER_INVAL")){
			action3 = XAException.XAER_INVAL;
		} else if (vote3.equals("XAER_PROTO")){
			action3 = XAException.XAER_PROTO;
		} 
		
		int action4 = 0;
		if (vote4.equals(VOTE_ROLLBACK)) {
			action4 = XAException.XA_RBROLLBACK;
		} else if (vote4.equals("readonly")) {
			action4 = XAException.XA_RDONLY;
		} else if (vote4.equals(VOTE_DIE)) {
			action4 = XAResourceImpl.DIE;
		} else if (vote4.equals("HRB")){
			action4 = XAException.XA_HEURRB;
		} else if (vote4.equals("HM")){
			action4 = XAException.XA_HEURMIX;
		} else if (vote4.equals("HC")){
			action4 = XAException.XA_HEURCOM;
		} else if (vote4.equals("HH")){
			action4 = XAException.XA_HEURHAZ;
		} else if (vote4.equals("RB")){
			action4 = XAException.XA_RBROLLBACK;
		} else if (vote4.equals("XAER_RMFAIL")){
			action4 = XAException.XAER_RMFAIL;
		} else if (vote4.equals("XAER_RMERR")){
			action4 = XAException.XAER_RMERR;
		} else if (vote4.equals("XAER_INVAL")){
			action4 = XAException.XAER_INVAL;
		} else if (vote4.equals("XAER_PROTO")){
			action4 = XAException.XAER_PROTO;
		} 

		final Serializable xaResInfo1 = XAResourceInfoFactory
				.getXAResourceInfo(0);
		final Serializable xaResInfo2 = XAResourceInfoFactory
				.getXAResourceInfo(1);
		final Serializable xaResInfo3 = XAResourceInfoFactory
				.getXAResourceInfo(2);
		final Serializable xaResInfo4 = XAResourceInfoFactory
				.getXAResourceInfo(3);
		XAResourceImpl xaRes1, xaRes2, xaRes3, xaRes4;
		try {
			if (action1 == 0) {
				xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo1);
			} else {
				if (step1.equals(STEP_PREPARE)){
					xaRes1 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo1)
						.setPrepareAction(action1);
				}else if (step1.equals(STEP_COMMIT)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setCommitAction(action1);
				}else if (step1.equals(STEP_ROLLBACK)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRollbackAction(action1);
				}else if (step1.equals(STEP_RECOVER_5_TIMES)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRecoverRepeatCount(5)
							.setRecoverAction(action1);
				}else if (step1.equals("recovery")){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRecoverAction(action1);
				}else{
					xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo1);
				}
			}

			if (action2 == 0) {
				xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo2);
			} else {
				if (step2.equals(STEP_PREPARE)){
					xaRes2 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo2)
						.setPrepareAction(action2);
				}else if (step2.equals(STEP_COMMIT)){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setCommitAction(action2);
				}else if (step2.equals(STEP_ROLLBACK)){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setRollbackAction(action2);
				}else if (step2.equals(STEP_RECOVER_5_TIMES)){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setRecoverRepeatCount(5)
							.setRecoverAction(action2);
				}else if (step2.equals("recovery")){
					xaRes2 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo2)
							.setRecoverAction(action2);
				}else {
					xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo2);
				}
			}
			
			if (action3 == 0) {
				xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo3);
			} else {
				if (step3.equals(STEP_PREPARE)){
					xaRes3 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo3)
						.setPrepareAction(action3);
				}else if (step3.equals(STEP_COMMIT)){
					xaRes3 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo3)
							.setCommitAction(action3);
				}else if (step3.equals(STEP_ROLLBACK)){
					xaRes3 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo3)
							.setRollbackAction(action3);
				}else if (step3.equals(STEP_RECOVER_5_TIMES)){
					xaRes3 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo3)
							.setRecoverRepeatCount(5)
							.setRecoverAction(action3);
				}else if (step3.equals("recovery")){
					xaRes3 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo3)
							.setRecoverAction(action3);
				}else {
					xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo3);
				}
			}
			
			if (action4 == 0) {
				xaRes4 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo4);
			} else {
				if (step4.equals(STEP_PREPARE)){
					xaRes4 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo4)
						.setPrepareAction(action4);
				}else if (step4.equals(STEP_COMMIT)){
					xaRes4 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo4)
							.setCommitAction(action4);
				}else if (step4.equals(STEP_ROLLBACK)){
					xaRes4 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo4)
							.setRollbackAction(action4);
				}else if (step4.equals(STEP_RECOVER_5_TIMES)){
					xaRes4 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo4)
							.setRecoverRepeatCount(5)
							.setRecoverAction(action4);
				}else if (step4.equals("recovery")){
					xaRes4 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo4)
							.setRecoverAction(action4);
				}else {
					xaRes4 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo4);
				}
			}


			final int recoveryId1 = TM.registerResourceInfo(filter,
					xaResInfo1);
			final int recoveryId2 = TM.registerResourceInfo(filter,
					xaResInfo2);
			final int recoveryId3 = TM.registerResourceInfo(filter,
					xaResInfo3);
			final int recoveryId4 = TM.registerResourceInfo(filter,
					xaResInfo4);
			xaRes1.setExpectedDirection(expectedDirection);
			xaRes2.setExpectedDirection(expectedDirection);
			xaRes3.setExpectedDirection(expectedDirection);
			xaRes4.setExpectedDirection(expectedDirection);
			result1 = TM.enlist(xaRes1, recoveryId1);
			result2 = TM.enlist(xaRes2, recoveryId2);
			result3 = TM.enlist(xaRes3, recoveryId3);
			result4 = TM.enlist(xaRes4, recoveryId4);
		} catch (Exception e) {
			return "Exception happens when enlisting XAResource: " + e.toString() 
					+ ". Please check the web service provider.";
		}
		return "Enlist XAResource1 voting '" + vote1 + (result1 ? "' successful" : " failed")
				+ "; Enlist XAResource2 voting '" + vote2 + (result2 ? "' successful" : " failed")
				+ "; Enlist XAResource3 voting '" + vote3 + (result3 ? "' successful" : " failed")
				+ "; Enlist XAResource4 voting '" + vote4 + (result4 ? "' successful" : " failed")
				+ "; Transaction Manager Status: "
				+ (TMstatus == Status.STATUS_ACTIVE ? "ACTIVE." : TMstatus + ".");
	}
	
	private String enlistTenXAResources(String vote1, String step1, int expectedDirection) {
		boolean result1 = false, result2 = false, result3 = false, result4 = false, result5 = false;
		boolean result6 = false, result7 = false, result8 = false, result9 = false, result10 = false;
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
		
		int action1 = 0;
		if (vote1.equals(VOTE_DIE)) {
			action1 = XAResourceImpl.DIE;
		} 
		final Serializable xaResInfo1 = XAResourceInfoFactory
				.getXAResourceInfo(0);
		final Serializable xaResInfo2 = XAResourceInfoFactory
				.getXAResourceInfo(1);
		final Serializable xaResInfo3 = XAResourceInfoFactory
				.getXAResourceInfo(2);
		final Serializable xaResInfo4 = XAResourceInfoFactory
				.getXAResourceInfo(3);
		final Serializable xaResInfo5 = XAResourceInfoFactory
				.getXAResourceInfo(4);
		final Serializable xaResInfo6 = XAResourceInfoFactory
				.getXAResourceInfo(5);
		final Serializable xaResInfo7 = XAResourceInfoFactory
				.getXAResourceInfo(6);
		final Serializable xaResInfo8 = XAResourceInfoFactory
				.getXAResourceInfo(7);
		final Serializable xaResInfo9 = XAResourceInfoFactory
				.getXAResourceInfo(8);
		final Serializable xaResInfo10 = XAResourceInfoFactory
				.getXAResourceInfo(9);
		XAResourceImpl xaRes1, xaRes2, xaRes3, xaRes4, xaRes5;
		XAResourceImpl xaRes6, xaRes7, xaRes8, xaRes9, xaRes10;
		try {
			if (action1 == 0) {
				xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
						xaResInfo1);
			} else {
				if (step1.equals(STEP_PREPARE)){
					xaRes1 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo1)
						.setPrepareAction(action1);
				}else if (step1.equals(STEP_COMMIT)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setCommitAction(action1);
				}else if (step1.equals(STEP_ROLLBACK)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRollbackAction(action1);
				}else if (step1.equals(STEP_RECOVER_5_TIMES)){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRecoverRepeatCount(5)
							.setRecoverAction(action1);
				}else if (step1.equals("recovery")){
					xaRes1 = XAResourceFactoryImpl.instance()
							.getXAResourceImpl(xaResInfo1)
							.setRecoverAction(action1);
				}else{
					xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(
							xaResInfo1);
				}
			}
			
			xaRes2 = XAResourceFactoryImpl.instance().getXAResourceImpl(
					xaResInfo2);
			xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(
					xaResInfo3);
			xaRes4 = XAResourceFactoryImpl.instance().getXAResourceImpl(
					xaResInfo4);
			xaRes5 = XAResourceFactoryImpl.instance().getXAResourceImpl(
					xaResInfo5);
			xaRes6 = XAResourceFactoryImpl.instance().getXAResourceImpl(
					xaResInfo6);
			xaRes7 = XAResourceFactoryImpl.instance().getXAResourceImpl(
					xaResInfo7);
			xaRes8 = XAResourceFactoryImpl.instance().getXAResourceImpl(
					xaResInfo8);
			xaRes9 = XAResourceFactoryImpl.instance().getXAResourceImpl(
					xaResInfo9);
			xaRes10 = XAResourceFactoryImpl.instance().getXAResourceImpl(
					xaResInfo10);
			
			final int recoveryId1 = TM.registerResourceInfo(filter,
					xaResInfo1);
			final int recoveryId2 = TM.registerResourceInfo(filter,
					xaResInfo2);
			final int recoveryId3 = TM.registerResourceInfo(filter,
					xaResInfo3);
			final int recoveryId4 = TM.registerResourceInfo(filter,
					xaResInfo4);
			final int recoveryId5 = TM.registerResourceInfo(filter,
					xaResInfo5);
			final int recoveryId6 = TM.registerResourceInfo(filter,
					xaResInfo6);
			final int recoveryId7 = TM.registerResourceInfo(filter,
					xaResInfo7);
			final int recoveryId8 = TM.registerResourceInfo(filter,
					xaResInfo8);
			final int recoveryId9 = TM.registerResourceInfo(filter,
					xaResInfo9);
			final int recoveryId10 = TM.registerResourceInfo(filter,
					xaResInfo10);
			
			xaRes1.setExpectedDirection(expectedDirection);
			xaRes2.setExpectedDirection(expectedDirection);
			xaRes3.setExpectedDirection(expectedDirection);
			xaRes4.setExpectedDirection(expectedDirection);
			xaRes5.setExpectedDirection(expectedDirection);
			xaRes6.setExpectedDirection(expectedDirection);
			xaRes7.setExpectedDirection(expectedDirection);
			xaRes8.setExpectedDirection(expectedDirection);
			xaRes9.setExpectedDirection(expectedDirection);
			xaRes10.setExpectedDirection(expectedDirection);
			result1 = TM.enlist(xaRes1, recoveryId1);
			result2 = TM.enlist(xaRes2, recoveryId2);
			result3 = TM.enlist(xaRes3, recoveryId3);
			result4 = TM.enlist(xaRes4, recoveryId4);
			result5 = TM.enlist(xaRes5, recoveryId5);
			result6 = TM.enlist(xaRes6, recoveryId6);
			result7 = TM.enlist(xaRes7, recoveryId7);
			result8 = TM.enlist(xaRes8, recoveryId8);
			result9 = TM.enlist(xaRes9, recoveryId9);
			result10 = TM.enlist(xaRes10, recoveryId10);
		} catch (Exception e) {
			return "Exception happens when enlisting XAResource: " + e.toString() 
					+ ". Please check the web service provider.";
		}
		return "Enlist XAResource1 voting '" + vote1 + (result1 ? "' successful" : " failed")
				+ (result2 ? "' successful" : " failed")
				+ (result3 ? "' successful" : " failed")
				+ (result4 ? "' successful" : " failed")
				+ (result5 ? "' successful" : " failed")
				+ (result6 ? "' successful" : " failed")
				+ (result7 ? "' successful" : " failed")
				+ (result8 ? "' successful" : " failed")
				+ (result9 ? "' successful" : " failed")
				+ (result10 ? "' successful" : " failed")
				+ "; Transaction Manager Status: "
				+ (TMstatus == Status.STATUS_ACTIVE ? "ACTIVE." : TMstatus + ".");

	}
	
	private String enlistTwoXAResources_killer(String vote1, String step1, String vote2, String step2, String method, String baseurl, int expectedDirection) {
		boolean result1 = false, result2 = false;
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
		int action1 = 0;
		if (vote1.equals(VOTE_ROLLBACK)) {
			action1 = XAException.XA_RBROLLBACK;	
		} else if (vote1.equals("readonly")) {
			action1 = XAException.XA_RDONLY;
		} else if (vote1.equals(VOTE_KILL_OTHER_SERVER)) {
			action1 = XAResourceImpl.DIE;
		} 
		int action2 = 0;
		if (vote2.equals(VOTE_ROLLBACK)) {
			action2 = XAException.XA_RBROLLBACK;
		} else if (vote2.equals("readonly")) {
			action2 = XAException.XA_RDONLY;
		} else if (vote2.equals(VOTE_KILL_OTHER_SERVER)) {
			action2 = XAResourceImpl.DIE;
		}

		final Serializable xaResInfo1 = XAResourceInfoFactory
				.getXAResourceInfo(0);
		final Serializable xaResInfo2 = XAResourceInfoFactory
				.getXAResourceInfo(1);
		if(method.equals(METHOD_KILL_OTHER))
		{
			XAResourceImpl xaRes1, xaRes2;
			try {
				xaRes1 = XAResourceFactoryImpl.
						instance().getXAResourceImpl(xaResInfo1);
	
				if(action1 == XAResourceImpl.DIE){
					xaRes1.setDoomedServer(new URL(baseurl+"/recoveryServer/SuicideServlet"));
					xaRes1.setCommitSuicide(false);
				}

				if (step1.equals(STEP_PREPARE)){
					xaRes1.setPrepareAction(action1);
				}else if (step1.equals(STEP_COMMIT)){
					xaRes1.setCommitAction(action1);
				}else if (step1.equals(STEP_ROLLBACK)){
					xaRes1.setRollbackAction(action1);
				}

				xaRes2 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo2);
				
				if(action2 == XAResourceImpl.DIE){
					xaRes2.setDoomedServer(new URL(baseurl+"/recoveryServer/SuicideServlet"));
					xaRes2.setCommitSuicide(false);
				}

				if (step2.equals(STEP_PREPARE)){
					xaRes2.setPrepareAction(action2);
				}else if (step2.equals(STEP_COMMIT)){
					xaRes2.setCommitAction(action2);
				}else if (step2.equals(STEP_ROLLBACK)){
					xaRes2.setRollbackAction(action2);
				}

				final int recoveryId1 = TM.registerResourceInfo(filter,
						xaResInfo1);
				final int recoveryId2 = TM.registerResourceInfo(filter,
						xaResInfo2);
				xaRes1.setExpectedDirection(expectedDirection);
				xaRes2.setExpectedDirection(expectedDirection);
				
				result1 = TM.enlist(xaRes1, recoveryId1);
				result2 = TM.enlist(xaRes2, recoveryId2);
			} catch (Exception e) {
				System.out.println("Get exception when killing other:" + e.toString());
				return "Exception happens when enlisting XAResource: " + e.toString() 
						+ ". Please check the web service provider.";
			}
			
		}
		else if(method.equals(METHOD_DUAL_KILLER))
		{
			XAResourceImpl xaRes1, xaRes2;
			try {
				xaRes1 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo1);

				if(action1 == XAResourceImpl.DIE){
					xaRes1.setDoomedServer(new URL(baseurl+"/recoveryServer/SuicideServlet"));
				}

				if (step1.equals(STEP_PREPARE)){
					xaRes1.setPrepareAction(action1);
				}else if (step1.equals(STEP_COMMIT)){
					xaRes1.setCommitAction(action1);
				}else if (step1.equals(STEP_ROLLBACK)){
					xaRes1.setRollbackAction(action1);
				}

				xaRes2 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo2);
				if(action2 == XAResourceImpl.DIE){
					xaRes2.setDoomedServer(new URL(baseurl+"/recoveryServer/SuicideServlet"));
				}

				if (step2.equals(STEP_PREPARE)){
					xaRes2.setPrepareAction(action2);
				}else if (step2.equals(STEP_COMMIT)){
					xaRes2.setCommitAction(action2);
				}else if (step2.equals(STEP_ROLLBACK)){
					xaRes2.setRollbackAction(action2);
				}

				final int recoveryId1 = TM.registerResourceInfo(filter,
						xaResInfo1);
				final int recoveryId2 = TM.registerResourceInfo(filter,
						xaResInfo2);
				xaRes1.setExpectedDirection(expectedDirection);
				xaRes2.setExpectedDirection(expectedDirection);
				
				result1 = TM.enlist(xaRes1, recoveryId1);
				result2 = TM.enlist(xaRes2, recoveryId2);
			} catch (Exception e) {
				System.out.println("Get exception in dual killer:" + e.toString());
				return "Exception happens when enlisting XAResource: " + e.toString() 
						+ ". Please check the web service provider.";
			}
		}
		return "Enlist XAResource1 voting '" + vote1 + (result1 ? "' successful" : " failed")
				+ "; Enlist XAResource2 voting '" + vote2 + (result2 ? "' successful" : " failed")
				+ "; Transaction Manager Status: "
				+ (TMstatus == Status.STATUS_ACTIVE ? "ACTIVE." : TMstatus + ".");
	}
	
	private String enlistThreeXAResources_killer(String vote1, String step1, String vote2, String step2,
			String vote3, String step3, String method,String baseurl, int expectedDirection) {
		boolean result1 = false, result2 = false, result3 = false;
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
		int action1 = 0;
		if (vote1.equals(VOTE_ROLLBACK)) {
			action1 = XAException.XA_RBROLLBACK;	
		} else if (vote1.equals("readonly")) {
			action1 = XAException.XA_RDONLY;
		} else if (vote1.equals(VOTE_KILL_OTHER_SERVER)) {
			action1 = XAResourceImpl.DIE;
		} 
		
		int action2 = 0;
		if (vote2.equals(VOTE_ROLLBACK)) {
			action2 = XAException.XA_RBROLLBACK;
		} else if (vote2.equals("readonly")) {
			action2 = XAException.XA_RDONLY;
		} else if (vote2.equals(VOTE_KILL_OTHER_SERVER)) {
			action2 = XAResourceImpl.DIE;
		}
		
		int action3 = 0;
		if (vote3.equals(VOTE_ROLLBACK)) {
			action3 = XAException.XA_RBROLLBACK;
		} else if (vote3.equals("readonly")) {
			action3 = XAException.XA_RDONLY;
		} else if (vote3.equals(VOTE_KILL_OTHER_SERVER)) {
			action3 = XAResourceImpl.DIE;
		}
		
		final Serializable xaResInfo1 = XAResourceInfoFactory
				.getXAResourceInfo(0);
		final Serializable xaResInfo2 = XAResourceInfoFactory
				.getXAResourceInfo(1);
		final Serializable xaResInfo3 = XAResourceInfoFactory
				.getXAResourceInfo(2);
		
		if(method.equals(METHOD_KILL_OTHER))
		{
			XAResourceImpl xaRes1, xaRes2, xaRes3;
			try {
				xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1);
	
				if(action1 == XAResourceImpl.DIE){
					xaRes1.setDoomedServer(new URL(baseurl+"/recoveryServer/SuicideServlet"));
					xaRes1.setCommitSuicide(false);
				}

				if (step1.equals(STEP_PREPARE)){
					xaRes1.setPrepareAction(action1);
				}else if (step1.equals(STEP_COMMIT)){
					xaRes1.setCommitAction(action1);
				}else if (step1.equals(STEP_ROLLBACK)){
					xaRes1.setRollbackAction(action1);
				}

				xaRes2 = XAResourceFactoryImpl.instance().
						getXAResourceImpl(xaResInfo2);
				if(action2 == XAResourceImpl.DIE){
					xaRes2.setDoomedServer(new URL(baseurl+"/recoveryServer/SuicideServlet"));
					xaRes2.setCommitSuicide(false);
				}

				if (step2.equals(STEP_PREPARE)){
					xaRes2.setPrepareAction(action2);
				}else if (step2.equals(STEP_COMMIT)){
					xaRes2.setCommitAction(action2);
				}else if (step2.equals(STEP_ROLLBACK)){
					xaRes2.setRollbackAction(action2);
				}

				xaRes3 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo3);
				if(action3 == XAResourceImpl.DIE){
					xaRes3.setDoomedServer(new URL(baseurl+"/recoveryServer/SuicideServlet"));
					xaRes3.setCommitSuicide(false);
				}
				
				if (step3.equals(STEP_PREPARE)){
					xaRes3.setPrepareAction(action3);
				}else if (step3.equals(STEP_COMMIT)){
					xaRes3.setCommitAction(action3);
				}else if (step3.equals(STEP_ROLLBACK)){
					xaRes3.setRollbackAction(action3);
				}

				final int recoveryId1 = TM.registerResourceInfo(filter,
						xaResInfo1);
				final int recoveryId2 = TM.registerResourceInfo(filter,
						xaResInfo2);
				final int recoveryId3 = TM.registerResourceInfo(filter,
						xaResInfo3);
				xaRes1.setExpectedDirection(expectedDirection);
				xaRes2.setExpectedDirection(expectedDirection);
				xaRes3.setExpectedDirection(expectedDirection);
				
				result1 = TM.enlist(xaRes1, recoveryId1);
				result2 = TM.enlist(xaRes2, recoveryId2);
				result3 = TM.enlist(xaRes3, recoveryId3);
			} catch (Exception e) {
				System.out.println("Get exception when killing other:" + e.toString());
				return "Exception happens when enlisting XAResource: " + e.toString() 
						+ ". Please check the web service provider.";
			}
			
		}
		else if(method.equals(METHOD_DUAL_KILLER))
		{
			XAResourceImpl xaRes1, xaRes2, xaRes3;
			try {
				xaRes1 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo1);

				if(action1 == XAResourceImpl.DIE){
					xaRes1.setDoomedServer(new URL(baseurl+"/recoveryServer/SuicideServlet"));
				}

				if (step1.equals(STEP_PREPARE)){
					xaRes1.setPrepareAction(action1);
				}else if (step1.equals(STEP_COMMIT)){
					xaRes1.setCommitAction(action1);
				}else if (step1.equals(STEP_ROLLBACK)){
					xaRes1.setRollbackAction(action1);
				}

				xaRes2 = XAResourceFactoryImpl.instance()
						.getXAResourceImpl(xaResInfo2);
				
				if(action2 == XAResourceImpl.DIE){
					xaRes2.setDoomedServer(new URL(baseurl+"/recoveryServer/SuicideServlet"));
				}
				
				if (step2.equals(STEP_PREPARE)){
					xaRes2.setPrepareAction(action2);
				}else if (step2.equals(STEP_COMMIT)){
					xaRes2.setCommitAction(action2);
				}else if (step2.equals(STEP_ROLLBACK)){
					xaRes2.setRollbackAction(action2);
				}

				xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo3);
				if(action3 == XAResourceImpl.DIE){
					xaRes3.setDoomedServer(new URL(baseurl+"/recoveryServer/SuicideServlet"));
				}

				if (step3.equals(STEP_PREPARE)){
					xaRes3.setPrepareAction(action3);
				}else if (step3.equals(STEP_COMMIT)){
					xaRes3.setCommitAction(action3);
				}else if (step3.equals(STEP_ROLLBACK)){
					xaRes3.setRollbackAction(action3);
				}

				final int recoveryId1 = TM.registerResourceInfo(filter,
						xaResInfo1);
				final int recoveryId2 = TM.registerResourceInfo(filter,
						xaResInfo2);
				final int recoveryId3 = TM.registerResourceInfo(filter,
						xaResInfo3);
				xaRes1.setExpectedDirection(expectedDirection);
				xaRes2.setExpectedDirection(expectedDirection);
				xaRes3.setExpectedDirection(expectedDirection);
				result1 = TM.enlist(xaRes1, recoveryId1);
				result2 = TM.enlist(xaRes2, recoveryId2);
				result3 = TM.enlist(xaRes3, recoveryId3);
			} catch (Exception e) {
				System.out.println("Get exception in dual killer:" + e.toString());
				return "Exception happens when enlisting XAResource: " + e.toString() 
						+ ". Please check the web service provider.";
			}
		}
		return "Enlist XAResource1 voting '" + vote1 + (result1 ? "' successful" : " failed")
				+ "; Enlist XAResource2 voting '" + vote2 + (result2 ? "' successful" : " failed")
				+ "; Enlist XAResource3 voting '" + vote3 + (result3 ? "' successful" : " failed")
				+ "; Transaction Manager Status: "
				+ (TMstatus == Status.STATUS_ACTIVE ? "ACTIVE." : TMstatus + ".");
	}


	private String mpr011bParticipant() {
		boolean result1 = false, result2 = false, result3 = false;
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		try {
			final Serializable xaResInfo1 = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1);
			xaRes1.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK).setPrepareAction(XAException.XA_RBROLLBACK);
			
			final Serializable xaResInfo2 = XAResourceInfoFactory
					.getXAResourceInfo(1);
			XAResourceImpl xaRes2 = XAResourceFactoryImpl.instance().
					getXAResourceImpl(xaResInfo2);
			xaRes2.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK).setRollbackAction(XAResourceImpl.DIE);
			
			final Serializable xaResInfo3 = XAResourceInfoFactory
					.getXAResourceInfo(2);
			XAResourceImpl xaRes3 = XAResourceFactoryImpl.instance()
					.getXAResourceImpl(xaResInfo3);
			xaRes3.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);

			final int recoveryId1 = TM.registerResourceInfo(filter,
					xaResInfo1);
			final int recoveryId2 = TM.registerResourceInfo(filter,
					xaResInfo2);
			final int recoveryId3 = TM.registerResourceInfo(filter,
					xaResInfo3);

			result1 = TM.enlist(xaRes1, recoveryId1);
			result2 = TM.enlist(xaRes2, recoveryId2);
			result3 = TM.enlist(xaRes3, recoveryId3);
		} catch (Exception e) {
			return e.getLocalizedMessage();
		}

		return result1 && result2 && result3 ? "successful" : "failed";
	}

	private String mpr011cParticipant(String url) {
		boolean result1 = false, result2 = false, result3 = false;
		final ExtendedTransactionManager TM = TransactionManagerFactory
				.getTransactionManager();

		try {
			final Serializable xaResInfo1 = XAResourceInfoFactory
					.getXAResourceInfo(0);
			XAResourceImpl xaRes1 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo1);
			xaRes1.setPrepareAction(XAException.XA_RBROLLBACK);
			
			final Serializable xaResInfo2 = XAResourceInfoFactory
					.getXAResourceInfo(1);
			XAResourceImpl xaRes2 = XAResourceFactoryImpl.instance().
					getXAResourceImpl(xaResInfo2);
			xaRes2.setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK).setRollbackAction(XAResourceImpl.DIE).setDoomedServer(new URL(url+"/recoveryServer/SuicideServlet"));
			
			final Serializable xaResInfo3 = XAResourceInfoFactory
					.getXAResourceInfo(2);
			XAResourceImpl xaRes3 = XAResourceFactoryImpl.instance().getXAResourceImpl(xaResInfo3).setExpectedDirection(XAResourceImpl.DIRECTION_ROLLBACK);

			final int recoveryId1 = TM.registerResourceInfo(filter,
					xaResInfo1);
			final int recoveryId2 = TM.registerResourceInfo(filter,
					xaResInfo2);
			final int recoveryId3 = TM.registerResourceInfo(filter,
					xaResInfo3);

			result1 = TM.enlist(xaRes1, recoveryId1);
			result2 = TM.enlist(xaRes2, recoveryId2);
			result3 = TM.enlist(xaRes3, recoveryId3);
		} catch (Exception e) {
			return e.getLocalizedMessage();
		}

		return result1 && result2 && result3 ? "successful" : "failed";
	}
}
