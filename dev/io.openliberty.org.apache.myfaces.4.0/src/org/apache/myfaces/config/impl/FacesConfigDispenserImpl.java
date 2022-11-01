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
package org.apache.myfaces.config.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.faces.render.RenderKitFactory;

import org.apache.myfaces.config.FacesConfigDispenser;
import org.apache.myfaces.config.element.Behavior;
import org.apache.myfaces.config.element.ClientBehaviorRenderer;
import org.apache.myfaces.config.element.FaceletsProcessing;
import org.apache.myfaces.config.element.FacesConfigExtension;
import org.apache.myfaces.config.element.NavigationRule;
import org.apache.myfaces.config.element.Renderer;
import org.apache.myfaces.config.element.Application;
import org.apache.myfaces.config.element.Component;
import org.apache.myfaces.config.element.ComponentTagDeclaration;
import org.apache.myfaces.config.element.ContractMapping;
import org.apache.myfaces.config.element.Converter;
import org.apache.myfaces.config.element.FaceletsTemplateMapping;
import org.apache.myfaces.config.element.FacesConfig;
import org.apache.myfaces.config.element.FacesFlowDefinition;
import org.apache.myfaces.config.element.Factory;
import org.apache.myfaces.config.element.LocaleConfig;
import org.apache.myfaces.config.element.NamedEvent;
import org.apache.myfaces.config.element.RenderKit;
import org.apache.myfaces.config.element.ResourceBundle;
import org.apache.myfaces.config.element.SystemEventListener;
import org.apache.myfaces.config.element.ViewPoolMapping;
import org.apache.myfaces.config.element.facelets.FaceletTagLibrary;
import org.apache.myfaces.config.impl.element.RenderKitImpl;

/**
 * @author <a href="mailto:oliver@rossmueller.com">Oliver Rossmueller</a>
 */
public class FacesConfigDispenserImpl extends FacesConfigDispenser
{
    private static final long serialVersionUID = 3550379003287939559L;
    
    private static final Logger log = Logger.getLogger(FacesConfigDispenserImpl.class.getName());
    
    // Factories
    private List<String> applicationFactories = new ArrayList<>();
    private List<String> exceptionHandlerFactories = new ArrayList<>();
    private List<String> externalContextFactories = new ArrayList<>();
    private List<String> facesContextFactories = new ArrayList<>();
    private List<String> lifecycleFactories = new ArrayList<>();
    private List<String> viewDeclarationLanguageFactories = new ArrayList<>();
    private List<String> partialViewContextFactories = new ArrayList<>();
    private List<String> renderKitFactories = new ArrayList<>();
    private List<String> tagHandlerDelegateFactories = new ArrayList<>();
    private List<String> visitContextFactories = new ArrayList<>();
    private List<String> faceletCacheFactories = new ArrayList<>();
    private List<String> flashFactories = new ArrayList<>();
    private List<String> clientWindowFactories = new ArrayList<>();
    private List<String> flowHandlerFactories = new ArrayList<>();
    private List<String> searchExpressionContextFactories = new ArrayList<>();
    
    private String defaultRenderKitId;
    private String messageBundle;
    private String facesVersion;
    
    private LocaleConfig localeConfig;

    private Map<String, Component> components = new HashMap<>();
    private Map<String, String> converterByClass = new HashMap<>();
    private Map<String, String> converterById = new HashMap<>();
    private Map<String, String> validators = new HashMap<>();
    private List<Behavior> behaviors = new ArrayList<>();
    
    private Map<String, Converter> converterConfigurationByClassName = new HashMap<>();
    
    private Map<String, RenderKit> renderKits = new LinkedHashMap<>();
    
    private List<String> actionListeners = new ArrayList<>();
    private List<String> elResolvers = new ArrayList<>();
    private List<String> lifecyclePhaseListeners = new ArrayList<>();
    private List<String> navigationHandlers = new ArrayList<>();
    private List<String> propertyResolver = new ArrayList<>();
    private List<String> resourceHandlers = new ArrayList<>();
    private List<String> stateManagers = new ArrayList<>();
    private List<String> variableResolver = new ArrayList<>();
    private List<String> viewHandlers = new ArrayList<>();
    private List<String> defaultValidatorIds = new ArrayList<>();
    private List<String> defaultAnnotatedValidatorIds = new ArrayList<>();
    private List<String> searchExpressionHandlers = new ArrayList<>();
    private List<String> searchKeywordResolvers = new ArrayList<>();
    
