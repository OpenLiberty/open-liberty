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
package org.apache.myfaces.shared.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;

import java.io.InputStreamReader;

import java.net.URL;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.faces.FacesException;

/**
 * Utility methods for accessing classes and resources using an appropriate
 * class loader.
 *
 * @version $Revision: 1200044 $ $Date: 2011-11-09 23:44:02 +0000 (Wed, 09 Nov 2011) $
 */
public final class ClassLoaderUtils
{
  // Utility class only, no instances
  private ClassLoaderUtils()
  {
  }
  
  /**
   * Loads the class with the specified name.  For Java 2 callers, the
   * current thread's context class loader is preferred, falling back on the
   * system class loader of the caller when the current thread's context is not
   * set, or the caller is pre Java 2.
   *
   * @param     name  the name of the class
   * @return    the resulting <code>Class</code> object
   * @exception ClassNotFoundException if the class was not found
   */
  public static Class<?> loadClass(
    String name) throws ClassNotFoundException
  {
    return loadClass(name, null);
  }

  /**
   * Locates the resource with the specified name.  For Java 2 callers, the
   * current thread's context class loader is preferred, falling back on the
   * system class loader of the caller when the current thread's context is not
   * set, or the caller is pre Java 2.
   *
   * @param     name  the name of the resource
   * @return    the resulting <code>URL</code> object
   */
  public static URL getResource(
    String name)
  {
    return getResource(name, null);
  }

  /**
   * Locates the stream resource with the specified name.  For Java 2 callers,
   * the current thread's context class loader is preferred, falling back on
   * the system class loader of the caller when the current thread's context is
   * not set, or the caller is pre Java 2.
   *
   * @param     name  the name of the resource
   * @return    the resulting <code>InputStream</code> object
   */
  public static InputStream getResourceAsStream(
    String name)
  {
    return getResourceAsStream(name, null);
  }

  /**
   * Loads the class with the specified name.  For Java 2 callers, the
   * current thread's context class loader is preferred, falling back on the
   * class loader of the caller when the current thread's context is not set,
   * or the caller is pre Java 2.  If the callerClassLoader is null, then
   * fall back on the system class loader.
   *
   * @param     name  the name of the class
   * @param     callerClassLoader  the calling class loader context
   * @return    the resulting <code>Class</code> object
   * @exception ClassNotFoundException if the class was not found
   */
  public static Class<?> loadClass(
    String      name,
    ClassLoader callerClassLoader) throws ClassNotFoundException
  {
    Class<?> clazz = null;

    try
    {
      ClassLoader loader = getContextClassLoader();

      if (loader != null)
      {
          clazz = loader.loadClass(name);
      }
    }
    catch (ClassNotFoundException e)
    {
      // treat as though loader not set
    }

    if (clazz == null)
    {
      if (callerClassLoader != null)
      {
          clazz = callerClassLoader.loadClass(name);
      }
      else
      {
          clazz = Class.forName(name);
      }
    }

    return clazz;
  }

  /**
   * Locates the resource with the specified name.  For Java 2 callers, the
   * current thread's context class loader is preferred, falling back on the
   * class loader of the caller when the current thread's context is not set,
   * or the caller is pre Java 2.  If the callerClassLoader is null, then
   * fall back on the system class loader.
   *
   * @param     name  the name of the resource
   * @param     callerClassLoader  the calling class loader context
   * @return    the resulting <code>URL</code> object
   */
  public static URL getResource(
    String      name,
    ClassLoader callerClassLoader)
  {
    _checkResourceName(name);

    URL url = null;

    ClassLoader loader = getContextClassLoader();

    if (loader != null)
    {
        url = loader.getResource(name);
    }

    if (url == null)
    {
      if (callerClassLoader != null)
      {
          url = callerClassLoader.getResource(name);
      }
      else
      {
          url = ClassLoader.getSystemResource(name);
      }
    }

    return url;
  }

  /**
   * Locates the resource stream with the specified name.  For Java 2 callers,
   * the current thread's context class loader is preferred, falling back on
   * the class loader of the caller when the current thread's context is not
   * set, or the caller is pre Java 2.  If the callerClassLoader is null, then
   * fall back on the system class loader.
   *
   * @param     name  the name of the resource
   * @param     callerClassLoader  the calling class loader context
   * @return    the resulting <code>InputStream</code> object
   */
  public static InputStream getResourceAsStream(
    String      name,
    ClassLoader callerClassLoader)
  {
    _checkResourceName(name);

    InputStream stream = null;

    ClassLoader loader = getContextClassLoader();

    if (loader != null)
    {
        stream = loader.getResourceAsStream(name);
    }

    if (stream == null)
    {
      if (callerClassLoader != null)
      {
          stream = callerClassLoader.getResourceAsStream(name);
      }
      else
      {
          stream = ClassLoader.getSystemResourceAsStream(name);
      }
    }

    return stream;
  }

