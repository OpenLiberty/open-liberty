/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.url.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.osgi.service.url.AbstractURLStreamHandlerService;

import com.ibm.ws.artifact.url.WSJarURLConnection;
import com.ibm.ws.artifact.zip.cache.ZipCachingService;
import com.ibm.ws.artifact.zip.cache.ZipFileHandle;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.wsspi.kernel.service.utils.ParserUtils;

/**
 * URL Handler for the classloader.
 * This handler adds support for URLs in the following format:
 * wsjar:file:{URL}!/entryname
 * Where {URL} is a URL that points to a jar.
 * WSJarURLConnection
 */
public class WSJarURLStreamHandler extends AbstractURLStreamHandlerService { // LIDB3418

    ZipCachingService zipCache = null;

    protected void setZipCachingService(ZipCachingService zcs) {
        this.zipCache = zcs;
    }

    // begin 412642
    @Override
    protected void parseURL(URL url, String spec, int start, int limit) {
        boolean hasScheme = start != 0 && spec.charAt(start - 1) == ':';
        spec = spec.substring(start, limit);

        String path;
        if (hasScheme) {
            // If the "wsjar:" protocol was specified in the spec, then we are
            // not parsing relative to another URL.  The "jar" protocol
            // requires that the path contain an entry path and that the base
            // URL be valid.  For backwards compatibility, we enforce neither.
            path = spec;
        } else {
            path = parseRelativeURL(url, spec);
        }

        setURL(url, "wsjar", url.getHost(), url.getPort(), url.getAuthority(), url.getUserInfo(), path, url.getQuery(), url.getRef());

    }

    // The goal of this function is to append a path to an existing URL and
    // canonicalize the resulting entry path.  For example, if the URL is:
    //
    //   wsjar:file:/a/b!/c
    //
    // ..and the spec is "../../d/e", then the result should be:
    //
    //   wsjar:file:/a/b!/d/e
    private static String parseRelativeURL(URL url, String spec) {
        String path = url.getPath();
        String result;

        int index = path.indexOf("!/");
        if (index == -1) {
            // The "jar" protocol requires that the path contain an entry path.
            // For backwards compatibility, we do not.  Instead, we just
            // canonicalize the base URL.
            try {
                url = Utils.newURL(Utils.newURL(path), spec);
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            }

            result = url.toString();
        } else {
            String entry;

            if (spec.startsWith("/")) {
                // This is an absolute entry path, so we ignore the existing
                // entry path.
                entry = canonicalize(spec);
            } else {
                // Extract the existing entry path.
                entry = path.substring(index + 1);

                if (entry.endsWith("/")) {
                    // The entry ends with '/', so we can directly append the
                    // relative entry path.
                    entry = canonicalize(entry + spec);
                } else if (entry.endsWith("/.")) {
                    // We don't want the logic below because "." is not a
                    // "true" basename.
                    entry = canonicalize(entry + '/' + spec);
                } else {
                    // Strip off the basename with a ".." path segment and
                    // append the relative entry path.
                    entry = canonicalize(entry + "/../" + spec);
                }
            }

            result = path.substring(0, index) + '!' + entry;
        }
        return result;
    }

    // Removes all ".." and "." path segments.  All paths passed to and
    // returned from this function must start with with "/".
    private static String canonicalize(String s) {

        int length = s.length();
        StringBuffer result = new StringBuffer(length);

        // The first character must be '/'.  Append it to the result, and then
        // skip the first character in the input string.
        result.append('/');

        for (int i = 1; i < length; i++) {
            char ch = s.charAt(i);

            if (ch != '.' || result.charAt(result.length() - 1) != '/') {
                // A character other than '.', or we're not at the beginning of
                // a path segment.
                result.append(ch);
            } else if (i + 1 == length || s.charAt(i + 1) == '/') {
                // We have a "." path segment.  Skip the following "/".
                i++;
            } else if (s.charAt(i + 1) == '.' && (i + 2 == length || s.charAt(i + 2) == '/')) {
                // We have a ".." path segment.  Skip the following "./".
                i += 2;

                int j = result.length();
                if (--j > 0) {
                    // Walk backwards until we find a '/'.
                    while (result.charAt(j - 1) != '/' && --j > 0);

                    result.setLength(j);
                }
            } else {
                // Found a path segment that starts with a '.' that is not "."
                // or "..".
                result.append('.');
            }
        }
        return result.toString();
    }

    // end 412642