    private List<NavigationRule> navigationRules = new ArrayList<>();
    private List<ResourceBundle> resourceBundles = new ArrayList<>();

    private List<SystemEventListener> systemEventListeners = new ArrayList<>();
    
    private List<NamedEvent> namedEvents = new ArrayList<>();
    
    private Map<String, FaceletsProcessing> faceletsProcessingByFileExtension
            = new HashMap<>();
    
    private List<FacesFlowDefinition> facesFlowDefinitions = new ArrayList<>();
    
    private List<String> protectedViewUrlPatterns = new ArrayList<>();
    private List<ContractMapping> resourceLibraryContractMappings = new ArrayList<>();
    
    private List<ComponentTagDeclaration> componentTagDeclarations = new ArrayList<>();
    private List<FaceletTagLibrary> faceletTagLibraries = new ArrayList<>();
    
    private List <String> resourceResolvers = new ArrayList<>();
    
    private List<ViewPoolMapping> viewPoolMappings = new ArrayList<>();
    private List<FaceletsTemplateMapping> faceletsTemplateMappings = new ArrayList<>();

    // Unmodifiable list/maps to avoid modifications
    private transient List<String> umapplicationFactories;
    private transient List<String> umexceptionHandlerFactories;
    private transient List<String> umexternalContextFactories;
    private transient List<String> umfacesContextFactories;
    private transient List<String> umlifecycleFactories;
    private transient List<String> umviewDeclarationLanguageFactories;
    private transient List<String> umpartialViewContextFactories;
    private transient List<String> umrenderKitFactories;
    private transient List<String> umtagHandlerDelegateFactories;
    private transient List<String> umvisitContextFactories;
    private transient List<String> umfaceletCacheFactories;
    private transient List<String> umflashFactories;
    private transient List<String> umclientWindowFactories;
    private transient List<String> umflowHandlerFactories;
    private transient List<String> umsearchExpressionContextFactories;
    private transient List<Behavior> umbehaviors;
    private transient List<String> umactionListeners;
    private transient List<String> umelResolvers;
    private transient List<String> umlifecyclePhaseListeners;
    private transient List<String> umnavigationHandlers;
    private transient List<String> umpropertyResolver;
    private transient List<String> umresourceHandlers;
    private transient List<String> umstateManagers;
    private transient List<String> umvariableResolver;
    private transient List<String> umviewHandlers;
    private transient List<String> umsearchExpressionHandlers;
    private transient List<String> umsearchKeywordResolvers;
    private transient List<NavigationRule> umnavigationRules;
    private transient List<ResourceBundle> umresourceBundles;
    private transient List<SystemEventListener> umsystemEventListeners;
    private transient List<NamedEvent> umnamedEvents;
    private transient List<FacesFlowDefinition> umfacesFlowDefinitions;
    private transient List<String> umprotectedViewUrlPatterns;
    private transient List<ContractMapping> umresourceLibraryContractMappings;
    private transient List<ComponentTagDeclaration> umcomponentTagDeclarations;
    private transient List<FaceletTagLibrary> umfaceletTagLibraries;
    private transient List <String> umresourceResolvers;
    private transient List<ViewPoolMapping> umviewPoolMappings;
    private transient List<FaceletsTemplateMapping> umfaceletsTemplateMappings;
    
