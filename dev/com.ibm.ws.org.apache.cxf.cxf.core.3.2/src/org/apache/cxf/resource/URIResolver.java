/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.CXFPermissions;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;

/**
 * Resolves a File, classpath resource, or URL according to the follow rules:
 * <ul>
 * <li>Check to see if a file exists, relative to the base URI.</li>
 * <li>If the file doesn't exist, check the classpath</li>
 * <li>If the classpath doesn't exist, try to create URL from the URI.</li>
 * </ul>
 */
public class URIResolver implements AutoCloseable {
    private static final Logger LOG = LogUtils.getLogger(URIResolver.class);

    private Map<String, LoadingByteArrayOutputStream> cache = new HashMap<>();
    private File file;
    private URI uri;
    private URL url;
    private InputStream is;
    private Class<?> calling;

    public URIResolver() {
    }

    public URIResolver(String path) throws IOException {
        this("", path);
    }

    public URIResolver(String baseUriStr, String uriStr) throws IOException {
        this(baseUriStr, uriStr, null);
    }

    public URIResolver(String baseUriStr, String uriStr, Class<?> calling) throws IOException {
        this.calling = (calling != null) ? calling : getClass();
        if (uriStr.startsWith("classpath:")) {
            tryClasspath(uriStr);
        } else if (baseUriStr != null
            && (baseUriStr.startsWith("jar:")
                || baseUriStr.startsWith("zip:")
                || baseUriStr.startsWith("wsjar:"))
            && !isAbsolute(uriStr)) {
            tryArchive(baseUriStr, uriStr);
        } else if (uriStr.startsWith("jar:")
            || uriStr.startsWith("zip:")
            || uriStr.startsWith("wsjar:")) {
            tryArchive(uriStr);
        } else {
            tryFileSystem(baseUriStr, uriStr);
        }
    }

    public void unresolve() {
        this.file = null;
        this.uri = null;
        this.is = null;
    }

    public void resolve(String baseUriStr, String uriStr, Class<?> callingCls) throws IOException {
        this.calling = (callingCls != null) ? callingCls : getClass();
        this.file = null;
        this.uri = null;

        this.is = null;

        if (uriStr.startsWith("classpath:")) {
            tryClasspath(uriStr);
        } else if (baseUriStr != null
            && (baseUriStr.startsWith("jar:")
                || baseUriStr.startsWith("zip:")
                || baseUriStr.startsWith("wsjar:"))
            && !isAbsolute(uriStr)) {
            tryArchive(baseUriStr, uriStr);
        } else if (uriStr.startsWith("jar:")
            || uriStr.startsWith("zip:")
            || uriStr.startsWith("wsjar:")) {
            tryArchive(uriStr);
        } else {
            tryFileSystem(baseUriStr, uriStr);
        }
    }

