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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class ESIInputStream extends DataInputStream
{
    private final static TraceComponent tc = Tr.register(ESIInputStream.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    
    public ESIInputStream (InputStream in)
    {
        super(in);   
    }

    public ESIInputStream (byte[] in)                                 
    {
        super(new ByteArrayInputStream(in));
    }

}


