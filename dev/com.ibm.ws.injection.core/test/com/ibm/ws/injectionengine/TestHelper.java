/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injectionengine;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.naming.spi.ObjectFactory;

import junit.framework.Assert;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;
import com.ibm.ws.javaee.dd.common.DataSource;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.EJBRef;
import com.ibm.ws.javaee.dd.common.EnvEntry;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.ws.javaee.dd.common.ResourceEnvRef;
import com.ibm.ws.javaee.dd.common.ResourceGroup;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.ws.resource.internal.ResourceRefConfigListImpl;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfigurationProvider;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.ReferenceContext;
import com.ibm.wsspi.injectionengine.factory.IndirectJndiLookupReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResAutoLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResRefReferenceFactory;

public class TestHelper
                extends ComponentNameSpaceConfiguration
{
    static
    {
        setupInitialContextFactoryBuilder();

        // Injection tests require Java 7 Resource.class on the bootclasspath.
        try
        {
            Resource.class.getMethod("lookup");
        } catch (NoSuchMethodException e)
        {
            throw new IllegalStateException("Resource.lookup() is required", e);
        }
    }

    private static void setupInitialContextFactoryBuilder()
    {
        try
        {
            NamingManager.setInitialContextFactoryBuilder(new InitialContextFactoryBuilder()
            {
                @Override
                public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> envmt)
                {
                    return new InitialContextFactory()
                    {
                        @Override
                        public Context getInitialContext(Hashtable<?, ?> envmt)
                        {
                            return (Context) Proxy.newProxyInstance(
                                                                    TestHelper.class.getClassLoader(),
                                                                    new Class<?>[] { Context.class },
                                                                    new InvocationHandler()
                                                                    {
                                                                        @Override
                                                                        public Object invoke(Object proxy, Method method, Object[] args)
                                                                        {
                                                                            if (method.getName().equals("lookup") &&
                                                                                args.length == 1 &&
                                                                                method.getParameterTypes()[0] == String.class)
                                                                            {
                                                                                return lookup((String) args[0]);
                                                                            }
                                                                            throw new UnsupportedOperationException(method.toString());
                                                                        }
                                                                    });
                        }
                    };
                }
            });
        } catch (IllegalStateException ex)
        {
            // Ignore (already registered).
        } catch (NamingException ex)
        {
            throw new AssertionError(ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T createProxyInstance(final Class<T> klass)
    {
        return (T) Proxy.newProxyInstance(
                                          TestHelper.class.getClassLoader(),
                                          new Class<?>[] { klass },
                                          new InvocationHandler()
                                          {
                                              @Override
                                              public Object invoke(Object proxy, Method method, Object[] args)
                                              {
                                                  if (method.getDeclaringClass() == Object.class)
                                                  {
                                                      if (method.getName().equals("toString"))
                                                      {
                                                          return toString() + '[' + klass + ']';
                                                      }
                                                      if (method.getName().equals("equals"))
                                                      {
                                                          return proxy == args[0];
                                                      }
                                                  }
                                                  throw new UnsupportedOperationException(method.toString());
                                              }
                                          });
    }

    public static String envName(Class<?> klass, String propertyName)
    {
        return klass.getName().replace('$', '.') + '/' + propertyName;
    }

    public static abstract class AbstractResourceGroup
                    implements ResourceGroup
    {
        private final String ivName;
        private final String ivLookupName;
        private final List<com.ibm.ws.javaee.dd.common.InjectionTarget> ivInjectionTargets;

        public AbstractResourceGroup(String name,
                                     String lookupName,
                                     List<com.ibm.ws.javaee.dd.common.InjectionTarget> injectionTargets)
        {
            ivName = name;
            ivLookupName = lookupName;
            ivInjectionTargets = injectionTargets;
        }

        @Override
        public String getName()
        {
            return ivName;
        }

        public List<Description> getDescriptions()
        {
            return Collections.emptyList();
        }

        @Override
        public String getLookupName()
        {
            return ivLookupName;
        }

        @Override
        public List<com.ibm.ws.javaee.dd.common.InjectionTarget> getInjectionTargets()
        {
            return ivInjectionTargets;
        }

        @Override
        public String getMappedName()
        {
            return null;
        }
    }

    public static class EnvEntryImpl
                    extends AbstractResourceGroup
                    implements EnvEntry
    {
        private final Class<?> ivType;
        private final String ivValue;

        public EnvEntryImpl(String name,
                            Class<?> type,
                            String value,
                            String lookup,
                            com.ibm.ws.javaee.dd.common.InjectionTarget... injectionTargets)
        {
            super(name, lookup, Arrays.asList(injectionTargets));
            ivType = type;
            ivValue = value;
        }

        @Override
        public String getTypeName()
        {
            return ivType == null ? null : ivType.getName();
        }

        @Override
        public String getValue()
        {
            return ivValue;
        }
    }

    public static com.ibm.ws.javaee.dd.common.InjectionTarget createInjectionTarget(final Class<?> klass, final String name)
    {
        return new com.ibm.ws.javaee.dd.common.InjectionTarget()
        {
            @Override
            public String getInjectionTargetClassName()
            {
                return klass.getName();
            }

            @Override
            public String getInjectionTargetName()
            {
                return name;
            }
        };
    }

    public static <A extends Annotation> void mergeSaved(InjectionBinding<A> binding1, InjectionBinding<?> binding2)
                    throws InjectionException
    {
        @SuppressWarnings("unchecked")
        InjectionBinding<A> binding2Unchecked = (InjectionBinding<A>) binding2;
        binding1.mergeSaved(binding2Unchecked);
    }

    public static void mergeSavedFail(InjectionBinding<?> binding1, InjectionBinding<?> binding2)
    {
        try
        {
            mergeSaved(binding1, binding2);
            Assert.fail();
        } catch (InjectionException ex) { /* expected */
        }
    }

    private static ThreadLocal<TestHelper> svCurrent = new ThreadLocal<TestHelper>();

    private final Set<Class<?>> ivClasses = new HashSet<Class<?>>();
    private final List<Object> ivInstances = new ArrayList<Object>();
    private final Map<String, String> ivIndirectJndiLookupTypes = new HashMap<String, String>();
    private final Map<String, Object> ivIndirectJndiLookupValues = new HashMap<String, Object>();
    private TestInjectionEngineImpl ivInjectionEngine;

    public final HashMap<Class<?>, InjectionTarget[]> ivTargets = new HashMap<Class<?>, InjectionTarget[]>();

    public TestHelper()
    {
        this("test");
    }

    public TestHelper(String name)
    {
        this(name, new J2EENameImpl("testapp", "testmod.jar", name));
    }

    public TestHelper(String name, J2EEName j2eeName)
    {
        super(name, j2eeName);
        svCurrent.set(this);
    }

    public TestHelper setTestLogicalModuleName(String logicalAppName, String logicalModuleName)
    {
        setLogicalModuleName(logicalAppName, logicalModuleName);
        return this;
    }

    public TestHelper setClassLoader()
    {
        setClassLoader(TestHelper.class.getClassLoader());
        return this;
    }

    public TestHelper setTestIndirectJndiLookupReferenceFactory(IndirectJndiLookupReferenceFactory factory)
    {
        setIndirectJndiLookupReferenceFactory(factory);
        return this;
    }

    public TestHelper addIndirectJndiLookupValue(String jndiName, Object value)
    {
        addIndirectJndiLookupValue(jndiName, value.getClass(), value);
        return this;
    }

    public TestHelper addIndirectJndiLookupValue(final String jndiName, Class<?> expectedType, Object value)
    {
        // InjectionBinding.setObjects(Object, Reference) won't create an
        // ObjectFactory without a class loader.
        Assert.assertNotNull(getClassLoader());

        if (getIndirectJndiLookupReferenceFactory() == null)
        {
            setIndirectJndiLookupReferenceFactory(new IndirectJndiLookupReferenceFactory()
            {
                @Override
                public Reference createIndirectJndiLookup(String refName, String boundToJndiName, String type)
                {
                    Assert.assertEquals(refName + "->" + boundToJndiName,
                                        ivIndirectJndiLookupTypes.get(boundToJndiName), type);
                    return IndirectJndiLookupObjectFactory.createReference(boundToJndiName, type);
                }

                @Override
                public Reference createIndirectJndiLookupInConsumerContext(String refName, String boundToJndiName, String type) throws InjectionException {
                    Assert.assertEquals(refName + "->" + boundToJndiName,
                                        ivIndirectJndiLookupTypes.get(boundToJndiName), type);
                    return IndirectJndiLookupObjectFactory.createReference(boundToJndiName, type);
                }
            });
        }

        if (getResIndirectJndiLookupReferenceFactory() == null)
        {
            setResIndirectJndiLookupReferenceFactory(getIndirectJndiLookupReferenceFactory());
        }

        ivIndirectJndiLookupTypes.put(jndiName, expectedType.getName());
        ivIndirectJndiLookupValues.put(jndiName, value);
        return this;
    }

    static Object lookup(String jndiName)
    {
        Object result = svCurrent.get().ivIndirectJndiLookupValues.get(jndiName);

        Assert.assertNotNull(jndiName, result);
        return result;
    }

    public static class IndirectJndiLookupObjectFactory
                    implements ObjectFactory
    {
        public static Reference createReference(String jndiName, String className)
        {
            final StringRefAddr refAddr = new StringRefAddr(IndirectJndiLookupObjectFactory.class.getName(), jndiName);
            return new Reference(className, refAddr, IndirectJndiLookupObjectFactory.class.getName(), null);
        }

        @Override
        public Object getObjectInstance(Object o, Name n, Context c, Hashtable<?, ?> envmt)
        {
            Reference ref = (Reference) o;
            String jndiName = (String) ref.get(IndirectJndiLookupObjectFactory.class.getName()).getContent();
            return lookup(jndiName);
        }
    }

    public TestHelper setTestResIndirectJndiLookupReferenceFactory()
    {
        return setTestResIndirectJndiLookupReferenceFactory(new IndirectJndiLookupReferenceFactory()
        {
            @Override
            public Reference createIndirectJndiLookup(String jndiName, String boundToJndiName, String type)
            {
                return IndirectJndiLookupObjectFactory.createReference(boundToJndiName, type);
            }

            @Override
            public Reference createIndirectJndiLookupInConsumerContext(String jndiName, String boundToJndiName, String type)
            {
                return IndirectJndiLookupObjectFactory.createReference(boundToJndiName, type);
            }
        });
    }

    public TestHelper setTestResIndirectJndiLookupReferenceFactory(IndirectJndiLookupReferenceFactory factory)
    {
        setResIndirectJndiLookupReferenceFactory(factory);
        return this;
    }

    public TestHelper setTestResRefReferenceFactory()
    {
        return setTestResRefReferenceFactory(new ResRefReferenceFactory()
        {
            @Override
            public Reference createResRefJndiLookup(ComponentNameSpaceConfiguration compNSConfig,
                                                    InjectionScope scope,
                                                    ResourceRefInfo resRef)
            {
                return IndirectJndiLookupObjectFactory.createReference(resRef.getJNDIName(), resRef.getType());
            }
        });
    }

    public TestHelper setTestResRefReferenceFactory(ResRefReferenceFactory factory)
    {
        setResRefReferenceFactory(factory);
        return this;
    }

    public TestHelper setTestResAutoLinkReferenceFactory(ResAutoLinkReferenceFactory factory)
    {
        setResAutoLinkReferenceFactory(factory);
        return this;
    }

    public TestHelper addResRefLookupValue(final String jndiName, Object value)
    {
        if (getResRefReferenceFactory() == null)
        {
            setResRefReferenceFactory(new ResRefReferenceFactory()
            {
                @Override
                public Reference createResRefJndiLookup(ComponentNameSpaceConfiguration compNSConfig,
                                                        InjectionScope scope,
                                                        ResourceRefInfo resRef)
                {
                    return IndirectJndiLookupObjectFactory.createReference(jndiName, null);
                }
            });
        }

        ivIndirectJndiLookupValues.put(jndiName, value);
        return this;
    }

    public ResourceRefConfigList createResourceRefConfigList()
    {
        if (getResourceRefConfigList() == null)
        {
            setResourceRefConfigList(new ResourceRefConfigListImpl());
        }
        return getResourceRefConfigList();
    }

    public TestHelper addInjectionClass(Class<?> klass)
    {
        if (getInjectionClasses() == null)
        {
            setInjectionClasses(new ArrayList<Class<?>>());
        }
        getInjectionClasses().add(klass);
        return this;
    }

    public TestHelper addInstance(Object o)
    {
        Class<?> klass = o.getClass();
        if (ivClasses.add(klass))
        {
            addInjectionClass(klass);
        }

        ivInstances.add(o);
        return this;
    }

    public TestHelper setJavaColonCompEnvMap()
    {
        setJavaColonCompEnvMap(new HashMap<String, InjectionBinding<?>>());
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TestHelper addEnvEntry(EnvEntry envEntry)
    {
        if (getEnvEntries() == null)
        {
            setEnvEntries(new ArrayList<EnvEntry>());
        }
        ((List) getEnvEntries()).add(envEntry);
        return this;
    }

    public TestHelper addEnvEntryValue(String name, String value)
    {
        if (getEnvEntryValues() == null)
        {
            setEnvEntryValues(new HashMap<String, String>());
        }
        getEnvEntryValues().put(name, value);
        return this;
    }

    public TestHelper addEnvEntryBinding(String name, String value)
    {
        if (getEnvEntryBindings() == null)
        {
            setEnvEntryBindings(new HashMap<String, String>());
        }
        getEnvEntryBindings().put(name, value);
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TestHelper addResourceRef(ResourceRef resourceRef)
    {
        if (getResourceRefs() == null)
        {
            setResourceRefs(new ArrayList<ResourceRef>());
        }
        ((List) getResourceRefs()).add(resourceRef);
        return this;
    }

    public TestHelper addResourceRefBinding(String name, String binding)
    {
        if (getResourceRefBindings() == null)
        {
            setResourceRefBindings(new HashMap<String, String>());
        }
        getResourceRefBindings().put(name, binding);
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TestHelper addResourceEnvRef(ResourceEnvRef resourceEnvRef)
    {
        if (getResourceEnvRefs() == null)
        {
            setResourceEnvRefs(new ArrayList<ResourceEnvRef>());
        }
        ((List) getResourceEnvRefs()).add(resourceEnvRef);
        return this;
    }

    public TestHelper addResourceEnvRefBinding(String name, String binding)
    {
        if (getResourceEnvRefBindings() == null)
        {
            setResourceEnvRefBindings(new HashMap<String, String>());
        }
        getResourceEnvRefBindings().put(name, binding);
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TestHelper addMsgDestRef(MessageDestinationRef msgDestRef)
    {
        if (getMsgDestRefs() == null)
        {
            setMsgDestRefs(new ArrayList<MessageDestinationRef>());
        }
        ((List) getMsgDestRefs()).add(msgDestRef);
        return this;
    }

    public TestHelper addMsgDestRefBinding(String name, String binding)
    {
        if (getMsgDestRefBindings() == null)
        {
            setMsgDestRefBindings(new HashMap<String, String>());
        }
        getMsgDestRefBindings().put(name, binding);
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TestHelper addEJBRef(EJBRef ejbRef)
    {
        if (getEJBRefs() == null)
        {
            setEJBRefs(new ArrayList<EJBRef>());
        }
        ((List) getEJBRefs()).add(ejbRef);
        return this;
    }

    public TestHelper addEJBRefBinding(String name, String binding)
    {
        if (getEJBRefBindings() == null)
        {
            setEJBRefBindings(new HashMap<String, String>());
        }
        getEJBRefBindings().put(name, binding);
        return this;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public TestHelper addDataSourceDefinition(DataSource dataSource)
    {
        if (getDataSourceDefinitions() == null)
        {
            setDataSourceDefinitions(new ArrayList<DataSource>());
        }
        ((List) getDataSourceDefinitions()).add(dataSource);
        return this;
    }

    public TestHelper addDataSourceDefinitionBinding(String name, String binding)
    {
        if (getDataSourceDefinitionBindings() == null)
        {
            setDataSourceDefinitionBindings(new HashMap<String, String>());
        }
        getDataSourceDefinitionBindings().put(name, binding);
        return this;
    }

    public TestHelper setInjectionEngine(TestInjectionEngineImpl injectionEngine)
    {
        ivInjectionEngine = injectionEngine;
        return this;
    }

    public TestInjectionEngineImpl createInjectionEngine()
    {
        if (ivInjectionEngine == null)
        {
            ivInjectionEngine = new TestInjectionEngineImpl();
        }
        return ivInjectionEngine;
    }

    public TestHelper process()
                    throws InjectionException
    {
        createInjectionEngine().processInjectionMetaData(ivTargets, this);
        return this;
    }

    public TestHelper inject()
                    throws InjectionException
    {
        for (Object instance : ivInstances)
        {
            InjectionTarget[] targets = ivTargets.get(instance.getClass());
            for (InjectionTarget target : targets)
            {
                ivInjectionEngine.inject(instance, target);
            }
        }
        return this;
    }

    public TestHelper processAndInject(Object... instances)
    {
        for (Object instance : instances)
        {
            addInstance(instance);
        }

        try
        {
            process();
            inject();
        } catch (InjectionException ex)
        {
            throw new RuntimeException(ex);
        }
        return this;
    }

    public InjectionBinding<?> processAndGetInjectionBinding()
    {
        setJavaColonCompEnvMap();

        try
        {
            process();
        } catch (InjectionException ex)
        {
            throw new RuntimeException(ex);
        }

        Map<String, InjectionBinding<?>> bindings = getJavaColonCompEnvMap();
        Assert.assertEquals(1, bindings.size());
        return bindings.values().iterator().next();
    }

    public ReferenceContext createReferenceContext()
    {
        ReferenceContext referenceContext = createInjectionEngine().createReferenceContext();
        referenceContext.add(this);
        return referenceContext;
    }

    public ComponentNameSpaceConfigurationProvider createComponentNameSpaceConfigurationProvider()
    {
        return new ComponentNameSpaceConfigurationProvider()
        {
            @Override
            public String toString()
            {
                return super.toString() + '[' + getJ2EEName() + ']';
            }

            @Override
            public ComponentNameSpaceConfiguration getComponentNameSpaceConfiguration()
            {
                return TestHelper.this;
            }
        };
    }
}
