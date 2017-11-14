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

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.BeforeClass;
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
 * Tests specific to cdi-1.2
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
                BeanManagerLookupTest.class,
                CDI12JNDIBeanManagerTest.class,
                CDI12ExtensionTest.class,
                CDI12WebServicesTest.class,
                CDICurrentTest.class,
                ClassExclusionTest.class,
                ClassLoadPrereqLogger.class,
                ClassMaskingTest.class,
                ConversationFilterTest.class,
                DecoratorOnBuiltInBeansTest.class,
                DeltaSpikeSchedulerTest.class,
                DisablingBeansXmlValidationTest.class,
                DynamicBeanExtensionTest.class,
                EjbConstructorInjectionTest.class,
                EjbDiscoveryTest.class,
                EjbMiscTest.class,
                EJB32Test.class,
                EjbTimerTest.class,
                EmptyCDITest.class,
                EnablingBeansXmlValidationTest.class,
                EventMetaDataTest.class,
                GloballyEnableUsingPriorityTest.class,
                ImplicitBeanArchivesDisabledTest.class,
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
                MultiModuleAppTest.class,
                MultipleNamedEJBTest.class,
                NonContextualInjectionPointTest.class,
                NonContextualTests.class,
                ObservesInitializedTest.class,
                PackagePrivateAccessTest.class,
                PassivationBeanTests.class,
                RootClassLoaderTest.class,
                SessionDestroyTests.class,
                SharedLibraryTest.class,
                SessionDestroyTests.class,
                SimpleJSFTest.class,
                SimpleJSFWithSharedLibTest.class,
                SimpleJSPTest.class,
                StatefulSessionBeanInjectionTest.class,
                ValidatorInJarTest.class,
                VisTest.class,
                WarLibsAccessWarBeansTest.class,
                WebBeansBeansXmlInWeldTest.class,
                WithAnnotationsTest.class
})
public class FATSuite {
    
    private static final Logger LOG = Logger.getLogger(FATSuite.class.getName());
    private static Map<String,List<Archive>> serversToApps = new HashMap<String,List<Archive>>();

