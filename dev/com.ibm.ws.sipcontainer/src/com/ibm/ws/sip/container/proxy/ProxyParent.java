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
package com.ibm.ws.sip.container.proxy;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

/**
 * ProxyDirector and ProxyBranch will implement this interface. both those
 * objects can be a parent for ProxyBranch.
 * 
 * Each time when a request is prxyied - Proxy Branch is created, and
 * ProxyDirector is it's parent.
 * 
 * When 3xx response received new ProxyBranch created and in this case the
 * ProxyBranch that received 3xx response is a parent for the new ProxyBranch.
 * 
 * @author anat
 * 
 */
public interface ProxyParent {
	
	/**
	 * Method which notifies the parent about final best response
	 * @param response
	 */
	void processResponse(ProxyBranchImpl branch, SipServletResponse response);
	
	/**
	 * Method which notifies the parent about 1xx response
	 * @param response
	 */
	void process1xxResponse(SipServletResponse response,ProxyBranchImpl branch);
	
	/**
	 * Method called from the child node (ProxyBranch to StatefullProxy or
	 * recurse ProxyBranch to ProxyBranch)
	 * @param branch
	 * @param request
	 */
	void onSendingRequest(ProxyBranchImpl branch, SipServletRequest request);

	/**
	 * Gets the parent of this proxy node.
	 * @return the parent of this proxy node, or null if this is the root
	 */
	ProxyParent getParent();
}
