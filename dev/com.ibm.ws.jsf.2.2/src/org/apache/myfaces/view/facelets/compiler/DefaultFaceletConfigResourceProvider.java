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
package org.apache.myfaces.view.facelets.compiler;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.context.ExternalContext;
import org.apache.myfaces.shared.config.MyfacesConfig;

import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.spi.FaceletConfigResourceProvider;
import org.apache.myfaces.util.ContainerUtils;
import org.apache.myfaces.config.util.GAEUtils;
import org.apache.myfaces.view.facelets.util.Classpath;

/**
 * 
 * @since 2.0.2
 * @author Leonardo Uribe
 */
public class DefaultFaceletConfigResourceProvider extends FaceletConfigResourceProvider
{
    private static final String META_INF_PREFIX = "META-INF/";

    private static final String FACELET_TAGLIB_SUFFIX = ".taglib.xml";

    public DefaultFaceletConfigResourceProvider()
    {
        super();
    }

    @Override
    public Collection<URL> getFaceletTagLibConfigurationResources(
            ExternalContext context) throws IOException
    {
        List<URL> urlSet = new ArrayList<URL>();
        
        String jarFilesToScanParam = MyfacesConfig.getCurrentInstance(context).getGaeJsfJarFiles();
        jarFilesToScanParam = jarFilesToScanParam != null ? jarFilesToScanParam.trim() : null;
        if (ContainerUtils.isRunningOnGoogleAppEngine(context) && 
            jarFilesToScanParam != null &&
            jarFilesToScanParam.length() > 0)
        {
            Collection<URL> urlsGAE = GAEUtils.searchInWebLib(
                    context, getClassLoader(), jarFilesToScanParam, META_INF_PREFIX, FACELET_TAGLIB_SUFFIX);
            if (urlsGAE != null)
            {
                urlSet.addAll(urlsGAE);
            }
        }
        else
        {
            //Scan files inside META-INF ending with .faces-config.xml
            URL[] urls = Classpath.search(getClassLoader(), META_INF_PREFIX, FACELET_TAGLIB_SUFFIX);
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
