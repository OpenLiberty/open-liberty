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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.faces.component.search.SearchKeywordResolver;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.el.PropertyResolver;
import jakarta.faces.el.VariableResolver;

import org.apache.myfaces.config.element.ComponentTagDeclaration;
import org.apache.myfaces.config.element.FaceletsProcessing;
import org.apache.myfaces.config.element.FaceletsTemplateMapping;
import org.apache.myfaces.config.element.ManagedBean;
import org.apache.myfaces.config.element.NavigationRule;
import org.apache.myfaces.config.element.ResourceBundle;
import org.apache.myfaces.config.element.ViewPoolMapping;
import org.apache.myfaces.config.element.facelets.FaceletTagLibrary;

/**
 * Holds all configuration information (from the faces-config xml files) that is needed later during runtime. The config
 * information in this class is only available to the MyFaces core implementation classes (i.e. the myfaces source
 * tree). See MyfacesConfig for config parameters that can be used for shared or component classes.
 * 
 * @author Manfred Geiler (latest modification by $Author$)
 * @version $Revision$ $Date$
 */
@SuppressWarnings("deprecation")
public class RuntimeConfig
{
    private static final Logger log = Logger.getLogger(RuntimeConfig.class.getName());

    private static final String APPLICATION_MAP_PARAM_NAME = RuntimeConfig.class.getName();

    private final Collection<NavigationRule> _navigationRules = new ArrayList<NavigationRule>();
    private final Map<String, ManagedBean> _managedBeans = new HashMap<String, ManagedBean>();
    private boolean _navigationRulesChanged = false;
    private final Map<String, ResourceBundle> _resourceBundles = new HashMap<String, ResourceBundle>();
    private final Map<String, ManagedBean> _oldManagedBeans = new HashMap<String, ManagedBean>();
    
    private String _facesVersion;
    
    private List<ELResolver> facesConfigElResolvers;
    private List<ELResolver> applicationElResolvers;

    private VariableResolver _variableResolver;
    private PropertyResolver _propertyResolver;

    private ExpressionFactory _expressionFactory;

    private PropertyResolver _propertyResolverChainHead;

    private VariableResolver _variableResolverChainHead;
    
    private Comparator<ELResolver> _elResolverComparator;
    
    private Predicate<ELResolver> _elResolverPredicate;

    private final Map<String, org.apache.myfaces.config.element.Converter> _converterClassNameToConfigurationMap =
        new ConcurrentHashMap<String, org.apache.myfaces.config.element.Converter>();
    
    private NamedEventManager _namedEventManager;
    
    private final Map<String, FaceletsProcessing> _faceletsProcessingByFileExtension =
        new HashMap<String, FaceletsProcessing>();
    
    /**
     * JSF 2.2 section 11.4.2.1. 
     * 
     * Scanning for all available contracts is necessary because the spec says 
     * "... if the information from the application configuration resources refers 
     * to a contract that is not available to the application, an informative error 
     * message must be logged. ..."
     */
    private Set<String> _externalContextResourceLibraryContracts = new HashSet<String>();
    private Set<String> _classLoaderResourceLibraryContracts = new HashSet<String>();
    private Set<String> _resourceLibraryContracts = new HashSet<String>();
    
    private Map<String, List<String>> _contractMappings = new HashMap<String, List<String>>();
    
    private List<ComponentTagDeclaration> _componentTagDeclarations = 
            new ArrayList<ComponentTagDeclaration>();
    
    private List<String> _resourceResolvers = new ArrayList<String>();
    
    private List<Object> _injectedObjects = new ArrayList<Object>();
    
    private List<FaceletTagLibrary> _faceletTagLibraries = new ArrayList<FaceletTagLibrary>();
    
    private Map<Integer, String> _namespaceById = new HashMap<Integer, String>();
    private Map<String, Integer> _idByNamespace = new HashMap<String, Integer>();
    
    private List<ViewPoolMapping> _viewPoolMappings = new ArrayList<ViewPoolMapping>();
    
    private List<FaceletsTemplateMapping> _faceletsTemplateMappings = new ArrayList<FaceletsTemplateMapping>();
    
    private List<SearchKeywordResolver> _searchExpressionResolvers = new ArrayList<SearchKeywordResolver>();
    
    private List<String> _faceletTemplates = new ArrayList<String>();