    private boolean isAbsolute(String uriStr) {
        try {
            return new URI(uriStr).isAbsolute();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private void tryFileSystem(String baseUriStr, String uriStr) throws IOException, MalformedURLException {
        // It is possible that spaces have been encoded.  We should decode them first.
        String fileStr = uriStr.replace("%20", " ");

        try {
            final File uriFileTemp = new File(fileStr);

            File uriFile = new File(AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return uriFileTemp.getAbsolutePath();
                }
            }));
            if (!SecurityActions.fileExists(uriFile, CXFPermissions.RESOLVE_URI)) {
                try {
                    URI urif = new URI(URLDecoder.decode(uriStr, "ASCII"));
                    if ("file".equals(urif.getScheme()) && urif.isAbsolute()) {
                        File f2 = new File(urif);
                        if (f2.exists()) {
                            uriFile = f2;
                        }
                    }
                } catch (URISyntaxException ex) {
                    //ignore
                }
            }
            final URI relative;
            if (!SecurityActions.fileExists(uriFile, CXFPermissions.RESOLVE_URI)) {
                relative = new URI(uriStr.replace(" ", "%20"));
            } else {
                relative = uriFile.getAbsoluteFile().toURI();
            }

            if (relative.isAbsolute()) {
                uri = relative;
                url = relative.toURL();

                try {
                    HttpURLConnection huc = createInputStream();
                    int status = huc.getResponseCode();
                    if (status != HttpURLConnection.HTTP_OK && followRedirect(status)) {
                        // only redirect once.
                        uri = new URI(huc.getHeaderField("Location"));
                        url = uri.toURL();
                        createInputStream();
                    }
                } catch (ClassCastException ex) {
                    is = url.openStream();
                }
            } else if (!StringUtils.isEmpty(baseUriStr)) {
                URI base;
                File baseFile = new File(baseUriStr);

                if (!baseFile.exists() && baseUriStr.startsWith("file:")) {
                    baseFile = new File(getFilePathFromUri(baseUriStr));
                }

                if (baseFile.exists()) {
                    base = baseFile.toURI();
                } else {
                    base = new URI(baseUriStr);
                }

                base = base.resolve(relative);
                if (base.isAbsolute() && "file".equalsIgnoreCase(base.getScheme())) {
                    try {
                        // decode space before create a file
                        baseFile = new File(base.getPath().replace("%20", " "));
                        if (baseFile.exists()) {
                            is = base.toURL().openStream();
                            uri = base;
                        } else {
                            tryClasspath(base.toString().startsWith("file:")
                                         ? base.toString().substring(5) : base.toString());
                        }
                    } catch (Throwable th) {
                        tryClasspath(base.toString().startsWith("file:")
                                     ? base.toString().substring(5) : base.toString());
                    }
                } else {
                    tryClasspath(base.toString().startsWith("file:")
                                 ? base.toString().substring(5) : base.toString());
                }
            } else {
                tryClasspath(fileStr.startsWith("file:")
                             ? fileStr.substring(5) : fileStr);
            }
        } catch (URISyntaxException e) {
            // do nothing
        }

        if (is == null && baseUriStr != null && baseUriStr.startsWith("classpath:")) {
            tryClasspath(baseUriStr + fileStr);
        }
        if (is == null && uri != null && "file".equals(uri.getScheme())) {
            try {
                file = new File(uri);
            } catch (IllegalArgumentException iae) {
                file = new File(uri.toURL().getPath());
                if (!file.exists()) {
                    file = null;
                }
            }
        }

