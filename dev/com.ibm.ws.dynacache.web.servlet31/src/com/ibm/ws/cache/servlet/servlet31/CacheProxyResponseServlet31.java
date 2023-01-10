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

/**
 * This class is a proxy to the WebSphere response object.
 * It has features added to enable caching.
 */
public class CacheProxyResponseServlet31 extends CacheProxyResponse {

    private static TraceComponent tc = Tr.register(CacheProxyResponseServlet31.class,
                                                   "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    /**
     * Constructor with parameter.
     * 
     * @param proxiedResponse The WebSphere response being proxied.
     */
    public CacheProxyResponseServlet31(HttpServletResponse proxiedResponse) {
        super(proxiedResponse);
    }

   
	@Override    
	public void setContentLengthLong(long length) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setContentLengthLong: length=" + length);

        if (composerActive)
            ((FragmentComposerServlet31)fragmentComposer).setContentLengthLong(length);           
			 super.setContentLengthLong(length);
	}
}