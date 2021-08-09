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
package com.ibm.ws.webcontainer.servlet;

import javax.servlet.Servlet;


public class TimedServletPoolElement {
	private long _lastAccessedTime;
	private Servlet _servlet;

	public TimedServletPoolElement(Servlet s)
	{
		_servlet = s;
		access();
	}

	long getLastAccessedTime()
	{
		return _lastAccessedTime;
	}

	Servlet getServlet()
	{
		return _servlet;
	}

	void access()
	{
		_lastAccessedTime = System.currentTimeMillis();
	}

	long getIdleTime()
	{
		long idleTime = System.currentTimeMillis() - getLastAccessedTime();
		return idleTime;
	}
}
