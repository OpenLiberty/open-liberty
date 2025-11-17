/*******************************************************************************
 * Copyright (c) 2003,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.sip.security.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.ibm.ws.common.crypto.CryptoUtils;

/**
 * @author Nitzan, May 27 2005
 * Stores variables staticly in a thread scope. 
 */
public class ThreadLocalStorage 
{
    /**
     * Holds the Messagedigest
     */
	private static ThreadLocal _msgDigest = new ThreadLocal();
	
	
	/**
	 * create MessageDigest for this thread
	 * @return MessageDigest object
	 */
	private static MessageDigest createMsgDigest(){
		MessageDigest digester = null;
		try {
			//Based on whether fips is enabled or not
			digester = CryptoUtils.isFips140_3Enabled() ? MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_SHA256) : MessageDigest.getInstance(CryptoUtils.MESSAGE_DIGEST_ALGORITHM_MD5);
			_msgDigest.set( digester);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	    return digester;
	}
	
	
	
	/**
	 * Get MessageDigest
	 * @return MessageDigest object
	 */
	public static MessageDigest getMessageDigest()
	{
		MessageDigest digester = (MessageDigest)_msgDigest.get();
		if(digester==null){
			digester = createMsgDigest();
		}
		
		return digester;
	}
}