    /**
     * Add another unmarshalled faces config object.
     * 
     * @param config
     *            unmarshalled faces config object
     */
    @Override
    public void feed(FacesConfig config)
    {
        for (Factory factory : config.getFactories())
        {
            applicationFactories.addAll(factory.getApplicationFactory());
            exceptionHandlerFactories.addAll(factory.getExceptionHandlerFactory());
            externalContextFactories.addAll(factory.getExternalContextFactory());
            facesContextFactories.addAll(factory.getFacesContextFactory());
            lifecycleFactories.addAll(factory.getLifecycleFactory());
            viewDeclarationLanguageFactories.addAll(factory.getViewDeclarationLanguageFactory());
            partialViewContextFactories.addAll(factory.getPartialViewContextFactory());
            renderKitFactories.addAll(factory.getRenderkitFactory());
            tagHandlerDelegateFactories.addAll(factory.getTagHandlerDelegateFactory());
            visitContextFactories.addAll(factory.getVisitContextFactory());
            faceletCacheFactories.addAll(factory.getFaceletCacheFactory());
            flashFactories.addAll(factory.getFlashFactory());
            clientWindowFactories.addAll(factory.getClientWindowFactory());
            flowHandlerFactories.addAll(factory.getFlowHandlerFactory());
            searchExpressionContextFactories.addAll(factory.getSearchExpressionContextFactory());
        }

        for (Component component : config.getComponents())
        {
            components.put(component.getComponentType(), component);
        }
        validators.putAll(config.getValidators());
        behaviors.addAll(config.getBehaviors());
        
        for (Application application : config.getApplications())
        {
            if (!application.getDefaultRenderkitId().isEmpty())
            {
                defaultRenderKitId =
                        application.getDefaultRenderkitId().get(application.getDefaultRenderkitId().size() - 1);
            }

            if (!application.getMessageBundle().isEmpty())
            {
                messageBundle = application.getMessageBundle().get(application.getMessageBundle().size() - 1);
            }

            if (!application.getLocaleConfig().isEmpty())
            {
                localeConfig = application.getLocaleConfig().get(application.getLocaleConfig().size() - 1);
            }
            
            actionListeners.addAll(application.getActionListener());
            navigationHandlers.addAll(application.getNavigationHandler());
            resourceHandlers.addAll(application.getResourceHandler());
            viewHandlers.addAll(application.getViewHandler());
            stateManagers.addAll(application.getStateManager());
            propertyResolver.addAll(application.getPropertyResolver());
            variableResolver.addAll(application.getVariableResolver());
            resourceBundles.addAll(application.getResourceBundle());
            elResolvers.addAll(application.getElResolver());
            resourceLibraryContractMappings.addAll(application.getResourceLibraryContractMappings());
            searchExpressionHandlers.addAll(application.getSearchExpressionHandler());
            searchKeywordResolvers.addAll(application.getSearchKeywordResolver());

            // Jsf 2.0 spec section 3.5.3 says this: 
            // ".... Any configuration resource that declares a list of default 
            // validators overrides any list provided in a previously processed
            // configuration resource. If an empty <default-validators/> element 
            // is found in a configuration resource, the list
            // of default validators must be cleared....."
            if (application.isDefaultValidatorsPresent())
            {
                // we have a <default-validators> element, so any existing
                // default validators should be removed
                defaultValidatorIds.clear();
                
                // now add all default-validator entries (could be zero)
                defaultValidatorIds.addAll(application.getDefaultValidatorIds());
            }
            else
            {
                //If isDefaultValidatorsPresent() is false, and there are still 
                //default validators, it means they were added using annotations, so
                //they are not affected by the empty entry according to section 3.5.3
                defaultAnnotatedValidatorIds.addAll(application.getDefaultValidatorIds());
            }
            
            systemEventListeners.addAll(application.getSystemEventListeners());
        }

        for (Converter converter : config.getConverters())
        {
            if (converter.getConverterId() != null)
            {
                converterById.put(converter.getConverterId(), converter.getConverterClass());
            }
            if (converter.getForClass() != null)
            {
                String oldConverter = converterByClass.get(converter.getForClass());
                // don't log if someone overwrites the built-in converters
                if (oldConverter != null && !oldConverter.startsWith("jakarta.faces.convert."))
                {
                    log.warning("There is already a converter defined for class: " + converter.getForClass() + "."
                            + " old: " + oldConverter
                            + " new: " + converter.getConverterClass());
                }

                converterByClass.put(converter.getForClass(), converter.getConverterClass());
            }

            converterConfigurationByClassName.put(converter.getConverterClass(), converter);
        }

        for (RenderKit renderKit : config.getRenderKits())
        {
            String renderKitId = renderKit.getId();

            if (renderKitId == null)
            {
                renderKitId = RenderKitFactory.HTML_BASIC_RENDER_KIT;
            }

            RenderKit existing = renderKits.get(renderKitId);

            if (existing == null)
            {
                existing = new RenderKitImpl();
                existing.merge(renderKit);
                renderKits.put(renderKitId, existing);
            }
            else
            {
                existing.merge(renderKit);
            }
        }

        for (FacesConfigExtension extension : config.getFacesConfigExtensions())
        {
            for (FaceletsProcessing faceletsProcessing : extension.getFaceletsProcessingList())
            {
                if (faceletsProcessing.getFileExtension() != null && faceletsProcessing.getFileExtension().length() > 0)
                {
                    faceletsProcessingByFileExtension.put(faceletsProcessing.getFileExtension(), faceletsProcessing);
                }
            }
        }

        componentTagDeclarations.addAll(config.getComponentTagDeclarations().values());

        faceletTagLibraries.addAll(config.getFaceletTagLibraryList());

        lifecyclePhaseListeners.addAll(config.getLifecyclePhaseListener());
        navigationRules.addAll(config.getNavigationRules());
        facesVersion = config.getVersion();
        namedEvents.addAll(config.getNamedEvents());
        facesFlowDefinitions.addAll(config.getFacesFlowDefinitions());
        protectedViewUrlPatterns.addAll(config.getProtectedViewsUrlPatternList());
        resourceResolvers.addAll(config.getResourceResolversList());
        for (FacesConfigExtension extension : config.getFacesConfigExtensions())
        {
            viewPoolMappings.addAll(extension.getViewPoolMappings());
        }
        for (FacesConfigExtension extension : config.getFacesConfigExtensions())
        {
            faceletsTemplateMappings.addAll(extension.getFaceletsTemplateMappings());
        }
    }