    public static RuntimeConfig getCurrentInstance(ExternalContext externalContext)
    {
        RuntimeConfig runtimeConfig = (RuntimeConfig) externalContext.getApplicationMap().get(
                APPLICATION_MAP_PARAM_NAME);
        if (runtimeConfig == null)
        {
            runtimeConfig = new RuntimeConfig();
            externalContext.getApplicationMap().put(APPLICATION_MAP_PARAM_NAME, runtimeConfig);
        }
        return runtimeConfig;
    }

    public void purge()
    {
        _navigationRules.clear();
        _oldManagedBeans.clear();
        _oldManagedBeans.putAll(_managedBeans);
        _managedBeans.clear();
        _navigationRulesChanged = false;
        _converterClassNameToConfigurationMap.clear();
        _externalContextResourceLibraryContracts.clear();
        _classLoaderResourceLibraryContracts.clear();
        _resourceLibraryContracts.clear();
        _injectedObjects.clear();
        _faceletTagLibraries.clear();
        _faceletTemplates.clear();
        
        _resourceBundles.clear();
        if (facesConfigElResolvers != null)
        {
            facesConfigElResolvers.clear();
        }
        if (applicationElResolvers != null)
        {
            applicationElResolvers.clear();
        }
        _faceletsProcessingByFileExtension.clear();
        _contractMappings.clear();
        _componentTagDeclarations.clear();
        _resourceResolvers.clear();
        _namespaceById = new HashMap<Integer, String>();
        _idByNamespace = new HashMap<String, Integer>();
        _viewPoolMappings.clear();
        _faceletsTemplateMappings.clear();
    }

    /**
     * Return the navigation rules that can be used by the NavigationHandler implementation.
     * 
     * @return a Collection of {@link org.apache.myfaces.config.element.NavigationRule NavigationRule}s
     */
    public Collection<NavigationRule> getNavigationRules()
    {
        return Collections.unmodifiableCollection(_navigationRules);
    }

    public void addNavigationRule(NavigationRule navigationRule)
    {
        _navigationRules.add(navigationRule);

        _navigationRulesChanged = true;
    }

    public boolean isNavigationRulesChanged()
    {
        return _navigationRulesChanged;
    }

    public void setNavigationRulesChanged(boolean navigationRulesChanged)
    {
        _navigationRulesChanged = navigationRulesChanged;
    }

    /**
     * Return the managed bean info that can be used by the VariableResolver implementation.
     * 
     * @return a {@link org.apache.myfaces.config.element.ManagedBean ManagedBean}
     */
    public ManagedBean getManagedBean(String name)
    {
        return _managedBeans.get(name);
    }

    public Map<String, ManagedBean> getManagedBeans()
    {
        return Collections.unmodifiableMap(_managedBeans);
    }

    public void addManagedBean(String name, ManagedBean managedBean)
    {
        _managedBeans.put(name, managedBean);
        if(_oldManagedBeans!=null)
        {
            _oldManagedBeans.remove(name);
        }
    }
    
    public void addComponentTagDeclaration(ComponentTagDeclaration declaration)
    {
        _componentTagDeclarations.add(declaration);
    }
    
    public List<ComponentTagDeclaration> getComponentTagDeclarations()
    {
        return Collections.unmodifiableList(_componentTagDeclarations);
    }
    
    public void addFaceletTagLibrary(FaceletTagLibrary library)
    {
        _faceletTagLibraries.add(library);
    }
    
    public List<FaceletTagLibrary> getFaceletTagLibraries()
    {
        return Collections.unmodifiableList(_faceletTagLibraries);
    }
    
    public final void addConverterConfiguration(final String converterClassName,
            final org.apache.myfaces.config.element.Converter configuration)
    {
        checkNull(converterClassName, "converterClassName");
        checkEmpty(converterClassName, "converterClassName");
        checkNull(configuration, "configuration");

        _converterClassNameToConfigurationMap.put(converterClassName, configuration);
    }
    
    public org.apache.myfaces.config.element.Converter getConverterConfiguration(String converterClassName)
    {
        return (org.apache.myfaces.config.element.Converter)
                _converterClassNameToConfigurationMap.get(converterClassName);
    }
    
    private void checkNull(final Object param, final String paramName)
    {
        if (param == null)
        {
            throw new NullPointerException(paramName + " can not be null.");
        }
    }

    private void checkEmpty(final String param, final String paramName)
    {
        if (param.length() == 0)
        {
            throw new NullPointerException("String " + paramName + " can not be empty.");
        }
    }

