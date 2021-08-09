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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.apache.myfaces.config.element.facelets.FaceletTagLibrary;



/**
 * 
 * @author Leonardo Uribe
 * @since 2.0.3
 */
public abstract class FacesConfigData implements Serializable
{

    /**
     * 
     */
    private static final long serialVersionUID = -5278120443255410184L;

    /** @return Iterator over ApplicationFactory class names */
    public abstract Collection<String> getApplicationFactoryIterator();
    
    /** @return Iterator over ExceptionHandlerFactory class names */
    public abstract Collection<String> getExceptionHandlerFactoryIterator();

    /** @return Iterator over ExternalContextFactory class names */
    public abstract Collection<String> getExternalContextFactoryIterator();

    /** @return Iterator over FacesContextFactory class names */
    public abstract Collection<String> getFacesContextFactoryIterator();

    /** @return Iterator over LifecycleFactory class names */
    public abstract Collection<String> getLifecycleFactoryIterator();

    /** @return Iterator over ViewDeclarationLanguageFactory class names */
    public abstract Collection<String> getViewDeclarationLanguageFactoryIterator();

    /** @return Iterator over PartialViewContextFactory class names */
    public abstract Collection<String> getPartialViewContextFactoryIterator();

    /** @return Iterator over RenderKit factory class names */
    public abstract Collection<String> getRenderKitFactoryIterator();
    
    /** @return Iterator over TagHandlerDelegateFactory factory class names */
    public abstract Collection<String> getTagHandlerDelegateFactoryIterator();

    /** @return Iterator over VisitContextFactory factory class names */
    public abstract Collection<String> getVisitContextFactoryIterator();
    
    /**
     * @since 2.1.0 
     * @return Iterator over FaceletCacheFactory factory class names
     */
    public Collection<String> getFaceletCacheFactoryIterator()
    {
        return Collections.emptyList();
    }
    
    /**
     * @since 2.2
     * @return 
     */
    public Collection<String> getFlashFactoryIterator()
    {
        return Collections.emptyList();
    }
    
    public Collection<String> getClientWindowFactoryIterator()
    {
        return Collections.emptyList();
    }
    
    /**
     * @since 2.2
     * @return 
     */
    public Collection<String> getFlowHandlerFactoryIterator()
    {
        return Collections.emptyList();
    }

    /** @return Iterator over ActionListener class names (in reverse order!) */
    public abstract Collection<String> getActionListenerIterator();

    /** @return the default render kit id */
    public abstract String getDefaultRenderKitId();

    /** @return Iterator over message bundle names (in reverse order!) */
    public abstract String getMessageBundle();

    /** @return Iterator over NavigationHandler class names */
    public abstract Collection<String> getNavigationHandlerIterator();

    /** @return Iterator over ViewHandler class names */
    public abstract Collection<String> getViewHandlerIterator();

    /** @return Iterator over StateManager class names*/
    public abstract Collection<String> getStateManagerIterator();
    
    /** @return Iterator over ResourceHandler class names*/
    public abstract Collection<String> getResourceHandlerIterator();

    /** @return Iterator over PropertyResolver class names */
    public abstract Collection<String> getPropertyResolverIterator();

    /** @return Iterator over VariableResolver class names  */
    public abstract Collection<String> getVariableResolverIterator();

    /** @return the default locale name */
    public abstract String getDefaultLocale();

    /** @return Iterator over supported locale names */
    public abstract Collection<String> getSupportedLocalesIterator();


    /** @return Iterator over all defined component types */
    public abstract Collection<String> getComponentTypes();

    /** @return component class that belongs to the given component type */
    public abstract String getComponentClass(String componentType);

    public abstract Map<String, String> getComponentClassesByType();

    /** @return Iterator over all defined converter ids */
    public abstract Collection<String> getConverterIds();

    /** @return Iterator over all classes with an associated converter  */
    public abstract Collection<String> getConverterClasses();
    
    public abstract Map<String, String> getConverterClassesById();
    
    public abstract Map<String, String> getConverterClassesByClass();

