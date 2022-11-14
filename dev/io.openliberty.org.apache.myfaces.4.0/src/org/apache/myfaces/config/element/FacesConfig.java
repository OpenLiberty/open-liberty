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
package org.apache.myfaces.config.element;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.myfaces.config.element.facelets.FaceletTagLibrary;

/**
 * @author <a href="mailto:oliver@rossmueller.com">Oliver Rossmueller</a>
 */
public abstract class FacesConfig implements Serializable
{
    public abstract List<Application> getApplications();

    public abstract List<Factory> getFactories();

    public abstract List<Component> getComponents();

    public abstract List<Converter> getConverters();

    public abstract List<NavigationRule> getNavigationRules();

    public abstract List<RenderKit> getRenderKits();

    public abstract List<String> getLifecyclePhaseListener();

    public abstract Map<String, String> getValidators();
    
    public abstract List<Behavior> getBehaviors ();
    
    public abstract List<NamedEvent> getNamedEvents ();
    
    public abstract RenderKit getRenderKit(String renderKitId);
    
    public abstract String getName();
    
    public abstract AbsoluteOrdering getAbsoluteOrdering();

    public abstract Ordering getOrdering();

    public abstract String getMetadataComplete();

    public abstract String getVersion ();
    
    /**
     * 
     * @since 2.1.0
     */
    public List<FacesConfigExtension> getFacesConfigExtensions()
    {
        return Collections.emptyList();
    }
    
    /**
     * @since 2.2.0
     * @return 
     */
    public List<FacesFlowDefinition> getFacesFlowDefinitions()
    {
        return Collections.emptyList();
    }

    
    /**
     * @since 2.2.0
     * @return 
     */
    public List<String> getProtectedViewsUrlPatternList()
    {
        return Collections.emptyList();
    }
    
    /**
     * @since 4.0.0
     * @return 
     */
    public List<ComponentTagDeclaration> getComponentTagDeclarations()
    {
        return Collections.emptyList();
    }
    
    /**
     * @since 2.2.0
     * @return 
     */
    public List<String> getResourceResolversList()
    {
        return Collections.emptyList();
    }
    
    /**
     * @since 2.2.0
     * @return 
     */
    public List<FaceletTagLibrary> getFaceletTagLibraryList()
    {
        return Collections.emptyList();
    }
}
