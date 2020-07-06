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
package com.ibm.ws.webcontainer.async;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.webcontainer.async.ListenerHelper.CheckDispatching;
import com.ibm.ws.webcontainer.async.ListenerHelper.ExecuteNextRunnable;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;
import com.ibm.ws.webcontainer.srt.SRTServletRequestThreadData;
import com.ibm.wsspi.webcontainer.servlet.AsyncContext;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

public class AsyncTimeoutRunnable implements Runnable {
	private static Logger logger= Logger.getLogger("com.ibm.ws.webcontainer.async");
    private static final String CLASS_NAME="com.ibm.ws.webcontainer.async.AsyncTimeoutRunnable";
	private AsyncContext asyncContext;
	private AsyncServletReentrantLock asyncServletReentrantLock;
        private SRTServletRequestThreadData requestDataOnTimedOutThread;
	
	public AsyncTimeoutRunnable(AsyncContext asyncContext, IExtendedRequest req) {
		if (logger.isLoggable(Level.FINEST)) {
                    logger.logp(Level.FINEST,CLASS_NAME, "<init>","this->"+this+", asyncContext->"+asyncContext);   
                }
		this.asyncContext = asyncContext;
                this.asyncServletReentrantLock = asyncContext.getErrorHandlingLock();
                SRTServletRequestThreadData existing = req instanceof SRTServletRequest ? SRTServletRequestThreadData.getInstance(((SRTServletRequest) req).getRequestData()) : SRTServletRequestThreadData.getInstance();
                requestDataOnTimedOutThread = new SRTServletRequestThreadData();
                requestDataOnTimedOutThread.init(existing);
	}
		
	@Override
	public void run() {
	        SRTServletRequestThreadData.getInstance().init(requestDataOnTimedOutThread);
		ListenerHelper.invokeAsyncErrorHandling(this.asyncContext, null, null, AsyncListenerEnum.TIMEOUT,ExecuteNextRunnable.TRUE,CheckDispatching.TRUE,this.asyncServletReentrantLock);
	}

}
