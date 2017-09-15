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
package com.ibm.wsspi.webcontainer.async;

import javax.servlet.AsyncEvent;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.ibm.wsspi.webcontainer.servlet.AsyncContext;

/**
 * 
 * Class extending javax AsyncEvent for IBM specific additional functionality
 * @ibm-private-in-use
 */
public class WSAsyncEvent extends AsyncEvent {

	private long elapsedTime;

	public long getElapsedTime() {
		return elapsedTime;
	}

	public WSAsyncEvent(AsyncContext asyncContext,
			ServletRequest servletRequest, ServletResponse servletResponse,
			long elapsedTime) {
		super(asyncContext,servletRequest,servletResponse);
		this.elapsedTime = elapsedTime;
	}

}
