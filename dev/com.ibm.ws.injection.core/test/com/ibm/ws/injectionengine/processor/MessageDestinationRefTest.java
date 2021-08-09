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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.ws.container.service.metadata.internal.J2EENameImpl;
import com.ibm.ws.injectionengine.TestHelper;
import com.ibm.ws.javaee.dd.common.InjectionTarget;
import com.ibm.ws.javaee.dd.common.MessageDestinationRef;
import com.ibm.wsspi.injectionengine.InjectionEngineAccessor;

public class MessageDestinationRefTest
{
    private static class MessageDestinationRefImpl
                    extends TestHelper.AbstractResourceGroup
                    implements MessageDestinationRef
    {
        private final Class<?> ivType;
        private final int ivUsage;
        private final String ivLink;

        MessageDestinationRefImpl(String name,
                                  Class<?> type,
                                  int usage,
                                  String link,
                                  String lookup,
                                  InjectionTarget... injectionTargets)
        {
            super(name, lookup, Arrays.asList(injectionTargets));
            ivType = type;
            ivUsage = usage;
            ivLink = link;
        }

        @Override
        public String getType()
        {
            return ivType == null ? null : ivType.getName();
        }

        @Override
        public String getLink()
        {
            return ivLink;
        }

        @Override
        public int getUsageValue()
        {
            return ivUsage;
        }
    }

    public static class TestDestination { /* empty */}

    @Test
    @Ignore
    public void testLink()
    {
        Map<String, Map<String, Map<String, String>>> links =
                        InjectionEngineAccessor.getMessageDestinationLinkInstance().getMessageDestinationLinks();
        Map<String, Map<String, String>> appLinks = new HashMap<String, Map<String, String>>();
        Map<String, String> modLinks = new HashMap<String, String>();
        Map<String, String> crossModuleLinks = new HashMap<String, String>();
        Map<String, String> sameAppModuleLinks = new HashMap<String, String>();

        modLinks.put("sameModule", "sameModuleBinding");
        crossModuleLinks.put("crossModule", "crossModuleBinding");
        sameAppModuleLinks.put("sameApp", "sameAppBinding");
        appLinks.put("testmod.jar", modLinks);
        appLinks.put("crossModule.jar", crossModuleLinks);
        appLinks.put("sameAppModule.jar", sameAppModuleLinks);
        links.put("testapp", appLinks);

        TestDestination sameModuleValue = new TestDestination();
        TestDestination crossModuleValue = new TestDestination();
        TestDestination sameAppValue = new TestDestination();

        TestLink instance = new TestLink();
        new TestHelper("test", new J2EENameImpl("testapp", "testmod.jar", "test"))
                        .setClassLoader()
                        //                        .setTestResAutoLinkReferenceFactory(new ResAutoLinkReferenceFactory()
                        //                        {
                        //                            @Override
                        //                            public Reference createResAutoLinkReference(ResourceInfo resourceInfo)
                        //                            {
                        //                                MessageDestinationInfo info = new MessageDestinationInfo(resourceInfo.getApplication(),
                        //                                                resourceInfo.getModule(),
                        //                                                resourceInfo.getLink());
                        //                                return new Reference(resourceInfo.getType(),
                        //                                                new MessageDestinationInfoRefAddr(info),
                        //                                                MessageDestinationObjectFactory.class.getName(), null); // d710771.1
                        //                            }
                        //                        })
                        .addMsgDestRef(new MessageDestinationRefImpl("sameModuleRef",
                                        TestDestination.class,
                                        MessageDestinationRef.USAGE_UNSPECIFIED,
                                        "sameModule",
                                        null,
                                        TestHelper.createInjectionTarget(TestLink.class, "ivSameModule")))
                        .addMsgDestRef(new MessageDestinationRefImpl("crossModuleRef",
                                        TestDestination.class,
                                        MessageDestinationRef.USAGE_UNSPECIFIED,
                                        "crossModule.jar#crossModule",
                                        null,
                                        TestHelper.createInjectionTarget(TestLink.class, "ivCrossModule")))
                        .addMsgDestRef(new MessageDestinationRefImpl("sameAppRef",
                                        TestDestination.class,
                                        MessageDestinationRef.USAGE_UNSPECIFIED,
                                        "sameApp",
                                        null,
                                        TestHelper.createInjectionTarget(TestLink.class, "ivSameApp")))
                        .addIndirectJndiLookupValue("sameModuleBinding", sameModuleValue)
                        .addIndirectJndiLookupValue("crossModuleBinding", crossModuleValue)
                        .addIndirectJndiLookupValue("sameAppBinding", sameAppValue)
                        .processAndInject(instance);

        Assert.assertEquals(sameModuleValue, instance.ivSameModule);
        Assert.assertEquals(crossModuleValue, instance.ivCrossModule);
        Assert.assertEquals(sameAppValue, instance.ivSameApp);
    }

    public static class TestLink
    {
        TestDestination ivSameModule;
        TestDestination ivCrossModule;
        TestDestination ivSameApp;
    }

    @Test
    public void testLookup()
    {
        TestLookup instance = new TestLookup();
        new TestHelper()
                        .setClassLoader()
                        .setTestResRefReferenceFactory()
                        .addMsgDestRef(new MessageDestinationRefImpl("xml",
                                        TestDestination.class,
                                        MessageDestinationRef.USAGE_CONSUMES,
                                        null,
                                        "lookup",
                                        TestHelper.createInjectionTarget(TestLookup.class, "ivXML")))
                        .addIndirectJndiLookupValue("lookup", new TestDestination())
                        .processAndInject(instance);
        Assert.assertNotNull(instance.ivAnnotation);
        Assert.assertNotNull(instance.ivXML);
    }

    public static class TestLookup
    {
        @Resource(lookup = "lookup")
        TestDestination ivAnnotation;
        TestDestination ivXML;
    }

    @Test
    public void testBinding()
    {
        TestBinding instance = new TestBinding();
        new TestHelper()
                        .setClassLoader()
                        .addMsgDestRef(new MessageDestinationRefImpl("xml",
                                        TestDestination.class,
                                        MessageDestinationRef.USAGE_CONSUMES,
                                        null,
                                        null,
                                        TestHelper.createInjectionTarget(TestBinding.class, "ivXML")))
                        .addMsgDestRefBinding("annotation", "binding")
                        .addMsgDestRefBinding("xml", "binding")
                        .addIndirectJndiLookupValue("binding", new TestDestination())
                        .processAndInject(instance);
        Assert.assertNotNull(instance.ivAnnotation);
        Assert.assertNotNull(instance.ivXML);
    }

    public static class TestBinding
    {
        @Resource(name = "annotation")
        TestDestination ivAnnotation;
        TestDestination ivXML;
    }
}
