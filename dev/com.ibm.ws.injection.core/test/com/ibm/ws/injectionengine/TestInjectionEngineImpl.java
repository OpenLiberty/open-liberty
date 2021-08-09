/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.Reference;

import com.ibm.ws.resource.ResourceFactoryBuilder;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.ws.resource.internal.ResourceRefConfigListImpl;
import com.ibm.ws.runtime.metadata.MetaData;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionProcessorContextImpl;
import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.injectionengine.factory.EJBLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.IndirectJndiLookupReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResAutoLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResRefReferenceFactory;

public class TestInjectionEngineImpl
                extends AbstractInjectionEngine
{
    private final InjectionScopeData ivGlobalScopeData = new InjectionScopeData(null);
    private ResourceRefConfig ivDefaultResourceRefConfig;
    private final Map<String, Object> ivBindings = new HashMap<String, Object>();
    private final Map<String, InjectionBinding<?>> ivCompletedBindings = new HashMap<String, InjectionBinding<?>>();
    private boolean ivBindNonCompInjectionBindings;
    private boolean ivSaveNonCompInjectionBindings;
    private Map<Class<?>, Map<String, InjectionBinding<?>>> ivSavedGlobalInjectionBindings;
    private Map<Class<?>, Map<String, InjectionBinding<?>>> ivSavedAppInjectionBindings;
    private Map<Class<?>, Map<String, InjectionBinding<?>>> ivSavedModuleInjectionBindings;

    public TestInjectionEngineImpl()
    {
        initialize();
    }

    @Override
    public InjectionScopeData getInjectionScopeData(MetaData md)
    {
        return ivGlobalScopeData;
    }

    @Override
    public boolean isEmbeddable()
    {
        return true;
    }

    @Override
    protected IndirectJndiLookupReferenceFactory getDefaultIndirectJndiLookupReferenceFactory()
    {
        return null;
    }

    @Override
    protected IndirectJndiLookupReferenceFactory getDefaultResIndirectJndiLookupReferenceFactory()
    {
        return null;
    }

    @Override
    protected ResRefReferenceFactory getDefaultResRefReferenceFactory()
    {
        return null;
    }

    @Override
    protected ResAutoLinkReferenceFactory getDefaultResAutoLinkReferenceFactory()
    {
        return null;
    }

    @Override
    protected EJBLinkReferenceFactory getDefaultEJBLinkReferenceFactory()
    {
        return null;
    }

    @Override
    public boolean isValidationLoggable(boolean appCustomPropertySetting)
    {
        return appCustomPropertySetting;
    }

    @Override
    public boolean isValidationFailable(boolean appCustomPropertySetting)
    {
        return appCustomPropertySetting;
    }

    @Override
    public ResourceRefConfigList createResourceRefConfigList()
    {
        return new ResourceRefConfigListImpl();
    }

    @Override
    public synchronized ResourceRefConfig getDefaultResourceRefConfig()
    {
        if (ivDefaultResourceRefConfig == null)
        {
            ResourceRefConfigList rrcl = createResourceRefConfigList();
            ivDefaultResourceRefConfig = rrcl.findOrAddByName("default");
        }
        return ivDefaultResourceRefConfig;
    }

    public Map<String, InjectionBinding<?>> getCompletedInjectionBindings()
    {
        return ivCompletedBindings;
    }

    public void setBindNonCompInjectionBindings(boolean value)
    {
        ivBindNonCompInjectionBindings = value;
    }

    public void setSaveNonCompInjectionBindings(boolean value)
    {
        ivSaveNonCompInjectionBindings = value;
        ivSavedGlobalInjectionBindings = new HashMap<Class<?>, Map<String, InjectionBinding<?>>>();
        ivSavedAppInjectionBindings = new HashMap<Class<?>, Map<String, InjectionBinding<?>>>();
        ivSavedModuleInjectionBindings = new HashMap<Class<?>, Map<String, InjectionBinding<?>>>();
    }

    public Map<Class<?>, Map<String, InjectionBinding<?>>> getSavedGlobalInjectionBindings()
    {
        return ivSavedGlobalInjectionBindings;
    }

    public Map<Class<?>, Map<String, InjectionBinding<?>>> getSavedAppInjectionBindings()
    {
        return ivSavedAppInjectionBindings;
    }

    public Map<Class<?>, Map<String, InjectionBinding<?>>> getSavedModuleInjectionBindings()
    {
        return ivSavedModuleInjectionBindings;
    }

    @Override
    protected void processInjectionMetaData(ComponentNameSpaceConfiguration compNSConfig, List<Class<?>> annotatedClasses)
                    throws InjectionException
    {
        InjectionProcessorContextImpl context = InjectionProcessorContextImpl.get(compNSConfig);

        if (!ivCompletedBindings.isEmpty())
        {
            context.ivCompletedInjectionBindings = ivCompletedBindings;
        }

        context.ivBindNonCompInjectionBindings = ivBindNonCompInjectionBindings;
        context.ivSaveNonCompInjectionBindings = ivSaveNonCompInjectionBindings;
        context.ivSavedGlobalInjectionBindings = ivSavedGlobalInjectionBindings;
        context.ivSavedAppInjectionBindings = ivSavedAppInjectionBindings;
        context.ivSavedModuleInjectionBindings = ivSavedModuleInjectionBindings;

        super.processInjectionMetaData(compNSConfig, annotatedClasses);
    }

    @Override
    protected void processClientInjections(ComponentNameSpaceConfiguration compNSConfig, InjectionProcessorContextImpl context)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerResourceFactoryBuilder(String type, ResourceFactoryBuilder builder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceFactoryBuilder unregisterResourceFactoryBuilder(String type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceFactoryBuilder getResourceFactoryBuilder(String type) throws InjectionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Reference createDefinitionReference(ComponentNameSpaceConfiguration nameSpaceConfig,
                                               InjectionScope scope,
                                               String refName,
                                               String bindingName,
                                               String type,
                                               Map<String, Object> props) throws InjectionException {
        return new DefinitionReference(scope, refName, bindingName, type, props);
    }

    @SuppressWarnings("serial")
    public static class DefinitionReference extends Reference {
        public final InjectionScope ivScope;
        public final String ivJndiName;
        public final String ivBindingName;
        public final Map<String, Object> ivProperties;

        DefinitionReference(InjectionScope scope,
                            String refName,
                            String bindingName,
                            String type,
                            Map<String, Object> props) {
            super(type, "com.ibm.Factory", null);
            ivScope = scope;
            ivJndiName = refName;
            ivBindingName = bindingName;
            ivProperties = props;
        }
    }

    @Override
    public void destroyDefinitionReference(Reference dataSourceReference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void bindJavaNameSpaceObject(ComponentNameSpaceConfiguration compNSConfig,
                                        InjectionScope scope,
                                        String name,
                                        InjectionBinding<?> binding,
                                        Object bindingObject)
    {
        if (bindingObject == null)
        {
            throw new IllegalArgumentException();
        }

        String qualifiedName = (scope == null ? "java:comp/env/" : scope.prefix()) + name;
        ivBindings.put(qualifiedName, bindingObject);
    }

    public Object getJavaNameSpaceObjectBinding(String qualifiedName)
    {
        return ivBindings.get(qualifiedName);
    }

    @Override
    public ReferenceContext createReferenceContext(MetaData md)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReferenceContext getCommonReferenceContext(ModuleMetaData mmd)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void injectClient(ComponentNameSpaceConfiguration compNSConfig)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context createComponentNameSpaceContext(Object componentNameSpace) throws NamingException
    {
        return null;
    }

    @Override
    public Object createJavaNameSpace(String logicalAppName,
                                      String moduleName,
                                      String logicalModuleName,
                                      String componentName) throws NamingException
    {
        return new JavaNameSpace(logicalAppName, moduleName, logicalModuleName, componentName);
    }

    public static class JavaNameSpace
    {
        final String ivLogicalAppName;
        final String ivModuleName;
        final String ivLogicalModuleName;
        final String ivComponentName;

        JavaNameSpace(String logicalAppName, String moduleName, String logicalModuleName, String componentName)
        {
            ivLogicalAppName = logicalAppName;
            ivModuleName = moduleName;
            ivLogicalModuleName = logicalModuleName;
            ivComponentName = componentName;
        }
    }

    @Override
    public Object getInjectableObject(InjectionBinding<?> binding,
                                      Object targetObject,
                                      InjectionTargetContext targetContext)
                    throws InjectionException
    {
        return binding.getInjectionObject(targetObject, targetContext);
    }
}
