/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.suite;

import java.nio.file.Files; 
import java.nio.file.StandardCopyOption; 
import java.nio.file.attribute.FileAttribute; 
import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import componenttest.rules.FeatureReplacementAction;
import componenttest.rules.RepeatTests;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.ibm.websphere.simplicity.ShrinkHelper;

import com.ibm.ws.cdi12.fat.tests.AfterTypeDiscoveryTest;
import com.ibm.ws.cdi12.fat.tests.AlterableContextTest;
import com.ibm.ws.cdi12.fat.tests.AppClientAdvancedTest;
import com.ibm.ws.cdi12.fat.tests.AppClientSecurityTest;
import com.ibm.ws.cdi12.fat.tests.AppClientTest;
import com.ibm.ws.cdi12.fat.tests.AppExtensionTest;
import com.ibm.ws.cdi12.fat.tests.AroundConstructBeanTest;
import com.ibm.ws.cdi12.fat.tests.AroundConstructEjbTest;
import com.ibm.ws.cdi12.fat.tests.BeanDiscoveryModeNoneTest;
import com.ibm.ws.cdi12.fat.tests.BeanLifecycleTest;
import com.ibm.ws.cdi12.fat.tests.BeanManagerLookupTest;
import com.ibm.ws.cdi12.fat.tests.CDI12ExtensionTest;
import com.ibm.ws.cdi12.fat.tests.CDI12JNDIBeanManagerTest;
import com.ibm.ws.cdi12.fat.tests.CDI12WebServicesTest;
import com.ibm.ws.cdi12.fat.tests.CDICurrentTest;
import com.ibm.ws.cdi12.fat.tests.CDIManagedBeanInterceptorTest;
import com.ibm.ws.cdi12.fat.tests.ClassExclusionTest;
import com.ibm.ws.cdi12.fat.tests.ClassLoadPrereqLogger;
import com.ibm.ws.cdi12.fat.tests.ClassMaskingTest;
import com.ibm.ws.cdi12.fat.tests.ConversationFilterTest;
import com.ibm.ws.cdi12.fat.tests.DecoratorOnBuiltInBeansTest;
import com.ibm.ws.cdi12.fat.tests.DeltaSpikeSchedulerTest;
import com.ibm.ws.cdi12.fat.tests.DisablingBeansXmlValidationTest;
import com.ibm.ws.cdi12.fat.tests.DynamicBeanExtensionTest;
import com.ibm.ws.cdi12.fat.tests.EJB32Test;
import com.ibm.ws.cdi12.fat.tests.EjbConstructorInjectionTest;
import com.ibm.ws.cdi12.fat.tests.EjbDiscoveryTest;
import com.ibm.ws.cdi12.fat.tests.EjbMiscTest;
import com.ibm.ws.cdi12.fat.tests.EjbTimerTest;
import com.ibm.ws.cdi12.fat.tests.EmptyCDITest;
import com.ibm.ws.cdi12.fat.tests.EnablingBeansXmlValidationTest;
import com.ibm.ws.cdi12.fat.tests.EventMetaDataTest;
import com.ibm.ws.cdi12.fat.tests.GloballyEnableUsingPriorityTest;
import com.ibm.ws.cdi12.fat.tests.InjectInjectionPointTest;
import com.ibm.ws.cdi12.fat.tests.InjectParameterTest;
import com.ibm.ws.cdi12.fat.tests.JEEInjectionTargetTest;
import com.ibm.ws.cdi12.fat.tests.JNDILookupTest;
import com.ibm.ws.cdi12.fat.tests.JarInRarTest;
import com.ibm.ws.cdi12.fat.tests.MultiModuleAppTest;
import com.ibm.ws.cdi12.fat.tests.MultipleBeansXmlTest;
import com.ibm.ws.cdi12.fat.tests.MultipleNamedEJBTest;
import com.ibm.ws.cdi12.fat.tests.NonContextualInjectionPointTest;
import com.ibm.ws.cdi12.fat.tests.NonContextualTests;
import com.ibm.ws.cdi12.fat.tests.ObservesInitializedTest;
import com.ibm.ws.cdi12.fat.tests.PackagePrivateAccessTest;
import com.ibm.ws.cdi12.fat.tests.PassivationBeanTests;
import com.ibm.ws.cdi12.fat.tests.RootClassLoaderTest;
import com.ibm.ws.cdi12.fat.tests.SessionDestroyTests;
import com.ibm.ws.cdi12.fat.tests.SharedLibraryTest;
import com.ibm.ws.cdi12.fat.tests.SimpleJSFTest;
import com.ibm.ws.cdi12.fat.tests.SimpleJSFWithSharedLibTest;
import com.ibm.ws.cdi12.fat.tests.SimpleJSPTest;
import com.ibm.ws.cdi12.fat.tests.StatefulSessionBeanInjectionTest;
import com.ibm.ws.cdi12.fat.tests.ValidatorInJarTest;
import com.ibm.ws.cdi12.fat.tests.VisTest;
import com.ibm.ws.cdi12.fat.tests.WarLibsAccessWarBeansTest;
import com.ibm.ws.cdi12.fat.tests.WebBeansBeansXmlInWeldTest;
import com.ibm.ws.cdi12.fat.tests.WithAnnotationsTest;
import com.ibm.ws.cdi12.fat.tests.implicit.ImplicitBeanArchiveNoAnnotationsTest;
import com.ibm.ws.cdi12.fat.tests.implicit.ImplicitBeanArchiveTest;
import com.ibm.ws.cdi12.fat.tests.implicit.ImplicitBeanArchivesDisabledTest;
import com.ibm.ws.cdi12.fat.tests.implicit.ImplicitWarLibJarsTest;
import com.ibm.ws.cdi12.fat.tests.implicit.ImplicitWarTest;
import com.ibm.ws.fat.util.FatLogHandler;

