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
package com.ibm.ws.webcontainer.util;

import com.ibm.ws.webcontainer.core.RequestMapper;
import com.ibm.wsspi.webcontainer.RequestProcessor;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 * @author asisin
 *
 */

public class VirtualHostMapper extends com.ibm.ws.util.VirtualHostMapper implements RequestMapper
{	
	/**
	 * @param vHostKey - the alias:port string
	 * @return RequestProcessor
	 */
	public RequestProcessor map(String vHostKey)
	{
        return (RequestProcessor) super.getMapping(vHostKey.toLowerCase());
	}
	
	public void addMapping(String vHostKey, Object target) {
		super.addMapping(vHostKey.toLowerCase(), target);
	}
	public Object getMapping(String vHostKey) {
		return super.getMapping(vHostKey.toLowerCase());
	}
	public void removeMapping(String vHostKey) {
		super.removeMapping(vHostKey.toLowerCase());
	}
	public Object replaceMapping(String vHostKey, Object target) throws Exception {
		return super.replaceMapping(vHostKey.toLowerCase(), target);
	}
	public boolean exists(String vHostKey) {
		return super.exists(vHostKey.toLowerCase());
	}
    
    public boolean exactMatchExists(String vHostKey) {
        return super.exactMatchExists(vHostKey.toLowerCase());
    }

    //PK65158
    protected Object findExactMatch(String vHostKey){
    	return super.findExactMatch(vHostKey.toLowerCase());
    }
    
    /**
	 * @see com.ibm.ws.core.RequestMapper#map(IWCCRequest)
	 */
	public RequestProcessor map(IExtendedRequest req)
	{
		return null;
	}
}
