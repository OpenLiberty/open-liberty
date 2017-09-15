/*******************************************************************************
 * Copyright (c) 1997, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.servlet;

import java.util.Collection;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import com.ibm.wsspi.webcontainer.async.WrapperRunnable;

import com.ibm.ws.webcontainer.async.AsyncListenerEntry;
import com.ibm.ws.webcontainer.async.AsyncServletReentrantLock;

/**
 * 
 * 
 * AsyncContext is a private spi for websphere components to make
 * use of Async Servlet features
 * 
 * @ibm-private-in-use
 * 
 * @since   WAS7.0
 * 
 */
public interface AsyncContext extends javax.servlet.AsyncContext{
    public void setRequestAndResponse(ServletRequest servletRequest, ServletResponse servletResponse);

    public void executeNextRunnable();

    public boolean isCompletePending();

	public void invalidate();

	List<AsyncListenerEntry> getAsyncListenerEntryList();

	public void initialize();

	public boolean isDispatchPending();

	public IServletContext getWebApp();

	public Collection<WrapperRunnable> getAndClearStartRunnables();

	public void addStartRunnable(WrapperRunnable wrapperRunnable);

	public void removeStartRunnable(WrapperRunnable wrapperRunnable);

	boolean isDispatching();

	public boolean cancelAsyncTimer();

	public void setInvokeErrorHandling(boolean b);

	public AsyncServletReentrantLock getErrorHandlingLock();

	public void setDispatching(boolean b);

	public boolean isComplete();
}
