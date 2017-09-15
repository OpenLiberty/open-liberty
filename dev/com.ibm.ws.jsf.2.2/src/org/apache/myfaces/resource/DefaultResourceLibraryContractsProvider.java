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
package org.apache.myfaces.resource;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.faces.application.ResourceHandler;
import javax.faces.context.ExternalContext;
import org.apache.myfaces.shared.util.ClassUtils;
import org.apache.myfaces.shared.util.WebConfigParamUtils;
import org.apache.myfaces.spi.ResourceLibraryContractsProvider;
import org.apache.myfaces.view.facelets.util.Classpath;

/**
 *
 * @author Leonardo Uribe
 */
public class DefaultResourceLibraryContractsProvider extends ResourceLibraryContractsProvider
{
    private static final String META_INF_CONTRACTS_PREFIX = "META-INF/contracts/";
    private static final String META_INF_CONTRACTS_SUFFIX = "javax.faces.contract.xml";
    private static final String META_INF_CONTRACTS_FILE = "/javax.faces.contract.xml";
    private static final String CONTRACTS = "contracts";

    @Override
    public Set<String> getExternalContextResourceLibraryContracts(ExternalContext context)
        throws IOException
    {
        String directory = WebConfigParamUtils.getStringInitParameter(context, 
            ResourceHandler.WEBAPP_CONTRACTS_DIRECTORY_PARAM_NAME, CONTRACTS);
        
        if (directory.startsWith("/"))
        {
            throw new IllegalStateException("javax.faces.WEBAPP_CONTRACTS_DIRECTORY cannot start with '/");
        }
        if (directory.endsWith("/"))
        {
            directory = directory.substring(0, directory.length()-1);
        }
        
        directory = "/"+directory+"/";
        Set<String> contracts = new HashSet<String>();
        Set<String> paths = context.getResourcePaths(directory);
        if (paths != null)
        {
            for (String path : paths)
            {
                if (path.endsWith("/"))
                {
                    //Add all subdirectories no questions asked.
                    contracts.add(path.substring(directory.length(), path.length()-1));
                }
            }
        }
        return contracts;
    }

    @Override
    public Set<String> getClassloaderResourceLibraryContracts(ExternalContext context)
        throws IOException
    {
        Set<String> contracts = new HashSet<String>();
        URL[] urls = Classpath.search(getClassLoader(), 
            META_INF_CONTRACTS_PREFIX, META_INF_CONTRACTS_SUFFIX);
        for (int i = 0; i < urls.length; i++)
        {
            String urlString = urls[i].toExternalForm();
            int suffixPos = urlString.lastIndexOf(META_INF_CONTRACTS_FILE);
            int slashPos = urlString.lastIndexOf("/", suffixPos-1);
            if (suffixPos > 0 && slashPos > 0)
            {
                contracts.add(urlString.substring(slashPos+1, suffixPos));
            }
        }
        return contracts;
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