/**
 * Tests that run on CDI 1.2 and again on CDI 2.0
 */
@RunWith(Suite.class)
@SuiteClasses({

                AfterTypeDiscoveryTest.class,
                AlterableContextTest.class,
                AroundConstructBeanTest.class,
                AroundConstructEjbTest.class,
                AppClientTest.class,
                AppClientAdvancedTest.class,
                AppClientSecurityTest.class,
                AppExtensionTest.class,
                BeanDiscoveryModeNoneTest.class,
                BeanLifecycleTest.class,
                CDI12ExtensionTest.class,
                CDI12WebServicesTest.class,
                CDICurrentTest.class,
                ClassExclusionTest.class,
                ClassMaskingTest.class,
                ConversationFilterTest.class,
                DecoratorOnBuiltInBeansTest.class,
                DeltaSpikeSchedulerTest.class,
                DisablingBeansXmlValidationTest.class,
                DynamicBeanExtensionTest.class,
                EjbConstructorInjectionTest.class,
                EjbDiscoveryTest.class,
                EjbMiscTest.class,
                EjbTimerTest.class,
                EmptyCDITest.class,
                EnablingBeansXmlValidationTest.class,
                EventMetaDataTest.class,
                GloballyEnableUsingPriorityTest.class,
                ImplicitBeanArchiveNoAnnotationsTest.class,
                ImplicitBeanArchiveTest.class,
                ImplicitWarLibJarsTest.class,
                ImplicitWarTest.class,
                InjectInjectionPointTest.class,
                InjectParameterTest.class,
                JarInRarTest.class,
                JEEInjectionTargetTest.class,
                JNDILookupTest.class,
                CDIManagedBeanInterceptorTest.class,
                MultipleBeansXmlTest.class,
                MultipleNamedEJBTest.class,
                NonContextualInjectionPointTest.class,
                PackagePrivateAccessTest.class,
                PassivationBeanTests.class,
                SharedLibraryTest.class,
                SimpleJSFTest.class,
                SimpleJSFWithSharedLibTest.class,
                StatefulSessionBeanInjectionTest.class,
                ValidatorInJarTest.class,

                
})
public class CDI20Suite {

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE8_FEATURES);

}
