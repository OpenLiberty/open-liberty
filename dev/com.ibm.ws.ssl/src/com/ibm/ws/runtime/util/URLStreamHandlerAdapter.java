/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.runtime.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerSetter;

import com.ibm.ws.ffdc.FFDCFilter;

/**
 * Provides an adapter between a standard java.net.URLStreamHandler and an OSGI
 * URLStreamHandlerService.
 */
public class URLStreamHandlerAdapter implements URLStreamHandlerService {

    // --------------------------------------------------------------------
    // This class provides an adapter between the java.net.URLStreamHandler
    // class and OSGis URLStreamHandlerService. This is required to get
    // a standard URLStreamHandler to work in OSGi without recompilation
    // (the OSGi spec makes a cavalier assumption that code can be
    // recompiled ... not true in the middleware world).
    //
    // The implementation uses reflection to map the public OSGi methods
    // onto the protected java.net.URLStreamHandler methods.
    // --------------------------------------------------------------------

    private URLStreamHandler urlStreamHandlerInstance = null;
    private String urlStreamHandlerClass = null;
    private String urlStreamHandlerClassPath = null;
    private String protocol = null;
    private ServiceRegistration registration = null;

    private static Method _equals;
    private static Method _getDefaultPort;
    private static Method _getHostAddress;
    private static Method _hashCode;
    private static Method _hostsEqual;
    private static Method _openConnection;
    private static Method _openConnectionProxy;
    private static Method _parseURL;
    private static Method _sameFile;
    private static Method _toExternalForm;
    private static Field _handlerField;

    static {
        try {
            _equals = URLStreamHandler.class.getDeclaredMethod("equals", new Class[] { URL.class, URL.class });
            _equals.setAccessible(true);

            _getDefaultPort = URLStreamHandler.class.getDeclaredMethod("getDefaultPort", (Class[]) null);
            _getDefaultPort.setAccessible(true);

            _getHostAddress = URLStreamHandler.class.getDeclaredMethod("getHostAddress", new Class[] { URL.class });
            _getHostAddress.setAccessible(true);

            _hashCode = URLStreamHandler.class.getDeclaredMethod("hashCode", new Class[] { URL.class });
            _hashCode.setAccessible(true);

            _hostsEqual = URLStreamHandler.class.getDeclaredMethod("hostsEqual", new Class[] { URL.class, URL.class });
            _hostsEqual.setAccessible(true);

            _openConnection = URLStreamHandler.class.getDeclaredMethod("openConnection", new Class[] { URL.class });
            _openConnection.setAccessible(true);

            _openConnectionProxy = URLStreamHandler.class.getDeclaredMethod("openConnection", new Class[] { URL.class, Proxy.class });
            _openConnectionProxy.setAccessible(true);

            _parseURL = URLStreamHandler.class.getDeclaredMethod("parseURL", new Class[] { URL.class, String.class, int.class, int.class });
            _parseURL.setAccessible(true);

            _sameFile = URLStreamHandler.class.getDeclaredMethod("sameFile", new Class[] { URL.class, URL.class });
            _sameFile.setAccessible(true);

            _toExternalForm = URLStreamHandler.class.getDeclaredMethod("toExternalForm", new Class[] { URL.class });
            _toExternalForm.setAccessible(true);

            _handlerField = URL.class.getDeclaredField("handler");
            _handlerField.setAccessible(true);

        } catch (Throwable t) {
            FFDCFilter.processException(t, URLStreamHandlerAdapter.class.getName(), "static init");
        }
    }