    // begin 408408.2
    @Override
    public URLConnection openConnection(URL url) throws IOException {
        String path = url.getPath();

        int resourceDelimiterIndex = path.indexOf("!/");
        URLConnection conn;

        if (resourceDelimiterIndex == -1) {
            // The "jar" protocol requires that the path contain an entry path.
            // For backwards compatibility, we do not.  Instead, we just
            // open a connection to the underlying URL.
            conn = Utils.newURL(path).openConnection();
        } else {
            // First strip the resource name out of the path
            String urlString = ParserUtils.decode(path.substring(0, resourceDelimiterIndex));

            // Note that we strip off the leading "/" because ZipFile.getEntry does
            // not expect it to be present.
            String entry = ParserUtils.decode(path.substring(resourceDelimiterIndex + 2));
            // Since the URL we were passed may reference a file in a remote file system with
            // a UNC based name (\\Myhost\Mypath), we must take care to construct a new "host agnostic"
            // URL so that when we call getPath() on it we get the whole path, irregardless of whether
            // the resource is on a local or a remote file system. We will also now validate
            // our urlString has the proper "file" protocol prefix.
            URL jarURL = constructUNCTolerantURL("file", urlString);

            conn = new WSJarURLConnectionImpl(url, jarURL.getPath(), entry, zipCache);
        }

        return conn;
    }

    private static class WSJarURLConnectionImpl extends URLConnection implements WSJarURLConnection {
        private final String path;
        private final String entry;
        private boolean connected;
        private String contentType;
        private final ZipCachingService zipCache;

        WSJarURLConnectionImpl(URL url, String path, String entry, ZipCachingService zcs) {
            super(url);
            this.path = path;
            this.entry = entry;
            this.zipCache = zcs;
        }

        @Override
        public synchronized void connect() throws IOException {
            if (!connected) {
                ZipFileHandle zipFileHandle = zipCache.openZipFile(path); // PK72252
                ZipFile zipFile = zipFileHandle.open(); // PK72252

                boolean exists;
                try {
                    exists = zipFile.getEntry(entry) != null;
                } finally {
                    zipFileHandle.close(); // PK72252
                }

                if (!exists) {
                    throw new FileNotFoundException("JAR entry " + entry + " not found in " + path);
                }

                connected = true;
            }
        }

        @Override
        // PK84650
        public long getLastModified() {
            long result = Utils.getLastModified(new File(path));
            return result;
        }

        @Override
        public synchronized InputStream getInputStream() throws IOException {

            Object token = ThreadIdentityManager.runAsServer();
            try {
                return getInputStreamInternal();
            } finally {
                ThreadIdentityManager.reset(token);
            }
        }

        /**
         * Called from getInputStream, after ensuring that we're running under
         * the server's ID (since we're doing file system access) rather than some
         * sync'ed user (it's a z-specific syncToOSThread thing).
         */
        private synchronized InputStream getInputStreamInternal() throws IOException {
            ZipFileHandle zipFileHandle = zipCache.openZipFile(path); // PK72252
            ZipFile zipFile = zipFileHandle.open();
            InputStream result;

            try {
                ZipEntry zipEntry = zipFile.getEntry(entry);
                if (zipEntry == null) {
                    throw new FileNotFoundException("JAR entry " + entry + " not found in " + path);
                }

                connected = true;

                InputStream input = zipFileHandle.getInputStream(zipFile, zipEntry);
                result = new ZipEntryInputStream(zipFileHandle, input); // PK72252

                zipFileHandle = null; // PK72252
            } finally {
                if (zipFileHandle != null) { // PK72252
                    zipFileHandle.close();
                }
            }
            return result;
        }

        @Override
        public Permission getPermission() throws IOException {
            String permissionPath;
            if (File.separatorChar != '/') {
                permissionPath = path.replace('/', File.separatorChar);
            } else {
                permissionPath = path;
            }

            return new FilePermission(permissionPath, "read");
        }

        @Override
        public String getContentType() {
            if (contentType == null) {
                try {
                    InputStream input = getInputStream();

                    try {
                        // guessContentTypeFromStream requires a stream that
                        // supports marks.  Since the input stream from a
                        // ZipFile doesn't support marks, we wrap the input
                        // stream in a BufferedInputStream.
                        contentType = guessContentTypeFromStream(new BufferedInputStream(input));
                    } finally {
                        input.close();
                    }
                } catch (IOException ex) {
                    //will be auto ffdc'd.
                }

                if (contentType == null) {
                    contentType = guessContentTypeFromName(entry);

                    if (contentType == null) {
                        contentType = "content/unknown";
                    }
                }
            }
            return contentType;
        }

