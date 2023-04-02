/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
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
package com.ibm.ws.sip.container.servlets;

import javax.servlet.sip.SipServletRequest;

/**
 * @author yaronr
 *
 * The SIP dialog listener, activated by the session when a new transaction 
 * 	arrived  to the dialog
 */
public interface SipDialogListener
{
	/**
	 * A new transaction arrived to the session
	 * 
	 * @param request the request that create this transaction
	 */
	public void newTransaction(SipServletRequest request);
}
