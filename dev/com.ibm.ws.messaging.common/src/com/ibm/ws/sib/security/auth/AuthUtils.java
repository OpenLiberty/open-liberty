/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.security.auth;

import javax.security.auth.Subject;

import com.ibm.ws.sib.comms.ConnectionMetaData;
import com.ibm.ws.sib.mfp.JsMessage;

public class AuthUtils {
	
	private boolean isSecurityEnabled = false;
	//As of now just constructing default subject.. in future the subject could be
	//filled up with any relevant details in case if needed.
	private Subject NoSecuritySubject = new Subject();
	
	/**
	 * Passing the Bus Name, if says if the Bus is Secured or not
	 * @param busName
	 * @return
	 */
	public boolean isBusSecure(String busName) {
		/*
		 * Yet to implement this method
		 * The information will come from Admin component, if security is enabled or not
		 * For now we are returning it as false
		 */
		return false;
	}

	public Subject getSIBServerSubject() {
		if(isSecurityEnabled) {
			/*
			 * Get the SIBServer Subject from AuthUtils
			 * authUtils.getSIBServerSubject()
			 */
		}
		return null;
	}

	public String getUserName(Subject subject) {
		if(isSecurityEnabled) {
			/*
			 * Call AuthUtils code of SIB Security Component to get the name
			 * authUtils.getUserName(subject)
			 */
		}
		return null;
	}
	
	public void logout(String busName, Subject subject) {
		if(isSecurityEnabled) {
			/*
			 * Call the below function from SIB Security component
			 * SibLoginFactory.getInstance().createNewSibLogin().logout(busName, _subject);
			 */
		}
	}

	public boolean isSIBServerSubject(Subject subject) {
		boolean result = true;
		if(isSecurityEnabled) {
			/**
			 * Call AuthUtils isSIBServerSubject(subject)
			 */
		}
		return result;
	}

	public boolean sentBySIBServer(JsMessage msg) {
		boolean result = true;
		if(isSecurityEnabled) {
			/*
			 * Call SIBSecurity Service's authUtils.sentBySIBServer(msg)
			 */
		}
		return result;
	}

	public void createBifurcatedConsumerSessionAuthorizationPassed(
			Subject _subject, String destination, long id) {
		if(isSecurityEnabled) {
			/*
			 * Call the SIB Security Code as mentioned below
			 * BusSecurity security = (BusSecurity)_messageProcessor.getMessagingEngine().getEngineComponent(BusSecurity.class);
			 * security.createBifurcatedConsumerSessionAuthorizationPassed(_subject, destination, id);
			 */
		}
	}

	public void deleteDurSubAuthorizationFailed(String theUser,
			String topicName, String topicSpaceName, long l) {
		if(isSecurityEnabled) {
			/*
			 * Call the SIB Security Code as mentioned below
			 * BusSecurity security = (BusSecurity)_messageProcessor.getMessagingEngine().getEngineComponent(BusSecurity.class);
			 * security.deleteDurSubAuthorizationFailed(theUser, topicName, topicSpaceName, 0L); 
			 */
		}
	}

	public void deleteDurSubAuthorizationPassed(String theUser,
			String topicName, String topicSpaceName, long l) {
		if(isSecurityEnabled) {
			/*
			 * Call the SIB Security Code as mentioned below
			 * BusSecurity security = (BusSecurity)_messageProcessor.getMessagingEngine().getEngineComponent(BusSecurity.class);
			 * security.deleteDurSubAuthorizationPassed(theUser, topicName, topicSpaceName, 0L); 
			 */
		}
	}

	public void createBifurcatedConsumerSessionAuthorizationFailed(
			Subject connsSubject, String destinationName, long id) {
		if(isSecurityEnabled) {
			/*
			 * Call the SIB Security Code as mentioned below
			 * BusSecurity security = (BusSecurity)_messageProcessor.getMessagingEngine().getEngineComponent(BusSecurity.class);
			 * security.createBifurcatedConsumerSessionAuthorizationFailed(_subject, destination, id);
			 */
		}
	}
	
	/**
	 * Get the Subject after authentication
	 * <li> If Security is enabled, fetch the Subject from SIBSecurity Service </li>
	 * <li> If Security is disabled, create a new Java Subject </li>
	 * @param userName
	 * @param password
	 * @return
	 */
	public Subject getSubject(String userName, String password) {
		if(isSecurityEnabled) {
			/*
			 * Check if the SIBSecurity Service is loaded. If it is not loaded,
			 * load SIBSecurity Service Invoke the Login method of the
			 * SIBSecurity Service to get the Subject
			 * 
			 * Existing code
			 * SibLoginFactory loginFactory = SibLoginFactory.getInstance();
			 * SibLogin myLogin = loginFactory.createNewSibLogin(); 
			 * subject = myLogin.login(theBus,userName, password);
			 */
			return null;
		} else {
			/*
			 * Create a JAVA Subject and return it, anyways it does not matter
			 */
			return NoSecuritySubject;
		}
	}
	
	/**
	 * Get the Subject after authentication
	 * <li> If Security is enabled, fetch the Subject from SIBSecurity Service </li>
	 * <li> If Security is disabled, create a new Java Subject </li>
	 * @param subject
	 * @return
	 */
	public Subject getSubject(Subject subject) {
		Subject result = null;
		if(isSecurityEnabled) {
			/*
			 * Check if the SIBSecurity Service is loaded. If it is not loaded,
			 * load SIBSecurity Service Invoke the Login method of the
			 * SIBSecurity Service to get the Subject
			 * 
			 * Existing Code 
			 * String userName = getAuthorisationUtils().getUserName(subject); 
			 * SibLoginFactory loginFactory = SibLoginFactory.getInstance(); 
			 * SibLogin myLogin = loginFactory.createNewSibLogin(); 
			 * subject = myLogin.login(theBus, subject);
			 */
		} else {
			/*
			 * Create a JAVA Subject and return it, anyways it does not matter
			 */
			result = new Subject();
		}
		return result;
	}

	public Subject getServerSubject() {
		// TODO Auto-generated method stub
		return null;
	}

	public static boolean isPermittedChain(String bus,
			String inboundTransportChain) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 
	 * @param bus
	 * @param userid
	 * @param metaData
	 * @return
	 */
	public Subject getSubject(String bus, String userid,
			ConnectionMetaData metaData) {
		// TODO Auto-generated method stub
		Subject subject = null;
		if(isSecurityEnabled) {
			/*
			 * Call SIBLoginImpl.login method and get the subject
			 */
		} else {
			subject = new Subject();
		}
		return subject;
	}

	public Subject getSubject(String bus, byte[] securityToken,
			String securityTokenType, ConnectionMetaData metaData) {
		Subject subject = null;
		if(isSecurityEnabled) {
			/*
			 * Call SIBLoginImpl.login method and get the subject
			 */
		} else {
			subject = new Subject();
		}
		return subject;
	}
}
