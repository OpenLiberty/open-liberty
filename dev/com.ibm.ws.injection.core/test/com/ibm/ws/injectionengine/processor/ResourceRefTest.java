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
package com.ibm.ws.injectionengine.processor;

import java.sql.Connection;
import java.util.Arrays;

import javax.annotation.Resource;
import javax.naming.Reference;
import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.injectionengine.TestHelper;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.dd.common.ResourceRef;
import com.ibm.ws.resource.ResourceRefConfig;
import com.ibm.ws.resource.ResourceRefConfigList;
import com.ibm.ws.resource.ResourceRefInfo;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionScope;
import com.ibm.wsspi.injectionengine.factory.ResAutoLinkReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResRefReferenceFactory;
import com.ibm.wsspi.injectionengine.factory.ResourceInfo;
import com.ibm.wsspi.resource.ResourceConfig;

public class ResourceRefTest
{
    private static class ResourceRefImpl
                    extends TestHelper.AbstractResourceGroup
                    implements ResourceRef
    {
        private final Class<?> ivType;
        private final int ivAuth;
        private final int ivSharingScope;

        ResourceRefImpl(String name,
                        Class<?> type,
                        int auth,
                        int sharingScope,
                        String lookup,
                        InjectionTarget... injectionTargets)
        {
            super(name, lookup, Arrays.asList(injectionTargets));
            ivType = type;
            ivAuth = auth;
            ivSharingScope = sharingScope;
        }

        @Override
        public String getType()
        {
            return ivType == null ? null : ivType.getName();
        }

        @Override
        public int getAuthValue()
        {
            return ivAuth;
        }

        @Override
        public int getSharingScopeValue()
        {
            return ivSharingScope;
        }
    }

    @Test
    public void testDataSource()
    {
        TestDataSource instance = new TestDataSource();
        TestHelper helper = new TestHelper();
        helper.setResRefReferenceFactory(new ResRefReferenceFactory()
        {
            @Override
            public Reference createResRefJndiLookup(ComponentNameSpaceConfiguration compNSConfig,
                                                    InjectionScope scope,
                                                    ResourceRefInfo resRef)
            {
                Assert.assertEquals(InjectionScope.COMP, scope);

                Assert.assertEquals(DataSource.class.getName(), resRef.getType());
                if (resRef.getName().startsWith("explicit"))
                {
                    Assert.assertEquals(resRef.toString(), ResourceConfig.AUTH_APPLICATION, resRef.getAuth());
                    Assert.assertEquals(resRef.toString(), ResourceConfig.SHARING_SCOPE_UNSHAREABLE, resRef.getSharingScope());
                }
                else
                {
                    Assert.assertEquals(resRef.toString(), ResourceConfig.AUTH_CONTAINER, resRef.getAuth());
                    Assert.assertEquals(resRef.toString(), ResourceConfig.SHARING_SCOPE_SHAREABLE, resRef.getSharingScope());
                }
                Assert.assertEquals("binding", resRef.getJNDIName());
                Assert.assertEquals(Connection.TRANSACTION_NONE, resRef.getIsolationLevel());
                Assert.assertEquals(0, resRef.getCommitPriority());
                Assert.assertEquals(ResourceRefInfo.BRANCH_COUPLING_UNSET, resRef.getBranchCoupling());
                return TestHelper.IndirectJndiLookupObjectFactory.createReference(resRef.getJNDIName(), DataSource.class.getName());
            }
        });

        helper
                        .setClassLoader()
                        .addResourceRef(new ResourceRefImpl("defaultXML",
                                        null,
                                        ResourceRef.AUTH_UNSPECIFIED,
                                        ResourceRef.SHARING_SCOPE_UNSPECIFIED,
                                        null,
                                        TestHelper.createInjectionTarget(TestDataSource.class, "ivDefaultXML")))
                        .addResourceRef(new ResourceRefImpl("explicitXML",
                                        DataSource.class,
                                        ResourceRef.AUTH_APPLICATION,
                                        ResourceRef.SHARING_SCOPE_UNSHAREABLE,
                                        null,
                                        TestHelper.createInjectionTarget(TestDataSource.class, "ivExplicitXML")))
                        .addResourceRefBinding("defaultAnnotation", "binding")
                        .addResourceRefBinding("defaultXML", "binding")
                        .addResourceRefBinding("explicitAnnotation", "binding")
                        .addResourceRefBinding("explicitXML", "binding")
                        .addIndirectJndiLookupValue("binding", TestHelper.createProxyInstance(DataSource.class))
                        .processAndInject(instance);
        Assert.assertNotNull(instance.ivDefaultAnnotation);
        Assert.assertNotNull(instance.ivExplicitAnnotation);
        Assert.assertNotNull(instance.ivDefaultXML);
        Assert.assertNotNull(instance.ivExplicitXML);
    }

