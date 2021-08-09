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
package org.apache.myfaces.application;

import javax.faces.application.ProjectStage;
import javax.faces.application.ResourceHandler;
import javax.faces.context.FacesContext;
import org.apache.myfaces.resource.ClassLoaderContractResourceLoader;
import org.apache.myfaces.resource.ExternalContextContractResourceLoader;
import org.apache.myfaces.resource.FacesFlowClassLoaderResourceLoader;

import org.apache.myfaces.resource.InternalClassLoaderResourceLoader;
import org.apache.myfaces.resource.RootExternalContextResourceLoader;
import org.apache.myfaces.resource.TempDirFileCacheContractResourceLoader;
import org.apache.myfaces.resource.TempDirFileCacheResourceLoader;
import org.apache.myfaces.shared.renderkit.html.util.ResourceUtils;
import org.apache.myfaces.shared.resource.BaseResourceHandlerSupport;
import org.apache.myfaces.shared.resource.ClassLoaderResourceLoader;
import org.apache.myfaces.shared.resource.ContractResourceLoader;
import org.apache.myfaces.shared.resource.ExternalContextResourceLoader;
import org.apache.myfaces.shared.resource.ResourceLoader;
import org.apache.myfaces.shared.util.WebConfigParamUtils;

/**
 * A ResourceHandlerSupport implementation for use with standard Java Servlet engines,
 * ie an engine that supports javax.servlet, and uses a standard web.xml file.
 * 
 * @author Leonardo Uribe (latest modification by $Author: lu4242 $)
 * @version $Revision: 1560226 $ $Date: 2014-01-22 00:24:03 +0000 (Wed, 22 Jan 2014) $
 */
public class DefaultResourceHandlerSupport extends BaseResourceHandlerSupport
{

    private static final String META_INF_RESOURCES = "META-INF/resources";
    private static final String RESOURCES = "resources";
    private static final String META_INF_INTERNAL_RESOURCES = "META-INF/internal-resources";
    private static final String META_INF_CONTRACTS = "META-INF/contracts";
    private static final String CONTRACTS = "contracts";

    private ResourceLoader[] _resourceLoaders;
    
    private ContractResourceLoader[] _contractResourceLoaders;
    
    private ResourceLoader[] _viewResourceLoaders;
    
    public DefaultResourceHandlerSupport()
    {
        super();
    }

