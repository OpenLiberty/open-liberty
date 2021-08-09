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
package org.apache.myfaces.spi;

import java.util.List;

import javax.faces.FacesWrapper;
import javax.faces.context.ExternalContext;

import org.apache.myfaces.config.element.FacesConfig;

/**
 * To wrap the default FacesConfigurationProvider, use a constructor like 
 * CustomFacesConfigurationProvider(FacesConfigurationProvider fcp)
 * and extend it from FacesConfigurationProviderWrapper
 * 
 * @author Leonardo Uribe
 * @since 2.0.3
 */
public abstract class FacesConfigurationProviderWrapper 
    extends FacesConfigurationProvider 
    implements FacesWrapper<FacesConfigurationProvider>
{

    public FacesConfigurationProviderWrapper()
    {

    }

    public FacesConfig getStandardFacesConfig(ExternalContext ectx)
    {
        return getWrapped().getStandardFacesConfig(ectx);
    }

    public FacesConfig getMetaInfServicesFacesConfig(ExternalContext ectx)
    {
        return getWrapped().getMetaInfServicesFacesConfig(ectx);
    }

    public FacesConfig getAnnotationsFacesConfig(ExternalContext ectx,
            boolean metadataComplete)
    {
        return getWrapped().getAnnotationsFacesConfig(ectx, metadataComplete);
    }

    public List<FacesConfig> getClassloaderFacesConfig(ExternalContext ectx)
    {
        return getWrapped().getClassloaderFacesConfig(ectx);
    }

    public List<FacesConfig> getContextSpecifiedFacesConfig(ExternalContext ectx)
    {
        return getWrapped().getContextSpecifiedFacesConfig(ectx);
    }

    public FacesConfig getWebAppFacesConfig(ExternalContext ectx)
    {
        return getWrapped().getWebAppFacesConfig(ectx);
    }
    
    public List<FacesConfig> getFacesFlowFacesConfig(ExternalContext ectx)
    {
        return getWrapped().getFacesFlowFacesConfig(ectx);
    }
    
    public List<FacesConfig> getApplicationConfigurationResourceDocumentPopulatorFacesConfig(ExternalContext ectx)
    {
        return getWrapped().getApplicationConfigurationResourceDocumentPopulatorFacesConfig(ectx);
    }

    public List<FacesConfig> getFaceletTaglibFacesConfig(ExternalContext ectx)
    {
        return getWrapped().getFaceletTaglibFacesConfig(ectx);
    }
}
