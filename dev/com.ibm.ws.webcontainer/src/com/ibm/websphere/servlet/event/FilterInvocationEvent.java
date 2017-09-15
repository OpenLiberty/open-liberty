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
package com.ibm.websphere.servlet.event;

import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;

import com.ibm.wsspi.webcontainer.util.ServletUtil;

/**
 * Event that reports information about a filter invocation.
 * 
 * @ibm-api
 */


public class FilterInvocationEvent extends FilterEvent {
	
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;
	private ServletRequest request = null;

    /**
     * FilterEvent contructor.
     * @param source the object that triggered this event.
     * @param filterConfig the filter's FilterConfig.
     * @param request the current request passed into the Filter.doFilter invocation.
     */
	public FilterInvocationEvent(Object source, FilterConfig filterConfig, ServletRequest request) {
		super(source, filterConfig);
		this.request = request;
	}

	/**
	 * @return Returns the current request passed into the Filter.doFilter method.
	 */
	public ServletRequest getServletRequest() {
		return ServletUtil.unwrapRequest(request);
	}
}
