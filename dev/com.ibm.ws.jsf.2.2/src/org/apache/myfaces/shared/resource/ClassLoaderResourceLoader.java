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
package org.apache.myfaces.shared.resource;

import java.io.InputStream;
import java.net.URL;

import org.apache.myfaces.shared.util.ClassUtils;

/**
 * A resource loader implementation which loads resources from the thread ClassLoader.
 * 
 */
public class ClassLoaderResourceLoader extends ResourceLoader
{
    /**
     * It checks version like this: 1, 1_0, 1_0_0, 100_100
     * 
     * Used on getLibraryVersion to filter resource directories
     **/
    //protected static Pattern VERSION_CHECKER = Pattern.compile("\\p{Digit}+(_\\p{Digit}*)*");

    /**
     * It checks version like this: /1.js, /1_0.js, /1_0_0.js, /100_100.js
     * 
     * Used on getResourceVersion to filter resources
     **/
    //protected static Pattern RESOURCE_VERSION_CHECKER = Pattern.compile("/\\p{Digit}+(_\\p{Digit}*)*\\..*");

    /*
    private FileFilter _libraryFileFilter = new FileFilter()
    {
        public boolean accept(File pathname)
        {
            if (pathname.isDirectory() && VERSION_CHECKER.matcher(pathname.getName()).matches())
            {
                return true;
            }
            return false;
        }
    };*/

    /*
    private FileFilter _resourceFileFilter = new FileFilter()
    {
        public boolean accept(File pathname)
        {
            if (pathname.isDirectory() && RESOURCE_VERSION_CHECKER.matcher(pathname.getName()).matches())
            {
                return true;
            }
            return false;
        }
    };*/
    
    public ClassLoaderResourceLoader(String prefix)
    {
        super(prefix);
    }

    @Override
    public String getLibraryVersion(String path)
    {
        return null;
        /*
        String libraryVersion = null;
        if (getPrefix() != null)
            path = getPrefix() + '/' + path;

        URL url = getClassLoader().getResource(path);

        if (url == null)
        {
            // This library does not exists for this
            // ResourceLoader
            return null;
        }

        // The problem here is how to scan the directory. When a ClassLoader
        // is used two cases could occur
        // 1. The files are unpacked so we can use Url.toURI and crawl
        // the directory using the api for files.
        // 2. The files are packed in a jar. This case is more tricky,
        // because we only have a URL. Checking the jar api we can use
        // JarURLConnection (Sounds strange, but the api of
        // URL.openConnection says that for a jar connection a
        // JarURLConnection is returned). From this point we can access
        // to the jar api and solve the algoritm.
        if (url.getProtocol().equals("file"))
        {
            try
            {
                File directory = new File(url.toURI());
                if (directory.isDirectory())
                {
                    File[] versions = directory.listFiles(_libraryFileFilter);
                    for (int i = 0; i < versions.length; i++)
                    {
                        String version = versions[i].getName();
                        if (VERSION_CHECKER.matcher(version).matches())
                        {
                            if (libraryVersion == null)
                            {
                                libraryVersion = version;
                            }
                            else if (getVersionComparator().compare(libraryVersion, version) < 0)
                            {
                                libraryVersion = version;
                            }
                        }
                    }
                }
            }
            catch (URISyntaxException e)
            {
                // Just return null, because library version cannot be
                // resolved.
                Logger log = Logger.getLogger(ClassLoaderResourceLoader.class.getName()); 
                if (log.isLoggable(Level.WARNING))
                {
                    log.log(Level.WARNING, "url "+url.toString()+" cannot be translated to uri: "+e.getMessage(), e);
                }
            }
        }
        else if (isJarResourceProtocol(url.getProtocol()))
        {
            try
            {
                url = getClassLoader().getResource(path + '/');

                if (url != null)
                {
                    JarURLConnection conn = (JarURLConnection)url.openConnection();
                    // See DIGESTER-29 for related problem
                    conn.setUseCaches(false);

                    try
                    {
                        if (conn.getJarEntry().isDirectory())
                        {
                            // Unfortunately, we have to scan all entry files
                            // because there is no proper api to scan it as a
                            // directory tree.
                            JarFile file = conn.getJarFile();
                            for (Enumeration<JarEntry> en = file.entries(); en.hasMoreElements();)
                            {
                                JarEntry entry = en.nextElement();
                                String entryName = entry.getName();
    
                                if (entryName.startsWith(path + '/'))
                                {
                                    if (entryName.length() == path.length() + 1)
                                    {
                                        // the same string, just skip it
                                        continue;
                                    }
    
                                    if (entryName.charAt(entryName.length() - 1) != '/')
                                    {
                                        // Skip files
                                        continue;
                                    }
    
                                    entryName = entryName.substring(path.length() + 1, entryName.length() - 1);
    
                                    if (entryName.indexOf('/') >= 0)
                                    {
                                        // Inner Directory
                                        continue;
                                    }
    
                                    String version = entryName;
                                    if (VERSION_CHECKER.matcher(version).matches())
                                    {
                                        if (libraryVersion == null)
                                        {
                                            libraryVersion = version;
                                        }
                                        else if (getVersionComparator().compare(libraryVersion, version) < 0)
                                        {
                                            libraryVersion = version;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    finally
                    {
                        //See TRINIDAD-73
                        //just close the input stream again if
                        //by inspecting the entries the stream
                        //was let open.
                        try
                        {
                            conn.getInputStream().close();
                        }
                        catch (Exception exception)
                        {
                            // Ignored
                        }
                    }
                }
            }
            catch (IOException e)
            {
                // Just return null, because library version cannot be
                // resolved.
                Logger log = Logger.getLogger(ClassLoaderResourceLoader.class.getName()); 
                if (log.isLoggable(Level.WARNING))
                {
                    log.log(Level.WARNING, "IOException when scanning for resource in jar file:", e);
                }
            }
        }
        return libraryVersion;
        */
    }