    /**
     * Add another ApplicationFactory class name
     * 
     * @param factoryClassName a class name
     */
    @Override
    public void feedApplicationFactory(String factoryClassName)
    {
        applicationFactories.add(factoryClassName);
    }

    @Override
    public void feedExceptionHandlerFactory(String factoryClassName)
    {
        exceptionHandlerFactories.add(factoryClassName);
    }

    @Override
    public void feedExternalContextFactory(String factoryClassName)
    {
        externalContextFactories.add(factoryClassName);
    }

    /**
     * Add another FacesContextFactory class name
     * 
     * @param factoryClassName a class name
     */
    @Override
    public void feedFacesContextFactory(String factoryClassName)
    {
        facesContextFactories.add(factoryClassName);
    }

    /**
     * Add another LifecycleFactory class name
     * 
     * @param factoryClassName a class name
     */
    @Override
    public void feedLifecycleFactory(String factoryClassName)
    {
        lifecycleFactories.add(factoryClassName);
    }

    @Override
    public void feedViewDeclarationLanguageFactory(String factoryClassName)
    {
        viewDeclarationLanguageFactories.add(factoryClassName);
    }

    @Override
    public void feedPartialViewContextFactory(String factoryClassName)
    {
        partialViewContextFactories.add(factoryClassName);
    }

    /**
     * Add another RenderKitFactory class name
     * 
     * @param factoryClassName a class name
     */
    @Override
    public void feedRenderKitFactory(String factoryClassName)
    {
        renderKitFactories.add(factoryClassName);
    }

    @Override
    public void feedTagHandlerDelegateFactory(String factoryClassName)
    {
        tagHandlerDelegateFactories.add(factoryClassName);
    }

    @Override
    public void feedVisitContextFactory(String factoryClassName)
    {
        visitContextFactories.add(factoryClassName);
    }

    /**
     * @return Collection over ApplicationFactory class names
     */
    @Override
    public Collection<String> getApplicationFactoryIterator()
    {
        if (umapplicationFactories == null)
        {
            umapplicationFactories = Collections.unmodifiableList(applicationFactories);
        }
        return umapplicationFactories;
    }

