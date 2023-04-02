/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
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
package com.ibm.ws.webcontainer.servlet.exception;

import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.webapp.WebAppErrorReport;

public class ServletUnavailableException extends WebAppErrorReport
{
   /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3544395785904272439L;

   public ServletUnavailableException(String servletName, UnavailableException cause)
   {
      super(cause.getMessage(), cause);
      setTargetServletName(servletName);
      setErrorCode(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
   }
}

