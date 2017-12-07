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
 * Tests that do not yet work on CDI-2.0
 */
@RunWith(Suite.class)
@SuiteClasses({
                //TODO segmentation errors might stem from a single source in CDI12BasicTest

                BeanManagerLookupTest.class, //segmentation error
                CDI12JNDIBeanManagerTest.class, //segmentation error
                ClassLoadPrereqLogger.class, //segmentation error
                EJB32Test.class,  //[ERROR   ] CWWKZ0002E: An exception occurred while starting the application ejbMisc. The exception message was: com.ibm.ws.container.service.state.StateChangeException: org.jboss.weld.exceptions.DefinitionException: WELD-000088: Observer method must be static or local business method:  [EnhancedAnnotatedMethodImpl] public com.ibm.ws.cdi12test.remoteEjb.ejb.TestObserver.observeRemote(@Observes EJBEvent) 	at com.ibm.ws.cdi12test.remoteEjb.ejb.TestObserver.observeRemote(TestObserver.java:0)
                ImplicitBeanArchivesDisabledTest.class,//ImplicitBeanArchivesDisabledTest.java App runs but returns wrong ouput: The body of Web Browser 6 Response 1 does not contain: Car Bike No Plane! - Response body: Car Bike
                MultiModuleAppTest.class, //segmentation error
                NonContextualTests.class, //segmentation error
                ObservesInitializedTest.class, //segmentation error
                RootClassLoaderTest.class,//segmentation error
                SessionDestroyTests.class,//segmentation error
                SimpleJSPTest.class,//segmentation error
               

})
public class CDI12Suite {
    
    private static final Logger LOG = Logger.getLogger(FATSuite.class.getName());
    

}