    public static class TestDataSource
    {
        @Resource(name = "defaultAnnotation")
        DataSource ivDefaultAnnotation;

        @Resource(
                  name = "explicitAnnotation",
                  authenticationType = Resource.AuthenticationType.APPLICATION,
                  shareable = false)
        DataSource ivExplicitAnnotation;

        DataSource ivDefaultXML;
        DataSource ivExplicitXML;
    }

    @Test
    public void testAutoLink()
    {
        TestAutoLink instance = new TestAutoLink();
        new TestHelper()
                        .setClassLoader()
                        .setTestResAutoLinkReferenceFactory(new ResAutoLinkReferenceFactory()
                        {
                            @Override
                            public Reference createResAutoLinkReference(ResourceInfo resourceInfo)
                            {
                                Assert.assertEquals(TestAutoLinkValue.class.getName(), resourceInfo.getType());
                                String refJndiName = resourceInfo.getName();
                                Resource.AuthenticationType authType = resourceInfo.getAuthenticationType();
                                if (refJndiName.equals("container"))
                                {
                                    Assert.assertEquals(Resource.AuthenticationType.CONTAINER, authType);
                                }
                                else if (refJndiName.equals("application"))
                                {
                                    Assert.assertEquals(Resource.AuthenticationType.APPLICATION, authType);
                                }
                                else
                                {
                                    Assert.fail(refJndiName);
                                }
                                return TestHelper.IndirectJndiLookupObjectFactory.createReference("binding", TestAutoLinkValue.class.getName());
                            }
                        })
                        .addIndirectJndiLookupValue("binding", new TestAutoLinkValue())
                        .processAndInject(instance);
        Assert.assertNotNull(instance.ivContainer);
    }

    public static class TestAutoLink
    {
        @Resource(name = "container")
        TestAutoLinkValue ivContainer;
        @Resource(name = "application", authenticationType = Resource.AuthenticationType.APPLICATION)
        TestAutoLinkValue ivApplication;
    }

    public static class TestAutoLinkValue { /* empty */}

    private static TestHelper createMergeSavedTestHelper()
    {
        return new TestHelper()
                        .setTestResRefReferenceFactory(new ResRefReferenceFactory()
                        {
                            @Override
                            public Reference createResRefJndiLookup(ComponentNameSpaceConfiguration compNSConfig,
                                                                    InjectionScope scope,
                                                                    ResourceRefInfo resRef)
                                            throws InjectionException
                            {
                                return TestHelper.IndirectJndiLookupObjectFactory.createReference("binding", DataSource.class.getName());
                            }
                        })
                        .setTestResAutoLinkReferenceFactory(new ResAutoLinkReferenceFactory()
                        {
                            @Override
                            public Reference createResAutoLinkReference(ResourceInfo resourceInfo)
                            {
                                return TestHelper.IndirectJndiLookupObjectFactory.createReference("binding", DataSource.class.getName());
                            }
                        });
    }

    private static InjectionBinding<?> getMergeSavedInjectionBinding(Class<?> klass)
                    throws InjectionException
    {
        return createMergeSavedTestHelper()
                        .addInjectionClass(klass)
                        .processAndGetInjectionBinding();
    }