    static{
        
        JavaArchive jarInRar172 = ShrinkWrap.create(JavaArchive.class,"jarInRar.jar")
                        .addClass("com.ibm.ws.cdi12.fat.jarinrar.rar.Amigo")
                        .addClass("com.ibm.ws.cdi12.fat.jarinrar.rar.TestResourceAdapter")
                        .add(new FileAsset(new File("test-applications/jarInRar.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");

        JavaArchive jarInRarEjb173 = ShrinkWrap.create(JavaArchive.class,"jarInRarEjb.jar")
                        .addClass("com.ibm.ws.cdi12.fat.jarinrar.ejb.MySingletonStartupBean")
                        .add(new FileAsset(new File("test-applications/jarInRarEjb.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml")
                        .addAsManifestResource(new File("test-applications/jarInRarEjb.jar/resources/META-INF/MANIFEST.MF"));     

        //This will do until we have gradle code to build bundles. 
        JavaArchive helloWorldBundle = ShrinkWrap.create(ZipImporter.class,"cdi.helloworld.extension_1.0.0.jar")
                        .importFrom(new File("test-applications/PreBuildArchives.jar/resources/cdi.helloworld.extension_1.0.0.jar"))
                        .as(JavaArchive.class);        
        JavaArchive rootClassLoaderExtension109 = ShrinkWrap.create(JavaArchive.class,"rootClassLoaderExtension.jar")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.RandomBean")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.OSName")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.OSNameBean")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.DefaultLiteral")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.MyExtension")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.TimerBean")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.extension.OSNameLiteral")
                        .add(new FileAsset(new File("test-applications/rootClassLoaderExtension.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");
        JavaArchive cdiCurrentTest129 = ShrinkWrap.create(JavaArchive.class,"cdiCurrentTest.jar")
                        .addClass("com.ibm.ws.cdi12.test.current.extension.CDICurrentTestBean")
                        .addClass("com.ibm.ws.cdi12.test.current.extension.MyDeploymentVerifier")
                        .addClass("com.ibm.ws.cdi12.test.current.extension.DefaultLiteral")
                        .addClass("com.ibm.ws.cdi12.test.current.extension.CDICurrent")
                        .add(new FileAsset(new File("test-applications/cdiCurrentTest.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");
        JavaArchive visTestWarAppClientLib48 = ShrinkWrap.create(JavaArchive.class,"visTestWarAppClientLib.jar")
                        .addClass("vistest.warAppClientLib.WarAppClientLibTestingBean")
                        .addClass("vistest.warAppClientLib.WarAppClientLibTargetBean");
        JavaArchive visTestAppClientAsAppClientLib169 = ShrinkWrap.create(JavaArchive.class,"visTestAppClientAsAppClientLib.jar")
                        .addClass("vistest.appClientAsAppClientLib.dummy.DummyMain")
                        .addClass("vistest.appClientAsAppClientLib.AppClientAsAppClientLibTargetBean")
                        .addClass("vistest.appClientAsAppClientLib.AppClientAsAppClientLibTestingBean");
        JavaArchive multiModuleAppLib334 = ShrinkWrap.create(JavaArchive.class,"multiModuleAppLib3.jar")
                        .addClass("com.ibm.ws.cdi12.test.lib3.BasicBean3A")
                        .addClass("com.ibm.ws.cdi12.test.lib3.BasicBean3")
                        .addClass("com.ibm.ws.cdi12.test.lib3.CustomNormalScoped");
        JavaArchive warLibAccessBeansInWarJar32 = ShrinkWrap.create(JavaArchive.class,"warLibAccessBeansInWarJar.jar")
                        .addClass("com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar.TestInjectionClass")
                        .addClass("com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar.WarBeanInterface")
                        .add(new FileAsset(new File("test-applications/warLibAccessBeansInWarJar.jar/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        JavaArchive utilLib79 = ShrinkWrap.create(JavaArchive.class,"utilLib.jar")
                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableListImpl")
                        .addClass("com.ibm.ws.cdi12.test.utils.Intercepted")
                        .addClass("com.ibm.ws.cdi12.test.utils.ChainableList")
                        .addClass("com.ibm.ws.cdi12.test.utils.Utils")
                        .addClass("com.ibm.ws.cdi12.test.utils.SimpleAbstract")
                        .addClass("com.ibm.ws.cdi12.test.utils.ForwardingList")
                        .add(new FileAsset(new File("test-applications/utilLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive visTestAppClient73 = ShrinkWrap.create(JavaArchive.class,"visTestAppClient.jar")
                        .addAsManifestResource(new File("test-applications/visTestAppClient.jar/resources/META-INF/MANIFEST.MF"))
                        .addClass("vistest.appClient.main.Main")
                        .addClass("vistest.appClient.AppClientTargetBean")
                        .addClass("vistest.appClient.AppClientTestingBean");
        JavaArchive visTestAppClientAsWarLib68 = ShrinkWrap.create(JavaArchive.class,"visTestAppClientAsWarLib.jar")
                        .addClass("vistest.appClientAsWarLib.dummy.DummyMain")
                        .addClass("vistest.appClientAsWarLib.AppClientAsWarLibTargetBean")
                        .addClass("vistest.appClientAsWarLib.AppClientAsWarLibTestingBean");
        JavaArchive maskedClassEjb31 = ShrinkWrap.create(JavaArchive.class,"maskedClassEjb.jar")
                        .add(new FileAsset(new File("build/classes/test/Type1RenameMeWhenImportingToShrinkwrap.class")), "/test/Type1.class")
                        .addClass("beans.SessionBean1");
        JavaArchive visTestEjb10 = ShrinkWrap.create(JavaArchive.class,"visTestEjb.jar")
                        .addClass("vistest.ejb.dummy.DummySessionBean")
                        .addClass("vistest.ejb.EjbTargetBean")
                        .addClass("vistest.ejb.EjbTestingBean")
                        .addAsManifestResource(new File("test-applications/visTestEjb.jar/resources/META-INF/MANIFEST.MF"));
        JavaArchive multipleWarEmbeddedJar18 = ShrinkWrap.create(JavaArchive.class,"multipleWarEmbeddedJar.jar")
                        .addClass("com.ibm.ws.cdi.lib.MyEjb")
                        .add(new FileAsset(new File("test-applications/multipleWarEmbeddedJar.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive multiModuleAppLib164 = ShrinkWrap.create(JavaArchive.class,"multiModuleAppLib1.jar")
                        .addClass("com.ibm.ws.cdi12.test.lib1.BasicBean1")
                        .addClass("com.ibm.ws.cdi12.test.lib1.BasicBean1A")
                        .add(new FileAsset(new File("test-applications/multiModuleAppLib1.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive visTestEjbAppClientLib50 = ShrinkWrap.create(JavaArchive.class,"visTestEjbAppClientLib.jar")
                        .addClass("vistest.ejbAppClientLib.EjbAppClientLibTestingBean")
                        .addClass("vistest.ejbAppClientLib.EjbAppClientLibTargetBean");
        JavaArchive HelloAppClient116 = ShrinkWrap.create(JavaArchive.class,"HelloAppClient.jar")
                        .addClass("com.ibm.ws.clientcontainer.fat.HelloBean")
                        .addClass("com.ibm.ws.clientcontainer.fat.TestLoginCallbackHandler")
                        .addClass("com.ibm.ws.clientcontainer.fat.HelloAppClient")
                        .addClass("com.ibm.ws.clientcontainer.fat.AppBean")
                        .add(new FileAsset(new File("test-applications/HelloAppClient.jar/resources/META-INF/application-client.xml")), "/META-INF/application-client.xml")
                        .add(new FileAsset(new File("test-applications/HelloAppClient.jar/resources/META-INF/MANIFEST.MF")), "/META-INF/MANIFEST.MF");
        JavaArchive dynamicallyAddedBeans83 = ShrinkWrap.create(JavaArchive.class,"dynamicallyAddedBeans.jar")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.DynamicBean1Bean")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.MyCDIExtension")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.DynamicBean2Bean")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.DynamicBean1")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.DefaultLiteral")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.DynamicBean2")
                        .add(new FileAsset(new File("test-applications/dynamicallyAddedBeans.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");
        JavaArchive globalPriorityLib119 = ShrinkWrap.create(JavaArchive.class,"globalPriorityLib.jar")
                        .addClass("com.ibm.ws.cdi12.test.priority.JarBean")
                        .addClass("com.ibm.ws.cdi12.test.priority.helpers.AbstractBean")
                        .addClass("com.ibm.ws.cdi12.test.priority.helpers.AbstractInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.priority.helpers.RelativePriority")
                        .addClass("com.ibm.ws.cdi12.test.priority.helpers.Bean")
                        .addClass("com.ibm.ws.cdi12.test.priority.helpers.AbstractDecorator")
                        .addClass("com.ibm.ws.cdi12.test.priority.JarDecorator")
                        .addClass("com.ibm.ws.cdi12.test.priority.LocalJarInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.priority.LocalJarDecorator")
                        .addClass("com.ibm.ws.cdi12.test.priority.FromJar")
                        .addClass("com.ibm.ws.cdi12.test.priority.JarInterceptor")
                        .add(new FileAsset(new File("test-applications/globalPriorityLib.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive alterableContextExtension5 = ShrinkWrap.create(JavaArchive.class,"alterableContextExtension.jar")
                        .addClass("com.ibm.ws.cdi12.alterablecontext.test.extension.DirtySingleton")
                        .addClass("com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextBean")
                        .addClass("com.ibm.ws.cdi12.alterablecontext.test.extension.AlterableContextExtension")
                        .add(new FileAsset(new File("test-applications/alterableContextExtension.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension")
                        .add(new FileAsset(new File("test-applications/alterableContextExtension.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive visTestEjbWarLib142 = ShrinkWrap.create(JavaArchive.class,"visTestEjbWarLib.jar")
                        .addClass("vistest.ejbWarLib.EjbWarLibTestingBean")
                        .addClass("vistest.ejbWarLib.EjbWarLibTargetBean");
        JavaArchive ObservesInitializedInJarsWebInfJar12 = ShrinkWrap.create(JavaArchive.class,"ObservesInitializedInJarsWebInfJar.jar")
                        .addClass("cdi12.observersinjars.webinf.WebInfAutostartObserver")
                        .addClass("cdi12.observersinjars.webinf.SomeClass");
        JavaArchive visTestFramework41 = ShrinkWrap.create(JavaArchive.class,"visTestFramework.jar")
                        .addClass("vistest.qualifiers.InWarWebinfLib")
                        .addClass("vistest.qualifiers.InEjb")
                        .addClass("vistest.qualifiers.InEarLib")
                        .addClass("vistest.qualifiers.InEjbAsEjbLib")
                        .addClass("vistest.qualifiers.InWarAppClientLib")
                        .addClass("vistest.qualifiers.InAppClient")
                        .addClass("vistest.qualifiers.InAppClientLib")
                        .addClass("vistest.qualifiers.InNonLib")
                        .addClass("vistest.qualifiers.InAppClientAsAppClientLib")
                        .addClass("vistest.qualifiers.InEjbAppClientLib")
                        .addClass("vistest.qualifiers.InEjbAsAppClientLib")
                        .addClass("vistest.qualifiers.InAppClientAsWarLib")
                        .addClass("vistest.qualifiers.InEjbAsWarLib")
                        .addClass("vistest.qualifiers.InEjbLib")
                        .addClass("vistest.qualifiers.InWar")
                        .addClass("vistest.qualifiers.InWar2")
                        .addClass("vistest.qualifiers.InAppClientAsEjbLib")
                        .addClass("vistest.qualifiers.InWarLib")
                        .addClass("vistest.qualifiers.InEjbWarLib")
                        .addClass("vistest.framework.TargetBean")
                        .addClass("vistest.framework.VisTester")
                        .addClass("vistest.framework.TestingBean");
        JavaArchive sharedLibrary33 = ShrinkWrap.create(JavaArchive.class,"sharedLibrary.jar")
                        .addClass("com.ibm.ws.cdi12.test.shared.NonInjectedHello")
                        .addClass("com.ibm.ws.cdi12.test.shared.InjectedHello");
        JavaArchive implicitBeanArchiveDisabled49 = ShrinkWrap.create(JavaArchive.class,"implicitBeanArchiveDisabled.jar")
                        .addClass("com.ibm.ws.cdi.implicit.bean.disabled.MyPlane");
        JavaArchive visTestWarLib102 = ShrinkWrap.create(JavaArchive.class,"visTestWarLib.jar")
                        .addClass("vistest.warLib.WarLibTestingBean")
                        .addClass("vistest.warLib.WarLibTargetBean");
        JavaArchive TestValidatorInJar20 = ShrinkWrap.create(JavaArchive.class,"TestValidatorInJar.jar")
                        .addClass("com.ibm.cdi.test.basic.injection.jar.AppScopedBean");
        JavaArchive visTestEjbLib23 = ShrinkWrap.create(JavaArchive.class,"visTestEjbLib.jar")
                        .addClass("vistest.ejbLib.EjbLibTargetBean")
                        .addClass("vistest.ejbLib.EjbLibTestingBean");
        JavaArchive visTestWarWebinfLib113 = ShrinkWrap.create(JavaArchive.class,"visTestWarWebinfLib.jar")
                        .addClass("vistest.warWebinfLib.WarWebinfLibTargetBean")
                        .addClass("vistest.warWebinfLib.WarWebinfLibTestingBean");
        JavaArchive libEjb92 = ShrinkWrap.create(JavaArchive.class,"libEjb.jar")
                        .add(new FileAsset(new File("test-applications/libEjb.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive TestVetoedAlternative111 = ShrinkWrap.create(JavaArchive.class,"TestVetoedAlternative.jar")
                        .addClass("com.ibm.cdi.test.vetoed.alternative.AppScopedBean")
                        .addClass("com.ibm.cdi.test.vetoed.alternative.VetoedAlternativeBean")
                        .add(new FileAsset(new File("test-applications/TestVetoedAlternative.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive visTestNonLib101 = ShrinkWrap.create(JavaArchive.class,"visTestNonLib.jar")
                        .addClass("vistest.nonLib.NonLibTestingBean")
                        .addClass("vistest.nonLib.NonLibTargetBean");
        JavaArchive ejbArchiveWithNoAnnotations19 = ShrinkWrap.create(JavaArchive.class,"ejbArchiveWithNoAnnotations.jar")
                        .addClass("com.ibm.ws.cdi12.test.SimpleEjbBean")
                        .addClass("com.ibm.ws.cdi12.test.SimpleEjbBean2")
                        .add(new FileAsset(new File("test-applications/ejbArchiveWithNoAnnotations.jar/resources/META-INF/ejb-jar.xml")), "/META-INF/ejb-jar.xml");
        JavaArchive visTestAppClientAsEjbLib74 = ShrinkWrap.create(JavaArchive.class,"visTestAppClientAsEjbLib.jar")
                        .addClass("vistest.appClientAsEjbLib.dummy.DummyMain")
                        .addClass("vistest.appClientAsEjbLib.AppClientAsEjbLibTestingBean")
                        .addClass("vistest.appClientAsEjbLib.AppClientAsEjbLibTargetBean");
        JavaArchive ejbJarInWarNoAnnotations93 = ShrinkWrap.create(JavaArchive.class,"ejbJarInWarNoAnnotations.jar")
                        .addClass("com.ibm.ws.cdi12.test.ejbJarInWarNoAnnotations.SimpleEjbBean")
                        .addClass("com.ibm.ws.cdi12.test.ejbJarInWarNoAnnotations.SimpleEjbBean2");
        JavaArchive implicitBeanNoBeansXml135 = ShrinkWrap.create(JavaArchive.class,"implicitBeanNoBeansXml.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicitBean.NoBeansXmlBean");
        JavaArchive statefulSessionBeanInjection77 = ShrinkWrap.create(JavaArchive.class,"statefulSessionBeanInjection.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicitEJB.InjectedEJBImpl")
                        .addClass("com.ibm.ws.cdi12.test.implicitEJB.InjectedEJB")
                        .addClass("com.ibm.ws.cdi12.test.implicitEJB.InjectedBean1")
                        .addClass("com.ibm.ws.cdi12.test.implicitEJB.InjectedBean2")
                        .add(new FileAsset(new File("test-applications/statefulSessionBeanInjection.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive appClientAdvanced155 = ShrinkWrap.create(JavaArchive.class,"appClientAdvanced.jar")
                        .addClass("com.ibm.ws.cdi.client.fat.counting.impl.CountingInterceptor")
                        .addClass("com.ibm.ws.cdi.client.fat.counting.impl.CountWarningLogger")
                        .addClass("com.ibm.ws.cdi.client.fat.counting.CountBean")
                        .addClass("com.ibm.ws.cdi.client.fat.counting.CountWarning")
                        .addClass("com.ibm.ws.cdi.client.fat.counting.Counted")
                        .addClass("com.ibm.ws.cdi.client.fat.greeting.impl.GreeterBean")
                        .addClass("com.ibm.ws.cdi.client.fat.greeting.impl.FrenchGreeterBean")
                        .addClass("com.ibm.ws.cdi.client.fat.greeting.impl.PirateGreeterDecorator")
                        .addClass("com.ibm.ws.cdi.client.fat.greeting.Greeter")
                        .addClass("com.ibm.ws.cdi.client.fat.greeting.French")
                        .addClass("com.ibm.ws.cdi.client.fat.greeting.English")
                        .addClass("com.ibm.ws.cdi.client.fat.AdvancedAppClass")
                        .addClass("com.ibm.ws.cdi.client.fat.AppBean")
                        .add(new FileAsset(new File("test-applications/appClientAdvanced.jar/resources/META-INF/MANIFEST.MF")), "/META-INF/MANIFEST.MF")
                        .add(new FileAsset(new File("test-applications/appClientAdvanced.jar/resources/META-INF/application-client.xml")), "/META-INF/application-client.xml");
        JavaArchive multiModuleAppLib225 = ShrinkWrap.create(JavaArchive.class,"multiModuleAppLib2.jar")
                        .addClass("com.ibm.ws.cdi12.test.lib2.BasicBean2")
                        .add(new FileAsset(new File("test-applications/multiModuleAppLib2.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive applicationExtension35 = ShrinkWrap.create(JavaArchive.class,"applicationExtension.jar")
                        .addClass("test.PlainExtension")
                        .addClass("bean.InLibJarBean")
                        .add(new FileAsset(new File("test-applications/applicationExtension.jar/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension")
                        .add(new FileAsset(new File("test-applications/applicationExtension.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive archiveWithNoImplicitBeans130 = ShrinkWrap.create(JavaArchive.class,"archiveWithNoImplicitBeans.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicit.nobeans.ClassWithInjectButNotABean");
        JavaArchive archiveWithBeansXML112 = ShrinkWrap.create(JavaArchive.class,"archiveWithBeansXML.jar")
                        .addClass("com.ibm.ws.cdi12.test.beansXML.UnannotatedBeanInAllModeBeanArchive")
                        .add(new FileAsset(new File("test-applications/archiveWithBeansXML.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive jeeInjectionTargetTestEJB143 = ShrinkWrap.create(JavaArchive.class,"jeeInjectionTargetTestEJB.jar")
                        .addClass("cdi12.helloworld.jeeResources.ejb.MySessionBean1")
                        .addClass("cdi12.helloworld.jeeResources.ejb.interceptors.MyAnotherEJBInterceptor")
                        .addClass("cdi12.helloworld.jeeResources.ejb.interceptors.MyEJBJARXMLDefinedInterceptor")
                        .addClass("cdi12.helloworld.jeeResources.ejb.interceptors.MyManagedBeanEJBInterceptor")
                        .addClass("cdi12.helloworld.jeeResources.ejb.interceptors.MyEJBInterceptor")
                        .addClass("cdi12.helloworld.jeeResources.ejb.MyEJBDefinedInXml")
                        .addClass("cdi12.helloworld.jeeResources.ejb.ManagedBeanInterface")
                        .addClass("cdi12.helloworld.jeeResources.ejb.MyCDIBean1")
                        .addClass("cdi12.helloworld.jeeResources.ejb.SessionBeanInterface")
                        .addClass("cdi12.helloworld.jeeResources.ejb.MyManagedBean1")
                        .addClass("cdi12.helloworld.jeeResources.ejb.MySessionBean2")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestEJB.jar/resources/META-INF/ejb-jar.xml")), "/META-INF/ejb-jar.xml")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestEJB.jar/resources/META-INF/ibm-managed-bean-bnd.xml")), "/META-INF/ibm-managed-bean-bnd.xml");
        JavaArchive implicitBeanAnnotatedMode42 = ShrinkWrap.create(JavaArchive.class,"implicitBeanAnnotatedMode.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicitBean.AnnotatedModeBean")
                        .add(new FileAsset(new File("test-applications/implicitBeanAnnotatedMode.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive archiveWithAnnotatedModeBeansXML140 = ShrinkWrap.create(JavaArchive.class,"archiveWithAnnotatedModeBeansXML.jar")
                        .addClass("com.ibm.ws.cdi12.test.annotatedBeansXML.DependentScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.annotatedBeansXML.UnannotatedClassInAnnotatedModeBeanArchive")
                        .add(new FileAsset(new File("test-applications/archiveWithAnnotatedModeBeansXML.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive archiveWithImplicitBeans118 = ShrinkWrap.create(JavaArchive.class,"archiveWithImplicitBeans.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.StereotypedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.MyExtendedScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.MyStereotype")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.SessionScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.ConversationScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.UnannotatedBeanInImplicitBeanArchive")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.MyExtendedNormalScoped")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.RequestScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.implicit.beans.ApplicationScopedBean");
        JavaArchive appClientSecurity166 = ShrinkWrap.create(JavaArchive.class,"appClientSecurity.jar")
                        .addClass("com.ibm.ws.cdi.client.security.fat.AppCallbackHandler")
                        .addClass("com.ibm.ws.cdi.client.security.fat.AppMainClass")
                        .addClass("com.ibm.ws.cdi.client.security.fat.TestCredentialBean")
                        .addClass("com.ibm.ws.cdi.client.security.fat.AppBean")
                        .add(new FileAsset(new File("test-applications/appClientSecurity.jar/resources/META-INF/MANIFEST.MF")), "/META-INF/MANIFEST.MF")
                        .add(new FileAsset(new File("test-applications/appClientSecurity.jar/resources/META-INF/application-client.xml")), "/META-INF/application-client.xml");
        JavaArchive visTestEjbAsAppClientLib158 = ShrinkWrap.create(JavaArchive.class,"visTestEjbAsAppClientLib.jar")
                        .addClass("vistest.ejbAsAppClientLib.dummy.DummySessionBean")
                        .addClass("vistest.ejbAsAppClientLib.EjbAsAppClientLibTestingBean")
                        .addClass("vistest.ejbAsAppClientLib.EjbAsAppClientLibTargetBean");
        JavaArchive visTestEjbAsWarLib6 = ShrinkWrap.create(JavaArchive.class,"visTestEjbAsWarLib.jar")
                        .addClass("vistest.ejbAsWarLib.dummy.DummySessionBean")
                        .addClass("vistest.ejbAsWarLib.EjbAsWarLibTestingBean")
                        .addClass("vistest.ejbAsWarLib.EjbAsWarLibTargetBean");
        JavaArchive maskedClassZAppClient96 = ShrinkWrap.create(JavaArchive.class,"maskedClassZAppClient.jar")
                        .addClass("test.TestBeanAppClientImpl")
                        .addClass("appclient.Main");
        JavaArchive warLibAccessBeansInWar2132 = ShrinkWrap.create(JavaArchive.class,"warLibAccessBeansInWar2.jar")
                        .addClass("com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar2.TestInjectionClass2")
                        .addClass("com.ibm.ws.cdi12.test.warLibAccessBeansInWarJar2.WarBeanInterface2");
        JavaArchive maskedClassLib78 = ShrinkWrap.create(JavaArchive.class,"maskedClassLib.jar")
                        .addClass("test.TestBean");
        JavaArchive ejbOnlyTest110 = ShrinkWrap.create(JavaArchive.class,"ejbOnlyTest.jar")
                        .add(new FileAsset(new File("test-applications/ejbOnlyTest.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive archiveWithNoScanBeansXML58 = ShrinkWrap.create(JavaArchive.class,"archiveWithNoScanBeansXML.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicit.noscan.RequestScopedButNoScan")
                        .add(new FileAsset(new File("test-applications/archiveWithNoScanBeansXML.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive ObservesInitializedInJarsManifestJar1 = ShrinkWrap.create(JavaArchive.class,"ObservesInitializedInJarsManifestJar.jar")
                        .addClass("cdi12.observersinjars.manifestjar.ManifestAutostartObserver")
                        .addClass("cdi12.observersinjars.manifestjar.SomeClass");
        JavaArchive implicitBeanExplicitArchive47 = ShrinkWrap.create(JavaArchive.class,"implicitBeanExplicitArchive.jar")
                        .addClass("com.ibm.ws.cdi12.test.implicitBean.InExplicitBeanArchive")
                        .add(new FileAsset(new File("test-applications/implicitBeanExplicitArchive.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive visTestEjbAsEjbLib17 = ShrinkWrap.create(JavaArchive.class,"visTestEjbAsEjbLib.jar")
                        .addClass("vistest.ejbAsEjbLib.dummy.DummySessionBean")
                        .addClass("vistest.ejbAsEjbLib.EjbAsEjbLibTestingBean")
                        .addClass("vistest.ejbAsEjbLib.EjbAsEjbLibTargetBean");
        JavaArchive visTestEarLib136 = ShrinkWrap.create(JavaArchive.class,"visTestEarLib.jar")
                        .addClass("vistest.earLib.EarLibTargetBean")
                        .addClass("vistest.earLib.EarLibTestingBean");
        JavaArchive explicitBeanArchive144 = ShrinkWrap.create(JavaArchive.class,"explicitBeanArchive.jar")
                        .addClass("com.ibm.ws.cdi.explicit.bean.MyBike")
                        .add(new FileAsset(new File("test-applications/explicitBeanArchive.jar/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        JavaArchive visTestAppClientLib52 = ShrinkWrap.create(JavaArchive.class,"visTestAppClientLib.jar")
                        .addClass("vistest.appClientLib.AppClientLibTargetBean")
                        .addClass("vistest.appClientLib.AppClientLibTestingBean");
        WebArchive visTestWar63 = ShrinkWrap.create(WebArchive.class, "visTestWar.war")
                        .addClass("vistest.war.WarTargetBean")
                        .addClass("vistest.war.servlet.VisibilityTestServlet")
                        .addClass("vistest.war.WarTestingBean")
                        .addAsManifestResource(new File("test-applications/visTestWar.war/resources/META-INF/MANIFEST.MF"))
                        .addAsLibrary(visTestWarWebinfLib113);
        WebArchive ejbConstructorInjection133 = ShrinkWrap.create(WebArchive.class, "ejbConstructorInjection.war")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.Servlet")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.BeanTwo")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.BeanThree")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.MyQualifier")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.MyForthQualifier")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.MyThirdQualifier")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.Iface")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.BeanFourWhichIsEJB")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.MySecondQualifier")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.BeanOne")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.BeanEJB")
                        .addClass("com.ibm.ws.cdi.ejb.constructor.test.StaticState")
                        .add(new FileAsset(new File("test-applications/ejbConstructorInjection.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        WebArchive rootClassLoaderApp105 = ShrinkWrap.create(WebArchive.class, "rootClassLoaderApp.war")
                        .addClass("com.ibm.ws.cdi12.test.rootClassLoader.web.RootClassLoaderServlet")
                        .addAsLibrary(rootClassLoaderExtension109);
        WebArchive alterableContextApp67 = ShrinkWrap.create(WebArchive.class, "alterableContextApp.war")
                        .addClass("com.ibm.ws.cdi12.alterablecontext.test.AlterableContextTestServlet")
                        .add(new FileAsset(new File("test-applications/alterableContextApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(alterableContextExtension5);
        WebArchive passivationBean138 = ShrinkWrap.create(WebArchive.class, "passivationBean.war")
                        .addClass("com.ibm.ws.cdi12.test.passivation.GlobalState")
                        .addClass("com.ibm.ws.cdi12.test.passivation.TransiantDependentScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.passivation.PassivationServlet")
                        .addClass("com.ibm.ws.cdi12.test.passivation.BeanHolder")
                        .addClass("com.ibm.ws.cdi12.test.passivation.TransiantDependentScopedBeanTwo")
                        .add(new FileAsset(new File("test-applications/passivationBean.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/passivationBean.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive aroundConstructApp115 = ShrinkWrap.create(WebArchive.class, "aroundConstructApp.war")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.AroundConstructLogger")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.StatelessAroundConstructLogger")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.Ejb")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.Bean")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.SuperConstructInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.InterceptorTwoBinding")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.DirectlyIntercepted")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.InterceptorOne")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.SubConstructInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.DirectBindingConstructInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.NonCdiInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.ConstructInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.InterceptorOneBinding")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.interceptors.InterceptorTwo")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.EjbServlet")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.BeanServlet")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.AroundConstructTestServlet")
                        .addClass("com.ibm.ws.cdi12.test.aroundconstruct.StatelessEjb")
                        .add(new FileAsset(new File("test-applications/aroundConstructApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(utilLib79);
        WebArchive webBeansBeansXmlDecorators108 = ShrinkWrap.create(WebArchive.class, "webBeansBeansXmlDecorators.war")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlDecorators.Bean")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlDecorators.DecoratedBean")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlDecorators.BeanDecorator")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlDecorators.SimpleTestServlet")
                        .add(new FileAsset(new File("test-applications/webBeansBeansXmlDecorators.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/webBeansBeansXmlDecorators.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive multipleWar23 = ShrinkWrap.create(WebArchive.class, "multipleWar2.war")
                        .addClass("test.multipleWar2.TestServlet")
                        .addClass("test.multipleWar2.MyBean")
                        .add(new FileAsset(new File("test-applications/multipleWar2.war/resources/WEB-INF/ejb-jar.xml")), "/WEB-INF/ejb-jar.xml")
                        .add(new FileAsset(new File("test-applications/multipleWar2.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(multipleWarEmbeddedJar18);
        WebArchive resourceWebServicesProvider53 = ShrinkWrap.create(WebArchive.class, "resourceWebServicesProvider.war")
                        .addClass("com.ibm.ws.cdi.services.impl.MyPojoUser")
                        .addClass("com.ibm.ws.cdi.services.impl.SayHelloPojoService")
                        .addClass("com.ibm.ws.cdi.services.SayHelloService")
                        .add(new FileAsset(new File("test-applications/resourceWebServicesProvider.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        WebArchive ejbOnlyTest131 = ShrinkWrap.create(WebArchive.class, "ejbOnlyTest.war")
                        .add(new FileAsset(new File("test-applications/ejbOnlyTest.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive WebListener163 = ShrinkWrap.create(WebArchive.class, "WebListener.war")
                        .addClass("com.ibm.ws.cdi.test.session.destroy.TestHttpSessionListener")
                        .addClass("com.ibm.ws.cdi.test.session.destroy.ResultsServlet")
                        .addClass("com.ibm.ws.cdi.test.session.destroy.TimeoutServlet")
                        .addClass("com.ibm.ws.cdi.test.session.destroy.SimpleSessionBean")
                        .addClass("com.ibm.ws.cdi.test.session.destroy.InvalidateServlet")
                        .add(new FileAsset(new File("test-applications/WebListener.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive defaultDecoratorApp139 = ShrinkWrap.create(WebArchive.class, "defaultDecoratorApp.war")
                        .addClass("com.ibm.ws.cdi12.test.defaultdecorator.ConversationDecorator")
                        .addClass("com.ibm.ws.cdi12.test.defaultdecorator.DefaultDecoratorServlet")
                        .add(new FileAsset(new File("test-applications/defaultDecoratorApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive simpleJSFWithSharedLib57 = ShrinkWrap.create(WebArchive.class, "simpleJSFWithSharedLib.war")
                        .addClass("com.ibm.ws.cdi12.test.jsf.sharelib.SimpleJsfBean")
                        .add(new FileAsset(new File("test-applications/simpleJSFWithSharedLib.war/resources/WEB-INF/faces-config.xml")), "/WEB-INF/faces-config.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSFWithSharedLib.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSFWithSharedLib.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSFWithSharedLib.war/resources/testBasicJsf.xhtml")), "/testBasicJsf.xhtml");
        WebArchive ObservesInitializedInJarsSecondWar150 = ShrinkWrap.create(WebArchive.class, "ObservesInitializedInJarsSecondWar.war")
                        .addClass("cdi12.observersinjarssecondwar.WarBeforeBeansObserver")
                        .addClass("cdi12.observersinjarssecondwar.SomeClass")
                        .addClass("cdi12.observersinjarssecondwar.TestServlet")
                        .add(new FileAsset(new File("test-applications/ObservesInitializedInJarsSecondWar.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");
        WebArchive eventMetaData148 = ShrinkWrap.create(WebArchive.class, "eventMetaData.war")
                        .addClass("com.ibm.ws.cdi12.test.MetaQualifier")
                        .addClass("com.ibm.ws.cdi12.test.MetaDataServlet")
                        .addClass("com.ibm.ws.cdi12.test.MyEvent")
                        .addClass("com.ibm.ws.cdi12.test.RequestScopedBean")
                        .add(new FileAsset(new File("test-applications/eventMetaData.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/eventMetaData.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive statefulSessionBeanInjection127 = ShrinkWrap.create(WebArchive.class, "statefulSessionBeanInjection.war")
                        .addClass("com.ibm.ws.cdi12.test.implicitEJB.servlet.RemoveServlet")
                        .addClass("com.ibm.ws.cdi12.test.implicitEJB.servlet.TestServlet")
                        .add(new FileAsset(new File("test-applications/statefulSessionBeanInjection.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(statefulSessionBeanInjection77);
        WebArchive libWeb154 = ShrinkWrap.create(WebArchive.class, "libWeb.war")
                        .add(new FileAsset(new File("test-applications/libWeb.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive TestVetoedAlternative124 = ShrinkWrap.create(WebArchive.class, "TestVetoedAlternative.war")
                        .addAsManifestResource(new File("test-applications/TestVetoedAlternative.war/resources/META-INF/MANIFEST.MF"))
                        .addClass("com.ibm.cdi.test.vetoed.alternative.WebServ")
                        .add(new FileAsset(new File("test-applications/TestVetoedAlternative.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/TestVetoedAlternative.war/resources/META-INF/beans.xml")), "/META-INF/beans.xml");
        WebArchive dynamicallyAddedBeans11 = ShrinkWrap.create(WebArchive.class, "dynamicallyAddedBeans.war")
                        .addClass("com.ibm.ws.cdi12.test.dynamicBeans.web.DynamicBeansServlet");
        WebArchive warLibAccessBeansInWar157 = ShrinkWrap.create(WebArchive.class, "warLibAccessBeansInWar.war")
                        .addAsManifestResource(new File("test-applications/warLibAccessBeansInWar.war/resources/META-INF/MANIFEST.MF"))
                        .addClass("com.ibm.ws.cdi12.test.warLibAccessBeansInWar.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.warLibAccessBeansInWar.WarBean")
                        .add(new FileAsset(new File("test-applications/warLibAccessBeansInWar.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(warLibAccessBeansInWarJar32);
        WebArchive implicitBeanDiscovery27 = ShrinkWrap.create(WebArchive.class, "implicitBeanDiscovery.war")
                        .addClass("com.ibm.ws.cdi12.test.implicitBean.TestServlet")
                        .add(new FileAsset(new File("test-applications/implicitBeanDiscovery.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(implicitBeanAnnotatedMode42)
                        .addAsLibrary(implicitBeanNoBeansXml135)
                        .addAsLibrary(implicitBeanExplicitArchive47)
                        .addAsLibrary(utilLib79);
        WebArchive implicitEJBInWar125 = ShrinkWrap.create(WebArchive.class, "implicitEJBInWar.war")
                        .addClass("com.ibm.ws.cdi12.test.implicit.ejb.Web1Servlet")
                        .addClass("com.ibm.ws.cdi12.test.implicit.ejb.SimpleEJB")
                        .addAsLibrary(utilLib79);
        WebArchive vetoedEJBStartup99 = ShrinkWrap.create(WebArchive.class, "vetoedEJBStartup.war")
                        .addClass("com.ibm.cdi.test.vetoedejbstart.VetoEJB")
                        .addClass("com.ibm.cdi.test.vetoedejbstart.MyEJB")
                        .add(new FileAsset(new File("test-applications/vetoedEJBStartup.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/vetoedEJBStartup.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/vetoedEJBStartup.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");
        WebArchive scopeActivationDestructionSecondApp123 = ShrinkWrap.create(WebArchive.class, "scopeActivationDestructionSecondApp.war")
                        .addClass("cd12.secondapp.scopedclasses.SecondServlet")
                        .addClass("cd12.secondapp.scopedclasses.ApplicationScopedBean")
                        .add(new FileAsset(new File("test-applications/scopeActivationDestructionSecondApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/scopeActivationDestructionSecondApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive scopeActivationDestructionTests126 = ShrinkWrap.create(WebArchive.class, "scopeActivationDestructionTests.war")
                        .addClass("cdi12.scopedclasses.SessionScopedBean")
                        .addClass("cdi12.scopedclasses.ConversationScopedBean")
                        .addClass("cdi12.scopedclasses.RequestScopedBean")
                        .addClass("cdi12.resources.GlobalState")
                        .addClass("cdi12.resources.Move")
                        .addClass("cdi12.resources.EndSessionServlet")
                        .addClass("cdi12.resources.State")
                        .addClass("cdi12.resources.BeanLifecycleServlet")
                        .addClass("cdi12.resources.StateMachine")
                        .add(new FileAsset(new File("test-applications/scopeActivationDestructionTests.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/scopeActivationDestructionTests.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/scopeActivationDestructionTests.war/resources/StateMachineDiagram.png")), "/StateMachineDiagram.png")
                        .add(new FileAsset(new File("test-applications/scopeActivationDestructionTests.war/resources/sequence diagram.png")), "/sequence diagram.png");
        WebArchive appNonContextual147 = ShrinkWrap.create(WebArchive.class, "appNonContextual.war")
                        .addClass("test.non.contextual.Foo")
                        .addClass("test.non.contextual.TestServlet")
                        .addClass("test.non.contextual.Baz")
                        .addClass("test.non.contextual.NonContextualBean")
                        .addClass("test.non.contextual.Bar")
                        .add(new FileAsset(new File("test-applications/appNonContextual.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive injectParameters167 = ShrinkWrap.create(WebArchive.class, "injectParameters.war")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestEjb")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestEjbServlet")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestServlet")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestProducer")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestUtils")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestCdiBean")
                        .addClass("com.ibm.ws.cdi12.fat.injectparameters.TestCdiBeanServlet");
        WebArchive deltaspikeTest137 = ShrinkWrap.create(WebArchive.class, "deltaspikeTest.war")
                        .addClass("com.ibm.ws.cdi.deltaspike.scheduler.GlobalResultHolder")
                        .addClass("com.ibm.ws.cdi.deltaspike.scheduler.RequestScopedNumberProvider")
                        .addClass("com.ibm.ws.cdi.deltaspike.scheduler.MyScheduler")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/quartz-config.xml")), "/WEB-INF/quartz-config.xml")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/deltaspike-scheduler-module-impl-1.5.0.jar")), "/WEB-INF/lib/deltaspike-scheduler-module-impl-1.5.0.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/quartz-2.2.1.jar")), "/WEB-INF/lib/quartz-2.2.1.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/deltaspike-core-impl-1.5.0.jar")), "/WEB-INF/lib/deltaspike-core-impl-1.5.0.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/deltaspike-scheduler-module-api-1.5.0.jar")), "/WEB-INF/lib/deltaspike-scheduler-module-api-1.5.0.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/deltaspike-cdictrl-weld-1.5.0.jar")), "/WEB-INF/lib/deltaspike-cdictrl-weld-1.5.0.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/slf4j-jdk14-1.7.7.jar")), "/WEB-INF/lib/slf4j-jdk14-1.7.7.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/slf4j-api-1.7.7.jar")), "/WEB-INF/lib/slf4j-api-1.7.7.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/deltaspike-cdictrl-api-1.5.0.jar")), "/WEB-INF/lib/deltaspike-cdictrl-api-1.5.0.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/lib/deltaspike-core-api-1.5.0.jar")), "/WEB-INF/lib/deltaspike-core-api-1.5.0.jar")
                        .add(new FileAsset(new File("test-applications/deltaspikeTest.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive helloWorldExtensionTest100 = ShrinkWrap.create(WebArchive.class, "helloWorldExtensionTest.war")
                        .addClass("cdi12.helloworld.extension.test.HelloWorldExtensionTestServlet")
                        .addClass("cdi12.helloworld.extension.test.HelloWorldExtensionBean")
                        .add(new FileAsset(new File("test-applications/helloWorldExtensionTest.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive beanDiscoveryModeNone114 = ShrinkWrap.create(WebArchive.class, "beanDiscoveryModeNone.war")
                        .addClass("com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.beanDiscoveryModeNone.TestBean1")
                        .add(new FileAsset(new File("test-applications/beanDiscoveryModeNone.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/beanDiscoveryModeNone.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive appConversationFilter120 = ShrinkWrap.create(WebArchive.class, "appConversationFilter.war")
                        .addClass("test.conversation.filter.ConversationActiveState")
                        .addClass("test.conversation.filter.ConversationBean")
                        .addClass("test.conversation.filter.TestServlet")
                        .addClass("test.conversation.filter.FirstFilter")
                        .add(new FileAsset(new File("test-applications/appConversationFilter.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        WebArchive resourceWebServicesClient45 = ShrinkWrap.create(WebArchive.class, "resourceWebServicesClient.war")
                        .addClass("servlets.TestWebServicesServlet")
                        .addClass("client.services.SayHello_Type")
                        .addClass("client.services.SayHelloResponse")
                        .addClass("client.services.package-info")
                        .addClass("client.services.SayHello")
                        .addClass("client.services.ObjectFactory")
                        .addClass("client.services.SayHelloPojoService")
                        .add(new FileAsset(new File("test-applications/resourceWebServicesClient.war/resources/META-INF/resources/wsdl/EmployPojoService.wsdl")), "/META-INF/resources/wsdl/EmployPojoService.wsdl");
        WebArchive TestClassLoadPrereqLogger8 = ShrinkWrap.create(WebArchive.class, "TestClassLoadPrereqLogger.war")
                        .addClass("com.ibm.cdi.test.WebServ")
                        .add(new FileAsset(new File("test-applications/TestClassLoadPrereqLogger.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        WebArchive multipleBeansXml75 = ShrinkWrap.create(WebArchive.class, "multipleBeansXml.war")
                        .addClass("com.ibm.ws.cdi12.multipleBeansXml.MultipleBeansXmlServlet")
                        .addClass("com.ibm.ws.cdi12.multipleBeansXml.MyBean")
                        .add(new FileAsset(new File("test-applications/multipleBeansXml.war/resources/WEB-INF/classes/META-INF/beans.xml")), "/WEB-INF/classes/META-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/multipleBeansXml.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive jndiLookup149 = ShrinkWrap.create(WebArchive.class, "jndiLookup.war")
                        .addClass("com.ibm.ws.cdi12.test.jndi.LookupServlet")
                        .addClass("com.ibm.ws.cdi12.test.jndi.JNDIStrings");
        WebArchive ejbTimer51 = ShrinkWrap.create(WebArchive.class, "ejbTimer.war")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.IncrementCountersRunnableTask")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.SessionScopedCounter")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.TestEjbTimerTimeOutServlet")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.RequestScopedCounter")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.EjbSessionBean2")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.view.EjbSessionBeanLocal")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.view.SessionBeanLocal")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.view.EjbSessionBean2Local")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.ApplicationScopedCounter")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.SessionBean")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.TestEjbNoTimerServlet")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.RequestScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.TestEjbTimerServlet")
                        .addClass("com.ibm.ws.cdi12.test.ejb.timer.EjbSessionBean")
                        .add(new FileAsset(new File("test-applications/ejbTimer.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive jeeInjectionTargetTest14 = ShrinkWrap.create(WebArchive.class, "jeeInjectionTargetTest.war")
                        .addClass("cdi12.helloworld.jeeResources.test.JEEResourceTestServletCtorInjection")
                        .addClass("cdi12.helloworld.jeeResources.test.MySessionBean")
                        .addClass("cdi12.helloworld.jeeResources.test.JEEResourceTestServletNoInjection")
                        .addClass("cdi12.helloworld.jeeResources.test.MyServerEndpoint")
                        .addClass("cdi12.helloworld.jeeResources.test.JEEResourceTestServlet")
                        .addClass("cdi12.helloworld.jeeResources.test.MyMessageDrivenBean")
                        .addClass("cdi12.helloworld.jeeResources.test.LoggerServlet")
                        .addClass("cdi12.helloworld.jeeResources.test.JEEResourceExtension")
                        .addClass("cdi12.helloworld.jeeResources.test.HelloWorldExtensionBean2")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTest.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTest.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");
        WebArchive invalidBeansXml13 = ShrinkWrap.create(WebArchive.class, "invalidBeansXml.war")
                        .addClass("com.ibm.ws.cdi12.test.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.TestBean")
                        .add(new FileAsset(new File("test-applications/invalidBeansXml.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/invalidBeansXml.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive cdi12helloworldtest60 = ShrinkWrap.create(WebArchive.class, "cdi12helloworldtest.war")
                        .addClass("cdi12.helloworld.test.HelloBean")
                        .addClass("cdi12.helloworld.test.HelloServlet")
                        .add(new FileAsset(new File("test-applications/cdi12helloworldtest.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/cdi12helloworldtest.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive applicationExtension80 = ShrinkWrap.create(WebArchive.class, "applicationExtension.war")
                        .addClass("main.TestServlet")
                        .addClass("main.bean.InSameWarBean")
                        .addAsLibrary(applicationExtension35);
        WebArchive implicitBeanArchiveDisabled9 = ShrinkWrap.create(WebArchive.class, "implicitBeanArchiveDisabled.war")
                        .addClass("com.ibm.ws.cdi12.implicit.archive.disabled.MyCar")
                        .addClass("com.ibm.ws.cdi12.implicit.archive.disabled.MyCarServlet")
                        .add(new FileAsset(new File("test-applications/implicitBeanArchiveDisabled.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(implicitBeanArchiveDisabled49);
        WebArchive beanManagerLookupApp146 = ShrinkWrap.create(WebArchive.class, "beanManagerLookupApp.war")
                        .addClass("cdi12.beanmanagerlookup.test.BeanManagerLookupServlet")
                        .addClass("cdi12.beanmanagerlookup.test.MyBean")
                        .add(new FileAsset(new File("test-applications/beanManagerLookupApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        WebArchive multiModuleAppWeb1159 = ShrinkWrap.create(WebArchive.class, "multiModuleAppWeb1.war")
                        .addClass("com.ibm.ws.cdi12.test.web1.Web1Servlet")
                        .add(new FileAsset(new File("test-applications/multiModuleAppWeb1.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive ejbJarInWarNoAnnotations84 = ShrinkWrap.create(WebArchive.class, "ejbJarInWarNoAnnotations.war")
                        .addClass("com.ibm.ws.cdi12.test.ejbJarInWarNoAnnotations.EjbServlet")
                        .add(new FileAsset(new File("test-applications/ejbJarInWarNoAnnotations.war/resources/WEB-INF/ejb-jar.xml")), "/WEB-INF/ejb-jar.xml")
                        .addAsLibrary(ejbJarInWarNoAnnotations93);
        WebArchive ejbArchiveWithNoAnnotations156 = ShrinkWrap.create(WebArchive.class, "ejbArchiveWithNoAnnotations.war")
                        .addClass("com.ibm.ws.cdi12.test.EjbServlet");
        WebArchive ejbMisc16 = ShrinkWrap.create(WebArchive.class, "ejbMisc.war")
                        .addClass("com.ibm.ws.cdi12test.remoteEjb.web.AServlet")
                        .addClass("com.ibm.ws.cdi12test.remoteEjb.ejb.TestObserver")
                        .addClass("com.ibm.ws.cdi12test.remoteEjb.api.RemoteInterface")
                        .addClass("com.ibm.ws.cdi12test.remoteEjb.api.EJBEvent")
                        .add(new FileAsset(new File("test-applications/ejbMisc.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive visTestWar24 = ShrinkWrap.create(WebArchive.class, "visTestWar2.war")
                        .addClass("vistest.war2.War2TargetBean")
                        .addClass("vistest.war2.War2TestingBean")
                        .addClass("vistest.war2.servlet.VisibilityTestServlet");
        WebArchive multipleWarNoBeans62 = ShrinkWrap.create(WebArchive.class, "multipleWarNoBeans.war")
                        .addClass("test.multipleWarNoBeans.TestServlet");
        WebArchive webBeansBeansXmlInterceptors86 = ShrinkWrap.create(WebArchive.class, "webBeansBeansXmlInterceptors.war")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlInterceptors.InterceptedBean")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlInterceptors.BasicInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlInterceptors.BasicInterceptorBinding")
                        .addClass("com.ibm.ws.cdi12.test.webBeansBeansXmlInterceptors.SimpleTestServlet")
                        .add(new FileAsset(new File("test-applications/webBeansBeansXmlInterceptors.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/webBeansBeansXmlInterceptors.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive ejbScope46 = ShrinkWrap.create(WebArchive.class, "ejbScope.war")
                        .addClass("com.ibm.ws.cdi12.test.ejb.scope.PostConstructingStartupBean")
                        .addClass("com.ibm.ws.cdi12.test.ejb.scope.PostConstructScopeServlet")
                        .addClass("com.ibm.ws.cdi12.test.ejb.scope.RequestScopedBean");
        WebArchive TestValidatorInJar164 = ShrinkWrap.create(WebArchive.class, "TestValidatorInJar.war")
                        .addClass("com.ibm.cdi.test.basic.injection.WebServ")
                        .addAsManifestResource(new File("test-applications/TestValidatorInJar.war/resources/META-INF/MANIFEST.MF"))
                        .add(new FileAsset(new File("test-applications/TestValidatorInJar.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/TestValidatorInJar.war/resources/WEB-INF/lib/jaxrs-analyzer-0.9.jar")), "/WEB-INF/lib/jaxrs-analyzer-0.9.jar");
        WebArchive simpleJSFApp91 = ShrinkWrap.create(WebArchive.class, "simpleJSFApp.war")
                        .addClass("com.ibm.ws.cdi12.test.jsf.SimpleJsfBean")
                        .addClass("com.ibm.ws.cdi12.test.jsf.OtherJsfBean")
                        .add(new FileAsset(new File("test-applications/simpleJSFApp.war/resources/WEB-INF/faces-config.xml")), "/WEB-INF/faces-config.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSFApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSFApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSFApp.war/resources/testBasicJsf.xhtml")), "/testBasicJsf.xhtml");
        WebArchive simpleJSPApp55 = ShrinkWrap.create(WebArchive.class, "simpleJSPApp.war")
                        .addClass("com.ibm.ws.cdi12.test.jsp.SimpleJspBean")
                        .add(new FileAsset(new File("test-applications/simpleJSPApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSPApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/simpleJSPApp.war/resources/index.jsp")), "/index.jsp");
        WebArchive injectInjectionPoint37 = ShrinkWrap.create(WebArchive.class, "injectInjectionPoint.war")
                        .addClass("com.ibm.ws.fat.cdi.injectInjectionPoint.InjectInjectionPointServlet")
                        .add(new FileAsset(new File("test-applications/injectInjectionPoint.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive postConstructErrorMessageApp81 = ShrinkWrap.create(WebArchive.class, "postConstructErrorMessageApp.war")
                        .addClass("com.ibm.ws.cdi12.test.errormessage.ErrorMessageServlet")
                        .addClass("com.ibm.ws.cdi12.test.errormessage.interceptors.ErrorMessageInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.errormessage.interceptors.ErrorMessageInterceptorBinding")
                        .addClass("com.ibm.ws.cdi12.test.errormessage.ErrorMessageTestEjb")
                        .add(new FileAsset(new File("test-applications/postConstructErrorMessageApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(utilLib79);
        WebArchive managedBeanApp15 = ShrinkWrap.create(WebArchive.class, "managedBeanApp.war")
                        .addClass("com.ibm.ws.cdi.test.managedbean.CounterUtil")
                        .addClass("com.ibm.ws.cdi.test.managedbean.ManagedBeanServlet")
                        .addClass("com.ibm.ws.cdi.test.managedbean.MyEJBBean")
                        .addClass("com.ibm.ws.cdi.test.managedbean.interceptors.MyInterceptorBase")
                        .addClass("com.ibm.ws.cdi.test.managedbean.interceptors.MyNonCDIInterceptor")
                        .addClass("com.ibm.ws.cdi.test.managedbean.interceptors.MyCDIInterceptorBinding")
                        .addClass("com.ibm.ws.cdi.test.managedbean.interceptors.MyCDIInterceptor")
                        .addClass("com.ibm.ws.cdi.test.managedbean.MyEJBBeanLocal")
                        .addClass("com.ibm.ws.cdi.test.managedbean.MyManagedBean")
                        .add(new FileAsset(new File("test-applications/managedBeanApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        WebArchive multipleWar189 = ShrinkWrap.create(WebArchive.class, "multipleWar1.war")
                        .addClass("test.multipleWar1.TestServlet")
                        .addClass("test.multipleWar1.MyBean")
                        .add(new FileAsset(new File("test-applications/multipleWar1.war/resources/WEB-INF/ejb-jar.xml")), "/WEB-INF/ejb-jar.xml")
                        .add(new FileAsset(new File("test-applications/multipleWar1.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(multipleWarEmbeddedJar18);
        WebArchive jeeInjectionTargetTestJSP2117 = ShrinkWrap.create(WebArchive.class, "jeeInjectionTargetTestJSP2.3.war")
                        .addClass("tagHandler.JspCdiHitMeTag")
                        .addClass("tagHandler.MethodInjectionTag")
                        .addClass("beans.TestMethodInjectionApplicationScoped")
                        .addClass("beans.TestConstructorInjectionDependentScoped")
                        .addClass("beans.Employee")
                        .addClass("beans.TestConstructorInjectionApplicationScoped")
                        .addClass("beans.TestConstructorInjectionSessionScoped")
                        .addClass("beans.TestMethodInjectionRequestScoped")
                        .addClass("beans.TestTagInjectionRequestBean")
                        .addClass("beans.TestFieldInjectionSessionScoped")
                        .addClass("beans.TestFieldInjectionRequestScoped")
                        .addClass("beans.EL30StaticFieldsAndMethodsEnum")
                        .addClass("beans.TestMethodInjectionDependentScoped")
                        .addClass("beans.EL30CoercionRulesTestBean")
                        .addClass("beans.EL30StaticFieldsAndMethodsBean")
                        .addClass("beans.EL30InvocationMethodExpressionTestBean")
                        .addClass("beans.EL30ReserverdWordsTestBean")
                        .addClass("beans.TestTagInjectionSessionBean")
                        .addClass("beans.TestFieldInjectionDependentScoped")
                        .addClass("beans.TestMethodInjectionSessionScoped")
                        .addClass("beans.TestTagInjectionDependentBean")
                        .addClass("beans.TestTagInjectionApplicationBean")
                        .addClass("beans.Pojo1")
                        .addClass("beans.TestConstructorInjectionRequestScoped")
                        .addClass("beans.EL30MapCollectionObjectBean")
                        .addClass("beans.TestFieldInjectionApplicationScoped")
                        .addClass("listeners.JspCdiTagLibraryEventListenerCI")
                        .addClass("listeners.JspCdiTagLibraryEventListenerMI")
                        .addClass("listeners.JspCdiTagLibraryEventListenerFI")
                        .addClass("servlets.SimpleTestServlet")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30PropertyNotFoundException.jsp")), "/EL30PropertyNotFoundException.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30OperatorPrecedences.jsp")), "/EL30OperatorPrecedences.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/TagLibraryEventListenerCI.jsp")), "/TagLibraryEventListenerCI.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/WEB-INF/tlds/EventListeners.tld")), "/WEB-INF/tlds/EventListeners.tld")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/WEB-INF/tlds/Tag2Lib.tld")), "/WEB-INF/tlds/Tag2Lib.tld")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/WEB-INF/tlds/Tag1Lib.tld")), "/WEB-INF/tlds/Tag1Lib.tld")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/WEB-INF/tags/EL30CoercionRulesTest.tag")), "/WEB-INF/tags/EL30CoercionRulesTest.tag")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/TagLibraryEventListenerMI.jsp")), "/TagLibraryEventListenerMI.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/lt.jsp")), "/EL30ReservedWords/lt.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/gt.jsp")), "/EL30ReservedWords/gt.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/false.jsp")), "/EL30ReservedWords/false.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/mod.jsp")), "/EL30ReservedWords/mod.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/le.jsp")), "/EL30ReservedWords/le.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/or.jsp")), "/EL30ReservedWords/or.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/NonReservedWords.jsp")), "/EL30ReservedWords/NonReservedWords.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/ne.jsp")), "/EL30ReservedWords/ne.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/instanceof.jsp")), "/EL30ReservedWords/instanceof.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/not.jsp")), "/EL30ReservedWords/not.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/ge.jsp")), "/EL30ReservedWords/ge.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/empty.jsp")), "/EL30ReservedWords/empty.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/eq.jsp")), "/EL30ReservedWords/eq.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/null.jsp")), "/EL30ReservedWords/null.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/true.jsp")), "/EL30ReservedWords/true.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/and.jsp")), "/EL30ReservedWords/and.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30ReservedWords/div.jsp")), "/EL30ReservedWords/div.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL22Operators.jsp")), "/EL22Operators.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30CoercionRules.jsp")), "/EL30CoercionRules.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30Lambda.jsp")), "/EL30Lambda.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30InvocationMethodExpressions.jsp")), "/EL30InvocationMethodExpressions.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/Tag2.jsp")), "/Tag2.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30MethodNotFoundException.jsp")), "/EL30MethodNotFoundException.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30PropertyNotWritableException.jsp")), "/EL30PropertyNotWritableException.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/TagLibraryEventListenerFI.jsp")), "/TagLibraryEventListenerFI.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30CollectionObjectOperations.jsp")), "/EL30CollectionObjectOperations.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30AssignmentOperatorException.jsp")), "/EL30AssignmentOperatorException.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30Operators.jsp")), "/EL30Operators.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/Tag1.jsp")), "/Tag1.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/EL30StaticFieldsAndMethodsTests.jsp")), "/EL30StaticFieldsAndMethodsTests.jsp")
                        .add(new FileAsset(new File("test-applications/jeeInjectionTargetTestJSP2.3.war/resources/Servlet31RequestResponseTest.jsp")), "/Servlet31RequestResponseTest.jsp");
        WebArchive packagePrivateAccessApp168 = ShrinkWrap.create(WebArchive.class, "packagePrivateAccessApp.war")
                        .addClass("jp.test.RunServlet")
                        .addClass("jp.test.bean.MyBeanHolder")
                        .addClass("jp.test.bean.MyExecutor")
                        .addClass("jp.test.bean.MyBean")
                        .add(new FileAsset(new File("test-applications/packagePrivateAccessApp.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml");
        WebArchive withAnnotationsApp161 = ShrinkWrap.create(WebArchive.class, "withAnnotationsApp.war")
                        .addClass("com.ibm.ws.cdi12.test.withAnnotations.WithAnnotationsServlet")
                        .addClass("com.ibm.ws.cdi12.test.withAnnotations.WithAnnotationsExtension")
                        .addClass("com.ibm.ws.cdi12.test.withAnnotations.NonAnnotatedBean")
                        .addClass("com.ibm.ws.cdi12.test.withAnnotations.RequestScopedBean")
                        .addClass("com.ibm.ws.cdi12.test.withAnnotations.ApplicationScopedBean")
                        .add(new FileAsset(new File("test-applications/withAnnotationsApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/withAnnotationsApp.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension")
                        .addAsLibrary(utilLib79);
        WebArchive classExclusion29 = ShrinkWrap.create(WebArchive.class, "classExclusion.war")
                        .addClass("cdi12.classexclusion.test.packageexcludedbyproperty.ExcludedByPropertyBean")
                        .addClass("cdi12.classexclusion.test.ExcludedBean")
                        .addClass("cdi12.classexclusion.test.packageprotectedbyclass.ProtectedByClassBean")
                        .addClass("cdi12.classexclusion.test.excludedpackage.ExcludedPackageBean")
                        .addClass("cdi12.classexclusion.test.IncludedBean")
                        .addClass("cdi12.classexclusion.test.excludedpackagetree.subpackage.ExcludedPackageTreeBean")
                        .addClass("cdi12.classexclusion.test.ProtectedByHalfComboBean")
                        .addClass("cdi12.classexclusion.test.TestServlet")
                        .addClass("cdi12.classexclusion.test.interfaces.IVetoedBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IExcludedPackageBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IProtectedByHalfComboBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IProtectedByClassBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IExcludedBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IIncludedBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IExcludedByPropertyBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IExcludedPackageTreeBean")
                        .addClass("cdi12.classexclusion.test.interfaces.IExcludedByComboBean")
                        .addClass("cdi12.classexclusion.test.fallbackbeans.FallbackForExcludedBean")
                        .addClass("cdi12.classexclusion.test.fallbackbeans.FallbackForExcludedPackageTreeBean")
                        .addClass("cdi12.classexclusion.test.fallbackbeans.FallbackForVetoedBean")
                        .addClass("cdi12.classexclusion.test.fallbackbeans.FallbackForExcludedPackageBean")
                        .addClass("cdi12.classexclusion.test.fallbackbeans.FallbackForExcludedByPropertyBean")
                        .addClass("cdi12.classexclusion.test.fallbackbeans.FallbackForExcludedByComboBean")
                        .addClass("cdi12.classexclusion.test.VetoedBean")
                        .addClass("cdi12.classexclusion.test.exludedbycombopackagetree.subpackage.ExcludedByComboBean")
                        .add(new FileAsset(new File("test-applications/classExclusion.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/classExclusion.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive maskedClassWeb56 = ShrinkWrap.create(WebArchive.class, "maskedClassWeb.war")
                        .addClass("test.TestBeanWarImpl")
                        .addClass("test.Type3")
                        .addClass("test.Type1")
                        .addClass("zservlet.TestServlet");
        WebArchive nonContextual134 = ShrinkWrap.create(WebArchive.class, "nonContextual.war")
                        .addClass("cdi12.noncontextual.test.Servlet")
                        .addClass("cdi12.noncontextual.test.NonContextualBean")
                        .add(new FileAsset(new File("test-applications/nonContextual.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/nonContextual.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive cdiCurrentTest66 = ShrinkWrap.create(WebArchive.class, "cdiCurrentTest.war")
                        .addClass("com.ibm.ws.cdi12.test.common.web.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.common.web.SimpleBean")
                        .addAsLibrary(cdiCurrentTest129);
        WebArchive sharedLibraryAppWeb171 = ShrinkWrap.create(WebArchive.class, "sharedLibraryAppWeb1.war")
                        .addClass("com.ibm.ws.cdi12.test.web1.SharedLibraryServlet");
        WebArchive afterTypeDiscoveryApp30 = ShrinkWrap.create(WebArchive.class, "afterTypeDiscoveryApp.war")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.GlobalState")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeNotAlternative")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.InterceptedAfterType")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeBeanDecorator")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeBean")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.InterceptedBean")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeExtension")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeAlternativeTwo")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeServlet")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeAlternativeInterface")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.UseAlternative")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeInterceptorImpl")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeAlternativeOne")
                        .addClass("com.ibm.ws.cdi12.aftertypediscovery.test.AfterTypeInterface")
                        .add(new FileAsset(new File("test-applications/afterTypeDiscoveryApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/afterTypeDiscoveryApp.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");
        WebArchive multiModuleAppWeb224 = ShrinkWrap.create(WebArchive.class, "multiModuleAppWeb2.war")
                        .addClass("com.ibm.ws.cdi12.test.web2.Web2Servlet")
                        .add(new FileAsset(new File("test-applications/multiModuleAppWeb2.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsManifestResource(new File("test-applications/multiModuleAppWeb2.war/resources/META-INF/MANIFEST.MF"))
                        .addAsLibrary(multiModuleAppLib225)
                        .addAsLibrary(multiModuleAppLib334);
        WebArchive transientReferenceInSessionPersist40 = ShrinkWrap.create(WebArchive.class, "transientReferenceInSessionPersist.war")
                        .addClass("cdi12.transientpassivationtest.GlobalState")
                        .addClass("cdi12.transientpassivationtest.BeanWithInjectionPointMetadata")
                        .addClass("cdi12.transientpassivationtest.MyStatefulSessionBean")
                        .addClass("cdi12.transientpassivationtest.ConstructorInjectionPointBean")
                        .addClass("cdi12.transientpassivationtest.MethodInjectionPointBean")
                        .addClass("cdi12.transientpassivationtest.AnimalStereotype")
                        .addClass("cdi12.transientpassivationtest.BeanHolder")
                        .addClass("cdi12.transientpassivationtest.FieldInjectionPointBean")
                        .addClass("cdi12.transientpassivationtest.PassivationBean")
                        .add(new FileAsset(new File("test-applications/transientReferenceInSessionPersist.war/resources/WEB-INF/web.xml")), "/WEB-INF/web.xml")
                        .add(new FileAsset(new File("test-applications/transientReferenceInSessionPersist.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/transientReferenceInSessionPersist.war/resources/PassivationCapability.jsp")), "/PassivationCapability.jsp");
        WebArchive ejbDiscovery44 = ShrinkWrap.create(WebArchive.class, "ejbDiscovery.war")
                        .addClass("com.ibm.ws.cdi12.ejbdiscovery.extension.DiscoveryExtension")
                        .addClass("com.ibm.ws.cdi12.ejbdiscovery.servlet.DiscoveryServlet")
                        .addClass("com.ibm.ws.cdi12.ejbdiscovery.ejbs.SingletonDdBean")
                        .addClass("com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatelessBean")
                        .addClass("com.ibm.ws.cdi12.ejbdiscovery.ejbs.interfaces.StatelessLocal")
                        .addClass("com.ibm.ws.cdi12.ejbdiscovery.ejbs.interfaces.StatelessDdLocal")
                        .addClass("com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatelessDdBean")
                        .addClass("com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatefulDdBean")
                        .addClass("com.ibm.ws.cdi12.ejbdiscovery.ejbs.SingletonBean")
                        .addClass("com.ibm.ws.cdi12.ejbdiscovery.ejbs.StatefulBean")
                        .add(new FileAsset(new File("test-applications/ejbDiscovery.war/resources/WEB-INF/ejb-jar.xml")), "/WEB-INF/ejb-jar.xml")
                        .add(new FileAsset(new File("test-applications/ejbDiscovery.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .add(new FileAsset(new File("test-applications/ejbDiscovery.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension");
        WebArchive sharedLibraryNoInjectionApp104 = ShrinkWrap.create(WebArchive.class, "sharedLibraryNoInjectionApp.war")
                        .addClass("com.ibm.ws.cdi12.test.web1.NoInjectionServlet");
        WebArchive ejbDiscoveryModeNone98 = ShrinkWrap.create(WebArchive.class, "ejbDiscoveryModeNone.war")
                        .addClass("com.ibm.ws.cdi12.ejbdiscovery.none.ejbs.StatelessBean")
                        .add(new FileAsset(new File("test-applications/ejbDiscoveryModeNone.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive multipleEJBsSingleClass90 = ShrinkWrap.create(WebArchive.class, "multipleEJBsSingleClass.war")
                        .addClass("com.ibm.ws.cdi12.test.multipleNamedEJBs.SimpleEJBImpl")
                        .addClass("com.ibm.ws.cdi12.test.multipleNamedEJBs.SimpleEJBLocalInterface2")
                        .addClass("com.ibm.ws.cdi12.test.multipleNamedEJBs.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.multipleNamedEJBs.SimpleManagedBean")
                        .addClass("com.ibm.ws.cdi12.test.multipleNamedEJBs.SimpleEJBLocalInterface1")
                        .add(new FileAsset(new File("test-applications/multipleEJBsSingleClass.war/resources/WEB-INF/ejb-jar.xml")), "/WEB-INF/ejb-jar.xml")
                        .add(new FileAsset(new File("test-applications/multipleEJBsSingleClass.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");
        WebArchive implicitWarApp121 = ShrinkWrap.create(WebArchive.class, "implicitWarApp.war")
                        .addClass("com.ibm.ws.cdi12.test.implicitWar.TestServlet")
                        .addClass("com.ibm.ws.cdi12.test.implicitWar.AnnotatedBean")
                        .addAsLibrary(utilLib79);
        WebArchive archiveWithNoBeansXml165 = ShrinkWrap.create(WebArchive.class, "archiveWithNoBeansXml.war")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.FirstManagedBeanInterface")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.EjbImpl")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.SimpleServlet")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.ManagedSimpleBean")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.OtherManagedSimpleBean")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.SecondManagedBeanInterface")
                        .addClass("com.ibm.ws.cdi12.test.ejbsNoBeansXml.ConstructorInjectionServlet")
                        .add(new FileAsset(new File("test-applications/archiveWithNoBeansXml.war/resources/WEB-INF/ejb-jar.xml")), "/WEB-INF/ejb-jar.xml");
        WebArchive implicitBeanArchive95 = ShrinkWrap.create(WebArchive.class, "implicitBeanArchive.war")
                        .addClass("com.ibm.ws.cdi12.test.implicit.servlet.Web1Servlet")
                        .add(new FileAsset(new File("test-applications/implicitBeanArchive.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml")
                        .addAsLibrary(archiveWithBeansXML112)
                        .addAsLibrary(archiveWithImplicitBeans118)
                        .addAsLibrary(archiveWithNoImplicitBeans130)
                        .addAsLibrary(archiveWithNoScanBeansXML58)
                        .addAsLibrary(archiveWithAnnotatedModeBeansXML140);
        WebArchive ObservesInitializedInJars2 = ShrinkWrap.create(WebArchive.class, "ObservesInitializedInJars.war")
                        .addClass("cdi12.observersinjarsbeforebean.WarBeforeBeansObserver")
                        .addClass("cdi12.observersinjars.SomeClass")
                        .addClass("cdi12.observersinjars.TestServlet")
                        .addClass("cdi12.observersinjars.WarAutostartObserver")
                        .addAsManifestResource(new File("test-applications/ObservesInitializedInJars.war/resources/META-INF/MANIFEST.MF"))
                        .add(new FileAsset(new File("test-applications/ObservesInitializedInJars.war/resources/META-INF/services/javax.enterprise.inject.spi.Extension")), "/META-INF/services/javax.enterprise.inject.spi.Extension")
                        .addAsLibrary(ObservesInitializedInJarsWebInfJar12);
        WebArchive globalPriorityWebApp54 = ShrinkWrap.create(WebArchive.class, "globalPriorityWebApp.war")
                        .addClass("com.ibm.ws.cdi12.test.priority.NoPriorityBean")
                        .addClass("com.ibm.ws.cdi12.test.priority.WarInterceptor")
                        .addClass("com.ibm.ws.cdi12.test.priority.FromWar")
                        .addClass("com.ibm.ws.cdi12.test.priority.WarBean")
                        .addClass("com.ibm.ws.cdi12.test.priority.WarDecorator")
                        .addClass("com.ibm.ws.cdi12.test.priority.GlobalPriorityTestServlet")
                        .add(new FileAsset(new File("test-applications/globalPriorityWebApp.war/resources/WEB-INF/beans.xml")), "/WEB-INF/beans.xml");


        ResourceAdapterArchive jarInRar173 = ShrinkWrap.create(ResourceAdapterArchive.class,"jarInRar.rar")
                        .addAsLibrary(jarInRar172)
                        .add(new FileAsset(new File("test-applications/jarInRar.rar/resources/META-INF/ra.xml")), "/META-INF/ra.xml");

        EnterpriseArchive cdi12helloworldtest106 = ShrinkWrap.create(EnterpriseArchive.class,"cdi12helloworldtest.ear")
                        .add(new FileAsset(new File("test-applications/cdi12helloworldtest.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(cdi12helloworldtest60);
        EnterpriseArchive appClientSecurity122 = ShrinkWrap.create(EnterpriseArchive.class,"appClientSecurity.ear")
                        .add(new FileAsset(new File("test-applications/appClientSecurity.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(appClientSecurity166);
        EnterpriseArchive resourceWebServices128 = ShrinkWrap.create(EnterpriseArchive.class,"resourceWebServices.ear")
                        .add(new FileAsset(new File("test-applications/resourceWebServices.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(resourceWebServicesClient45)
                        .addAsModule(resourceWebServicesProvider53);
        EnterpriseArchive implicitBeanArchiveDisabled69 = ShrinkWrap.create(EnterpriseArchive.class,"implicitBeanArchiveDisabled.ear")
                        .add(new FileAsset(new File("test-applications/implicitBeanArchiveDisabled.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsLibrary(explicitBeanArchive144)
                        .addAsModule(implicitBeanArchiveDisabled9);
        EnterpriseArchive ejbDiscovery22 = ShrinkWrap.create(EnterpriseArchive.class,"ejbDiscovery.ear")
                        .addAsModule(ejbDiscovery44)
                        .addAsModule(ejbDiscoveryModeNone98);
        EnterpriseArchive beanManagerLookupApp152 = ShrinkWrap.create(EnterpriseArchive.class,"beanManagerLookupApp.ear")
                        .add(new FileAsset(new File("test-applications/beanManagerLookupApp.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(beanManagerLookupApp146);
        EnterpriseArchive alterableContextsApp162 = ShrinkWrap.create(EnterpriseArchive.class,"alterableContextsApp.ear")
                        .add(new FileAsset(new File("test-applications/alterableContextsApp.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(alterableContextApp67);
        EnterpriseArchive helloWorldExension153 = ShrinkWrap.create(EnterpriseArchive.class,"helloWorldExension.ear")
                        .add(new FileAsset(new File("test-applications/helloWorldExension.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(helloWorldExtensionTest100);
        EnterpriseArchive ejbArchiveWithNoAnnotations82 = ShrinkWrap.create(EnterpriseArchive.class,"ejbArchiveWithNoAnnotations.ear")
                        .add(new FileAsset(new File("test-applications/ejbArchiveWithNoAnnotations.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(ejbArchiveWithNoAnnotations156)
                        .addAsModule(ejbArchiveWithNoAnnotations19);
        EnterpriseArchive eventMetaData94 = ShrinkWrap.create(EnterpriseArchive.class,"eventMetaData.ear")
                        .add(new FileAsset(new File("test-applications/eventMetaData.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(eventMetaData148);
        EnterpriseArchive ejbJarInWarNoAnnotations88 = ShrinkWrap.create(EnterpriseArchive.class,"ejbJarInWarNoAnnotations.ear")
                        .add(new FileAsset(new File("test-applications/ejbJarInWarNoAnnotations.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(ejbJarInWarNoAnnotations84);
        EnterpriseArchive webBeansBeansXmlInterceptors39 = ShrinkWrap.create(EnterpriseArchive.class,"webBeansBeansXmlInterceptors.ear")
                        .add(new FileAsset(new File("test-applications/webBeansBeansXmlInterceptors.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(webBeansBeansXmlInterceptors86);
        EnterpriseArchive multipleWars87 = ShrinkWrap.create(EnterpriseArchive.class,"multipleWars.ear")
                        .add(new FileAsset(new File("test-applications/multipleWars.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsLibrary(sharedLibrary33)
                        .addAsModule(multipleWar189)
                        .addAsModule(multipleWar23);
        EnterpriseArchive nonContextual7 = ShrinkWrap.create(EnterpriseArchive.class,"nonContextual.ear")
                        .add(new FileAsset(new File("test-applications/nonContextual.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(nonContextual134);
        EnterpriseArchive TestValidatorInJar36 = ShrinkWrap.create(EnterpriseArchive.class,"TestValidatorInJar.ear")
                        .add(new FileAsset(new File("test-applications/TestValidatorInJar.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(TestValidatorInJar164)
                        .addAsModule(TestValidatorInJar20);
        EnterpriseArchive globalPriorityApp61 = ShrinkWrap.create(EnterpriseArchive.class,"globalPriorityApp.ear")
                        .add(new FileAsset(new File("test-applications/globalPriorityApp.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsLibrary(globalPriorityLib119)
                        .addAsLibrary(utilLib79)
                        .addAsModule(globalPriorityWebApp54);
        EnterpriseArchive classExclusion85 = ShrinkWrap.create(EnterpriseArchive.class,"classExclusion.ear")
                        .add(new FileAsset(new File("test-applications/classExclusion.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(classExclusion29);
        EnterpriseArchive ObservesInitializedInJars170 = ShrinkWrap.create(EnterpriseArchive.class,"ObservesInitializedInJars.ear")
                        .add(new FileAsset(new File("test-applications/ObservesInitializedInJars.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(ObservesInitializedInJars2)
                        .addAsModule(ObservesInitializedInJarsSecondWar150)
                        .addAsModule(ObservesInitializedInJarsManifestJar1);
        EnterpriseArchive passivationBean65 = ShrinkWrap.create(EnterpriseArchive.class,"passivationBean.ear")
                        .add(new FileAsset(new File("test-applications/passivationBean.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(passivationBean138);
        EnterpriseArchive multiModuleApp2171 = ShrinkWrap.create(EnterpriseArchive.class,"multiModuleApp2.ear")
                        .add(new FileAsset(new File("test-applications/multiModuleApp2.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsLibrary(multiModuleAppLib164)
                        .addAsLibrary(multiModuleAppLib334)
                        .addAsModule(multiModuleAppWeb1159)
                        .addAsModule(multiModuleAppWeb224);
        EnterpriseArchive visTest107 = ShrinkWrap.create(EnterpriseArchive.class,"visTest.ear")
                        .addAsModule(visTestWar63)
                        .addAsModule(visTestEjb10)
                        .addAsModule(visTestAppClient73)
                        .addAsModule(visTestEjbAsEjbLib17)
                        .addAsModule(visTestEjbAsWarLib6)
                        .addAsModule(visTestEjbAsAppClientLib158)
                        .addAsModule(visTestAppClientAsEjbLib74)
                        .addAsModule(visTestAppClientAsWarLib68)
                        .addAsModule(visTestAppClientAsAppClientLib169)
                        .addAsModule(visTestWar24)
                        .addAsModule(visTestWarLib102)
                        .addAsModule(visTestEjbLib23)
                        .addAsModule(visTestAppClientLib52)
                        .addAsModule(visTestEjbWarLib142)
                        .addAsModule(visTestEjbAppClientLib50)
                        .addAsModule(visTestWarAppClientLib48)
                        .addAsModule(visTestNonLib101)
                        .addAsLibrary(visTestFramework41)
                        .addAsLibrary(visTestEarLib136);
        EnterpriseArchive multiModuleApp170 = ShrinkWrap.create(EnterpriseArchive.class,"multiModuleApp1.ear")
                        .add(new FileAsset(new File("test-applications/multiModuleApp1.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsLibrary(multiModuleAppLib164)
                        .addAsModule(multiModuleAppWeb1159)
                        .addAsModule(multiModuleAppWeb224);
        EnterpriseArchive multipleWars259 = ShrinkWrap.create(EnterpriseArchive.class,"multipleWars2.ear")
                        .add(new FileAsset(new File("test-applications/multipleWars2.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(multipleWar189)
                        .addAsModule(multipleWarNoBeans62);
        EnterpriseArchive TestVetoedAlternative141 = ShrinkWrap.create(EnterpriseArchive.class,"TestVetoedAlternative.ear")
                        .add(new FileAsset(new File("test-applications/TestVetoedAlternative.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(TestVetoedAlternative124)
                        .addAsModule(TestVetoedAlternative111);
        EnterpriseArchive webBeansBeansXmlDecorators103 = ShrinkWrap.create(EnterpriseArchive.class,"webBeansBeansXmlDecorators.ear")
                        .add(new FileAsset(new File("test-applications/webBeansBeansXmlDecorators.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(webBeansBeansXmlDecorators108);
        EnterpriseArchive maskedClass38 = ShrinkWrap.create(EnterpriseArchive.class,"maskedClass.ear")
                        .addAsModule(maskedClassEjb31)
                        .addAsModule(maskedClassWeb56)
                        .addAsModule(maskedClassZAppClient96)
                        .addAsLibrary(maskedClassLib78);
        EnterpriseArchive appClientAdvanced151 = ShrinkWrap.create(EnterpriseArchive.class,"appClientAdvanced.ear")
                        .add(new FileAsset(new File("test-applications/appClientAdvanced.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(appClientAdvanced155);
        EnterpriseArchive jeeInjectionTargetTest43 = ShrinkWrap.create(EnterpriseArchive.class,"jeeInjectionTargetTest.ear")
                        .addAsModule(jeeInjectionTargetTestEJB143)
                        .addAsModule(jeeInjectionTargetTest14)
                        .addAsModule(jeeInjectionTargetTestJSP2117);
        EnterpriseArchive HelloAppClient72 = ShrinkWrap.create(EnterpriseArchive.class,"HelloAppClient.ear")
                        .add(new FileAsset(new File("test-applications/HelloAppClient.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(HelloAppClient116);
        EnterpriseArchive dynamicallyAddedBeans26 = ShrinkWrap.create(EnterpriseArchive.class,"dynamicallyAddedBeans.ear")
                        .add(new FileAsset(new File("test-applications/dynamicallyAddedBeans.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(dynamicallyAddedBeans11)
                        .addAsLibrary(dynamicallyAddedBeans83);
        EnterpriseArchive scopeActivationDestructionSecondApp28 = ShrinkWrap.create(EnterpriseArchive.class,"scopeActivationDestructionSecondApp.ear")
                        .add(new FileAsset(new File("test-applications/scopeActivationDestructionSecondApp.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(scopeActivationDestructionSecondApp123);
        EnterpriseArchive scopeActivationDestructionTests160 = ShrinkWrap.create(EnterpriseArchive.class,"scopeActivationDestructionTests.ear")
                        .add(new FileAsset(new File("test-applications/scopeActivationDestructionTests.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(scopeActivationDestructionTests126);
        EnterpriseArchive warLibAccessBeansInWar21 = ShrinkWrap.create(EnterpriseArchive.class,"warLibAccessBeansInWar.ear")
                        .add(new FileAsset(new File("test-applications/warLibAccessBeansInWar.ear/resources/META-INF/application.xml")), "/META-INF/application.xml")
                        .addAsModule(warLibAccessBeansInWar157)
                        .addAsModule(warLibAccessBeansInWar2132);
        EnterpriseArchive DeltaSpikeTCCL97 = ShrinkWrap.create(EnterpriseArchive.class,"DeltaSpikeTCCL.ear");
        
        EnterpriseArchive jarInRarEar = ShrinkWrap.create(EnterpriseArchive.class,"jarInRar.ear")
                        .addAsModule(jarInRarEjb173)
                        .addAsModule(jarInRar173);

        exportAppToServer("aparServer", vetoedEJBStartup99);
        exportAppToServer("cdi12AfterTypeDiscoveryServer", afterTypeDiscoveryApp30, "/apps");
        exportAppToServer("cdi12AppExtensionServer", applicationExtension80, "/apps");
        exportAppToServer("cdi12AlterableContextServer", alterableContextsApp162, "/apps");
        exportAppToServer("cdi12BasicServer", beanManagerLookupApp152);
        exportAppToServer("cdi12BasicServer", cdi12helloworldtest106);
        exportAppToServer("cdi12BasicServer", nonContextual7);
        exportAppToServer("cdi12BasicServer", ObservesInitializedInJars170);
        exportAppToServer("cdi12BasicServer", rootClassLoaderApp105);
        exportAppToServer("cdi12BasicServer", simpleJSPApp55);
        exportAppToServer("cdi12BasicServer", TestClassLoadPrereqLogger8);
        exportAppToServer("cdi12BasicServer", WebListener163);
        exportAppToServer("cdi12BeanDiscoveryModeNoneServer", beanDiscoveryModeNone114);
        exportAppToServer("cdi12BeanLifecycleTestServer", scopeActivationDestructionSecondApp28);
        exportAppToServer("cdi12BeanLifecycleTestServer", scopeActivationDestructionTests160);
        exportAppToServer("cdi12BeansXmlValidationServer", invalidBeansXml13);
        exportAppToServer("cdi12CDICurrentServer", cdiCurrentTest66);
        exportAppToServer("cdi12ClassExclusionTestServer", classExclusion85);
        exportAppToServer("cdi12ClassExclusionTestServer", TestVetoedAlternative141);
        exportAppToServer("cdi12ClassMasking", maskedClass38);
        exportAppToServer("cdi12DecoratorOnBuiltInBeansTestServer", defaultDecoratorApp139);
        exportAppToServer("cdi12DeltaSpikeServer", deltaspikeTest137, "/apps");
        exportAppToServer("cdi12DisableImplicitBeanArchiveServer", implicitBeanArchiveDisabled69);
        exportAppToServer("cdi12DynamicallyAddedBeansServer", dynamicallyAddedBeans26);
        exportAppToServer("cdi12EJB32FullServer", ejbMisc16);
        exportAppToServer("cdi12EJB32Server", aroundConstructApp115);
        exportAppToServer("cdi12EJB32Server", ejbScope46);
        exportAppToServer("cdi12EJB32Server", ejbTimer51);
        exportAppToServer("cdi12EJB32Server", injectParameters167);
        exportAppToServer("cdi12EJB32Server", multipleEJBsSingleClass90);
        exportAppToServer("cdi12EJB32Server", multipleWars87);
        exportAppToServer("cdi12EJB32Server", postConstructErrorMessageApp81);
        exportAppToServer("cdi12EjbConstructorInjectionServer", ejbConstructorInjection133);
        exportAppToServer("cdi12EjbDefInXmlServer", archiveWithNoBeansXml165);
        exportAppToServer("cdi12EjbDefInXmlServer", ejbArchiveWithNoAnnotations82);
        exportAppToServer("cdi12EjbDefInXmlServer", ejbJarInWarNoAnnotations88);
        exportAppToServer("cdi12EjbDiscoveryServer", ejbDiscovery22);
        exportAppToServer("cdi12EventMetadataServer", eventMetaData94);
        exportAppToServer("cdi12GlobalPriorityServer", globalPriorityApp61);
        exportAppToServer("cdi12ImplicitServer", implicitBeanArchive95);
        exportAppToServer("cdi12ImplicitServer", implicitBeanDiscovery27);
        exportAppToServer("cdi12ImplicitServer", implicitEJBInWar125);
        exportAppToServer("cdi12ImplicitServer", implicitWarApp121);
        exportAppToServer("cdi12InjectInjectionPointServer", injectInjectionPoint37);
        exportAppToServer("cdi12JarInRar", jarInRarEar);
        exportAppToServer("cdi12JEEInjectionTargetTestServer", jeeInjectionTargetTest43);
        exportAppToServer("cdi12JNDIServer", jndiLookup149);
        exportAppToServer("cdi12JSFServer", simpleJSFApp91);
        exportAppToServer("cdi12JSFWithSharedLibServer", simpleJSFWithSharedLib57, "/apps");
        exportAppToServer("cdi12ManagedBeanTestServer", managedBeanApp15);
        exportAppToServer("cdi12MultipleBeansXmlServer", multipleBeansXml75);
        exportAppToServer("cdi12MultiModuleServer", multiModuleApp170);
        exportAppToServer("cdi12MultiModuleServer", multiModuleApp2171);
        exportAppToServer("cdi12NoBeansXmlValidationServer", invalidBeansXml13);
        exportAppToServer("cdi12PassivationServer", passivationBean65);
        exportAppToServer("cdi12PassivationServer", transientReferenceInSessionPersist40);
        exportAppToServer("cdi12RuntimeExtensionServer", helloWorldExension153);
        exportAppToServer("cdi12RuntimeExtensionServer", multipleWars259);
        exportAppToServer("cdi12SharedLibraryServer", sharedLibraryAppWeb171, "/apps");
        exportAppToServer("cdi12SharedLibraryServer", sharedLibraryNoInjectionApp104, "/apps");
        exportAppToServer("cdi12StatefulSessionBeanServer", statefulSessionBeanInjection127);
        exportAppToServer("cdi12ValidatorInJarServer", TestValidatorInJar36);
        exportAppToServer("cdi12WarLibsAccessWarServer", warLibAccessBeansInWar21);
        exportAppToServer("cdi12WebBeansBeansXmlServer", webBeansBeansXmlDecorators103);
        exportAppToServer("cdi12WebBeansBeansXmlServer", webBeansBeansXmlInterceptors39);
        exportAppToServer("cdi12WebServicesServer", resourceWebServices128);
        exportAppToServer("cdi12WithAnnotationsServer", withAnnotationsApp161);
        exportAppToServer("cdiServerForClient", HelloAppClient72);
        exportAppToServer("conversationFilterServer", appConversationFilter120);
        exportAppToServer("nonContextualServer", appNonContextual147);
        exportAppToServer("packagePrivateAccessServer", packagePrivateAccessApp168);
        exportAppToServer("visTestServer", visTest107, "/apps");
        
        exportAppToServer("cdi12SharedLibraryServer", sharedLibrary33, "/InjectionSharedLibrary");
        exportAppToServer("cdi12JSFWithSharedLibServer", sharedLibrary33, "/InjectionSharedLibrary");
        
        exportAppToClient("cdiClientSecurity", appClientSecurity122, "/apps");
        exportAppToClient("cdiClient", HelloAppClient72, "/apps");
        exportAppToClient("cdiClientAdvanced", appClientAdvanced151, "/apps");
        exportAppToClient("visTestClient", visTest107, "/apps");
        
        exportBundle(helloWorldBundle); 


    }
    
    private static void exportAppToShared(Archive archive, String path){
        String destinationPath = "publish/shared/" + path;
        exportArchive(archive, destinationPath);
    }
    
    private static void exportAppToServer(String serverName, Archive archive){
        exportAppToServer(serverName, archive, "/dropins");
    }

    private static void exportAppToServer(String serverName, Archive archive, String path){
        String destinationPath = "publish/servers/" + serverName + path;
        exportArchive(archive, destinationPath);
    }
    
    private static void exportBundle(Archive archive){
        ShrinkHelper.exportArtifact(archive, "publish/bundles/");
    }
    
    private static void exportAppToClient(String clientName, Archive archive){
        exportAppToServer(clientName, archive, "/dropins");
    }

    private static void exportAppToClient(String clientName, Archive archive, String path){
        String destinationPath = "publish/clients/" + clientName + path;
        exportArchive(archive, destinationPath);
    }
    
    private static void exportArchive(Archive archive, String path){
        ShrinkHelper.exportArtifact(archive, path);
    }

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