    @Override
    public Collection<String> getExceptionHandlerFactoryIterator()
    {
        if (umexceptionHandlerFactories == null)
        {
            umexceptionHandlerFactories = Collections.unmodifiableList(exceptionHandlerFactories);
        }
        return umexceptionHandlerFactories;
    }

    @Override
    public Collection<String> getExternalContextFactoryIterator()
    {
        if (umexternalContextFactories == null)
        {
            umexternalContextFactories = Collections.unmodifiableList(externalContextFactories);
        }
        return umexternalContextFactories;
    }

    /**
     * @return Collection over FacesContextFactory class names
     */
    @Override
    public Collection<String> getFacesContextFactoryIterator()
    {
        if (umfacesContextFactories == null)
        {
            umfacesContextFactories = Collections.unmodifiableList(facesContextFactories);
        }
        return umfacesContextFactories;
    }

    /**
     * @return Collection over LifecycleFactory class names
     */
    @Override
    public Collection<String> getLifecycleFactoryIterator()
    {
        if (umlifecycleFactories == null)
        {
            umlifecycleFactories = Collections.unmodifiableList(lifecycleFactories);
        }
        return umlifecycleFactories;
    }

    @Override
    public Collection<String> getViewDeclarationLanguageFactoryIterator()
    {
        if (umviewDeclarationLanguageFactories == null)
        {
            umviewDeclarationLanguageFactories = Collections.unmodifiableList(viewDeclarationLanguageFactories);
        }
        return umviewDeclarationLanguageFactories;
    }

    @Override
    public Collection<String> getPartialViewContextFactoryIterator()
    {
        if (umpartialViewContextFactories == null)
        {
            umpartialViewContextFactories = Collections.unmodifiableList(partialViewContextFactories);
        }
        return umpartialViewContextFactories;
    }

    /**
     * @return Collection over RenderKit factory class names
     */
    @Override
    public Collection<String> getRenderKitFactoryIterator()
    {
        if (umrenderKitFactories == null)
        {
            umrenderKitFactories = Collections.unmodifiableList(renderKitFactories);
        }
        return umrenderKitFactories;
    }

    @Override
    public Collection<String> getTagHandlerDelegateFactoryIterator()
    {
        if (umtagHandlerDelegateFactories == null)
        {
            umtagHandlerDelegateFactories = Collections.unmodifiableList(tagHandlerDelegateFactories);
        }
        return umtagHandlerDelegateFactories;
    }

    @Override
    public Collection<String> getVisitContextFactoryIterator()
    {
        if (umvisitContextFactories == null)
        {
            umvisitContextFactories = Collections.unmodifiableList(visitContextFactories);
        }
        return umvisitContextFactories;
    }

    /**
     * @return Collection over ActionListener class names
     */
    @Override
    public Collection<String> getActionListenerIterator()
    {
        if (umactionListeners == null)
        {
            umactionListeners = Collections.unmodifiableList(actionListeners);
        }
        return umactionListeners;
    }

    /**
     * @return the default render kit id
     */
    @Override
    public String getDefaultRenderKitId()
    {
        return defaultRenderKitId;
    }

    /**
     * @return Collection over message bundle names
     */
    @Override
    public String getMessageBundle()
    {
        return messageBundle;
    }

    /**
     * @return Collection over NavigationHandler class names
     */
    @Override
    public Collection<String> getNavigationHandlerIterator()
    {
        if (umnavigationHandlers == null)
        {
            umnavigationHandlers = Collections.unmodifiableList(navigationHandlers);
        }
        return umnavigationHandlers;
    }
    
    /**
     * @return Collection over ResourceHandler class names
     */
    @Override
    public Collection<String> getResourceHandlerIterator()
    {
        if (umresourceHandlers == null)
        {
            umresourceHandlers = Collections.unmodifiableList(resourceHandlers);
        }
        return umresourceHandlers;
    }

    /**
     * @return Collection over ViewHandler class names
     */
    @Override
    public Collection<String> getViewHandlerIterator()
    {
        if (umviewHandlers == null)
        {
            umviewHandlers = Collections.unmodifiableList(viewHandlers);
        }
        return umviewHandlers;
    }

