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
package org.apache.myfaces.config.impl.element;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.myfaces.config.element.AbsoluteOrdering;
import org.apache.myfaces.config.element.Application;
import org.apache.myfaces.config.element.Behavior;
import org.apache.myfaces.config.element.Component;
import org.apache.myfaces.config.element.ComponentTagDeclaration;
import org.apache.myfaces.config.element.Converter;
import org.apache.myfaces.config.element.FacesConfigExtension;
import org.apache.myfaces.config.element.FacesFlowDefinition;
import org.apache.myfaces.config.element.Factory;
import org.apache.myfaces.config.element.NamedEvent;
import org.apache.myfaces.config.element.NavigationRule;
import org.apache.myfaces.config.element.Ordering;
import org.apache.myfaces.config.element.RenderKit;
import org.apache.myfaces.config.element.facelets.FaceletTagLibrary;

/**
 * @author <a href="mailto:oliver@rossmueller.com">Oliver Rossmueller</a>
 */
public class FacesConfigImpl extends org.apache.myfaces.config.element.FacesConfig implements Serializable
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(FacesConfigImpl.class.getName());

    private List<Application> applications;
    private List<Factory> factories;
    private List<Component> components;
    private List<ComponentTagDeclaration> componentTagDeclarations;
    private List<Converter> converters;
    private List<NavigationRule> navigationRules;
    private List<RenderKit> renderKits;
    private List<String> lifecyclePhaseListener;
    private Map<String, String> validators;
    private List<Behavior> behaviors;
    private List<NamedEvent> namedEvents;
    private List<FacesConfigExtension> facesConfigExtensions;
    private List <FacesFlowDefinition> facesFlowDefinitions;
    private List <String> protectedViewsUrlPatternList;
    private List <String> resourceResolvers;
    private List<FaceletTagLibrary> faceletTagLibraryList;
    
    private transient List<Application> unmodifiableApplications;
    private transient List<Factory> unmodifiableFactories;
    private transient List<Component> unmodifiableComponents;
    private transient List<ComponentTagDeclaration> unmodifiableComponentTagDeclarations;
    private transient List<Converter> unmodifiableConverters;
    private transient List<NavigationRule> unmodifiableNavigationRules;
    private transient List<RenderKit> unmodifiableRenderKits;
    private transient List<String> unmodifiableLifecyclePhaseListener;
    private transient Map<String, String> unmodifiableValidators;
    private transient List<Behavior> unmodifiableBehaviors;
    private transient List<NamedEvent> unmodifiableNamedEvents;
    private transient List<FacesConfigExtension> unmodifiableFacesConfigExtensions;
    private transient List<FacesFlowDefinition> unmodifiableFacesFlowDefinitions;
    private transient List<String> unmodifiableProtectedViewsUrlPatternList;
    private transient List<String> unmodifiableResourceResolvers;
    private transient List<FaceletTagLibrary> unmodifiableFaceletTagLibraryList;
    
    private String metadataComplete;
    private String version;
    //Ordering variables
    //This information are not merged, and helps
    //with preprocessing of faces-config files
    private String name;
    private AbsoluteOrdering absoluteOrdering;
    private Ordering ordering;

    public void addApplication(Application application)
    {
        if (applications == null)
        {
            applications = new ArrayList<>();
        }
        applications.add(application); 
    }

    public void addFactory(Factory factory)
    {
        if (factories == null)
        {
            factories = new ArrayList<>();
        }
        factories.add(factory);
    }

    public void addComponent(Component component)
    {
        if (components == null)
        {
            components = new ArrayList<>();
        }
        components.add(component);
    }
    
    public void addComponentTagDeclaration(ComponentTagDeclaration tagDeclaration)
    {
        if (componentTagDeclarations == null)
        {
            componentTagDeclarations = new ArrayList<>();
        }
        componentTagDeclarations.add(tagDeclaration);
    }

    public void addConverter(Converter converter)
    {
        if (converters == null)
        {
            converters = new ArrayList<>();
        }
        converters.add(converter);
    }

    public void addNavigationRule(NavigationRule rule)
    {
        if (navigationRules == null)
        {
            navigationRules = new ArrayList<>();
        }
        navigationRules.add(rule);
    }

    public void addRenderKit(RenderKit renderKit)
    {
        if (renderKits == null)
        {
            renderKits = new ArrayList<>();
        }
        renderKits.add(renderKit);
    }

    public void addLifecyclePhaseListener(String value)
    {
        if (lifecyclePhaseListener == null)
        {
            lifecyclePhaseListener = new ArrayList<>();
        }
        lifecyclePhaseListener.add(value);
    }

    public void addValidator(String id, String validatorClass)
    {
        if (validators == null)
        {
            validators = new HashMap<>();
        }

        String oldValidator = validators.get(id);
        // don't log if someone overwrites the built-in converters
        if (oldValidator != null && !oldValidator.startsWith("jakarta.faces.validator."))
        {
            LOG.warning("There is already a validator defined for id: " + id + "."
                    + " old: " + oldValidator
                    + " new: " + validatorClass);
        }

        validators.put(id, validatorClass);
    }
    
    public void addBehavior(Behavior behavior)
    {
        if (behaviors == null)
        {
            behaviors = new ArrayList<>();
        }
        behaviors.add (behavior);
    }
    
    public void addNamedEvent(NamedEvent namedEvent)
    {
        if (namedEvents == null)
        {
            namedEvents = new ArrayList<>();
        }
        namedEvents.add(namedEvent);
    }
    
    public void addFacesConfigExtension(FacesConfigExtension elem)
    {
        if (facesConfigExtensions == null)
        {
            facesConfigExtensions = new ArrayList<>();
        }
        facesConfigExtensions.add(elem);
    }
    
    public void addFacesFlowDefinition(FacesFlowDefinition elem)
    {
        if (facesFlowDefinitions == null)
        {
            facesFlowDefinitions = new ArrayList<>();
        }
        facesFlowDefinitions.add(elem);
    }
    
    public void addProtectedViewUrlPattern(String urlPattern)
    {
        if (protectedViewsUrlPatternList == null)
        {
            protectedViewsUrlPatternList = new ArrayList<>();
        }
        protectedViewsUrlPatternList.add(urlPattern);
    }
    
    public void addResourceResolver(String resourceResolverClass)
    {
        if (resourceResolvers == null)
        {
            resourceResolvers = new ArrayList<>();
        }
        resourceResolvers.add(resourceResolverClass);
    }

    public void addFaceletTagLibrary(FaceletTagLibrary library)
    {
        if (faceletTagLibraryList == null)
        {
            faceletTagLibraryList = new ArrayList<>();
        }
        faceletTagLibraryList.add(library);
    }

    @Override
    public List<Application> getApplications()
    {
        if (applications == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableApplications == null)
        {
            unmodifiableApplications = Collections.unmodifiableList(applications);
        }
        return unmodifiableApplications;
    }

    @Override
    public List<Factory> getFactories()
    {
        if (factories == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableFactories == null)
        {
            unmodifiableFactories =  Collections.unmodifiableList(factories);
        }
        return unmodifiableFactories;
    }

    @Override
    public List<Component> getComponents()
    {
        if (components == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableComponents == null)
        {
            unmodifiableComponents = Collections.unmodifiableList(components);
        }
        return unmodifiableComponents;
    }
    
    @Override
    public List<ComponentTagDeclaration> getComponentTagDeclarations()
    {
        if (componentTagDeclarations == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableComponentTagDeclarations == null)
        {
            unmodifiableComponentTagDeclarations = Collections.unmodifiableList(componentTagDeclarations);
        }
        return unmodifiableComponentTagDeclarations;
    }

    @Override
    public List<Converter> getConverters()
    {
        if (converters == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableConverters == null)
        {
            unmodifiableConverters = Collections.unmodifiableList(converters);
        }
        return unmodifiableConverters;
    }

    @Override
    public List<NavigationRule> getNavigationRules()
    {
        if (navigationRules == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableNavigationRules == null)
        {
            unmodifiableNavigationRules = Collections.unmodifiableList(navigationRules);
        }
        return unmodifiableNavigationRules;
    }

    @Override
    public List<RenderKit> getRenderKits()
    {
        if (renderKits == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableRenderKits == null)
        {
            unmodifiableRenderKits = Collections.unmodifiableList(renderKits);
        }
        return unmodifiableRenderKits;
    }

    @Override
    public List<String> getLifecyclePhaseListener()
    {
        if (lifecyclePhaseListener == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableLifecyclePhaseListener == null)
        {
            unmodifiableLifecyclePhaseListener = Collections.unmodifiableList(lifecyclePhaseListener);
        }
        return unmodifiableLifecyclePhaseListener;
    }

    @Override
    public Map<String, String> getValidators()
    {
        if (validators == null)
        {
            return Collections.emptyMap();
        }
        if (unmodifiableValidators == null)
        {
            unmodifiableValidators = Collections.unmodifiableMap(validators);
        }
        return unmodifiableValidators;
    }
    
    @Override
    public List<Behavior> getBehaviors ()
    {
        if (behaviors == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableBehaviors == null)
        {
            unmodifiableBehaviors = Collections.unmodifiableList(behaviors);
        }
        return unmodifiableBehaviors;
    }
    
    @Override
    public List<NamedEvent> getNamedEvents()
    {
        if (namedEvents == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableNamedEvents == null)
        {
            unmodifiableNamedEvents = Collections.unmodifiableList(namedEvents);
        }
        return unmodifiableNamedEvents;
    }
    
    @Override
    public RenderKit getRenderKit(String renderKitId)
    {
        for (RenderKit rk : getRenderKits())
        {
            if (renderKitId != null && renderKitId.equals(rk.getId()))
            {
                return rk;
            }
            else if (renderKitId == null && rk.getId() == null)
            {
                return rk;
            }
        }
        return null;
    }
    
    @Override
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
    
    @Override
    public org.apache.myfaces.config.element.AbsoluteOrdering getAbsoluteOrdering()
    {
        return absoluteOrdering;
    }

    public void setAbsoluteOrdering(org.apache.myfaces.config.element.AbsoluteOrdering absoluteOrdering)
    {
        this.absoluteOrdering = absoluteOrdering;
    }

    @Override
    public org.apache.myfaces.config.element.Ordering getOrdering()
    {
        return ordering;
    }

    public void setOrdering(org.apache.myfaces.config.element.Ordering ordering)
    {
        this.ordering = ordering;
    }

    @Override
    public String getMetadataComplete()
    {
        return metadataComplete;
    }

    public void setMetadataComplete(String metadataComplete)
    {
        this.metadataComplete = metadataComplete;
    }
    
    @Override
    public String getVersion()
    {
        return version;
    }
    
    public void setVersion(String version)
    {
        this.version = version;
    }

    @Override
    public List<org.apache.myfaces.config.element.FacesConfigExtension> getFacesConfigExtensions()
    {
        if (facesConfigExtensions == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableFacesConfigExtensions == null)
        {
            unmodifiableFacesConfigExtensions = Collections.unmodifiableList(facesConfigExtensions);
        }
        return unmodifiableFacesConfigExtensions;
    }
    
    @Override
    public List<FacesFlowDefinition> getFacesFlowDefinitions()
    {
        if (facesFlowDefinitions == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableFacesFlowDefinitions == null)
        {
            unmodifiableFacesFlowDefinitions = Collections.unmodifiableList(facesFlowDefinitions);
        }
        return unmodifiableFacesFlowDefinitions;
    }
    
    @Override
    public List<String> getProtectedViewsUrlPatternList()
    {
        if (protectedViewsUrlPatternList == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableProtectedViewsUrlPatternList == null)
        {
            unmodifiableProtectedViewsUrlPatternList = Collections.unmodifiableList(protectedViewsUrlPatternList);
        }
        return unmodifiableProtectedViewsUrlPatternList;
    }
    
    @Override
    public List<String> getResourceResolversList()
    {
        if (resourceResolvers == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableResourceResolvers == null)
        {
            unmodifiableResourceResolvers = Collections.unmodifiableList(resourceResolvers);
        }
        return unmodifiableResourceResolvers;
    }
    
    @Override
    public List<FaceletTagLibrary> getFaceletTagLibraryList()
    {
        if (faceletTagLibraryList == null)
        {
            return Collections.emptyList();
        }
        if (unmodifiableFaceletTagLibraryList == null)
        {
            unmodifiableFaceletTagLibraryList = Collections.unmodifiableList(faceletTagLibraryList);
        }
        return unmodifiableFaceletTagLibraryList;
    }
}