    private static InjectionBinding<?> getMergeSavedInjectionBinding(Class<?> klass, String resourceRefName, ResourceRefConfigUpdater updater)
                    throws InjectionException
    {
        TestHelper helper = createMergeSavedTestHelper()
                        .addInjectionClass(klass);

        ResourceRefConfigList resourceRefConfigList = helper.createResourceRefConfigList();
        updater.update(resourceRefConfigList.findOrAddByName(resourceRefName));

        return helper.processAndGetInjectionBinding();
    }

    private static InjectionBinding<?> getMergeSavedInjectionBinding(ResourceRef resourceRef)
                    throws InjectionException
    {
        return createMergeSavedTestHelper()
                        .addResourceRef(resourceRef)
                        .processAndGetInjectionBinding();
    }

    private interface ResourceRefConfigUpdater
    {
        void update(ResourceRefConfig resourceRefConfig);
    }

    private static ResourceRefConfigUpdater createResourceRefConfigUpdater(final String methodName, final Object... params)
    {
        return new ResourceRefConfigUpdater()
        {
            @Override
            public void update(ResourceRefConfig resourceRefConfig)
            {
                Class<?>[] paramTypes = params.length == 2 ? new Class<?>[] { String.class, String.class } :
                                params[0].getClass() == Integer.class ? new Class<?>[] { int.class } :
                                                new Class<?>[] { params[0].getClass() };
                try
                {
                    ResourceRefConfig.class.getMethod(methodName, paramTypes).invoke(resourceRefConfig, params);
                } catch (Exception ex)
                {
                    throw new Error(ex);
                }
            }
        };
    }

    @Test
    public void testMergeSavedDefault()
                    throws Exception
    {
        InjectionBinding<?> annBinding = getMergeSavedInjectionBinding(TestMergeSavedDefault.class);
        TestHelper.mergeSaved(annBinding, annBinding);

        InjectionBinding<?> xmlBinding = getMergeSavedInjectionBinding(
                        new ResourceRefImpl("ds", DataSource.class, ResourceRef.AUTH_UNSPECIFIED, ResourceRef.SHARING_SCOPE_UNSPECIFIED, null));
        TestHelper.mergeSaved(xmlBinding, xmlBinding);

        TestHelper.mergeSaved(annBinding, xmlBinding);
    }

    @Resource(name = "ds", type = DataSource.class)
    public static class TestMergeSavedDefault { /* empty */}

    // TODO testMergeSavedResType
    // TODO testMergeSavedResAuth
    // TODO testMergeSavedResSharingScope

    @Test
    public void testMergeSavedLookup()
                    throws Exception
    {
        InjectionBinding<?> annBinding = getMergeSavedInjectionBinding(TestMergeSavedLookup.class);
        TestHelper.mergeSaved(annBinding, annBinding);

        InjectionBinding<?> xmlBinding = getMergeSavedInjectionBinding(
                        new ResourceRefImpl("ds", DataSource.class, ResourceRef.AUTH_UNSPECIFIED, ResourceRef.SHARING_SCOPE_UNSPECIFIED, "lookup"));
        TestHelper.mergeSaved(xmlBinding, xmlBinding);

        TestHelper.mergeSaved(annBinding, xmlBinding);
    }

    @Resource(name = "ds", type = DataSource.class, lookup = "lookup")
    public static class TestMergeSavedLookup { /* empty */}

    // TODO testMergeSavedBinding

    @Test
    public void testMergeSavedExtensions()
                    throws Exception
    {
        InjectionBinding<?> binding = getMergeSavedInjectionBinding(TestMergeSavedLookup.class, "ds", new ResourceRefConfigUpdater()
        {
            @Override
            public void update(ResourceRefConfig resourceRefConfig)
            {
                resourceRefConfig.setLoginConfigurationName("lcn");
                resourceRefConfig.addLoginProperty("property", "value");
                resourceRefConfig.setIsolationLevel(Connection.TRANSACTION_READ_COMMITTED);
                resourceRefConfig.setCommitPriority(1);
                resourceRefConfig.setBranchCoupling(ResourceRefConfig.BRANCH_COUPLING_LOOSE);
            }
        });
        TestHelper.mergeSaved(binding, binding);
    }

    @Resource(name = "ds", type = DataSource.class)
    public static class TestMergeSavedExtensions { /* empty */}

