/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.servlet.cache;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cache.servlet.ESIProcessor;

public class ESIInvalidatorServlet extends HttpServlet implements ExternalCacheAdapter {

	private static final long serialVersionUID = 8842671181312469591L;
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		ESIProcessor.run(request.getRemoteHost(), request.getInputStream(), response.getOutputStream());
	}

	public void setAddress(String address) {
	}

	public void writePages(Iterator externalCacheEntries) {
	}

	public void invalidatePages(Iterator urls) {
	}

	public synchronized void invalidateIds(Iterator ids) {
		ESIProcessor.invalidateIds(ids);
	}

	public void preInvoke(ServletCacheRequest sreq, HttpServletResponse sresp) {
	}

	public void postInvoke(ServletCacheRequest sreq, HttpServletResponse sresp) {
	}

	public void clear() {
		ESIProcessor.clearCaches();
	}

}
