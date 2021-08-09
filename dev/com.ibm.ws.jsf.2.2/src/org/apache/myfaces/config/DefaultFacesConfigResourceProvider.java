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
package org.apache.myfaces.config;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.faces.context.ExternalContext;
import org.apache.myfaces.shared.config.MyfacesConfig;

import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.spi.FacesConfigResourceProvider;
import org.apache.myfaces.util.ContainerUtils;
import org.apache.myfaces.config.util.GAEUtils;
import org.apache.myfaces.view.facelets.util.Classpath;

/**
 * 
 * @since 2.0.2
 * @author Leonardo Uribe
 */
public class DefaultFacesConfigResourceProvider extends FacesConfigResourceProvider
{
    private static final String META_INF_PREFIX = "META-INF/";

    private static final String FACES_CONFIG_SUFFIX = ".faces-config.xml";
    
    /**
     * <p>Resource path used to acquire implicit resources buried
     * inside application JARs.</p>
     */
    private static final String FACES_CONFIG_IMPLICIT = "META-INF/faces-config.xml";


    public DefaultFacesConfigResourceProvider()
    {
        super();
    }

    @Override
    public Collection<URL> getMetaInfConfigurationResources(
            ExternalContext context) throws IOException
    {
        List<URL> urlSet = new ArrayList<URL>();
        
        //This usually happens when maven-jetty-plugin is used
        //Scan jars looking for paths including META-INF/faces-config.xml
        Enumeration<URL> resources = getClassLoader().getResources(FACES_CONFIG_IMPLICIT);
        while (resources.hasMoreElements())
        {
            urlSet.add(resources.nextElement());
        }

        String jarFilesToScanParam = MyfacesConfig.getCurrentInstance(context).getGaeJsfJarFiles();
        jarFilesToScanParam = jarFilesToScanParam != null ? jarFilesToScanParam.trim() : null;
        if (ContainerUtils.isRunningOnGoogleAppEngine(context) && 
            jarFilesToScanParam != null &&
            jarFilesToScanParam.length() > 0)
        {
            Collection<URL> urlsGAE = GAEUtils.searchInWebLib(
                    context, getClassLoader(), jarFilesToScanParam, META_INF_PREFIX, FACES_CONFIG_SUFFIX);
            if (urlsGAE != null)
            {
                urlSet.addAll(urlsGAE);
            }
        }
        else
        {
            //Scan files inside META-INF ending with .faces-config.xml
            URL[] urls = Classpath.search(getClassLoader(), META_INF_PREFIX, FACES_CONFIG_SUFFIX);
            for (int i = 0; i < urls.length; i++)
            {
                urlSet.add(urls[i]);
            }
        }
        
        return urlSet;
    }

    private ClassLoader getClassLoader()
    {
        ClassLoader loader = ClassUtils.getContextClassLoader();
        if (loader == null)
        {
            loader = this.getClass().getClassLoader();
        }
        return loader;
    }
}
