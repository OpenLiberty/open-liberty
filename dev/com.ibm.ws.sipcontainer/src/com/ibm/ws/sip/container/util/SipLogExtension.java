/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.util;

import java.util.Iterator;

import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipSession;

import com.ibm.websphere.logging.hpel.LogRecordContext;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
import com.ibm.ws.sip.properties.CoreProperties;

/**
 * HPEL log extension for SIP information.
 * This extension adds the SIP Application Session, Call and SIP Session IDs
 * to HPEL logging. The information will be appended to any message in the log.
 * 
 * When there are several SIP sessions and/or call IDs associated with the same SAS, 
 * only the first two will be added.
 * 
 * @author hagit
 * @since Sep 23, 2012
 */
public class SipLogExtension {
	
	/**
	 * The extension key for Sip Application Session ID..
	 */
	private static final String SAS_ID_EXT_KEY = "SIPASId";
	
	/**
	 * The extension key for call ID.
	 */
	private static final String CALL_ID_EXT_KEY = "SIPCallId";
	
	/**
	 * The extension key for SIP session ID.
	 */
	private static final String SESSION_ID_EXT_KEY = "SIPSessionId";
	
	/**
	 * The extension key for the second call ID.
	 */
	private static final String CALL_ID_2_EXT_KEY = "SIPCallId2";
	
	/**
	 * The extension key for the second SIP session ID.
	 */
	private static final String SESSION_ID_2_EXT_KEY = "SIPSessionId2";
	
	/**
	 * A strong reference to the LogRecordContext.Extension to make sure it is not garbage collected.
	 * The SAS ID extension.
	 */
    private final static LogRecordContext.Extension _sasIdExtension = new LogRecordContext.Extension() {
            public String getValue() {
            	return getSasId();
            }
    };
    
    /**
	 * The call ID extension.
	 */
    private final static LogRecordContext.Extension _callIdExtension = new LogRecordContext.Extension() {
            public String getValue() {
            	return getCallId();
            }
    };
    
    /**
	 * The SIP session ID extension.
	 */
    private final static LogRecordContext.Extension _sessionIdExtension = new LogRecordContext.Extension() {
            public String getValue() {
                    return getSessionId();
            }
    };
    
    /**
	 * The second call ID extension 
	 * (when there's more than one call ID associated to the same SAS, e.g., b2b).
	 */
    private final static LogRecordContext.Extension _addCallIdExtension = new LogRecordContext.Extension() {
            public String getValue() {
                    return getSecondCallId();
            }
    };
    
    /**
	 * The second SIP session ID extension
	 * (when there's more than one SIP session associated to the same SAS, e.g., b2b).
	 */
    private final static LogRecordContext.Extension _addSessionIdExtension = new LogRecordContext.Extension() {
            public String getValue() {
                    return getSecondSessionId();
            }
    };
    
