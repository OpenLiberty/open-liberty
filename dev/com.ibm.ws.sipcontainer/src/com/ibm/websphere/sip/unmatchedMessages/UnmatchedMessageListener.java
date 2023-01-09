/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.sip.unmatchedMessages;

import java.util.EventListener;

import com.ibm.websphere.sip.unmatchedMessages.events.UnmatchedRequestEvent;
import com.ibm.websphere.sip.unmatchedMessages.events.UnmatchedResponseEvent;

/**
 * @ibm-api
 * 
 * Implementations of this interface are notified when unmatched incoming
 * request or response messages received in SIP Container.
 * 
 * @author Anat Fradin
 */
public interface UnmatchedMessageListener extends EventListener{

	/**
	 * Unmatched request received by SIP Container.
	 * @param request
	 */
	public void unmatchedRequestReceived(UnmatchedRequestEvent evt);
	
	/**
	 * Unmatched response received by SIP Container.
	 * @param response
	 */
	public void unmatchedResponseReceived(UnmatchedResponseEvent evt);
}