    /**
     * Return the resourcebundle which was configured in faces config by var name
     * 
     * @param name
     *            the name of the resource bundle (content of var)
     * @return the resource bundle or null if not found
     */
    public ResourceBundle getResourceBundle(String name)
    {
        return _resourceBundles.get(name);
    }

    /**
     * @return the resourceBundles
     */
    public Map<String, ResourceBundle> getResourceBundles()
    {
        return _resourceBundles;
    }

    public void addResourceBundle(ResourceBundle bundle)
    {
        if (bundle == null)
        {
            throw new IllegalArgumentException("bundle must not be null");
        }
        String var = bundle.getVar();
        if (_resourceBundles.containsKey(var) && log.isLoggable(Level.WARNING))
        {
            log.warning("Another resource bundle for var '" + var + "' with base name '"
                    + _resourceBundles.get(var).getBaseName() + "' is already registered. '"
                    + _resourceBundles.get(var).getBaseName() + "' will be replaced with '" + bundle.getBaseName()
                    + "'.");
        }
        _resourceBundles.put(var, bundle);
    }

    public void addFacesConfigElResolver(ELResolver resolver)
    {
        if (facesConfigElResolvers == null)
        {
            facesConfigElResolvers = new ArrayList<ELResolver>();
        }
        facesConfigElResolvers.add(resolver);
    }

    public List<ELResolver> getFacesConfigElResolvers()
    {
        return facesConfigElResolvers;
    }

    public void addApplicationElResolver(ELResolver resolver)
    {
        if (applicationElResolvers == null)
        {
            applicationElResolvers = new ArrayList<ELResolver>();
        }
        applicationElResolvers.add(resolver);
    }

    public List<ELResolver> getApplicationElResolvers()
    {
        return applicationElResolvers;
    }

    public void setVariableResolver(VariableResolver variableResolver)
    {
        _variableResolver = variableResolver;
    }

    public VariableResolver getVariableResolver()
    {
        return _variableResolver;
    }

    public void setPropertyResolver(PropertyResolver propertyResolver)
    {
        _propertyResolver = propertyResolver;
    }

    public PropertyResolver getPropertyResolver()
    {
        return _propertyResolver;
    }

    public ExpressionFactory getExpressionFactory()
    {
        return _expressionFactory;
    }

    public void setExpressionFactory(ExpressionFactory expressionFactory)
    {
        _expressionFactory = expressionFactory;
    }

    public void setPropertyResolverChainHead(PropertyResolver resolver)
    {
        _propertyResolverChainHead = resolver;
    }

    public PropertyResolver getPropertyResolverChainHead()
    {
        return _propertyResolverChainHead;
    }

    public void setVariableResolverChainHead(VariableResolver resolver)
    {
        _variableResolverChainHead = resolver;
    }

    public VariableResolver getVariableResolverChainHead()
    {
        return _variableResolverChainHead;
    }

    public Map<String, ManagedBean> getManagedBeansNotReaddedAfterPurge()
    {
        return _oldManagedBeans;
    }

    public void resetManagedBeansNotReaddedAfterPurge()
    {
        _oldManagedBeans.clear();
    }
    
    public String getFacesVersion ()
    {
        return _facesVersion;
    }
    
    void setFacesVersion (String facesVersion)
    {
        _facesVersion = facesVersion;
    }

    public NamedEventManager getNamedEventManager()
    {
        return _namedEventManager;
    }

    public void setNamedEventManager(NamedEventManager namedEventManager)
    {
        this._namedEventManager = namedEventManager;
    }

    public Comparator<ELResolver> getELResolverComparator()
    {
        return _elResolverComparator;
    }
    
    public void setELResolverComparator(Comparator<ELResolver> elResolverComparator)
    {
        _elResolverComparator = elResolverComparator;
    }
    
    public Predicate<ELResolver> getELResolverPredicate()
    {
        return _elResolverPredicate;
    }
    
    public void setELResolverPredicate(Predicate<ELResolver> elResolverPredicate)
    {
        _elResolverPredicate = elResolverPredicate;
    }
    
    public void addFaceletProcessingConfiguration(String fileExtension, FaceletsProcessing configuration)
    {
        checkNull(fileExtension, "fileExtension");
        checkEmpty(fileExtension, "fileExtension");
        checkNull(configuration, "configuration");

        this._faceletsProcessingByFileExtension.put(fileExtension, configuration);
    }
    
    public FaceletsProcessing getFaceletProcessingConfiguration(String fileExtensions)
    {
        return _faceletsProcessingByFileExtension.get(fileExtensions);
    }
    
