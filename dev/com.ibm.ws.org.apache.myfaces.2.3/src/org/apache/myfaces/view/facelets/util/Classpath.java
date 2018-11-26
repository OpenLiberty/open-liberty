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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.myfaces.shared.util.ClassUtils;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
/**
 * @author Jacob Hookom
 * @author Roland Huss
 * @author Ales Justin (ales.justin@jboss.org)
 * @version $Id$
 */
public final class Classpath
{
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Set<String> EXCLUDED_PREFIX_SET = new HashSet<String>(Arrays.asList("rar:", "sar:"));
    private static final Set<String> EXCLUDED_SUFFIX_SET = new HashSet<String>(Arrays.asList(".rar", ".sar"));

    private Classpath()
    {
    }

    public static URL[] search(String prefix, String suffix) throws IOException
    {
        return search(ClassUtils.getContextClassLoader(), prefix, suffix);
    }

    public static URL[] search(ClassLoader loader, String prefix, String suffix) throws IOException
    {
        Set<URL> all = new LinkedHashSet<URL>();

        _searchResource(all, loader, prefix, prefix, suffix);
        _searchResource(all, loader, prefix + "MANIFEST.MF", prefix, suffix);

        URL[] urlArray = (URL[]) all.toArray(new URL[all.size()]);

        return urlArray;
    }

