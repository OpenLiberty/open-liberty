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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ejs.j2c.HandleList;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.collaborator.ICollaboratorMetaData;
import com.ibm.wsspi.webcontainer.metadata.WebComponentMetaData;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

public class CollaboratorMetaDataImpl implements ICollaboratorMetaData {
	

	private WebComponentMetaData componentMetaData;
	private HttpServletRequest httpServletRequest;
	private HttpServletResponse httpServletResponse;
	private IWebAppDispatcherContext webAppDispatcherContext;
	private Object securityObject;
	private IServletConfig servletConfig;
	private Object transactionConfig;
	private IServletContext servletContext;
	private boolean postInvokeNecessary;
	private ClassLoader origClassLoader;
	private Object transaction;
	private int callbacksID;
	private HandleList handleList;
    private RequestProcessor requestProcessor;
    private boolean servletRequestCreated;
    private boolean sessionInvokeRequired;

    public CollaboratorMetaDataImpl(WebComponentMetaData componentMetaData,
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse,
			IWebAppDispatcherContext webAppDispatcherContext,
			IServletConfig servletConfig, IServletContext servletContext,RequestProcessor requestProcessor) {
		super();
		this.componentMetaData = componentMetaData;
		this.httpServletRequest = httpServletRequest;
		this.httpServletResponse = httpServletResponse;
		this.webAppDispatcherContext = webAppDispatcherContext;
		this.servletConfig = servletConfig;
		this.servletContext = servletContext;
		this.requestProcessor = requestProcessor;
	}

    public RequestProcessor getRequestProcessor() {
        return requestProcessor;
    }
    
	public WebComponentMetaData getComponentMetaData() {
		return this.componentMetaData;
	}

	public HttpServletRequest getHttpServletRequest() {
		return this.httpServletRequest;
	}

	public HttpServletResponse getHttpServletResponse() {
		return this.httpServletResponse;
	}

	public Object getSecurityObject() {
		return this.securityObject;
	}

	public IServletConfig getServletConfig() {
		return this.servletConfig;
	}

	public Object getTransactionConfig() {
		return this.transactionConfig;
	}

	public IWebAppDispatcherContext getWebAppDispatcherContext() {
		return webAppDispatcherContext;
	}

	public void setWebAppDispatcherContext(
			IWebAppDispatcherContext webAppDispatcherContext) {
		this.webAppDispatcherContext = webAppDispatcherContext;
	}

	public void setComponentMetaData(WebComponentMetaData componentMetaData) {
		this.componentMetaData = componentMetaData;
	}

	public void setHttpServletRequest(HttpServletRequest httpServletRequest) {
		this.httpServletRequest = httpServletRequest;
	}

	public void setHttpServletResponse(HttpServletResponse httpServletResponse) {
		this.httpServletResponse = httpServletResponse;
	}

	public void setSecurityObject(Object securityObject) {
		this.securityObject = securityObject;
	}

	public void setServletConfig(IServletConfig servletConfig) {
		this.servletConfig = servletConfig;
	}

	public void setTransactionConfig(Object transactionConfig) {
		this.transactionConfig = transactionConfig;
	}

	public IServletContext getServletContext() {
		// TODO Auto-generated method stub
		return this.servletContext;
	}

	public void setServletContext(IServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public boolean isPostInvokeNecessary() {
		return postInvokeNecessary;
		
	}

	public void setOrigClassLoader(ClassLoader origClassLoader) {
		this.origClassLoader = origClassLoader;
	}

	public void setPostInvokeNecessary(boolean postInvokeNecessary) {
		this.postInvokeNecessary = postInvokeNecessary;
	}

	public void setTransaction(Object transaction) {
		this.transaction = transaction;
	}

	public ClassLoader getOrigClassLoader() {
		return origClassLoader;
	}

	public Object getTransaction() {
		return transaction;
	}

	public int getCallbacksID() {
		// TODO Auto-generated method stub
		return callbacksID;
	}

	public HandleList getConnectionHandleList() {
		// TODO Auto-generated method stub
		return handleList;
	}

	public void setCallbacksID(int callbacksID) {
		this.callbacksID = callbacksID;
	}

	public void setConnectionHandleList(HandleList handleList) {
		this.handleList = handleList;
	}

    @Override
    public void setServletRequestCreated(boolean notifyServletRequestCreated) {
        this.servletRequestCreated = notifyServletRequestCreated;
    }

    @Override
    public boolean isServletRequestCreated() {
        return servletRequestCreated;
    }

    @Override
    public boolean isSessionInvokeRequired() {
        return this.sessionInvokeRequired;
    }

    @Override
    public void setSessionInvokeRequired(boolean sessionInvoke) {
        this.sessionInvokeRequired = sessionInvoke;
    }


}
