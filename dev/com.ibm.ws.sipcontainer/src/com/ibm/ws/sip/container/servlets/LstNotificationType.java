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

/**
 * Helper class which defined different type of Listeners notifications
 * on ApplicationSession.
 * 
 * @author anat
 *
 */
public enum LstNotificationType {
	APP_ATTRIBUTE_ADDED,
	APP_ATTRIBUTE_REPLACED,
	APP_ATTRIBUTE_REMOVED,
	APP_ATTRIBUTE_BOUND,
	APP_ATTRIBUTE_UNBOUND,
	APPLICATION_CREATED,
	APPLICATION_DESTROYED,
	APPLICATION_EXPIRED,
	SESSION_ATTRIBUTE_ADDED,
	SESSION_ATTRIBUTE_REPLACED,
	SESSION_ATTRIBUTE_REMOVED,
	SESSION_CREATED,
	SESSION_DESTROYED,
	SESSION_ATTRIBUTE_BOUND,
	SESSION_ATTRIBUTE_UNBOUND,
	SESSION_READY_TO_INVALIDATE,
	UNMATCHED_REQUEST_RECEIVED,
	UNMATCHED_RESPONSE_RECEIVED
}
