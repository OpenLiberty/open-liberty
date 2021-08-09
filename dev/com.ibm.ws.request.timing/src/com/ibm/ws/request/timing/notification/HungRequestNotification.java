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

package com.ibm.ws.request.timing.notification;

/**
 * Hung request notification interface.
 * 
 */
public interface HungRequestNotification {
	
	/*
	 * Notify implementer that a request is hung.
	 * 
	 * @parm requestId : Request id of the request that is hung.
	 * @parm threadId  : Thread id of the request that is hung.
	 */
	void hungRequestDetected(String requestId, long threadId);

}
