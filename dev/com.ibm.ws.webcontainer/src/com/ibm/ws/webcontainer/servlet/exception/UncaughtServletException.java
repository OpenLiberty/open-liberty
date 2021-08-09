/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet.exception;

import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.webapp.WebAppErrorReport;

public class UncaughtServletException extends WebAppErrorReport
{
   /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3834591006632260147L;
private String _targetServletName;
   public UncaughtServletException(String servletName, Throwable cause)
   {
      super("Server caught unhandled exception from servlet [" + servletName + "]: " + cause.getMessage(), cause);
      setErrorCode(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      setTargetServletName(servletName);
   }
}
