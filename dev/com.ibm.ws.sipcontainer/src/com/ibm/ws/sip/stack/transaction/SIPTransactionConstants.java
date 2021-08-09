/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transaction;

public class SIPTransactionConstants
{
	private SIPTransactionConstants(){}
	
	/** every transaction should begin with this string */
	public static final String BRANCH_MAGIC_COOKIE = "z9hG4bK";
	public static final int BRANCH_MAGIC_COOKIE_SIZE = 7;
	
	/** base number to set transactions time */
	public static final long T1 = SIPTransactionStack.instance().getConfiguration().getTimerT1();
	
	/** base number to set transactions time */
	public static final long T2 = SIPTransactionStack.instance().getConfiguration().getTimerT2(); 
	
	/** base number to set transactions time */
	public static final long _64T1 = 64*T1; 
	
	/** 
	 * T4 represents the amount of time the network will take to clear messages between 
	 * client and server transactions.
	 * The default value of T4 is 5s */
	public static final long T4 = SIPTransactionStack.instance().getConfiguration().getTimerT4();
	
	/** sip uri parameters */
	public static final String UDP = "udp";
	public static final String TCP = "tcp";
	public static final String TLS = "tls";
	public static final String TRANSPORT = "transport";
	public static final String MADDR = "maddr";
	public static final String TTL = "ttl";
	public static final String LR = "lr";
	public static final String SIP = "sip";
	public static final String SIPS = "sips";
	public static final String ALIAS = "alias";
	public static final String RECEIVED = "received";		
	
	/** sip returned codes */
	public static final int  RETCODE_INFO_TRYING = 100;	
	public static final int RETCODE_CLIENT_BAD_REQUEST         	= 400;
	public static final int RETCODE_CLIENT_REQUEST_TIMEOUT     	= 408;	
}