    @Override
    public InputStream getResourceInputStream(ResourceMeta resourceMeta)
    {
        InputStream is = null;
        if (getPrefix() != null && !"".equals(getPrefix()))
        {
            String name = getPrefix() + '/' + resourceMeta.getResourceIdentifier();
            is = getClassLoader().getResourceAsStream(name);
            if (is == null)
            {
                is = this.getClass().getClassLoader().getResourceAsStream(name);
            }
            return is;
        }
        else
        {
            is = getClassLoader().getResourceAsStream(resourceMeta.getResourceIdentifier());
            if (is == null)
            {
                is = this.getClass().getClassLoader().getResourceAsStream(resourceMeta.getResourceIdentifier());
            }
            return is;
        }
    }

    //@Override
    public URL getResourceURL(String resourceId)
    {
        URL url = null;
        if (getPrefix() != null && !"".equals(getPrefix()))
        {
            String name = getPrefix() + '/' + resourceId;
            url = getClassLoader().getResource(name);
            if (url == null)
            {
                url = this.getClass().getClassLoader().getResource(name);
            }
            return url;
        }
        else
        {
            url = getClassLoader().getResource(resourceId);
            if (url == null)
            {
                url = this.getClass().getClassLoader().getResource(resourceId);
            }
            return url;
        }
    }
    
    @Override
    public URL getResourceURL(ResourceMeta resourceMeta)
    {
        return getResourceURL(resourceMeta.getResourceIdentifier());
    }

