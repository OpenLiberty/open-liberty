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

public class NoTargetForURIException extends WebAppErrorReport
{
   /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 3257565122414392883L;

public NoTargetForURIException(String uri)
   {
      super("No target servlet configured for uri: " + uri);
      setErrorCode(HttpServletResponse.SC_NOT_FOUND);
   }
}
