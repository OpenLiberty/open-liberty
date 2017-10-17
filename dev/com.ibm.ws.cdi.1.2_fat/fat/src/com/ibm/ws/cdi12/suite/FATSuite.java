/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012, 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.cdi12.suite;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

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

    /**
     * @see {@link FatLogHandler#generateHelpFile()}
     */
    @BeforeClass
    public static void generateHelpFile() {
        FatLogHandler.generateHelpFile();
    }

}
