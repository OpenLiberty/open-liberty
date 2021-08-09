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
package com.ibm.ws.sib.processor.impl;

import javax.security.auth.Subject;

import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.matching.TopicAuthorization;
import com.ibm.ws.sib.security.auth.OperationType;

public class AccessChecker {
	
	private boolean isSecurityEnabled = false; 
	
	public AccessChecker(MessageProcessor messageProcessor) {
		/*
		 * Create a new instance of Security Component's AccessChecker
		 */
	}
	
	/**
	 * Check the Bus Access for the given Subject
	 * @param subject
	 * @return
	 */
	public boolean checkBusAccess(Subject subject) {
		boolean result = false;
		if(isSecurityEnabled) {
			/*
			 * Check if the Subject has access to the Bus
			 * This will call the SIBSecurity Service
			 */
		} else {
			result = true;
		}
		return result;
	}
	
	public void fireBusAccessNotAuthorizedEvent(String busName, String userName, String nlsProperty) {
		
	}
	
	public void listTopicAuthorisations() {
		if(isSecurityEnabled) {
			/*
			 * Call SIBSecurity Service method of listTopicAuthorization
			 */
		}
	}

	public void setTopicAuthorization(TopicAuthorization _topicAuthorization) {
		if(isSecurityEnabled) {
			/*
			 * Call the AccessChecker in Security Component and invoke this method
			 */
		}
	}

	public void fireDestinationAccessNotAuthorizedEvent(
			String destinationPrefix, String userName, OperationType create,
			String nlsMessage) {
		if(isSecurityEnabled) {
			/*
			 * Call the AccessChecker at the SIBSecurity code to fire an event of not authorized
			 * _accessChecker.fireDestinationAccessNotAuthorizedEvent(destinationPrefix, userName, OperationType.CREATE, nlsMessage);
			 */
		}
	}

	public boolean checkTemporaryDestinationAccess(SecurityContext secContext,
			Object object, String string, OperationType create) {
		boolean result = true;
		if(isSecurityEnabled) {
			/*
			 * Call SIB Security Component's checkTemporaryDestinationAccess method
			 */
		}
		return result;
	}

	public boolean checkDestinationAccess(SecurityContext secContext,
			Object object, String systemDefaultExceptionDestinationPrefix,
			OperationType receive) {
		boolean result = true;
		if(isSecurityEnabled) {
			/*
			 *Call SIB Security Component's AccessChecker.checkDestinationAccess() 
			 */
		}
		return result;
	}

	public boolean checkDiscriminatorAccess(SecurityContext secContext,
			BaseDestinationHandler baseDestinationHandler,
			String discriminator, OperationType operation) {
		boolean result = true;
		if(isSecurityEnabled) {
			/*
			 * Call the AccessChecker from the SIB Security component
			 */
		}
		return result;
	}

	public boolean checkForeignBusAccess(SecurityContext secContext,
			String name, OperationType operation) {
		boolean result = true;
		if(isSecurityEnabled) {
			/*
			 * Call the AccessChecker from the SIB Security component
			 */
		}
		return result;
	}

	public void setSecurityIDInMessage(String _linkOutboundUserid, JsMessage msg) {
		if(isSecurityEnabled) {
			/*
			 * Call the AccessChecker from the SIB Security component
			 */
		}
	}

	public void setSecurityIDInMessage(Subject securitySubject, JsMessage msg) {
		if(isSecurityEnabled) {
			/*
			 * Call the AccessChecker from the SIB Security component
			 */
		}
	}

}