  /**
   * Dynamically accesses the current context class loader. 
   * Includes a check for priviledges against java2 security 
   * to ensure no security related exceptions are encountered. 
   * Returns null if there is no per-thread context class loader.
   */
  public static ClassLoader getContextClassLoader()
  {
      if (System.getSecurityManager() != null) 
      {
          try 
          {
              ClassLoader cl = AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>()
                      {
                          public ClassLoader run() throws PrivilegedActionException
                          {
                              return Thread.currentThread().getContextClassLoader();
                          }
                      });
              return cl;
          }
          catch (PrivilegedActionException pae)
          {
              throw new FacesException(pae);
          }
      }
      else
      {
          return Thread.currentThread().getContextClassLoader();
      }
  }

  /**
   * Instantiate a service from a file in /META-INF/services.
   * <P>
   * The following is an excerpt from the JAR File specification:
   * A service provider identifies itself by placing a provider-configuration file 
   * in the resource directory META-INF/services. 
   * The file's name should consist of the fully-qualified name of the abstract service class. 
   * The file should contain a newline-separated list of unique concrete provider-class names. 
   * Space and tab characters, as well as blank lines, are ignored. The comment character is '#' (0x23); 
   * on each line all characters following the first comment character are ignored. 
   * The file must be encoded in UTF-8. 
   * 
   * @param service the classname of the abstract service class.
   * eg: javax.servlet.Filter
   */
  @SuppressWarnings("unchecked")
  public static <T> List<T> getServices(String service)
  {
    String serviceUri ="META-INF/services/" + service;
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    try
    {
      Enumeration<URL> urls = loader.getResources(serviceUri);
      if (urls.hasMoreElements())
      {
        List<T> services = new ArrayList<T>(1);
        Set<String> keys = new HashSet<String>(20);
        do
        {
          URL url = urls.nextElement();
          
          if (_LOG.isLoggable(Level.FINEST))
          {
            _LOG.finest("Processing: " + url);
          }
          try
          {
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            try
            {
              while(true)
              {
                String line = in.readLine();
                if (line == null)
                {
                    break;
                }
                
                String className = _parseLine(line);
                
                if(className!=null && keys.add(className))
                {
                  T instance = (T) _getClass(loader, className);
                  services.add(instance);
                }                
              }
            }
            finally
            {
              in.close();
            }
          }
          catch (Exception e)
          {
            if (_LOG.isLoggable(Level.WARNING))
            {
              _LOG.log(Level.WARNING, "Error parsing URL: " + url, e);
            }
          }
        } 
        while(urls.hasMoreElements());
        
        if (services.size() == 1)
        {
            return Collections.singletonList(services.get(0));
        }
        
        return Collections.unmodifiableList(services);
      }
    }
    catch (IOException e)
    {
      if (_LOG.isLoggable(Level.SEVERE))
      {
        _LOG.log(Level.SEVERE, "Error loading Resource: " + serviceUri, e);
      }
    }

    return Collections.emptyList();
  }
  
  private static String _parseLine(String line)
  {
    // Eliminate any comments
    int hashIndex = line.indexOf('#');
    if (hashIndex >= 0)
    {
        line = line.substring(0, hashIndex);
    }

    // and any whitespace
    line = line.trim();
    if (line.length() > 0)
    {
      return line;
    }
    
    return null;
  }
  
  private static Object _getClass(ClassLoader loader, String className)
    throws ClassNotFoundException, InstantiationException,
           IllegalAccessException
  {
    Class<?> clazz = loader.loadClass(className);
    return clazz.newInstance();
  }

  private static void _checkResourceName(String name)
  {
    if ((name != null) && name.startsWith("/"))
    {
      if (_LOG.isLoggable(Level.WARNING))
      {
        _LOG.log(Level.WARNING, "Resource name not portable: " +name);
      }
    }
  }

  private static final Logger _LOG = Logger.getLogger(ClassLoaderUtils.class.getName());
}
