/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.security.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
			digester = MessageDigest.getInstance("MD5");
			_msgDigest.set( digester);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
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
