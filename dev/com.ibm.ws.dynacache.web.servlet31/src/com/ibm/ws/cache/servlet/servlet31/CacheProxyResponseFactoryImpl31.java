/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet.servlet31;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.servlet.CacheProxyResponse;
import com.ibm.ws.cache.servlet.CacheProxyResponseFactory;

import org.osgi.service.component.annotations.Component;

@Component(service=CacheProxyResponseFactory.class, property = { "service.ranking:Integer=10" })
public class CacheProxyResponseFactoryImpl31 implements CacheProxyResponseFactory {
	 private static TraceComponent tc = Tr.register(CacheProxyResponseFactoryImpl31.class,
             "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
	public CacheProxyResponse createCacheProxyResponse(HttpServletResponse response) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
		Tr.debug(tc, " Create cache proxy response factory31 ");
		return new CacheProxyResponseServlet31(response); 
	}
}
