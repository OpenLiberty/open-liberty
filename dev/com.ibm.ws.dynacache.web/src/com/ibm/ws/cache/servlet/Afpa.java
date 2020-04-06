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
package com.ibm.ws.cache.servlet;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.cache.ExternalCacheAdapter;
import com.ibm.websphere.servlet.cache.ExternalCacheEntry;
import com.ibm.websphere.servlet.cache.ServletCacheRequest;

import com.ibm.ws.cache.eca.ECAConnection;
import com.ibm.ws.cache.eca.ECAListener;

public class Afpa extends Thread implements ExternalCacheAdapter {

    private static TraceComponent tc = Tr.register(Afpa.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    boolean listening = false;
    ECAListener listener;
    ExternalCache externalCache[] = new ExternalCache[0];
    HashMap uris = new HashMap();

    /**
      * Set the TCP/IP address of the cache adapter
      */
    public void setAddress(String address) {
        try {
            if (listening) {
                // error, only one listening port is supported
                return;
            }
            listener = new ECAListener(Integer.valueOf(address).intValue());
            setDaemon(true);
            start();
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.Afpa.setAddress", "64", this);
        }
    }

    public void run() {
        // On z/OS, in the SR, this iterates only once
        // to populate a dummy ExternalCache object.
        while (ECAListener.isAccepting()) {
            try {
                ECAConnection conn = 
                    (ECAConnection)AccessController.doPrivileged(
                        new PrivilegedExceptionAction() {
                            public Object run() throws IOException {
                                return listener.accept();
                            }});
                InputStream is = conn.getInputStream();
                OutputStream os = conn.getOutputStream();
                DataOutputStream dos = new DataOutputStream(os);
                synchronized (this) {
                    ExternalCache newEXT[] = new ExternalCache[externalCache.length + 1];
                    System.arraycopy(externalCache, 0, newEXT, 0, externalCache.length);
                    newEXT[newEXT.length - 1] = new ExternalCache();
                    newEXT[newEXT.length - 1].dos = dos;
                    newEXT[newEXT.length - 1].is = is;
                    newEXT[newEXT.length - 1].connection = conn;
                    externalCache = newEXT;
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Connected to an AFPA enabled server at" + conn.getInetAddress());
            } catch (Exception ex) {
                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.Afpa.run", "93", this);
            }
        }
    }

    /**
     * This method writes pages to the external cache.
     *
     * @param externalCacheEntries The Enumeration of ExternalCacheEntry
     * objects for the pages that are to be cached.
     */
    public void writePages(Iterator externalCacheEntries) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "writePages");
        while (externalCacheEntries.hasNext()) {
            ExternalCacheEntry e = (ExternalCacheEntry) externalCacheEntries.next();
            String virtualHost = e.host;
            String uri = e.uri;
            HashSet vhosts = (HashSet) uris.get(uri);
            if (vhosts == null) {
                vhosts = new HashSet();
                uris.put(uri, vhosts);
            }
            vhosts.add(virtualHost);

            fixupHeaderTable(e);
            String contentType = getHeader(e, "content-type");
            String contentEncoding = getHeader(e, "content-encoding");
            String contentLanguage = getHeader(e, "content-language");
            String contentCharset = getHeader(e, "content-charset");
            String eTag = getHeader(e, "etag");
            String expires = getHeader(e, "expires");
            String cacheControl = getHeader(e, "cache-control");

            synchronized (this) {
                for (int i = 0; i < externalCache.length; i++) {
                    DataOutputStream dos = externalCache[i].dos;
                    try {
                        dos.writeInt(0);                // operation 0=add
                        dos.writeUTF(virtualHost);      // virtual host
                        dos.writeUTF(uri);              // uri
                        dos.writeInt(e.content.length); // content length
                        dos.write(e.content, 0, e.content.length); // content
                        dos.writeUTF(contentType);      // contentType
                        dos.writeUTF(contentEncoding);  // contentEncoding
                        dos.writeUTF(contentLanguage);  // contentLanguage
                        dos.writeUTF(contentCharset);   // contentCharset
                        dos.writeUTF(eTag);             // eTag
                        dos.writeUTF(expires);          // expires
                        dos.writeUTF(cacheControl);     // cacheControl
                        //dos.writeUTF(""); //TEMP!!!!  // cacheControl
                        dos.flush();
                    } catch (Exception ex) {
                        com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.Afpa.writePages", "145", this);
                        removeConnection(i);
                    }
                }
            } //sync
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "writePages");
    }