    private static InjectionBinding<?> getMergeSavedConflictExtensionInjectionBinding()
                    throws InjectionException
    {
        return getMergeSavedInjectionBinding(TestMergeSavedConflict.class);
    }

    private static InjectionBinding<?> getMergeSavedConflictExtensionInjectionBinding(String methodName, Object... args)
                    throws InjectionException
    {
        return getMergeSavedInjectionBinding(TestMergeSavedConflict.class, "ds", createResourceRefConfigUpdater(methodName, args));
    }

    @Test
    public void testMergeSavedConflict()
                    throws Exception
    {
        // TODO - res-type
        // TODO - res-auth
        // TODO - res-sharing-scope

        TestHelper.mergeSavedFail(getMergeSavedConflictExtensionInjectionBinding(),
                                  getMergeSavedConflictExtensionInjectionBinding("setLoginConfigurationName", "lcn1"));

        TestHelper.mergeSavedFail(getMergeSavedConflictExtensionInjectionBinding(),
                                  getMergeSavedConflictExtensionInjectionBinding("addLoginProperty", "name", "value"));
        TestHelper.mergeSavedFail(getMergeSavedConflictExtensionInjectionBinding("addLoginProperty", "name", "value"),
                                  getMergeSavedConflictExtensionInjectionBinding());
        TestHelper.mergeSavedFail(getMergeSavedConflictExtensionInjectionBinding("addLoginProperty", "name", "value1"),
                                  getMergeSavedConflictExtensionInjectionBinding("addLoginProperty", "nam1", "value2"));
        TestHelper.mergeSavedFail(getMergeSavedConflictExtensionInjectionBinding("addLoginProperty", "name1", "value"),
                                  getMergeSavedConflictExtensionInjectionBinding("addLoginProperty", "name2", "value"));
        TestHelper.mergeSavedFail(getMergeSavedInjectionBinding(TestMergeSavedConflict.class, "ds", new ResourceRefConfigUpdater()
        {
            @Override
            public void update(ResourceRefConfig resourceRefConfig)
            {
                resourceRefConfig.addLoginProperty("name1", "value1");
                resourceRefConfig.addLoginProperty("name2", "value2");
            }
        }),
                                  getMergeSavedConflictExtensionInjectionBinding("addLoginProperty", "name2", "value2"));

        TestHelper.mergeSavedFail(getMergeSavedConflictExtensionInjectionBinding(),
                                  getMergeSavedConflictExtensionInjectionBinding("setIsolationLevel", Connection.TRANSACTION_READ_COMMITTED));
        TestHelper.mergeSavedFail(getMergeSavedConflictExtensionInjectionBinding("setIsolationLevel", Connection.TRANSACTION_READ_COMMITTED),
                                  getMergeSavedConflictExtensionInjectionBinding("setIsolationLevel", Connection.TRANSACTION_READ_UNCOMMITTED));

        TestHelper.mergeSavedFail(getMergeSavedConflictExtensionInjectionBinding(),
                                  getMergeSavedConflictExtensionInjectionBinding("setCommitPriority", 1));

        TestHelper.mergeSavedFail(getMergeSavedConflictExtensionInjectionBinding(),
                                  getMergeSavedConflictExtensionInjectionBinding("setBranchCoupling", ResourceRefConfig.BRANCH_COUPLING_LOOSE));
        TestHelper.mergeSavedFail(getMergeSavedConflictExtensionInjectionBinding(),
                                  getMergeSavedConflictExtensionInjectionBinding("setBranchCoupling", ResourceRefConfig.BRANCH_COUPLING_TIGHT));
        TestHelper.mergeSavedFail(getMergeSavedConflictExtensionInjectionBinding("setBranchCoupling", ResourceRefConfig.BRANCH_COUPLING_LOOSE),
                                  getMergeSavedConflictExtensionInjectionBinding("setBranchCoupling", ResourceRefConfig.BRANCH_COUPLING_TIGHT));
    }

    @Resource(name = "ds", type = DataSource.class)
    public static class TestMergeSavedConflict { /* empty */}
}