        if (is == null && file != null && file.exists()) {
            uri = file.toURI();
            try {
                is = Files.newInputStream(file.toPath());
            } catch (FileNotFoundException e) {
                throw new RuntimeException("File was deleted! " + fileStr, e);
            }
            url = file.toURI().toURL();
        } else if (is == null) {
            tryClasspath(fileStr);
        }
    }

    private static boolean followRedirect(int status) {
        return (status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_SEE_OTHER)
              && Boolean.parseBoolean(SystemPropertyAction.getPropertyOrNull("http.autoredirect"));
    }

    private HttpURLConnection createInputStream() throws IOException {
        HttpURLConnection huc = (HttpURLConnection)url.openConnection();

        String host = SystemPropertyAction.getPropertyOrNull("http.proxyHost");
        if (host != null) {
            //comment out unused port to pass pmd check
            /*String ports = SystemPropertyAction.getProperty("http.proxyPort");
            int port = 80;
            if (ports != null) {
                port = Integer.parseInt(ports);
            }*/

            String username = SystemPropertyAction.getPropertyOrNull("http.proxy.user");
            String password = SystemPropertyAction.getPropertyOrNull("http.proxy.password");

            if (username != null && password != null) {
                String encoded = Base64Utility.encode((username + ":" + password).getBytes());
                huc.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
            }
        }
        huc.setConnectTimeout(30000);
        huc.setReadTimeout(60000);
        is = huc.getInputStream();
        return huc;
    }

    /**
     * Assumption: URI scheme is "file"
     */
    private String getFilePathFromUri(String uriString) {
        String path = null;

        try {
            path = new URL(uriString).getPath();
        } catch (MalformedURLException e) {
            // ignore
        }

        if (path == null) {
            if (uriString.startsWith("file:/")) {
                path = uriString.substring(6);
            } else if (uriString.startsWith("file:")) {
                // handle Windows file URI such as "file:C:/foo/bar"
                path = uriString.substring(5);
            }
        }

        // decode spaces before returning otherwise File.exists returns false
        if (path != null) {
            return path.replace("%20", " ");
        }
        return null;
    }

    private void tryArchive(String baseStr, String uriStr) throws IOException {
        int i = baseStr.indexOf('!');
        if (i == -1) {
            tryFileSystem(baseStr, uriStr);
        }

        String archiveBase = baseStr.substring(0, i + 1);
        String archiveEntry = baseStr.substring(i + 1);
        try {
            URI u = new URI(archiveEntry).resolve(uriStr);

            tryArchive(archiveBase + u.toString());

            if (is != null) {
                if (u.isAbsolute()) {
                    url = u.toURL();
                }
                return;
            }
        } catch (URISyntaxException e) {
            // do nothing
        }

        tryFileSystem("", uriStr);
    }

    private void tryArchive(String uriStr) throws IOException {
        int i = uriStr.indexOf('!');
        if (i == -1) {
            return;
        }

        url = new URL(uriStr);
        try {
            is = url.openStream();
            try {
                uri = url.toURI();
            } catch (URISyntaxException ex) {
                // ignore
            }
        } catch (IOException e) {
            uriStr = uriStr.substring(i + 1);
            tryClasspath(uriStr);
        }
    }

    private void tryClasspath(String uriStr) throws IOException {
        boolean isClasspathURL = false;
        if (uriStr.startsWith("classpath:")) {
            uriStr = uriStr.substring(10);
            isClasspathURL = true;
        }
        url = ClassLoaderUtils.getResource(uriStr, calling);
        if (url == null) {
            tryRemote(uriStr);
        } else {
            try {
                uri = url.toURI();
            } catch (URISyntaxException e) {
                // yep, some versions of the JDK can't handle spaces when URL.toURI() is called,
                // and lots of people on windows have their maven repositories at
                // C:/Documents and Settings/<userid>/.m2/repository
                // re: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6506304
                if (url.toString().contains(" ")) {
                    url = new URL(url.toString().replace(" ", "%20"));
                }
                //let's try this again
                try {
                    uri = url.toURI();
                } catch (URISyntaxException e1) {
                    // processing the jar:file:/ type value
                    String urlStr = url.toString();
                    if (urlStr.startsWith("jar:")
                        || urlStr.startsWith("zip:")
                        || urlStr.startsWith("wsjar:")) {
                        int pos = urlStr.indexOf('!');
                        if (pos != -1) {
                            try {
                                uri = new URI("classpath:" + urlStr.substring(pos + 1));
                            } catch (URISyntaxException ue) {
                                // ignore
                            }
                        }
                    }
                }

            }
            is = url.openStream();
        }
        if (is == null && isClasspathURL) {
            LOG.log(Level.WARNING, "NOT_ON_CLASSPATH", uriStr);
        }
    }

    private void tryRemote(String uriStr) throws IOException {
        try {
            LoadingByteArrayOutputStream bout = cache.get(uriStr);
            url = new URL(uriStr);
            uri = new URI(url.toString());
            if (bout == null) {
                URLConnection connection = url.openConnection();
                is = connection.getInputStream();
                bout = new LoadingByteArrayOutputStream(1024);
                IOUtils.copy(is, bout);
                is.close();
                cache.put(uriStr, bout);
            }
            is = bout.createInputStream();
        } catch (MalformedURLException | URISyntaxException e) {
            // do nothing
        }
    }

    public URI getURI() {
        return uri;
    }

    public URL getURL() {
        return url;
    }

    public InputStream getInputStream() {
        return is;
    }

    public boolean isFile() {
        if (file != null) {
            return file.exists();
        }
        return false;
    }

    public File getFile() {
        return file;
    }

    public boolean isResolved() {
        return is != null;
    }
    
    @Override
    public void close() throws IOException {
        if (isResolved()) {
            is.close();
            unresolve();
        }
    }
}
