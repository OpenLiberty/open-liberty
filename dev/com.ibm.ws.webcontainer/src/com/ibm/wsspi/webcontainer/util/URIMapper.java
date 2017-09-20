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
package com.ibm.wsspi.webcontainer.util;

import com.ibm.ws.webcontainer.core.RequestMapper;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.ws.webcontainer.WebContainer;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;


public class URIMapper extends com.ibm.ws.webcontainer.util.URIMapper implements RequestMapper
{
	public URIMapper()
	{
	    // Servlet 4.0 : Use URIMatcherFactory
	    matcher = WebContainer.getWebContainer().getURIMatcherFactory().createURIMatcher();
	}
	
	public URIMapper(boolean scalable)
	{
	    // Serlvet 4.0 : Use URIMatcherFactory
	    matcher = WebContainer.getWebContainer().getURIMatcherFactory().createURIMatcher(scalable);
	}

	/**
	 * @see com.ibm.ws.core.RequestMapper#map(String)
	 */
	public RequestProcessor map(String reqURI)
	{
		RequestProcessor r = (RequestProcessor) matcher.match(reqURI);
		return r;
	}
	
	public Object replaceMapping(String path, Object target) throws Exception
	{
		return matcher.replace(path, target);
	}

	/**
	 * @see com.ibm.ws.core.RequestMapper#map(IWCCRequest)
	 */
	public RequestProcessor map(IExtendedRequest req)
	{
		RequestProcessor r = (RequestProcessor)((URIMatcher)matcher).match(req);
		return r;
	}

	/* (non-Javadoc)
	 * @see com.ibm.ws.webcontainer.core.RequestMapper#exists(java.lang.String)
	 */
	public boolean exists(String path)
	{
		return matcher.exists(path);
	}

}
