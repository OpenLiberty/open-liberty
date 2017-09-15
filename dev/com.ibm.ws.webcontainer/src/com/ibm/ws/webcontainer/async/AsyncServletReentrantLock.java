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
package com.ibm.ws.webcontainer.async;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.wsspi.webcontainer.logging.LoggerFactory;

public class AsyncServletReentrantLock extends ReentrantLock {
	protected static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer.async");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer.async.AsyncServletReentrantLock";
	private AtomicBoolean isValid = new AtomicBoolean(true);
	public boolean getAndSetIsValid(boolean value){
		boolean ret = isValid.getAndSet(value);
		if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINEST))
		{
			logger.logp(Level.FINER,CLASS_NAME,"getAndSetIsValid","isValid->"+ret);
		}
		return ret;
	}
}