    public ResourceLoader[] getResourceLoaders()
    {
        if (_resourceLoaders == null)
        {
            FacesContext facesContext = FacesContext.getCurrentInstance(); 
            
            String directory = WebConfigParamUtils.getStringInitParameter(facesContext.getExternalContext(), 
                ResourceHandler.WEBAPP_RESOURCES_DIRECTORY_PARAM_NAME, RESOURCES);
            
            if (TempDirFileCacheResourceLoader.isValidCreateTemporalFiles(facesContext))
            {
                //The ExternalContextResourceLoader has precedence over
                //ClassLoaderResourceLoader, so it goes first.
                String renderedJSFJS = WebConfigParamUtils.getStringInitParameter(facesContext.getExternalContext(),
                        InternalClassLoaderResourceLoader.MYFACES_JSF_MODE,
                        ResourceUtils.JSF_MYFACES_JSFJS_NORMAL);

                if (facesContext.isProjectStage(ProjectStage.Development) ||
                     !renderedJSFJS.equals(ResourceUtils.JSF_MYFACES_JSFJS_NORMAL))
                {
                    _resourceLoaders = new ResourceLoader[] {
                            new TempDirFileCacheResourceLoader(new ExternalContextResourceLoader("/"+directory)),
                            new TempDirFileCacheResourceLoader(new FacesFlowClassLoaderResourceLoader()),
                            new TempDirFileCacheResourceLoader(
                                             new InternalClassLoaderResourceLoader(META_INF_INTERNAL_RESOURCES)),
                            new TempDirFileCacheResourceLoader(new ClassLoaderResourceLoader(META_INF_RESOURCES))
                    };
                }
                else
                {
                    _resourceLoaders = new ResourceLoader[] {
                            new TempDirFileCacheResourceLoader(new ExternalContextResourceLoader("/"+directory)),
                            new TempDirFileCacheResourceLoader(new FacesFlowClassLoaderResourceLoader()),
                            new TempDirFileCacheResourceLoader(new ClassLoaderResourceLoader(META_INF_RESOURCES))
                    };
                }
            }
            else
            {            
                //The ExternalContextResourceLoader has precedence over
                //ClassLoaderResourceLoader, so it goes first.
                String renderedJSFJS = WebConfigParamUtils.getStringInitParameter(facesContext.getExternalContext(),
                        InternalClassLoaderResourceLoader.MYFACES_JSF_MODE,
                        ResourceUtils.JSF_MYFACES_JSFJS_NORMAL);

                if (facesContext.isProjectStage(ProjectStage.Development) ||
                     !renderedJSFJS.equals(ResourceUtils.JSF_MYFACES_JSFJS_NORMAL))
                {
                    _resourceLoaders = new ResourceLoader[] {
                            new ExternalContextResourceLoader("/"+directory),
                            new FacesFlowClassLoaderResourceLoader(),
                            new InternalClassLoaderResourceLoader(META_INF_INTERNAL_RESOURCES),
                            new ClassLoaderResourceLoader(META_INF_RESOURCES)
                    };
                }
                else
                {
                    _resourceLoaders = new ResourceLoader[] {
                            new ExternalContextResourceLoader("/"+directory),
                            new FacesFlowClassLoaderResourceLoader(),
                            new ClassLoaderResourceLoader(META_INF_RESOURCES)
                    };
                }
            }
        }
        return _resourceLoaders;
    }
    
    @Override
    public ContractResourceLoader[] getContractResourceLoaders()
    {
        if (_contractResourceLoaders == null)
        {
            FacesContext facesContext = FacesContext.getCurrentInstance(); 
            
            String directory = WebConfigParamUtils.getStringInitParameter(facesContext.getExternalContext(), 
                ResourceHandler.WEBAPP_CONTRACTS_DIRECTORY_PARAM_NAME, CONTRACTS);

            if (directory.startsWith("/"))
            {
                throw new IllegalStateException("javax.faces.WEBAPP_CONTRACTS_DIRECTORY cannot start with '/");
            }
            
            if (TempDirFileCacheResourceLoader.isValidCreateTemporalFiles(facesContext))
            {
                _contractResourceLoaders= new ContractResourceLoader[] { 
                    new TempDirFileCacheContractResourceLoader(
                        new ExternalContextContractResourceLoader("/"+directory)),
                    new TempDirFileCacheContractResourceLoader(
                        new ClassLoaderContractResourceLoader(META_INF_CONTRACTS))
                };
            }
            else
            {
            
                _contractResourceLoaders= new ContractResourceLoader[] { 
                    new ExternalContextContractResourceLoader("/"+directory),
                    new ClassLoaderContractResourceLoader(META_INF_CONTRACTS)
                };
            }
        }
        return _contractResourceLoaders;
    }
    
    @Override
    public ResourceLoader[] getViewResourceLoaders()
    {
        if (_viewResourceLoaders == null)
        {
            FacesContext facesContext = FacesContext.getCurrentInstance(); 
            if (TempDirFileCacheResourceLoader.isValidCreateTemporalFiles(facesContext))
            {
                _viewResourceLoaders = new ResourceLoader[] {
                        new RootExternalContextResourceLoader(),
                        new TempDirFileCacheResourceLoader(new FacesFlowClassLoaderResourceLoader())
                };
            }
            else
            {
                _viewResourceLoaders = new ResourceLoader[] {
                        new RootExternalContextResourceLoader(),
                        new FacesFlowClassLoaderResourceLoader()
                };
            }
        }
        return _viewResourceLoaders;
    }

}