    /**
     * @return Collection over StateManager class names
     */
    @Override
    public Collection<String> getStateManagerIterator()
    {
        if (umstateManagers == null)
        {
            umstateManagers = Collections.unmodifiableList(stateManagers);
        }
        return umstateManagers;
    }

    /**
     * @return Collection over PropertyResolver class names
     */
    @Override
    public Collection<String> getPropertyResolverIterator()
    {
        if (umpropertyResolver == null)
        {
            umpropertyResolver = Collections.unmodifiableList(propertyResolver);
        }
        return umpropertyResolver;
    }

    /**
     * @return Collection over VariableResolver class names
     */
    @Override
    public Collection<String> getVariableResolverIterator()
    {
        if (umvariableResolver == null)
        {
            umvariableResolver = Collections.unmodifiableList(variableResolver);
        }
        return umvariableResolver;
    }

    /**
     * @return the default locale name
     */
    @Override
    public String getDefaultLocale()
    {
        if (localeConfig != null)
        {
            return localeConfig.getDefaultLocale();
        }
        return null;
    }

    /**
     * @return Collection over supported locale names
     */
    @Override
    public Collection<String> getSupportedLocalesIterator()
    {
        Collection<String> locale;
        if (localeConfig != null)
        {
            locale = Collections.unmodifiableCollection(localeConfig.getSupportedLocales());
        }
        else
        {
            locale = Collections.emptyList();
        }

        return locale;
    }

    /**
     * @return Collection over all defined component types
     */
    @Override
    public Collection<String> getComponentTypes()
    {
        return Collections.unmodifiableCollection(components.keySet());
    }

    @Override
    public Map<String, Component> getComponentsByType()
    {
        return Collections.unmodifiableMap(components);
    }

    /**
     * @return component class that belongs to the given component type
     */
    @Override
    public String getComponentClass(String componentType)
    {
        Component component = components.get(componentType);
        return component == null ? null : component.getComponentClass();
    }

    /**
     * @return Collection over all defined converter ids
     */
    @Override
    public Collection<String> getConverterIds()
    {
        return Collections.unmodifiableCollection(converterById.keySet());
    }

    @Override
    public Map<String, String> getConverterClassesById()
    {
        return Collections.unmodifiableMap(converterById);
    }

    /**
     * @return Collection over all classes with an associated converter
     */
    @Override
    public Collection<String> getConverterClasses()
    {
        return Collections.unmodifiableCollection(converterByClass.keySet());
    }

    @Override
    public Map<String, String> getConverterClassesByClass()
    {
        return Collections.unmodifiableMap(converterByClass);
    }

    @Override
    public Collection<String> getConverterConfigurationByClassName()
    {
        return Collections.unmodifiableCollection(converterConfigurationByClassName.keySet());
    }

    @Override
    public Converter getConverterConfiguration(String converterClassName)
    {
        return converterConfigurationByClassName.get(converterClassName);
    }

    /**
     * @return converter class that belongs to the given converter id
     */
    @Override
    public String getConverterClassById(String converterId)
    {
        return converterById.get(converterId);
    }

    /**
     * @return converter class that is associated with the given class name
     */
    @Override
    public String getConverterClassByClass(String className)
    {
        return converterByClass.get(className);
    }

    /**
     * @return Collection over all defined default validator ids
     */
    @Override
    public Collection<String> getDefaultValidatorIds ()
    {
        List<String> allDefaultValidatorIds = new ArrayList<>();
        allDefaultValidatorIds.addAll(defaultAnnotatedValidatorIds);
        allDefaultValidatorIds.addAll(defaultValidatorIds);
        return Collections.unmodifiableCollection(allDefaultValidatorIds);
    }
    
    /**
     * @return Collection over all defined validator ids
     */
    @Override
    public Collection<String> getValidatorIds()
    {
        return Collections.unmodifiableCollection(validators.keySet());
    }

    @Override
    public Map<String, String> getValidatorClassesById()
    {
        return Collections.unmodifiableMap(validators);
    }

    /**
     * @return validator class name that belongs to the given validator id
     */
    @Override
    public String getValidatorClass(String validatorId)
    {
        return validators.get(validatorId);
    }