    private static void _searchResource(Set<URL> result, ClassLoader loader, String resource, String prefix,
                                        String suffix) throws IOException
    {
        for (Enumeration<URL> urls = loader.getResources(resource); urls.hasMoreElements();)
        {
            URL url = urls.nextElement();
            URLConnection conn = url.openConnection();
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);

            JarFile jar = null;
            if (conn instanceof JarURLConnection)
            {
                try
                {
                    jar = ((JarURLConnection) conn).getJarFile();
                }
                
                catch (Throwable e)
                {
                    // This can happen if the classloader provided us a URL that it thinks exists
                    // but really doesn't.  In particular, if a JAR contains META-INF/MANIFEST.MF
                    // but not META-INF/, some classloaders may incorrectly report that META-INF/
                    // exists and we'll end up here.  Just ignore this case.
                    
                    continue;
                }
            }
            else
            {
                jar = _getAlternativeJarFile(url);
            }

            if (jar != null)
            {
                _searchJar(loader, result, jar, prefix, suffix);
            }
            else
            {
                if (!_searchDir(result, new File(URLDecoder.decode(url.getFile(), "UTF-8")), suffix))
                {
                    _searchFromURL(result, prefix, suffix, url);
                }
            }
        }
    }

    private static boolean _searchDir(Set<URL> result, File dir, String suffix) throws IOException
    {
        boolean dirExists = false;
        if (System.getSecurityManager() != null)
        {
            final File finalDir = dir;
            dirExists = (Boolean) AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run() 
                {
                    return finalDir.exists();
                }
            });
        }  
        else
        {
            dirExists = dir.exists();
        }
        if (dirExists && dir.isDirectory())
        {
            File[] dirFiles = dir.listFiles();
            if (dirFiles != null) 
            {
                for (File file : dirFiles)
                {
                    String path = file.getAbsolutePath();
                    if (file.isDirectory())
                    {
                        _searchDir(result, file, suffix);
                    }
                    else if (path.endsWith(suffix))
                    {
                        result.add(file.toURI().toURL());
                    }
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Search from URL. Fall back on prefix tokens if not able to read from original url param.
     * 
     * @param result
     *            the result urls
     * @param prefix
     *            the current prefix
     * @param suffix
     *            the suffix to match
     * @param url
     *            the current url to start search
     * @throws IOException
     *             for any error
     */
    private static void _searchFromURL(Set<URL> result, String prefix, String suffix, URL url) throws IOException
    {
        boolean done = false;

        InputStream is = _getInputStream(url);
        if (is != null)
        {
            try
            {
                ZipInputStream zis;
                if (is instanceof ZipInputStream)
                {
                    zis = (ZipInputStream) is;
                }
                else
                {
                    zis = new ZipInputStream(is);
                }

                try
                {
                    ZipEntry entry = zis.getNextEntry();
                    // initial entry should not be null
                    // if we assume this is some inner jar
                    done = entry != null;

                    while (entry != null)
                    {
                        String entryName = entry.getName();
                        if (entryName.endsWith(suffix))
                        {
                            result.add(new URL(url.toExternalForm() + entryName));
                        }

                        entry = zis.getNextEntry();
                    }
                }
                finally
                {
                    zis.close();
                }
            }
            catch (Exception ignore)
            {
            }
        }

        if (!done && prefix.length() > 0)
        {
            // we add '/' at the end since join adds it as well
            String urlString = url.toExternalForm() + "/";

            String[] split = prefix.split("/");

            prefix = _join(split, true);

            String end = _join(split, false);
            urlString = urlString.substring(0, urlString.lastIndexOf(end));
            if (isExcludedPrefix(urlString))
            {
                // excluded URL found, ignore it
                return;
            }
            url = new URL(urlString);

            _searchFromURL(result, prefix, suffix, url);
        }
    }

    /**
     * Join tokens, exlude last if param equals true.
     * 
     * @param tokens
     *            the tokens
     * @param excludeLast
     *            do we exclude last token
     * @return joined tokens
     */
    private static String _join(String[] tokens, boolean excludeLast)
    {
        StringBuilder join = new StringBuilder();
        int length = tokens.length - (excludeLast ? 1 : 0);
        for (int i = 0; i < length; i++)
        {
            join.append(tokens[i]).append("/");
        }

        return join.toString();
    }

    /**
     * Open input stream from url. Ignore any errors.
     * 
     * @param url
     *            the url to open
     * @return input stream or null if not possible
     */
    private static InputStream _getInputStream(URL url)
    {
        try
        {
            return url.openStream();
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    /**
     * For URLs to JARs that do not use JarURLConnection - allowed by the servlet spec - attempt to produce a JarFile
     * object all the same. Known servlet engines that function like this include Weblogic and OC4J. This is not a full
     * solution, since an unpacked WAR or EAR will not have JAR "files" as such.
     */
    private static JarFile _getAlternativeJarFile(URL url) throws IOException
    {
        String urlFile = url.getFile();

        // Find suffix prefixed by "!/" on Weblogic
        int wlIndex = urlFile.indexOf("!/");
        // Find suffix prefixed by '!' on OC4J
        int oc4jIndex = urlFile.indexOf('!');
        // Take the first found suffix
        int separatorIndex = wlIndex == -1 && oc4jIndex == -1 ? -1 : wlIndex < oc4jIndex ? wlIndex : oc4jIndex;

        if (separatorIndex != -1)
        {
            String jarFileUrl = urlFile.substring(0, separatorIndex);
            // And trim off any "file:" prefix.
            if (jarFileUrl.startsWith("file:"))
            {
                jarFileUrl = jarFileUrl.substring("file:".length());
            }
            // make sure this is a valid file system path by removing escaping of white-space characters, etc. 
            jarFileUrl = decodeFilesystemUrl(jarFileUrl);
            if (isExcludedPrefix(jarFileUrl) || isExcludedSuffix(jarFileUrl))
            {
                // excluded URL found, ignore it
                return null;
            }
            return new JarFile(jarFileUrl);
        }

        return null;
    }

    private static boolean isExcludedPrefix(String url)
    {
        return EXCLUDED_PREFIX_SET.contains(url.substring(0, 4));
    }

    private static boolean isExcludedSuffix(String url)
    {
        int length = url.length();
        return EXCLUDED_SUFFIX_SET.contains(url.substring(length - 4, length));
    }

    private static void _searchJar(ClassLoader loader, Set<URL> result, JarFile file, String prefix, String suffix)
            throws IOException
    {
        Enumeration<JarEntry> e = file.entries();
        while (e.hasMoreElements())
        {
            try
            {
                String name = e.nextElement().getName();
                if (name.startsWith(prefix) && name.endsWith(suffix))
                {
                    Enumeration<URL> e2 = loader.getResources(name);
                    while (e2.hasMoreElements())
                    {
                        result.add(e2.nextElement());
                    }
                }
            }
            catch (Throwable t)
            {
                // shallow
            }
        }
    }

    private static String decodeFilesystemUrl(String url)
    {
        //borrowed from commons-io FileUtils.
        String decoded = url;
        if (url != null && url.indexOf('%') >= 0)
        {
            int n = url.length();
            StringBuffer buffer = new StringBuffer();
            ByteBuffer bytes = ByteBuffer.allocate(n);
            for (int i = 0; i < n; )
            {
                if (url.charAt(i) == '%')
                {
                    try
                    {
                        do
                        {
                            byte octet = (byte) Integer.parseInt(url.substring(i + 1, i + 3), 16);
                            bytes.put(octet);
                            i += 3;
                        } while (i < n && url.charAt(i) == '%');
                        continue;
                    }
                    catch (RuntimeException e)
                    {
                        // malformed percent-encoded octet, fall through and
                        // append characters literally
                    }
                    finally
                    {
                        if (bytes.position() > 0)
                        {
                            bytes.flip();
                            buffer.append(UTF8.decode(bytes).toString());
                            bytes.clear();
                        }
                    }
                }
                buffer.append(url.charAt(i++));
            }
            decoded = buffer.toString();
        }
        return decoded;
    }

}