    /**
     * Registers the extensions. 
     */
    public static void init() {
    	//Use custom property to enable/disable the feature
    	if (PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.ENABLE_HPEL_SIP_LOG_EXTENSION)) {
            LogRecordContext.registerExtension(SAS_ID_EXT_KEY, _sasIdExtension);
            LogRecordContext.registerExtension(CALL_ID_EXT_KEY, _callIdExtension);
            LogRecordContext.registerExtension(SESSION_ID_EXT_KEY, _sessionIdExtension);
            LogRecordContext.registerExtension(CALL_ID_2_EXT_KEY, _addCallIdExtension);
            LogRecordContext.registerExtension(SESSION_ID_2_EXT_KEY, _addSessionIdExtension);
    	}
    }
    
    /**
     * Unregisters the extensions. 
     */
    public static void destroy() {
    	//Use custom property to enable/disable the feature
    	if (PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.ENABLE_HPEL_SIP_LOG_EXTENSION)) {
            LogRecordContext.unregisterExtension(SAS_ID_EXT_KEY);
            LogRecordContext.unregisterExtension(CALL_ID_EXT_KEY);
            LogRecordContext.unregisterExtension(SESSION_ID_EXT_KEY);
            LogRecordContext.unregisterExtension(CALL_ID_2_EXT_KEY);
            LogRecordContext.unregisterExtension(SESSION_ID_2_EXT_KEY);
    	}
    }
    
    /**
     * Retrieves the SIP Application ID from ThreadLocal.
     * 
     * @return SAS ID
     */
    private static String getSasId() {
    	
    	SipApplicationSession sas = ThreadLocalStorage.getApplicationSession();
    	if (sas != null) {
    		return sas.getId(); 
    	}
    	
    	//If there's no SAS on ThreadLocal, get the ID from the TU on ThreadLocal
		TransactionUserWrapper tu = ThreadLocalStorage.getTuWrapper();
		if (tu != null) {
			try {
				return tu.getApplicationId();
			} catch (IllegalStateException e) {
				// this exception can be thrown when the transaction is in TERMINATED state
			}
		}
		
		//Look for the SAS ID on the stack's ThreadLocal
		return com.ibm.ws.sip.stack.util.ThreadLocalStorage.getSasID();
    }
    
    /**
     * Retrieves the call ID from ThreadLocal.
     * 
     * @return call ID
     */
    private static String getCallId() {
    	
    	SipApplicationSession sas = ThreadLocalStorage.getApplicationSession();
    	if (sas != null) {
    		Iterator<SipSession> i = sas.getSessions("SIP");
    		//Get the first call ID
    		if (i.hasNext()) {
    			SipSession session = i.next();
    			return session.getCallId();
    		}
    	}
    	
    	//If there's no SAS on ThreadLocal, get the ID from the TU on ThreadLocal
		TransactionUserWrapper tu = ThreadLocalStorage.getTuWrapper();
		if (tu != null) {
			try {
				return tu.getCallId();
			} catch (IllegalStateException e) {
				// this exception can be thrown when the transaction is in TERMINATED state
			}
		}
		
		//Look for the call ID on the stack's ThreadLocal
		return com.ibm.ws.sip.stack.util.ThreadLocalStorage.getCallID();
    }
    
    /**
     * Retrieves the SIP session ID from ThreadLocal.
     * 
     * @return SIP session ID
     */
    private static String getSessionId() {
    	
    	SipApplicationSession sas = ThreadLocalStorage.getApplicationSession();
    	if (sas != null) {
    		Iterator<SipSession> i = sas.getSessions("SIP");
    		//Get the first session ID
    		if (i.hasNext()) {
    			SipSession session = i.next();
    			return session.getId();
    		}
    	}
    	
    	//If there's no SAS on ThreadLocal, get the ID from the TU on ThreadLocal
		TransactionUserWrapper tu = ThreadLocalStorage.getTuWrapper();
		if (tu != null) {
			try {
				return tu.getSipSessionId();
			} catch (IllegalStateException e) {
				// this exception can be thrown when the transaction is in TERMINATED state
			}
		}
		
		return null;
    }
    
    /**
     * Retrieves the second call ID (if exists) from ThreadLocal.
     * 
     * @return second call ID
     */
    private static String getSecondCallId() {
    	
    	SipApplicationSession sas = ThreadLocalStorage.getApplicationSession();
    	
    	if (sas != null) {
    		Iterator<SipSession> i = sas.getSessions("SIP");
    		int sessionsCounter = 0;
    		//When there are several SIP sessions and/or call IDs associated 
    		//with the same SAS, only the first two will be printed.
    		//Here we retrieve the second call ID
    		while (i.hasNext() && sessionsCounter < 2) {
    			SipSession session = i.next();
    			sessionsCounter++;
    			if (sessionsCounter == 2) {
    				return session.getCallId();
    			}
    		}
    	}
    	
		return null;
    }
    
    /**
     * Retrieves the second SIP session ID (if exists) from ThreadLocal.
     * 
     * @return second SIP session ID
     */
    private static String getSecondSessionId() {
    	
    	SipApplicationSession sas = ThreadLocalStorage.getApplicationSession();
    	
    	if (sas != null) {
    		Iterator<SipSession> i = sas.getSessions("SIP");
    		int sessionsCounter = 0;
    		//When there are several SIP sessions and/or call IDs associated 
    		//with the same SAS, only the first two will be printed.
    		//Here we retrieve the second SIP session ID
    		while (i.hasNext() && sessionsCounter < 2) {
    			SipSession session = i.next();
    			sessionsCounter++;
    			if (sessionsCounter == 2) {
    				return session.getId();
    			}
    		}
    	}
    	
		return null;
    }
}