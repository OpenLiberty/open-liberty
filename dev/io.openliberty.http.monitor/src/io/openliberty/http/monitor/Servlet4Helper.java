/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.monitor;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40;
import com.ibm.wsspi.webcontainer.webapp.IWebAppDispatcherContext;

public class Servlet4Helper {

	private static volatile boolean isServlet4Up = false;;
	private static volatile boolean isInit = false;

	public static String getPattern(IWebAppDispatcherContext webAppdispatcherContext) {
		if (!isInit) init();
		
		if (isServlet4Up) {
			if (webAppdispatcherContext instanceof WebAppDispatcherContext40) {
				WebAppDispatcherContext40 webAppDispatcherContext40 = (WebAppDispatcherContext40) webAppdispatcherContext;
				return webAppDispatcherContext40.getServletMapping().getPattern();
			}
			return null;
		} else {
			return null;
		}

	}

	public static boolean isServlet4Up() {
		if (!isInit) init();
		return isServlet4Up;
	}
	
	public static String getMatchValue(IWebAppDispatcherContext webAppdispatcherContext) {
		if (!isInit) init();
		
		if (isServlet4Up) {
			if (webAppdispatcherContext instanceof WebAppDispatcherContext40) {
				WebAppDispatcherContext40 webAppDispatcherContext40 = (WebAppDispatcherContext40) webAppdispatcherContext;
				return webAppDispatcherContext40.getServletMapping().getMatchValue();
			}
			return null;
		} else {
			return null;
		}

	}
	
	public static String getServletPathForMapping(IWebAppDispatcherContext webAppdispatcherContext) {
		if (!isInit) init();
		
		if (isServlet4Up) {
			if (webAppdispatcherContext instanceof WebAppDispatcherContext40) {
				WebAppDispatcherContext40 webAppDispatcherContext40 = (WebAppDispatcherContext40) webAppdispatcherContext;
				return webAppDispatcherContext40.getServletPathForMapping();
			}
			return null;
		} else {
			return null;
		}

	}

	@FFDCIgnore(ClassNotFoundException.class)
	private static void init() {
		try {
			Class.forName("com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40");
			isServlet4Up = true;
		} catch (ClassNotFoundException e) {
			isServlet4Up = false;
		}
		isInit = true;
	}
}