    /**
     * @return Collection over {@link org.apache.myfaces.config.element.NavigationRule NavigationRule}s
     */
    @Override
    public Collection<NavigationRule> getNavigationRules()
    {
        if (umnavigationRules == null)
        {
            umnavigationRules = Collections.unmodifiableList(navigationRules);
        }
        return umnavigationRules;
    }

    /**
     * @return Collection over all defined renderkit ids
     */
    @Override
    public Collection<String> getRenderKitIds()
    {
        return Collections.unmodifiableCollection(renderKits.keySet());
    }

    /**
     * @return renderkit class name for given renderkit id
     */
    @Override
    public Collection<String> getRenderKitClasses(String renderKitId)
    {
        return renderKits.get(renderKitId).getRenderKitClasses();
    }

    /**
     * @return Iterator over
     * {@link org.apache.myfaces.config.element.ClientBehaviorRenderer ClientBehaviorRenderer}s
     * for the given renderKitId
     */
    @Override
    public Collection<ClientBehaviorRenderer> getClientBehaviorRenderers (String renderKitId)
    {
        return renderKits.get (renderKitId).getClientBehaviorRenderers();
    }
    
    /**
     * @return Collection over {@link org.apache.myfaces.config.element.Renderer Renderer}s for the given renderKitId
     */
    @Override
    public Collection<Renderer> getRenderers(String renderKitId)
    {
        return renderKits.get(renderKitId).getRenderer();
    }

    /**
     * @return Collection over {@link jakarta.faces.event.PhaseListener} implementation class names
     */
    @Override
    public Collection<String> getLifecyclePhaseListeners()
    {
        if (umlifecyclePhaseListeners == null)
        {
            umlifecyclePhaseListeners = Collections.unmodifiableList(lifecyclePhaseListeners);
        }
        return umlifecyclePhaseListeners;
    }

    @Override
    public Collection<ResourceBundle> getResourceBundles()
    {
        if (umresourceBundles == null)
        {
            umresourceBundles = Collections.unmodifiableList(resourceBundles);
        }
        return umresourceBundles;
    }

    @Override
    public Collection<String> getElResolvers()
    {
        if (umelResolvers == null)
        {
            umelResolvers = Collections.unmodifiableList(elResolvers);
        }
        return umelResolvers;
    }

    @Override
    public Collection<SystemEventListener> getSystemEventListeners()
    {        
        if (umsystemEventListeners == null)
        {
            umsystemEventListeners = Collections.unmodifiableList(systemEventListeners);
        }
        return umsystemEventListeners;
    }
    
    @Override
    public Collection<Behavior> getBehaviors()
    {
        if (umbehaviors == null)
        {
            umbehaviors = Collections.unmodifiableList(behaviors);
        }
        return umbehaviors;
    }
    
    @Override
    public String getFacesVersion()
    {
        return facesVersion;
    }
    
    @Override
    public Collection<NamedEvent> getNamedEvents()
    {
        if (umnamedEvents == null)
        {
            umnamedEvents = Collections.unmodifiableList(namedEvents);
        }
        return umnamedEvents;
    }
    
    @Override
    public Collection<FaceletsProcessing> getFaceletsProcessing()
    {
        return Collections.unmodifiableCollection(faceletsProcessingByFileExtension.values());
    }

    @Override
    public FaceletsProcessing getFaceletsProcessingConfiguration(String fileExtension)
    {
        return faceletsProcessingByFileExtension.get(fileExtension);
    }

    @Override
    public void feedFaceletCacheFactory(String factoryClassName)
    {
        faceletCacheFactories.add(factoryClassName);
    }

    @Override
    public Collection<String> getFaceletCacheFactoryIterator()
    {
        if (umfaceletCacheFactories == null)
        {
            umfaceletCacheFactories = Collections.unmodifiableList(faceletCacheFactories);
        }
        return umfaceletCacheFactories;
    }

    @Override
    public void feedFlashFactory(String factoryClassName)
    {
        flashFactories.add(factoryClassName);
    }

    @Override
    public Collection<String> getFlashFactoryIterator()
    {
        if (umflashFactories == null)
        {
            umflashFactories = Collections.unmodifiableList(flashFactories);
        }
        return umflashFactories;
    }
    
