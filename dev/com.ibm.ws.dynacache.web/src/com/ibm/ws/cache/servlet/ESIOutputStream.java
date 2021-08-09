/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class ESIOutputStream extends DataOutputStream
{
    private final static TraceComponent tc = Tr.register(ESIOutputStream.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");    
    
    private static boolean proxyIsUp = false;
    
    public ESIOutputStream (OutputStream out) throws IOException
    {
     
        super(out);
        
        if (tc.isEntryEnabled()) 
            Tr.entry(tc, "constructor");


        if (tc.isEntryEnabled()) 
            Tr.exit(tc, "constructor");
    }
    
	public void flush() throws IOException {
		if (tc.isEntryEnabled())
			Tr.entry(tc, "flush() " + proxyIsUp);

		super.out.flush();

		if (tc.isEntryEnabled())
			Tr.exit(tc, "flush()");

		return;

	}

	public ESIInputStream flushWithResponse() throws IOException {
		if (tc.isEntryEnabled())
			Tr.entry(tc, "flushWithResponse() " + proxyIsUp);

		super.out.flush(); // ... but just in case.

		if (tc.isEntryEnabled())
			Tr.exit(tc, "flushWithResponse()");

		return null;

	}
}
