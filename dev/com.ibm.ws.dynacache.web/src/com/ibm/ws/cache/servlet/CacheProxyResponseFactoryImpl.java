/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

@Component(service=CacheProxyResponseFactory.class, property = { "service.vendor=IBM" })
public class CacheProxyResponseFactoryImpl implements CacheProxyResponseFactory {
	private static TraceComponent tc = Tr.register(CacheProxyResponseFactoryImpl.class,
			"WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

	public CacheProxyResponse createCacheProxyResponse(HttpServletResponse response) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, " Create cache proxy response factory ");

		return new CacheProxyResponse(response); 
	}
}