    @Override
    public Collection<String> getFlowHandlerFactoryIterator()
    {
        if (umflowHandlerFactories == null)
        {
            umflowHandlerFactories = Collections.unmodifiableList(flowHandlerFactories);
        }
        return umflowHandlerFactories;
    }

    @Override
    public void feedClientWindowFactory(String factoryClassName)
    {
        clientWindowFactories.add(factoryClassName);
    }

    @Override
    public Collection<String> getClientWindowFactoryIterator()
    {
        if (umclientWindowFactories == null)
        {
            umclientWindowFactories = Collections.unmodifiableList(clientWindowFactories);
        }
        return umclientWindowFactories;
    }
    
    @Override
    public Collection<FacesFlowDefinition> getFacesFlowDefinitions()
    {
        if (umfacesFlowDefinitions == null)
        {
            umfacesFlowDefinitions = Collections.unmodifiableList(facesFlowDefinitions);
        }
        return umfacesFlowDefinitions;
    }

    @Override
    public Collection<String> getProtectedViewUrlPatterns()
    {
        if (umprotectedViewUrlPatterns == null)
        {
            umprotectedViewUrlPatterns = Collections.unmodifiableList(protectedViewUrlPatterns);
        }
        return umprotectedViewUrlPatterns;
    }

    @Override
    public Collection<ContractMapping> getResourceLibraryContractMappings()
    {
        if (umresourceLibraryContractMappings == null)
        {
            umresourceLibraryContractMappings = Collections.unmodifiableList(resourceLibraryContractMappings);
        }
        return umresourceLibraryContractMappings;
    }

    @Override
    public Collection<ComponentTagDeclaration> getComponentTagDeclarations()
    {
        if (umcomponentTagDeclarations == null)
        {
            umcomponentTagDeclarations = Collections.unmodifiableList(componentTagDeclarations);
        }
        return umcomponentTagDeclarations;
    }

    @Override
    public Collection<String> getResourceResolvers()
    {
        if (umresourceResolvers == null)
        {
            umresourceResolvers = Collections.unmodifiableList(resourceResolvers);
        }
        return umresourceResolvers;
    }

    @Override
    public Collection<FaceletTagLibrary> getTagLibraries()
    {
        if (umfaceletTagLibraries == null)
        {
            umfaceletTagLibraries = Collections.unmodifiableList(faceletTagLibraries);
        }
        return umfaceletTagLibraries;
    }
    
    @Override
    public Collection<ViewPoolMapping> getViewPoolMappings()
    {
        if (umviewPoolMappings == null)
        {
            umviewPoolMappings = Collections.unmodifiableList(viewPoolMappings);
        }
        return umviewPoolMappings;
    }
    
    @Override
    public void feedSearchExpressionContextFactory(String factoryClassName)
    {
        searchExpressionContextFactories.add(factoryClassName);
    }

    @Override
    public Collection<String> getSearchExpressionContextFactoryIterator()
    {
        if (umsearchExpressionContextFactories == null)
        {
            umsearchExpressionContextFactories = Collections.unmodifiableList(searchExpressionContextFactories);
        }
        return umsearchExpressionContextFactories;
    }
    
    @Override
    public Collection<String> getSearchKeywordResolvers()
    {
        if (umsearchKeywordResolvers == null)
        {
            umsearchKeywordResolvers = Collections.unmodifiableList(searchKeywordResolvers);
        }
        return umsearchKeywordResolvers;
    }    
    
    @Override
    public Collection<String> getSearchExpressionHandlerIterator()
    {
        if (umsearchExpressionHandlers == null)
        {
            umsearchExpressionHandlers = Collections.unmodifiableList(searchExpressionHandlers);
        }
        return umsearchExpressionHandlers;
    }

    @Override
    public Collection<FaceletsTemplateMapping> getFaceletsTemplateMappings()
    {
        if (umfaceletsTemplateMappings == null)
        {
            umfaceletsTemplateMappings = Collections.unmodifiableList(faceletsTemplateMappings);
        }
        return umfaceletsTemplateMappings;
    }
}
