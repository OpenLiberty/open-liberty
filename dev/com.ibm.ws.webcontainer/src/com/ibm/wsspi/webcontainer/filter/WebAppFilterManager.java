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
package com.ibm.wsspi.webcontainer.filter;

import java.io.IOException;
import java.util.EnumSet;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.collaborator.CollaboratorInvocationEnum;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;

public interface WebAppFilterManager {

	/**
	 * Invokes the filters defined for a webapp.
	 * 
	 * @param request
	 * @param response
	 * @param target
	 * @param context
	 * @param invoker
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
    public boolean invokeFilters(ServletRequest request, ServletResponse response, IServletContext context, RequestProcessor requestProcessor, EnumSet<CollaboratorInvocationEnum> colEnum) 
		throws ServletException,IOException;

}
