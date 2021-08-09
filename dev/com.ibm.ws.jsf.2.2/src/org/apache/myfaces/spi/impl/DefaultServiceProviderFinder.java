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
package org.apache.myfaces.spi.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import javax.faces.context.ExternalContext;

import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.spi.ServiceProviderFinder;

/**
 * 
 * @author Leonardo Uribe
 * @since 2.0.3
 *
 */
public class DefaultServiceProviderFinder extends ServiceProviderFinder
{
    private static final String META_INF_SERVICES = "META-INF/services/";
    
    private Map<String, List<String>> knownServicesMap = null;

    protected Set<URL> getURLs(String spiClass)
    {
        // Use LinkedHashSet to preserve iteration order
        Enumeration<URL> profiles = null;
        try
        {
            profiles = ClassUtils.getContextClassLoader().getResources(
                    META_INF_SERVICES + spiClass);
        }
        catch (IOException e)
        {
            return null;
        }
        if (null != profiles && profiles.hasMoreElements())
        {
            Set<URL> urls = new LinkedHashSet<URL>();
            while (profiles.hasMoreElements())
            {
                URL url = profiles.nextElement();
                urls.add(url);
            }
            return urls;
        }
        return Collections.emptySet();
    }

    @Override
    public List<String> getServiceProviderList(String spiClass)
    {
        if (knownServicesMap != null)
        {
            List<String> result = knownServicesMap.get(spiClass);
            if (result != null)
            {
                return result;
            }
        }
        
        Set<URL> urls = getURLs(spiClass);

        if (!urls.isEmpty())
        {
            List<String> results = new LinkedList<String>();
            for (URL url : urls)
            {
                InputStream is = null;
    
                try
                {
                    try
                    {
                        is = openStreamWithoutCache(url);
                        if (is != null)
                        {
                            // This code is needed by EBCDIC and other
                            // strange systems.  It's a fix for bugs
                            // reported in xerces
                            BufferedReader rd;
                            try
                            {
                                rd = new BufferedReader(new InputStreamReader(is,
                                        "UTF-8"));
                            }
                            catch (java.io.UnsupportedEncodingException e)
                            {
                                rd = new BufferedReader(new InputStreamReader(is));
                            }
    
                            try
                            {
                                String serviceImplName;
                                while ((serviceImplName = rd.readLine()) != null)
                                {
                                    int idx = serviceImplName.indexOf('#');
                                    if (idx >= 0)
                                    {
                                        serviceImplName = serviceImplName
                                                .substring(0, idx);
                                    }
                                    serviceImplName = serviceImplName.trim();
    
                                    if (serviceImplName.length() != 0)
                                    {
                                        results.add(serviceImplName);
                                    }
                                }
                            }
                            finally
                            {
                                if (rd != null)
                                {
                                    rd.close();
                                }
                            }
                        }
                    }
                    finally
                    {
                        if (is != null)
                        {
                            is.close();
                        }
                    }
                }
                catch (IOException e)
                {
                    // ignore
                }
            }
            return results;
        }
        return Collections.emptyList();
    }

    private InputStream openStreamWithoutCache(URL url) throws IOException
    {
        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }
    
    @Override
    public <S> ServiceLoader<S> load(Class<S> spiClass)
    {
        return ServiceLoader.load(spiClass);
    }

    @Override
    public void initKnownServiceProviderMapInfo(ExternalContext ectx, Map<String, List<String>> map)
    {
        knownServicesMap = map;
    }
}
