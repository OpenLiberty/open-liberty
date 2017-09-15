/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.eca;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;


// Created by MD18759

public class ECAConnection  

{  
    private static TraceComponent tc = Tr.register(ECAConnection.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    private static boolean usingSocket = true;

    Socket sock;


    public ECAConnection (Socket s)
    {
        sock = s;
    }
 	
    public InputStream getInputStream()
    {
     	if (tc.isEntryEnabled())
            Tr.entry(tc, "getInputStream");
        
        InputStream s = null;

        try
        {
            if (usingSocket)
                s = sock.getInputStream();
        }
        catch (java.io.IOException e)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getInputStream exception "+e);
        }
        
        if (tc.isEntryEnabled())
                Tr.exit(tc, "getInputStream stream : "+s);

        return s;
    }
       
    public OutputStream getOutputStream()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getOutputStream");
        
        OutputStream o = null;
        
        try
        {
            if (usingSocket)
                o = sock.getOutputStream();
            
        }
        catch (java.io.IOException e)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getOutputStream exception "+e);
        }

        if (tc.isEntryEnabled())
                Tr.exit(tc, "getOutputStream stream : "+o);
        
        return o;
    }
    
    public InetAddress getInetAddress()
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getInetAddress");
        
        InetAddress a = null;

        try
        {
            if (usingSocket)
                a = sock.getInetAddress();
            else
                a= InetAddress.getLocalHost();
        }
        catch (java.io.IOException e)
        {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getInetAddress exception "+e);
        }
        
        if (tc.isEntryEnabled())
                Tr.exit(tc, "getInetAddress addr : "+a);

        return a;
    }

    public void close()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "close");
        
        if (usingSocket)
            try
            {
                sock.close();
            }
            catch (java.io.IOException e)
            {
                if (tc.isDebugEnabled())
                     Tr.debug(tc, "close exception "+e);
            }
        
        if (tc.isEntryEnabled())
            Tr.exit(tc, "close");
    }
}
