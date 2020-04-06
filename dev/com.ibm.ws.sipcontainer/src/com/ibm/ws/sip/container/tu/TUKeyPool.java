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
package com.ibm.ws.sip.container.tu;

import jain.protocol.ip.sip.message.Response;

import com.ibm.ws.sip.container.servlets.SipServletMessageImpl.MessageType;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.util.ContainerObjectPool;

public class TUKeyPool {
	
	private final static String MAX_TU_KEY_POOL_SIZE = "max.tukey.pool.size";
    
	/** pool of TransactionUsers objects */
    public static ContainerObjectPool s_tuKeyPool = 
    		new ContainerObjectPool(TUKey.class,MAX_TU_KEY_POOL_SIZE);
    
    /**
     * This Object is used to synchronize between reusing operation.
     */
    private static final Object _syncObject = new Object();
    
    protected static TUKey getTUKey(){
		return (TUKey)s_tuKeyPool.get();
	}
	
	/**
	 * Method that retunes the TUKey object to the pool
	 * @param key
	 */
	public static void finishToUseKey(TUKey key){
		synchronized (_syncObject) {
			key.cleanKey();
		    s_tuKeyPool.putBack(key);
		   }		
	}
	
	/**
	 * Method used for Proxy mode for INCOMING request
	 * @param request
	 * @param type
	 * @param sessionId session ID extraced from the Route header
	 */
	public static TUKey getTUKey(SipServletRequestImpl request, String sessionId, MessageType type){
		TUKey key = TUKeyPool.getTUKey();
		key.setParams(request, sessionId, type);
		return key;
	}
	
	
	/**
	 * Method used for Proxy mode for INCOMING request
	 * @param request
	 * @param type
	 * @param sessionId session ID extraced from the Route header
	 */
	public static TUKey getTUKey(Response resp, String keyStr ,boolean isProxy){
		TUKey key = TUKeyPool.getTUKey();
		key.setParams(resp, keyStr, isProxy);
		return key;
	}
	
	/**
	 * Method used for UAC/UAS mode for INCOMING request
	 * @param request
	 * @param type
	 */
	public static TUKey getTUKey(SipServletRequestImpl request, MessageType type){
		TUKey key = TUKeyPool.getTUKey();
		key.setParams(request, type);
		return key;
	}
	
	/**
	 * Ctor used for UAC/UAS mode for INCOMING request
	 * @param request
	 * @param type
	 */
	public static TUKey getTUKey_1(Response response, MessageType type){
		TUKey key = TUKeyPool.getTUKey();
		key.setParams(response, type);
		return key;
	}
    
}