    /** @return Iterator over the config classes for the converters  */
    public abstract Collection<String> getConverterConfigurationByClassName();

    /** delivers a converter-configuration for one class-name */
    public abstract Converter getConverterConfiguration(String converterClassName);

    /** @return converter class that belongs to the given converter id */
    public abstract String getConverterClassById(String converterId);

    /** @return converter class that is associated with the given class name  */
    public abstract String getConverterClassByClass(String className);


    /** @return Iterator over all defined validator ids */
    public abstract Collection<String> getValidatorIds();

    /** @return validator class name that belongs to the given validator id */
    public abstract String getValidatorClass(String validatorId);

    public abstract Map<String, String> getValidatorClassesById();

    /**
     * @return Iterator over {@link org.apache.myfaces.config.element.ManagedBean ManagedBean}s
     */
    public abstract Collection<ManagedBean> getManagedBeans();

    /**
     * @return Iterator over {@link org.apache.myfaces.config.element.NavigationRule NavigationRule}s
     */
    public abstract Collection<NavigationRule> getNavigationRules();



    /** @return Iterator over all defined renderkit ids */
    public abstract Collection<String> getRenderKitIds();

    /** @return renderkit class name for given renderkit id */
    public abstract Collection<String> getRenderKitClasses(String renderKitId);

    /**
     * @return Iterator over {@link org.apache.myfaces.config.element.ClientBehaviorRenderer ClientBehaviorRenderer}s
     *         for the given renderKitId
     */
    public abstract Collection<ClientBehaviorRenderer> getClientBehaviorRenderers (String renderKitId);
    
    /**
     * @return Iterator over {@link org.apache.myfaces.config.element.Renderer Renderer}s for the given renderKitId
     */
    public abstract Collection<Renderer> getRenderers(String renderKitId);


    /**
     * @return Iterator over {@link javax.faces.event.PhaseListener} implementation class names
     */
    public abstract Collection<String> getLifecyclePhaseListeners();

    /**
     * @return Iterator over {@link ResourceBundle}
     */
    public abstract Collection<ResourceBundle> getResourceBundles();

    /**
     * @return Iterator over {@link javax.el.ELResolver} implementation class names
     */
    public abstract Collection<String> getElResolvers();
    
    /**
     * @return Iterator over (@link SystemEventListener) implementation class names 
     */
    public abstract Collection<SystemEventListener> getSystemEventListeners();
    
    /**
     * @return Collection over behaviors
     */
    public abstract Collection<Behavior> getBehaviors ();
    
    /**
     * @return Collection over all defined default validator ids
     */
    public abstract Collection<String> getDefaultValidatorIds ();
    
    /**
     * @return the partial traversal class name
     */
    public abstract String getPartialTraversal ();
    
    /**
     * @return Faces application version.
     */
    public abstract String getFacesVersion ();
    
    /**
     * 
     * @return
     */
    public abstract Collection<NamedEvent> getNamedEvents();

    /**
     * 
     * @since 2.1.0
     */
    public Collection<FaceletsProcessing> getFaceletsProcessing()
    {
        return Collections.emptyList();
    }
    
    public FaceletsProcessing getFaceletsProcessingConfiguration(String fileExtension)
    {
        return null;
    }

    /**
     * @since 2.2.0
     * @return 
     */
    public Collection<FacesFlowDefinition> getFacesFlowDefinitions()
    {
        return Collections.emptyList();
    }
    
    public Collection<String> getProtectedViewUrlPatterns()
    {
        return Collections.emptyList();
    }
    
    public Collection<ContractMapping> getResourceLibraryContractMappings()
    {
        return Collections.emptyList();
    }
    
    public Collection<ComponentTagDeclaration> getComponentTagDeclarations()
    {
        return Collections.emptyList();
    }
    
    public Collection<String> getResourceResolvers()
    {
        return Collections.emptyList();
    }
    
    public Collection<FaceletTagLibrary> getTagLibraries()
    {
        return Collections.emptyList();
    }
    
    public Collection<ViewPoolMapping> getViewPoolMappings()
    {
        return Collections.emptyList();
    }
}
