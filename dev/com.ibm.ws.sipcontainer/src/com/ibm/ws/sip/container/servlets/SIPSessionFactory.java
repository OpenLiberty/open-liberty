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
package com.ibm.ws.sip.container.servlets;

import javax.servlet.sip.SipSession;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;

public class SIPSessionFactory {
	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(SIPSessionFactory.class);
    
    /**
     * The Object Grid implementation class for SIP session
     * Eventhough it has similar package name the class itself
     * reside in a different JAR (which is located in WAS/Appserver/lib directory)
     */
    public static final String className = "com.ibm.ws.sip.container.servlets.ObjGridSipSessImpl";


	/**
	 * Creates a sip session based on the given TransactionUser.
	 * 
	 * @param tUser the transaction user to create the sip session for
	 * 
	 * @return the created sip session
	 * 
	 * @see SipSessionImplementation
	 * @see TransactionUserWrapper
	 */
	public static SipSessionImplementation createSIPSession(TransactionUserWrapper tUser) {
		SipSessionImplementation result = new SipSessionImplementation(tUser);
		handleCreatedSession(result, tUser);
    	return result;
	}
	
	public static SipSessionImplementation createDerivedSIPSession(
												TransactionUserWrapper tUser,
												SipSessionImplementation originalSipSession) {
		SipSessionImplementation result =  new SipSessionImplementation(tUser,originalSipSession);
		handleCreatedSession(result, tUser);
    	return result;
	}
	
	/**
	 * Joint operations to be performed on the created SIP session:
	 * set the session on the TransactionUserWrapper, send notifications
	 * to listeners and replicate.
	 * 
	 * @param session the created SIP session
	 * @param tUser   TransactionUserWrapper to set the session on
	 * 
	 * @see SipSessionImplementation
	 * @see TransactionUserWrapper
	 */
	private static void handleCreatedSession(SipSessionImplementation session, TransactionUserWrapper tUser) {
		
		//Set the session on the TU, to avoid infinite loop when
		//the notification below is received by the listeners.
		tUser.getTuBase().setSipSession(session);
		
		//Notify listeners that the session was created. Should be called here
		//after the constructor is finished to avoid infinite loop.
		session.sendSessionNotification(LstNotificationType.SESSION_CREATED);

		//Saves the object in the UnifiedSipSessionMgr tables.
		session.store() ;
	}
}