    private synchronized void removeConnection(int removeIdx) {
        ExternalCache newEXT[] = new ExternalCache[externalCache.length - 1];
        int j = 0;
        for (int i = 0; i < externalCache.length; i++) {
            if (i != removeIdx) {
                newEXT[j] = externalCache[i];
                j++;
            } else {
                try {
                    externalCache[removeIdx].dos.close();
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.Afpa.removeConnection", "163", this);
                };
                try {
                    externalCache[removeIdx].is.close();
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.Afpa.removeConnection", "166", this);
                };
                try {
                    externalCache[removeIdx].connection.close();
                } catch (Exception ex) {
                    com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.Afpa.removeConnection", "169", this);
                };
            }
        }
        externalCache = newEXT;
    }

    /**
     * This method invalidates pages that are in the external cache.
     *
     * @param urls The Enumeration of URLs for the pages that have
     * previously been written to the external cache and need invalidation.
     */
    public void invalidatePages(Iterator urls) {
        while (urls.hasNext()) {
            String uri = (String) urls.next();
            HashSet vhosts = (HashSet) uris.remove(uri);
            if (vhosts != null) {
                Iterator it = vhosts.iterator();
                while (it.hasNext()) {
                    String virtualHost = (String) it.next();
                    synchronized (this) {
                        for (int i = 0; i < externalCache.length; i++) {
                            DataOutputStream dos = externalCache[i].dos;            
                            try {
                                dos.writeInt(1);            // operation 1=remove
                                dos.writeUTF(virtualHost);  // virtual host
                                dos.writeUTF(uri);          // uri
                                dos.flush();
                            } catch (Exception ex) {
                                com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.servlet.Afpa.invalidatePages", "200", this);
                                removeConnection(i);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This method invalidates all pages from the external cache.
     */
	public void clear() {
		while (externalCache.length != 0)
			removeConnection(0);
	}

    private void fixupHeaderTable(ExternalCacheEntry e) {
        for (int i = 0; i < e.headerTable[0].size(); i++)
            e.headerTable[0].setElementAt(((String) e.headerTable[0].elementAt(i)).toLowerCase(), i);
    }

    private String getHeader(ExternalCacheEntry e, String name) {
        //       String s = (String) e.headerTable.get(name);
        //       if (s==null) return "";
        //       return s;
        int index = e.headerTable[0].indexOf(name);
        if (index == -1)
            return "";
        return(String) e.headerTable[1].get(index);
    }

    /**
     * This method invalidates dependency ids that are in the external cache.
     *
     * @param ids The Enumeration of dependency ids that must be invalidated
     */
    public void invalidateIds(Iterator ids) {
        //nothing to do
    }

    /**
     * This method is invoked before processing a cache hit or miss
     * of an externally cacheable element
     *
     * @param sreq    The request object being used for this invocation
     * @param sresp   The response object being used for this invocation
     */
    public void preInvoke(ServletCacheRequest sreq, HttpServletResponse sresp) {
        //nothing to do
    }

    /**
     * This method is invoked after processing a cache hit or miss
     * of an externally cacheable element
     *
     * @param sreq    The request object being used for this invocation
     * @param sresp   The response object being used for this invocation
     */
    public void postInvoke(ServletCacheRequest sreq, HttpServletResponse sresp) {
        //nothing to do
    }

    class ExternalCache {
        public DataOutputStream dos;
        public InputStream is;
        public ECAConnection connection;
    }

}
