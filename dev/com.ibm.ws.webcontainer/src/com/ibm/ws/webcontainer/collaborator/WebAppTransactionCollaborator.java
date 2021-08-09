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
package com.ibm.ws.webcontainer.collaborator;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.ibm.wsspi.webcontainer.collaborator.IWebAppTransactionCollaborator;
import com.ibm.wsspi.webcontainer.collaborator.TxCollaboratorConfig;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

public class WebAppTransactionCollaborator implements
		IWebAppTransactionCollaborator {
	protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.collaborator");
	private static final String CLASS_NAME="com.ibm.ws.webcontainer.collaborator.WebAppTransactionCollaborator";

	public TxCollaboratorConfig preInvoke(HttpServletRequest request, boolean isServlet23)
			throws ServletException {
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
    		logger.entering(CLASS_NAME,"preInvoke", new Object [] {request, Boolean.valueOf(isServlet23)});
    		logger.exiting(CLASS_NAME,"preInvoke",null);
		}
		return null;
	}

	public void postInvoke(HttpServletRequest request, Object txConfig,
			boolean isServlet23) throws ServletException {
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable (Level.FINE)){
    		logger.entering(CLASS_NAME,"postInvoke", new Object [] {request, txConfig, Boolean.valueOf(isServlet23)});
    		logger.exiting(CLASS_NAME,"postInvoke",null);
		}

	}

}
