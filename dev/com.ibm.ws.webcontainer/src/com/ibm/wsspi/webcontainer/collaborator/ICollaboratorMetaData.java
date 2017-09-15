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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.j2c.HandleList;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

public interface ICollaboratorMetaData {
	public WebComponentMetaData getComponentMetaData();
	public HttpServletRequest getHttpServletRequest();
	public HttpServletResponse getHttpServletResponse();
	public IServletConfig getServletConfig();
	public IServletContext getServletContext();
	public IWebAppDispatcherContext getWebAppDispatcherContext();
	public Object getSecurityObject();
	public void setSecurityObject(Object securityObject);
	public Object getTransactionConfig();
	public void setTransactionConfig(Object transactionConfig);
	public void setPostInvokeNecessary(boolean b);
	public boolean isPostInvokeNecessary();
	public void setTransaction(Object transaction);
	public Object getTransaction();
	public void setOrigClassLoader(ClassLoader origClassLoader);
	public ClassLoader getOrigClassLoader();
	public void setCallbacksID(int callbacksID);
	public int getCallbacksID( );
	public void setConnectionHandleList(HandleList handleList);
	public HandleList getConnectionHandleList();
    public RequestProcessor getRequestProcessor();
    public void setServletRequestCreated(boolean notifyServletRequestCreated);
    public boolean isServletRequestCreated();
    public boolean isSessionInvokeRequired();
    public void setSessionInvokeRequired(boolean sessionInvoke);
}