    /**
     * Constructor.
     * 
     * @param className
     * @param classPath
     */
    public URLStreamHandlerAdapter(String protocol, String className, String classPath) {
        this.urlStreamHandlerClass = className;
        this.urlStreamHandlerClassPath = classPath;
        this.protocol = protocol;

        // Get the bundle context
        Bundle b = FrameworkUtil.getBundle(StreamHandlerUtils.class);
        BundleContext ctx = b.getBundleContext();

        Hashtable properties = new Hashtable();
        properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] { protocol });
        registration = ctx.registerService(URLStreamHandlerService.class.getName(), this, properties);

    }

    void destroy() {
        if (registration != null) {
            registration.unregister();
        }
    }

    /*
     * @see org.osgi.service.url.URLStreamHandlerService#openConnection(java.net.URL)
     */
    @Override
    @SuppressWarnings("unused")
    public URLConnection openConnection(URL url) throws IOException {
        try {
            return (URLConnection) _openConnection.invoke(getInstance(), new Object[] { url });
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "openConnection", url);
            return null;
        }
    }

    /*
     * OSGi Core 4.3, section 52.3.5
     */
    @SuppressWarnings("unused")
    public URLConnection openConnection(URL url, Proxy proxy) throws IOException {
        try {
            return (URLConnection) _openConnectionProxy.invoke(getInstance(), new Object[] { url, proxy });
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "openConnection", new Object[] { url, proxy });
            return null;
        }
    }

    /*
     * @see org.osgi.service.url.URLStreamHandlerService#parseURL(org.osgi.service.url.URLStreamHandlerSetter,
     * java.net.URL, java.lang.String, int, int)
     */
    @Override
    @SuppressWarnings("unused")
    public void parseURL(URLStreamHandlerSetter setter, URL url, String arg2, int arg3, int arg4) {

        // ------------------------------------------------------------------------------
        // NOTE: the java.net.URLStreamHandler expects that parseURL will be called
        // for the handler associated with a given URL. This associated handler is
        // actually an OSGi handler proxy. Thus we must tempoorarily alter the URL's
        // handler.
        //
        // See org.eclipse.osgi.framework.internal.protocol.URLStreamHandlerProxy
        // and java.net.URLStreamHandler for details.
        // ------------------------------------------------------------------------------

        URLStreamHandler currentHandler = null;

        try {
            currentHandler = (URLStreamHandler) _handlerField.get(url);
            _handlerField.set(url, getInstance());
            _parseURL.invoke(getInstance(), new Object[] { url, arg2, Integer.valueOf(arg3), Integer.valueOf(arg4) });
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "parseURL", new Object[] { url, this, currentHandler });
        } finally {
            if (currentHandler != null) {
                try {
                    _handlerField.set(url, currentHandler);
                } catch (Exception e) {
                    FFDCFilter.processException(e, getClass().getName(), "parseURL", new Object[] { url, this, currentHandler });
                }
            }
        }
    }

    /*
     * @see org.osgi.service.url.URLStreamHandlerService#toExternalForm(java.net.URL)
     */
    @Override
    public String toExternalForm(URL url) {
        try {
            return (String) _toExternalForm.invoke(getInstance(), new Object[] { url });
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "toExternalForm", url);
            return null;
        }
    }

    /*
     * @see org.osgi.service.url.URLStreamHandlerService#equals(java.net.URL,
     * java.net.URL)
     */
    @Override
    public boolean equals(URL url1, URL url2) {
        try {
            Boolean result = (Boolean) _equals.invoke(getInstance(), new Object[] { url1, url2 });
            return result.booleanValue();
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "equals", new Object[] { url1, url2 });
            return false;
        }
    }

    /*
     * @see org.osgi.service.url.URLStreamHandlerService#getDefaultPort()
     */
    @Override
    public int getDefaultPort() {
        try {
            Integer result = (Integer) _getDefaultPort.invoke(getInstance(), (Object[]) null);
            return result.intValue();
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getDefaultPort");
            return 0;
        }
    }

    /*
     * @see org.osgi.service.url.URLStreamHandlerService#getHostAddress(java.net.URL)
     */
    @Override
    public InetAddress getHostAddress(URL url) {
        try {
            return (InetAddress) _getHostAddress.invoke(getInstance(), new Object[] { url });
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "getHostAddress", url);
            return null;
        }
    }

    /*
     * @see org.osgi.service.url.URLStreamHandlerService#hashCode(java.net.URL)
     */
    @Override
    public int hashCode(URL url) {
        try {
            Integer result = (Integer) _hashCode.invoke(getInstance(), new Object[] { url });
            return result.intValue();
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "hashCode", url);
            return url.hashCode();
        }
    }

    /*
     * @see org.osgi.service.url.URLStreamHandlerService#hostsEqual(java.net.URL, java.net.URL)
     */
    @Override
    public boolean hostsEqual(URL url1, URL url2) {
        try {
            Boolean result = (Boolean) _hostsEqual.invoke(getInstance(),
                                                          new Object[] { url1, url2 });
            return result.booleanValue();
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "hostsEqual", new Object[] { url1, url2 });
            return false;
        }
    }

    /*
     * @see org.osgi.service.url.URLStreamHandlerService#sameFile(java.net.URL, java.net.URL)
     */
    @Override
    public boolean sameFile(URL url1, URL url2) {
        try {
            Boolean result = (Boolean) _sameFile.invoke(getInstance(), new Object[] { url1, url2 });
            return result.booleanValue();
        } catch (Exception e) {
            FFDCFilter.processException(e, getClass().getName(), "sameFile", new Object[] { url1, url2 });
            return false;
        }
    }

    private synchronized Object getInstance() throws ClassNotFoundException, IllegalAccessException,
                    InstantiationException, MalformedURLException {
        if (urlStreamHandlerInstance == null) {
            urlStreamHandlerInstance = (URLStreamHandler) Class.forName(urlStreamHandlerClass, true,
                                                                        StreamHandlerUtils.getResourceClassLoader(urlStreamHandlerClassPath, null)).newInstance();
        }
        return urlStreamHandlerInstance;

    }

}
