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

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.servlet.CacheProxyRequest;
import com.ibm.ws.cache.servlet.CacheProxyResponse;
import com.ibm.ws.cache.servlet.FragmentComposer;
import com.ibm.ws.cache.servlet.FragmentComposerFactory;

@Component(service=FragmentComposerFactory.class, property = { "service.ranking:Integer=10" })
public class FragmentComposerFactoryImpl31 implements FragmentComposerFactory {
	private static TraceComponent tc = Tr.register(FragmentComposerFactoryImpl31.class,
			"WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

	public FragmentComposer createFragmentComposer() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, " Create FragmentComposer factory 31");

		return new FragmentComposerServlet31(); 
	}
	
	public FragmentComposer createFragmentComposer(CacheProxyResponse response, CacheProxyRequest request) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			Tr.debug(tc, " Create FragmentComposer factory 31");

		return new FragmentComposerServlet31(request,response); 
	}
}

