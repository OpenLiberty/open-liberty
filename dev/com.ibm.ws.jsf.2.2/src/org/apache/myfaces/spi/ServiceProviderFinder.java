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
import java.util.Map;
import java.util.ServiceLoader;
import javax.faces.FactoryFinder;
import javax.faces.context.ExternalContext;

/**
 * This class provides an interface to override SPI handling done by
 * MyFaces.
 * 
 * This is useful on environments like in OSGi, because it allows to
 * put custom code to find SPI interfaces under META-INF/services/
 * 
 * @author Leonardo Uribe
 * @since 2.0.3
 *
 */
public abstract class ServiceProviderFinder
{
    public static final String[] KNOWN_SERVICES = 
    {
        FactoryFinder.APPLICATION_FACTORY,
        FactoryFinder.CLIENT_WINDOW_FACTORY,
        FactoryFinder.EXCEPTION_HANDLER_FACTORY,
        FactoryFinder.EXTERNAL_CONTEXT_FACTORY,
        FactoryFinder.FACELET_CACHE_FACTORY,
        FactoryFinder.FACES_CONTEXT_FACTORY,
        FactoryFinder.FLASH_FACTORY,
        FactoryFinder.FLOW_HANDLER_FACTORY,
        FactoryFinder.LIFECYCLE_FACTORY,
        FactoryFinder.PARTIAL_VIEW_CONTEXT_FACTORY,
        FactoryFinder.RENDER_KIT_FACTORY,
        FactoryFinder.TAG_HANDLER_DELEGATE_FACTORY,
        FactoryFinder.VIEW_DECLARATION_LANGUAGE_FACTORY,
        FactoryFinder.VISIT_CONTEXT_FACTORY,
        "org.apache.myfaces.spi.AnnotationProvider",
        "org.apache.myfaces.spi.AnnotationProviderFactory",
        "org.apache.myfaces.spi.FaceletConfigResourceProvider",
        "org.apache.myfaces.spi.FaceletConfigResourceProviderFactory",
        "org.apache.myfaces.spi.FacesConfigResourceProvider",
        "org.apache.myfaces.spi.FacesConfigResourceProviderFactory",
        "org.apache.myfaces.spi.FacesConfigurationMerger",
        "org.apache.myfaces.spi.FacesConfigurationMergerFactory",
        "org.apache.myfaces.spi.FacesConfigurationProvider",
        "org.apache.myfaces.spi.FacesConfigurationProviderFactory",
        "org.apache.myfaces.spi.FacesFlowProvider",
        "org.apache.myfaces.spi.FacesFlowProviderFactory",
        "org.apache.myfaces.spi.FactoryFinderProvider",
        "org.apache.myfaces.spi.FactoryFinderProviderFactory",
        "org.apache.myfaces.spi.InjectionProvider",
        "org.apache.myfaces.spi.InjectionProviderFactory",
        "org.apache.myfaces.spi.ResourceLibraryContractsProvider",
        "org.apache.myfaces.spi.ResourceLibraryContractsProviderFactory",
        "org.apache.myfaces.spi.ViewScopeProvider",
        "org.apache.myfaces.spi.ViewScopeProviderFactory",
        "org.apache.myfaces.spi.WebConfigProvider",
        "org.apache.myfaces.spi.WebConfigProviderFactory",
        "org.apache.myfaces.config.annotation.LifecycleProvider",
        "org.apache.myfaces.config.annotation.LifecycleProviderFactory",
    };
    
    /**
     * Gets the list of classes bound to the spiClass key, looking
     * for entries under META-INF/services/[spiClass]
     * 
     * @param spiClass
     * @return
     */
    public abstract List<String> getServiceProviderList(String spiClass);
    
    public <S> ServiceLoader<S> load(Class<S> spiClass)
    {
        return ServiceLoader.load(spiClass);
    }
    
    /**
     * If ServiceProviderFinderFactory knows beforehand or has stored somewhere the
     * known locations of the SPI interfaces, this method helps to set this config
     * information so the implementation of this interface can use it. The idea is
     * MyFaces initialization algorithm will call getKnownServiceProviderMapInfo
     * method and if the value is not null it will call this method to pass the
     * map back to the ServiceProviderFinder, so it can take it.
     * 
     * @param map 
     */
    public void initKnownServiceProviderMapInfo(ExternalContext ectx, Map<String, List<String>> map)
    {
    }

    public Map<String, List<String>> calculateKnownServiceProviderMapInfo(ExternalContext ectx, 
        String[] knownServices)
    {
        //Map<String, List<String>> map = new HashMap<String, List<String>>();
        //for (String service : knownServices)
        //{
        //    map.put(service, this.getServiceProviderList(service));
        //}
        //return map;
        return null;
    }
}
