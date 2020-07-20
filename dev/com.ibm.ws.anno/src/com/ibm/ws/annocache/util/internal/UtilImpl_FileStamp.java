/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;

import org.eclipse.osgi.storage.url.BundleURLConnection;

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

                return computeStamp(length, lastModified);
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
        if ( physicalPath != null ) {
            return computeStamp( new File(physicalPath) );
        } else {
            System.out.println("computeStamp: No physical path for container [ " + container.getName() + " ] [ " + container.getPath() + " ] [ " + container.getClass().getName() + " ]");
        }

        Collection<URL> containerUrls = container.getURLs();

        String lastStamp = null;
        StringBuilder stampBuilder = null;

        for ( URL containerUrl : containerUrls ) {
            if ( lastStamp != null ) {
                if ( stampBuilder == null ) {
                    stampBuilder = new StringBuilder(lastStamp);
                } else {
                    stampBuilder.append(',');
                    stampBuilder.append(lastStamp);
                }
            }

            lastStamp = computeStamp(containerUrl);
            System.out.println("computeStamp: URL [ " + containerUrl + " ]: Stamp [ " + lastStamp + " ]");
        }

        if ( stampBuilder != null ) {
            stampBuilder.append(',');
            stampBuilder.append(lastStamp);

            String compositeStamp = stampBuilder.toString();
            System.out.println("computeStamp: Composite stamp [ " + compositeStamp + " ]");
            return compositeStamp;

        } else if ( lastStamp == null ) {
            System.out.println("computeStamp: No URLs: Answering null");
            return null;

        } else {
            return lastStamp;
        }
    }

    public static String computeStamp(URL url) {
        URLConnection urlConnection;
        try {
            urlConnection = url.openConnection(); // throws IOException
        } catch ( IOException e ) {
            // FFDC
            System.out.println("computeStamp: URL [ " + url + " ] Connection failure [ " + e.getMessage() + " ]: Answering null");
            return null;
        }

        // Rely on the bundle file, not the bundle connection parameters.
        // The difference is that the bundle connection may obtain values from zip entries.
        // Neither content length not last modified values are reliably obtained.
        // (Typically, the return value for both is 0.)

        File bundleFile = getBundleFile(urlConnection);
        if ( bundleFile != null ) {
            return computeStamp(bundleFile);

        } else {
            long length = urlConnection.getContentLengthLong();
            long lastModified = urlConnection.getLastModified();

            return computeStamp(length, lastModified);
        }
    }

    /**
     * Compute the text form of the time stamp for given length and last modified values.
     *
     * @param length The length value to place into the text time stamp.
     * @param lastModified The last modified value to place into the text time stamp.
     * 
     * @return The text form of the time stampl.
     */
    public static String computeStamp(long length, long lastModified) {
        return Long.toString(length) + "," + Long.toString(lastModified);
    }

    /**
     * Obtain the JAR file which is reached by a bundle connection.
     * 
     * Answer null if the connection is not a bundle connection, or does not reach
     * a bundle file.
     *
     * @param urlConnection The connection from which to retrieve the JAR file.
     *
     * @return The JAR file from the URL connection.
     */
    public static File getBundleFile(URLConnection urlConnection) {
        if ( !(urlConnection instanceof BundleURLConnection) ) {
            System.out.println("getBundleFile: Not a bundle URL connection [ " + urlConnection.getClass().getName() + " ]");
            return null;
        }

        BundleURLConnection bundleConnection = (BundleURLConnection) urlConnection;
        URL localURL = bundleConnection.getLocalURL();
        if ( localURL == null ) {
            System.out.println("getBundleFile: No local URL");
            return null;
        } else {
            System.out.println("getBundleFile: Local URL [ " + localURL + " ]");
        }
        
        // The expected form of the bundle URL:
        //
        // new URL( "jar:" + bundleFile.basefile.toURL() +
        //          "!/" + zipEntry.getName());
        //
        // See: package org.eclipse.osgi.storage.bundlefile.ZipBundleEntry.toLocalURL.
        //
        // Additional cases may be needed for bundle connections which are not mapped to
        // jar files.

        String localPath = localURL.getPath();

        if ( !localPath.startsWith("file:") ) {
            System.out.println("getBundleFile: Not a file type path [ " + localPath + " ]");
            return null;
        }

        int endPos = localPath.indexOf('!');
        if ( endPos == -1 ) {
            System.out.println("getBundleFile: No entry specified within the path [ " + localPath + " ]");
            return null;
        }
        
        String localFilePath = localPath.substring(5, endPos);

        File localFile = new File(localFilePath);
        if ( !localFile.exists() ) {
            System.out.println("getBundleFile: Bundle file does not exist [ " + localPath + " ]");
            return null;
        }

        System.out.println("getBundleFile: Bundle file [ " + localPath + " ]");
        return localFile;
    }

//    @SuppressWarnings("deprecation") // 'File.toURL' is deprecated
//    public static void main(String[] parms) {
//        File testFile = new File("c:\\dev\\anno_patch_02-Jul-2020.jar");
//        String testEntryName = "entry";
//
//        URL testURL;
//        try {
//            testURL = new URL("jar:" + testFile.toURL() + "!/" + testEntryName);
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//            return;
//        }
//
//        System.out.println("Test file [ " + testFile + " ]");
//        System.out.println("Test entry [ " + testEntryName + " ]");
//        System.out.println("Test URL [ " + testURL + " ]");
//        System.out.println("Test URL protocol [ " + testURL.getProtocol() + " ]");
//        
//        String testPath = testURL.getPath();
//        System.out.println("Test URL path [ " + testPath + " ]");
//
//        int startPos;
//        if ( testPath.startsWith("file:") ) {
//            startPos = 5;
//        } else {
//            startPos = 0;
//        }
//
//        int endPos = testPath.indexOf('!');
//        if ( endPos == -1 ) {
//            endPos = testPath.length();
//        }
//
//        String filePath = testPath.substring(startPos, endPos);
//        System.out.println("Test file path [ " + filePath + " ]");
//        
//        File recoveredFile = new File(filePath);
//        if ( !recoveredFile.exists() ) {
//            System.out.println("Test file does not exist!");
//        } else {
//            System.out.println("Test file length [ " + recoveredFile.length() + " ]");
//            System.out.println("Test file last modified [ " + recoveredFile.lastModified() + " ]");
//        }
//    }
}
