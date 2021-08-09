/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.myfaces.view.facelets.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

/**
 * @author Roland Huss
 * 
 */
public final class Resource
{

    //protected final static Logger log = Logger.getLogger("facelets.factory");
    protected final static Logger log = Logger.getLogger(Resource.class.getName());

    /**
     * Get an URL of an internal resource. First, {@link javax.faces.context.ExternalContext#getResource(String)} is
     * checked for an non-null URL return value. In the case of a null return value (as it is the case for Weblogic 8.1
     * for a packed war), a URL with a special URL handler is constructed, which can be used for <em>opening</em> a
     * serlvet resource later. Internally, this special URL handler will call
     * {@link ServletContext#getResourceAsStream(String)} when an inputstream is requested. This works even on Weblogic
     * 8.1
     * 
     * @param ctx
     *            the faces context from which to retrieve the resource
     * @param path
     *            an URL path
     * 
     * @return an url representing the URL and on which getInputStream() can be called to get the resource
     * @throws MalformedURLException
     */
    public static URL getResourceUrl(FacesContext ctx, String path) throws MalformedURLException
    {
        final ExternalContext externalContext = ctx.getExternalContext();
        URL url = externalContext.getResource(path);
        if (log.isLoggable(Level.FINE))
        {
            log.fine("Resource-Url from external context: " + url);
        }
        if (url == null)
        {
            // This might happen on Servlet container which doesnot return
            // anything
            // for getResource() (like weblogic 8.1 for packaged wars) we
            // are trying
            // to use an own URL protocol in order to use
            // ServletContext.getResourceAsStream()
            // when opening the url
            if (resourceExist(externalContext, path))
            {
                url = getUrlForResourceAsStream(externalContext, path);
            }
        }
        return url;
    }

    // This method could be used above to provide a 'fail fast' if a
    // resource
    // doesnt exist. Otherwise, the URL will fail on the first access.
    private static boolean resourceExist(ExternalContext externalContext, String path)
    {
        if ("/".equals(path))
        {
            // The root context exists always
            return true;
        }
        Object ctx = externalContext.getContext();
        if (ctx instanceof ServletContext)
        {
            ServletContext servletContext = (ServletContext) ctx;
            InputStream stream = servletContext.getResourceAsStream(path);
            if (stream != null)
            {
                try
                {
                    stream.close();
                }
                catch (IOException e)
                {
                    // Ignore here, since we donnot wanted to read from this
                    // resource anyway
                }
                return true;
            }
        }
        return false;
    }

    // Construct URL with special URLStreamHandler for proxying
    // ServletContext.getResourceAsStream()
    private static URL getUrlForResourceAsStream(final ExternalContext externalContext, String path)
            throws MalformedURLException
    {
        URLStreamHandler handler = new URLStreamHandler()
        {
            protected URLConnection openConnection(URL u) throws IOException
            {
                final String file = u.getFile();
                return new URLConnection(u)
                {
                    public void connect() throws IOException
                    {
                    }

                    public InputStream getInputStream() throws IOException
                    {
                        if (log.isLoggable(Level.FINE))
                        {
                            log.fine("Opening internal url to " + file);
                        }
                        Object ctx = externalContext.getContext();
                        // Or maybe fetch the external context afresh ?
                        // Object ctx =
                        // FacesContext.getCurrentInstance().getExternalContext().getContext();

                        if (ctx instanceof ServletContext)
                        {
                            ServletContext servletContext = (ServletContext) ctx;
                            InputStream stream = servletContext.getResourceAsStream(file);
                            if (stream == null)
                            {
                                throw new FileNotFoundException("Cannot open resource " + file);
                            }
                            return stream;
                        }
                        else
                        {
                            throw new IOException("Cannot open resource for an context of "
                                    + (ctx != null ? ctx.getClass() : null));
                        }
                    }
                };
            }
        };
        return new URL("internal", null, 0, path, handler);
    }
}