    @Override
    public String getResourceVersion(String path)
    {
        return null;
        /*
        String resourceVersion = null;

        if (getPrefix() != null)
            path = getPrefix() + '/' + path;

        URL url = getClassLoader().getResource(path);

        if (url == null)
        {
            // This library does not exists for this
            // ResourceLoader
            return null;
        }

        if (url.getProtocol().equals("file"))
        {
            try
            {
                File directory = new File(url.toURI());
                if (directory.isDirectory())
                {
                    File[] versions = directory.listFiles(_resourceFileFilter);
                    for (int i = 0; i < versions.length; i++)
                    {
                        String version = versions[i].getName();
                        if (resourceVersion == null)
                        {
                            resourceVersion = version;
                        }
                        else if (getVersionComparator().compare(resourceVersion, version) < 0)
                        {
                            resourceVersion = version;
                        }
                    }
                    //Since it is a directory and no version found set resourceVersion as invalid
                    if (resourceVersion == null)
                    {
                        resourceVersion = VERSION_INVALID;
                    }
                }
            }
            catch (URISyntaxException e)
            {
                Logger log = Logger.getLogger(ClassLoaderResourceLoader.class.getName()); 
                if (log.isLoggable(Level.WARNING))
                {
                    log.log(Level.WARNING, "url "+url.toString()+" cannot be translated to uri: "+e.getMessage(), e);
                }
            }
        }
        else if (isJarResourceProtocol(url.getProtocol()))
        {
            try
            {
                url = getClassLoader().getResource(path + '/');

                if (url != null)
                {
                    JarURLConnection conn = (JarURLConnection)url.openConnection();
                    // See DIGESTER-29 for related problem
                    conn.setUseCaches(false);

                    try
                    {
                        if (conn.getJarEntry().isDirectory())
                        {
                            // Unfortunately, we have to scan all entry files
                            JarFile file = conn.getJarFile();
                            for (Enumeration<JarEntry> en = file.entries(); en.hasMoreElements();)
                            {
                                JarEntry entry = en.nextElement();
                                String entryName = entry.getName();
    
                                if (entryName.startsWith(path + '/'))
                                {
                                    if (entryName.length() == path.length() + 1)
                                    {
                                        // the same string, just skip it
                                        continue;
                                    }
        
                                    entryName = entryName.substring(path.length());
                                    if (RESOURCE_VERSION_CHECKER.matcher(entryName).matches())
                                    {
                                        String version = entryName.substring(1, entryName.lastIndexOf('.'));
                                        if (resourceVersion == null)
                                        {
                                            resourceVersion = version;
                                        }
                                        else if (getVersionComparator().compare(resourceVersion, version) < 0)
                                        {
                                            resourceVersion = version;
                                        }
                                    }
                                }
                            }
                            if (resourceVersion == null)
                            {
                                resourceVersion = VERSION_INVALID;
                            }
                        }
                    }
                    finally
                    {
                        //See TRINIDAD-73
                        //just close the input stream again if
                        //by inspecting the entries the stream
                        //was let open.
                        try
                        {
                            conn.getInputStream().close();
                        }
                        catch (Exception exception)
                        {
                            // Ignored
                        }
                    }

                }
            }
            catch (IOException e)
            {
                // Just return null, because library version cannot be
                // resolved.
                Logger log = Logger.getLogger(ClassLoaderResourceLoader.class.getName()); 
                if (log.isLoggable(Level.WARNING))
                {
                    log.log(Level.WARNING, "IOException when scanning for resource in jar file:", e);
                }
            }
        }
        return resourceVersion;
        */
    }

    @Override
    public ResourceMeta createResourceMeta(String prefix, String libraryName, String libraryVersion,
                                           String resourceName, String resourceVersion)
    {
        //if (_developmentStage && libraryName != null && 
        //        ResourceUtils.JAVAX_FACES_LIBRARY_NAME.equals(libraryName) &&
        //        ResourceUtils.JSF_JS_RESOURCE_NAME.equals(resourceName))
        //{
            // InternalClassLoaderResourceLoader will serve it, so return null in this case.
        //    return null;
        //} else if (_developmentStage && libraryName != null &&
        //        ResourceUtils.MYFACES_LIBRARY_NAME.equals(libraryName) &&
        //        ResourceUtils.MYFACES_JS_RESOURCE_NAME.equals(resourceName)) {
            // InternalClassLoaderResourceLoader will serve it, so return null in this case.
        //     return null;
        //} else
        //{
            return new ResourceMetaImpl(prefix, libraryName, libraryVersion, resourceName, resourceVersion);
        //}
    }

    /**
     * Returns the ClassLoader to use when looking up resources under the top level package. By default, this is the
     * context class loader.
     * 
     * @return the ClassLoader used to lookup resources
     */
    protected ClassLoader getClassLoader()
    {
        return ClassUtils.getContextClassLoader();
    }

    @Override
    public boolean libraryExists(String libraryName)
    {
        if (getPrefix() != null && !"".equals(getPrefix()))
        {
            URL url = getClassLoader().getResource(getPrefix() + '/' + libraryName);
            if (url == null)
            {
                url = this.getClass().getClassLoader().getResource(getPrefix() + '/' + libraryName);
            }
            if (url != null)
            {
                return true;
            }
        }
        else
        {
            URL url = getClassLoader().getResource(libraryName);
            if (url == null)
            {
                url = this.getClass().getClassLoader().getResource(libraryName);
            }
            if (url != null)
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * <p>Determines whether the given URL resource protocol refers to a JAR file. Note that
     * BEA WebLogic and IBM WebSphere don't use the "jar://" protocol for some reason even
     * though you can treat these resources just like normal JAR files, i.e. you can ignore
     * the difference between these protocols after this method has returned.</p>
     *
     * @param protocol the URL resource protocol you want to check
     *
     * @return <code>true</code> if the given URL resource protocol refers to a JAR file,
     *          <code>false</code> otherwise
     */
    /*
    private static boolean isJarResourceProtocol(String protocol)
    {
        // Websphere uses the protocol "wsjar://" and Weblogic uses the protocol "zip://".
        return "jar".equals(protocol) || "wsjar".equals(protocol) || "zip".equals(protocol); 
    }*/

}
