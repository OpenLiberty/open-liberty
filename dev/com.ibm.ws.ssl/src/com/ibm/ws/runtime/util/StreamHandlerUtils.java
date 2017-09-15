/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.runtime.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Security needs a class to anchor a URL Stream Handler Factory. The Stream
 * Handler Factory contains stream handlers for https, RACF software keyrings
 * and RACF Hardware keyrings. Java allows only 1 Stream Handler Factory per
 * instance of a JVM.
 */

// ---------------------------------------------------------------------------
// LIDB3418 NOTES: This class has historically provide a set of utilities
// to manage URL Stream Handlers (java.net.URLStreamHandler). Mostly, it
// has fronted the com.ibm.ws.runtime.util.URLHandlerFactory class.
// Unfortunately, no attempt was made to hide the URLHandlerFactory class
// as an implementation details and thus not all client code can be
// guaranteed to go through these utilities to get to the factory.
//
// The URLHandlerFactory is rendered useless in an OSGi environment. This
// is because OSGi mandates the installation of a handler factory, and, as
// noted above, Java does not support more than one factory (uggh!).
//
// It is recommended that the URLHandlerFactory class be ultimately
// removed. To do this, the few places that are invoking it directly
// (e.g. JSP compiler) need to be fixed. Furthermore, it needs to be
// verified that all clients of this class are running in an OSGi
// environment.
//
// See Chapter 8 of the OSGi specification for additional motivation and
// explanation of the pattern used here.
// ---------------------------------------------------------------------------
public final class StreamHandlerUtils {

    private static TraceComponent tc = Tr.register(StreamHandlerUtils.class, "StreamHandler", "com.ibm.ws.ssl.resources.ssl");

    private static Map<String, URLStreamHandlerAdapter> streamHandlerAdapters = Collections.synchronizedMap(new HashMap<String, URLStreamHandlerAdapter>());

    private StreamHandlerUtils() {
        // do nothing
    }

    /**
     * Create the singleton instance of the class, if not already created.
     */
    public static void create() {}

    /**
     * Add a Stream handler for a provider.
     * 
     * @param provider
     * @param handlerClass
     * @throws Exception
     */
    public static void addProvider(String provider, String handlerClass) throws Exception {

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "addProvider", new Object[] { provider, handlerClass });
        }

        URLStreamHandlerAdapter adapter = new URLStreamHandlerAdapter(provider, handlerClass, null);
        streamHandlerAdapters.put(provider, adapter);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "addProvider", new Object[] { provider, handlerClass });
        }
    }

    /**
     * Remove the listed provider from the cached storage.
     * 
     * @param provider
     */
    public static void removeProvider(String provider) {

        if (tc.isEntryEnabled()) {
            Tr.entry(tc, "removeProvider", provider);
        }

        URLStreamHandlerAdapter adapter = streamHandlerAdapters.remove(provider);
        if (adapter != null) {
            adapter.destroy();
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, "removeProvider", provider);
        }
    }

    /**
     * Check if a handler has already been registered for the
     * specified provider.
     * 
     * @param provider
     * @return boolean
     */
    public static boolean queryProvider(String provider) {
        return streamHandlerAdapters.containsKey(provider);
    }

    static String getProviderClasspath(List<String> providerClasspath) {
        StringBuilder sb = new StringBuilder();
        String segment = null;
        for (String classpathSegment : providerClasspath) {
            segment = classpathSegment.trim();
            if (0 == segment.length()) {
                continue;
            }
            if (0 < sb.length()) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(segment);
        }
        return sb.toString();
    }

    static String getClassName(String path) {
        return path.replace(File.pathSeparatorChar, '.');
    }

    static ClassLoader getResourceClassLoader(String classpath, ClassLoader parent)
                    throws MalformedURLException {
        ClassLoader classloader = null;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getResourceClassLoader " + classpath + " : " + parent);

        if (classpath == null || 0 == classpath.length()) {
            classloader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    ClassLoader result = Thread.currentThread().getContextClassLoader();
                    if (result == null) {
                        result = ClassLoader.getSystemClassLoader();
                    }
                    return result;
                }
            });
        } else {
            // Trace the raw classpath so we can determine when the user has
            // typed it incorrectly.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "parsing urls classpath=" + classpath);

            // Parse the string and avoid creating an URL for an empty string.
            // The paths need to be separated by spaces, which was a decision made by the java group.
            // My guess is they did this to have a platform-independent method of separating the paths.
            // If you were to separate them with semi-colons for Windows, you'd have to change them to
            // colons on Unix--you wouldn't be able to use the same jar file on different platforms.
            // Unfortunately, this prevents you from using names with spaces on Windows.
            StringTokenizer st = new StringTokenizer(classpath.trim(), File.pathSeparator);
            List<String> entries = new ArrayList<String>();
            while (st.hasMoreTokens()) {
                String urlString = st.nextToken().trim();
                if (0 < urlString.length()) {
                    entries.add(urlString);
                }
            }

            // Convert the url strings into an array of URLs. This method will properly convert
            // directories, which require additional processing over files. Pass the file separator
            // as the uriRoot, which makes the URLs relative to the root. All of our URLs here should be
            // absolute--what would they be relative to in this case?
            //
            // NOTE: the common archive call may open up the files, so a
            // doPrivileged is performed here

            //final List<String> entriesF = entries;
            URL[] urls = AccessController.doPrivileged(new PrivilegedAction<URL[]>() {
                @Override
                public URL[] run() {
                    // TODO url conversion... this path never happens in XTP currently
                    return new URL[0];
                    // return
                    // org.eclipse.jst.j2ee.commonarchivecore.internal.util.ArchiveUtil.toLocalURLs(entriesF,
                    // File.separator);
                }
            });

            // Trace the URLs that will be loaded with the class loader.
            if (tc.isDebugEnabled()) {
                StringBuilder msgurls = new StringBuilder(64 * urls.length);
                for (int i = 0; i < urls.length; i++)
                    msgurls.append("\n       ").append(urls[i]);
                Tr.debug(tc, "loadingurls: " + msgurls.toString());
            }

            final ClassLoader parentF = parent;
            final URL[] urlsF = urls;
            classloader = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    if (parentF == null) {
                        return new URLClassLoader(urlsF,
                                        Thread.currentThread().getContextClassLoader());
                    }
                    return new URLClassLoader(urlsF, parentF);
                }
            });
        }

        return classloader;
    }

}
