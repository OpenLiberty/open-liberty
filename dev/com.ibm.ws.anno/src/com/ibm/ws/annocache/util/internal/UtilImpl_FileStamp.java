/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.util.internal;

import java.io.File;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;

import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

public class UtilImpl_FileStamp {
    /**
     * Answer a stamp for a file.  Answer null if the file does not exist,
     * or is not a simple file.
     * 
     * The stamp is the length of the file, plus ",", plus the last modified
     * time of the file.
     * 
     * See {@link File#length()} and {@link File#lastModified()}.
     * 
     * @param file The file for which to answer a stamp.
     * 
     * @return The stamp of the file.
     */
    public static String computeStamp(final File file) {
        return AccessController.doPrivileged( new PrivilegedAction<String>() {
            @Override
            public String run() {
                if ( !FileUtils.fileExists(file) || !FileUtils.fileIsFile(file) ) {
                    return null;
                }

                // Don't use ':' as the separator.  That confuses the simple
                // cache parser, which relies on there being at most one ":"
                // on read lines.

                long length = file.length();
                long lastModified = file.lastModified();
                return Long.toString(length) + "," + Long.toString(lastModified);
            }
        } );
    }

    /**
     * Answer the physical path of a container.
     *
     * The physical path is available only if the container has
     * a single URL, and that URL uses the file protocol.
     *
     * See {@link URL#getProtocol()} and {@link URL#getPath()}.
     *
     * @param container The container for which to answer the
     * physical path.
     *
     * @return The physical path of the container.  Null if the
     *     container has multiple URLs, or if the container does
     *     not use a file URL.
     */
    public static String getPhysicalPath(Container container) {
        Collection<URL> urls = container.getURLs();
        URL url = null;
        for ( URL nextURL : urls ) {
            url = nextURL;
            break;
        }
        if ( url == null ) {
            return null;
        }
        String protocol = url.getProtocol();
        if ( (protocol == null) || !protocol.equals("file") ) {
            return null;
        }
        return url.getPath();
    }

    /**
     * Compute the stamp of a container.  This is the stamp of the
     * physical file of the container, if the container has a single
     * physical file.  If the container does not have a single physical
     * file answer null.  If the file of the container is not a simple
     * file (meaning, the file of the container is not a directory type
     * file), answer null.
     *
     * Containers with multiple physical files do not generate a stamp:
     * A list of simple size and last modified values does not extend to
     * multiple files because the identities of the files can change without
     * changing the stamp value.  For example, for a container which has
     * two physical files which have the same size and time stamp but which
     * have different contents, the stamp of the containers is unchanged
     * across a transposition of the two containers. 
     * 
     * @param container The container for which to compute a stamp.
     *
     * @return The stamp of the container.  Null if no stamp is available for
     *     the container.
     */
    public static String computeStamp(Container container) {
        String physicalPath = getPhysicalPath(container);
        return ( (physicalPath == null) ? null : computeStamp( new File(physicalPath) ) );
    }
}
