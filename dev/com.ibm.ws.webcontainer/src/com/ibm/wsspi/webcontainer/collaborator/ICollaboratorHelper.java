/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.collaborator;

import java.io.IOException;
import java.util.EnumSet;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.servlet.error.ServletErrorReport;
import com.ibm.ws.webcontainer.spiadapter.collaborator.IInvocationCollaborator;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.security.SecurityViolationException;

public interface ICollaboratorHelper {


	public  Object processSecurityPreInvokeException(
			SecurityViolationException wse, RequestProcessor requestProcessor,
			HttpServletRequest request, HttpServletResponse response,
			WebAppDispatcherContext dispatchContext, WebApp context, String name)
			throws ServletErrorReport;
	public IWebAppTransactionCollaborator getWebAppTransactionCollaborator();

	public  IConnectionCollaborator getWebAppConnectionCollaborator();

	public  void doInvocationCollaboratorsPreInvoke(
			IInvocationCollaborator[] webAppInvocationCollaborators,
			com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData cmd,
			ServletRequest request, ServletResponse response);

	public  void doInvocationCollaboratorsPostInvoke(
			IInvocationCollaborator[] webAppInvocationCollaborators,
			com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData cmd,
			ServletRequest request, ServletResponse response);

	public  void doInvocationCollaboratorsPreInvoke(
			IInvocationCollaborator[] webAppInvocationCollaborators,
			com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData cmd);

	public  void doInvocationCollaboratorsPostInvoke(
			IInvocationCollaborator[] webAppInvocationCollaborators,
			com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData cmd);

	public  IWebAppSecurityCollaborator getSecurityCollaborator();

	
	  public IWebAppNameSpaceCollaborator getWebAppNameSpaceCollaborator();

	  public void preInvokeCollaborators (ICollaboratorMetaData collabMetaData, EnumSet<CollaboratorInvocationEnum> colEnum) throws ServletException, IOException, Exception;
	  
	  public void postInvokeCollaborators (ICollaboratorMetaData collabMetaData, EnumSet<CollaboratorInvocationEnum> colEnum) throws ServletException, IOException, Exception;

}