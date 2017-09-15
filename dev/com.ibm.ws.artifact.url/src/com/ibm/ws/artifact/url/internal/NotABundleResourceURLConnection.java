/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.url.internal;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;

public class NotABundleResourceURLConnection extends URLConnection {
    //the protocol name MUST MATCH that declared in the bnd.bnd file.
    private static final String PROTOCOL = "notabundleresource";

    private static final String MANIFESTPATH = "META-INF/MANIFEST.MF";

    private static class Delegate {
        URL delegateURL;
        String owningBundleManifestKey;
    }

    private static final Map<String, Delegate> delegates = Collections.synchronizedMap(new HashMap<String, Delegate>());

    /**
     * Simple little helper to get a unique string key for any bundle.
     * <p>
     * Currently uses the string form of the url for META-INF/MANIFEST.MF
     */
    private final static String getManifestKeyForBundle(Bundle owningBundle) {
        URL manifestKeyURL = owningBundle.getEntry(MANIFESTPATH);
        if (manifestKeyURL == null) {
            //'bundle' did not have a manifest.
            throw new IllegalArgumentException(owningBundle.getSymbolicName());
        }
        String manifestKey = manifestKeyURL.toExternalForm();
        return manifestKey;
    }

    public static URL addURL(final URL urlToConvert, Bundle owningBundle) {
        try {
            String manifestKey = getManifestKeyForBundle(owningBundle);

            URL newURL = AccessController.doPrivileged(new PrivilegedAction<URL>() {
                @Override
                public URL run() {
                    try {
                        return new URL(PROTOCOL, urlToConvert.getHost(), urlToConvert.getPort(), urlToConvert.getFile());
                    } catch (MalformedURLException mue) {
                        //means the protocol handler has not been registered yet.
                        //will not happen unless a developer breaks the bnd, eg by having one 
                        //protocol name there, and another in this class, or by using the wrong
                        //service property when publishing.
                        throw new IllegalStateException(mue);
                    }
                }
            });
            String key = newURL.toExternalForm();

            Delegate d = new Delegate();
            d.delegateURL = urlToConvert;
            d.owningBundleManifestKey = manifestKey;

            synchronized (delegates) {
                NotABundleResourceURLConnection.delegates.put(key, d);
            }
            return newURL;
        } catch (IllegalStateException ise) {
            //means the bundle is uninstalled.
            //users should not be trying to use urls for uninstalled bundles.    
            //(it's pretty hard to get here, as the bundle throws ise itself on getEntry if uninstalled)
            throw ise;
        }
    }

    public static void forgetBundle(Bundle owningBundle) {
        //This isn't the most optimised approach (which would use 2nd map to track the reverse lookups)
        //but only gets driven at app stop.. 
        String manifestKey = getManifestKeyForBundle(owningBundle);
        synchronized (delegates) {
            List<String> toRemove = new ArrayList<String>();
            for (Map.Entry<String, Delegate> delegateEntry : delegates.entrySet()) {
                if (delegateEntry.getValue().owningBundleManifestKey.equals(manifestKey)) {
                    toRemove.add(delegateEntry.getKey());
                }
            }
            for (String s : toRemove) {
                delegates.remove(s);
            }
        }
    }

    public NotABundleResourceURLConnection(URL url) {
        super(url);
    }

    /** {@inheritDoc} */
    @Override
    public void connect() throws IOException { /* NO OP */}

    /** {@inheritDoc} */
    @Override
    public InputStream getInputStream() throws IOException {
        synchronized (delegates) {
            String key = getURL().toExternalForm();
            if (delegates.containsKey(key)) {
                Delegate d = delegates.get(key);
                if (d != null) {
                    return d.delegateURL.openStream();
                }
                //means somehow we got a null in our map for the given key,
                //unexpected, and means the code is broken somehow. 
                throw new IOException(new IllegalStateException("" + getURL()));
            }
        }
        //we don't have a url to map to for this invocation. 
        //can happen if the url is accessed after the source providing
        //bundle is uninstalled.
        throw new FileNotFoundException("" + getURL());
    }
}