    public Collection<FaceletsProcessing> getFaceletProcessingConfigurations()
    {
        return _faceletsProcessingByFileExtension.values();
    }

    /**
     * @return the _externalContextResourceLibraryContracts
     */
    public Set<String> getExternalContextResourceLibraryContracts()
    {
        return _externalContextResourceLibraryContracts;
    }

    /**
     * @param externalContextResourceLibraryContracts the _externalContextResourceLibraryContracts to set
     */
    public void setExternalContextResourceLibraryContracts(Set<String> externalContextResourceLibraryContracts)
    {
        this._externalContextResourceLibraryContracts = externalContextResourceLibraryContracts;
        this._resourceLibraryContracts.clear();
        this._resourceLibraryContracts.addAll(this._externalContextResourceLibraryContracts);
        this._resourceLibraryContracts.addAll(this._classLoaderResourceLibraryContracts);
    }

    /**
     * @return the _classLoaderResourceLibraryContracts
     */
    public Set<String> getClassLoaderResourceLibraryContracts()
    {
        return _classLoaderResourceLibraryContracts;
    }

    /**
     * @param classLoaderResourceLibraryContracts the _classLoaderResourceLibraryContracts to set
     */
    public void setClassLoaderResourceLibraryContracts(Set<String> classLoaderResourceLibraryContracts)
    {
        this._classLoaderResourceLibraryContracts = classLoaderResourceLibraryContracts;
        this._resourceLibraryContracts.clear();
        this._resourceLibraryContracts.addAll(this._externalContextResourceLibraryContracts);
        this._resourceLibraryContracts.addAll(this._classLoaderResourceLibraryContracts);
    }

    /**
     * @return the _resourceLibraryContracts
     */
    public Set<String> getResourceLibraryContracts()
    {
        return _resourceLibraryContracts;
    }

    /**
     * @return the _contractMappings
     */
    public Map<String, List<String>> getContractMappings()
    {
        return _contractMappings;
    }

    public void addContractMapping(String urlPattern, String[] contracts)
    {
        List<String> contractsList = _contractMappings.get(urlPattern);
        if (contractsList == null)
        {
            contractsList = new ArrayList<String>();
            _contractMappings.put(urlPattern, contractsList);
        }
        Collections.addAll(contractsList, contracts);
    }
    
    public void addContractMapping(String urlPattern, String contract)
    {
        List<String> contractsList = _contractMappings.get(urlPattern);
        if (contractsList == null)
        {
            contractsList = new ArrayList<String>();
            _contractMappings.put(urlPattern, contractsList);
        }
        contractsList.add(contract);
    }    
    
    public List<String> getResourceResolvers()
    {
        return _resourceResolvers;
    }
    
    public void addResourceResolver(String resourceResolver)
    {
        _resourceResolvers.add(resourceResolver);
    }

    /**
     * @return the _injectedObjects
     */
    public List<Object> getInjectedObjects()
    {
        return _injectedObjects;
    }

    public void addInjectedObject(Object object)
    {
        _injectedObjects.add(object);
    }

    public Map<Integer, String> getNamespaceById()
    {
        return _namespaceById;
    }

    public void setNamespaceById(Map<Integer, String> namespaceById)
    {
        this._namespaceById = namespaceById;
    }

    public Map<String, Integer> getIdByNamespace()
    {
        return _idByNamespace;
    }

    public void setIdByNamespace(Map<String, Integer> idByNamespace)
    {
        this._idByNamespace = idByNamespace;
    }

    public List<ViewPoolMapping> getViewPoolMappings()
    {
        return _viewPoolMappings;
    }
    
    public void addViewPoolMapping(ViewPoolMapping mapping)
    {
        _viewPoolMappings.add(mapping);
    }
    
    public void addApplicationSearchExpressionResolver(SearchKeywordResolver resolver)
    {
        if (_searchExpressionResolvers == null)
        {
            _searchExpressionResolvers = new ArrayList<SearchKeywordResolver>();
        }
        _searchExpressionResolvers.add(resolver);
    }

    public List<SearchKeywordResolver> getApplicationSearchExpressionResolvers()
    {
        return _searchExpressionResolvers;
    }
    
    public List<FaceletsTemplateMapping> getFaceletsTemplateMappings()
    {
        return _faceletsTemplateMappings;
    }
    
    public void addFaceletsTemplateMapping(FaceletsTemplateMapping mapping)
    {
        _faceletsTemplateMappings.add(mapping);
    }
}
