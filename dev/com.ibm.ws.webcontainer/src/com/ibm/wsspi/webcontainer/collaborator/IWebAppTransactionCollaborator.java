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
package com.ibm.wsspi.webcontainer.collaborator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public interface IWebAppTransactionCollaborator {

	//
	// Collaborator preInvoke.
	//
	// Called before the webapp is invoked, to ensure that a LTC is started
	// in the absence of a global transaction. A global tran may be active
	// on entry if the webapp that caused this dispatch left a global tran
	// active. The WebAppRequestDispatcher ensures that any global tran started
	// during a dispatch is rolledback if not completed during the dispatch.
	// If there is a global tran on entry, then we don't start an LTC.
	// If there isn't a global tran on entry, then we do start an LTC after
	// first suspending any LTC started by a previous dispatch.
	// If an LTC is suspended, the LTC is returned by this method and
	// passed back to the caller who will resupply it to postInvoke() who will resume the
	// suspended Tx.
	//
	// Request is not null for a proper servlet service call. It is null if called for 
	// a servlet context change, for an init or destroy servlet.
	//
	public TxCollaboratorConfig preInvoke(HttpServletRequest request, boolean isServlet23)
			throws ServletException;

	public void postInvoke(HttpServletRequest request, Object txConfig,
			boolean isServlet23) throws ServletException;

}