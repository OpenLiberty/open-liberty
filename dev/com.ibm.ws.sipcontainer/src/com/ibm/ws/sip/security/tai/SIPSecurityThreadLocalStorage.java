/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
//package com.ibm.ws.sip.security.tai;
//
//import com.ibm.ws.sip.container.parser.SipAppDesc;
//
//public class SIPSecurityThreadLocalStorage {
//	/**
//     * Holds the Sip Application Discriptor
//     */
//	private static ThreadLocal _sipAppDesc = new ThreadLocal();
//	
//	/**
//	 * Holds servletName
//	 */
//	private static ThreadLocal _sipServletName = new ThreadLocal();
//
//	/**
//	 * get the Sip Application Descriptor
//	 * @return
//	 */
//	public static SipAppDesc getSipAppDesc() {
//		return (SipAppDesc) _sipAppDesc.get();
//	}
//
//	/**
//	 * set the Sip Application Descriptor
//	 * @param appDesc
//	 */
//	public static void setSipAppDesc(SipAppDesc appDesc) {
//		_sipAppDesc.set(appDesc);
//	}
//	
//	/**
//	 * get the Sip ServletName 
//	 * @return
//	 */
//	public static String getSipServletName() {
//		return (String) _sipServletName.get();
//	}
//
//	/**
//	 * set the Sip servlet name
//	 * @param servletName
//	 */
//	public static void setSipServletName(String servletName) {
//		_sipServletName.set(servletName);
//	}
//}