        @Override
        public File getFile() {
            return new File(path);
        }

        @Override
        public String getEntry() {
            return (entry == null) ? "" : entry;
        }

        //@Override - this is only an override in Java 7
        @Override
        public long getContentLengthLong() {
            long length;
            ZipFileHandle zipFileHandle = null;
            try {
                zipFileHandle = zipCache.openZipFile(path);
                ZipFile zipFile = zipFileHandle.open();
                ZipEntry zipEntry = zipFile.getEntry(entry);
                if (zipEntry == null) {
                    length = -1;
                } else {
                    length = zipEntry.getSize();
                    if (length < 0) {
                        // unable to determine the uncompressed size from the ZipFile, so we must
                        // check the size manually by reading the zipEntry's stream
                        length = Utils.getStreamLength(zipFile.getInputStream(zipEntry));
                    }
                }
            } catch (IOException ex) {
                //bci ffdc
                length = -1;
            } finally {
                if (zipFileHandle != null)
                    zipFileHandle.close();
            }
            return length;
        }

        @Override
        public int getContentLength() {
            long l = getContentLengthLong();
            return l > Integer.MAX_VALUE ? -1 : (int) l;
        }
    }

    private static class ZipEntryInputStream extends FilterInputStream {

        private ZipFileHandle zipFileHandle;

        ZipEntryInputStream(ZipFileHandle zipFileHandle, InputStream in) {
            super(in);
            this.zipFileHandle = zipFileHandle;
        }

        @Override
        public synchronized void close() throws IOException { // 578280
            if (zipFileHandle != null) { // 578280
                try {
                    // D531229.1 - Always close the input stream even though
                    // ZipFile.close claims it does automatically.  See 531229.2.
                    super.close();
                } catch (IOException ex) {
                    //will be auto ffdc'd.
                }
                zipFileHandle.close();
                zipFileHandle = null; // 578280
            }
        }

        @Override
        protected void finalize() {
            try {
                close();
            } catch (IOException ex) {

            }

        }
    }

    // end 408408.2

    /**
     * <liberty> brought across from ClassLoaderUtils from /SERV1/ws/code/classloader/src/com/ibm/ws/classloader/</liberty>
     * A method for constructing a purely path based URL from an input string that may
     * optionally contain UNC compliant host names. Examples of properly formatted urlString inputs
     * are as follows:
     *
     * Remote file system (UNC convention)
     * protocolPrefix://host/path
     * protocolPrefix:////host/path
     *
     * Local file system
     * protocolPrefix:/path
     * protocolPrefix:/C:/path
     *
     * The input urlString is validated such that it must begin with the protocolPrefix.
     *
     * Note: UNC paths are used by several OS platforms to map remote file systems. Using this
     * method guarantees that even if the pathComponent contains a UNC based host, the host
     * prefix of the path will be treated as path information and not interpreted as a host
     * component part of the URL returned by this method.
     *
     * @param protocolPrefix The String which is used verify the presence of the urlString protocol before it is removed.
     * @param urlString      A string which will be processed, parsed and used to construct a Hostless URL.
     * @return A URL that is constructed consisting of only path information (no implicit host information).
     * @throws IOException is Thrown if the input urlString does not begin with the specified protocolPrefix.
     */
    protected static URL constructUNCTolerantURL(String protocolPrefix, String urlString) throws IOException {
        int protocolDelimiterIndex = urlString.indexOf(':');
        String urlProtocol, urlPath;
        if (protocolDelimiterIndex < 0 || !(urlProtocol = urlString.substring(0, protocolDelimiterIndex)).equalsIgnoreCase(protocolPrefix)) {
            throw new IOException("invalid protocol prefix: the passed urlString: " + urlString + " is not of the specified protocol: " + protocolPrefix);
        }
        urlPath = urlString.substring(protocolDelimiterIndex + 1);

        // By using this constructor we make sure the JDK does not attempt to interpret leading characters
        // as a host name. This assures that any embedded UNC encoded string prefixes are ignored and
        // subsequent calls to getPath on this URL will always return the full path string (including the
        // UNC host prefix).
        URL jarURL = Utils.newURL(urlProtocol, "", -1, urlPath);
        return jarURL;
    }

}
