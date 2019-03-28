/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.equinox.region.Region;
import org.eclipse.equinox.region.RegionDigraph;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.feature.ServerStarted;
import com.ibm.ws.kernel.feature.internal.BundleList.FeatureResourceHandler;
import com.ibm.ws.kernel.feature.internal.FeatureManager.FeatureChange;
import com.ibm.ws.kernel.feature.internal.FeatureManager.ProvisioningMode;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureRepository;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureResourceImpl;
import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Result;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry.BundleRepositoryHolder;
import com.ibm.ws.kernel.provisioning.ContentBasedLocalBundleRepository;
import com.ibm.ws.kernel.service.location.internal.SymbolRegistry;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

import junit.framework.AssertionFailedError;
import test.common.SharedLocationManager;
import test.common.SharedOutputManager;
import test.utils.SharedConstants;
import test.utils.TestUtils;
import test.utils.TestUtils.TestBundleRevision;
import test.utils.TestUtils.TestBundleStartLevel;
import test.utils.TestUtils.TestFrameworkStartLevel;
import test.utils.TestUtils.TestFrameworkWiring;

/**
 *
 */
@RunWith(JMock.class)
public class FeatureManagerTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=audit=enabled:featureManager=all=enabled");

    static final String serverName = "FeatureManagerTest";
    static final Collection<ProvisioningFeatureDefinition> noKernelFeatures = Collections.<ProvisioningFeatureDefinition> emptySet();
    static WsLocationAdmin locSvc;
    static Field bListResources;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();

        bListResources = BundleList.class.getDeclaredField("resources");
        bListResources.setAccessible(true);

        File root = SharedConstants.TEST_DATA_FILE.getCanonicalFile();
        File lib = new File(root, "lib");

        TestUtils.setUtilsInstallDir(root);
        TestUtils.setKernelUtilsBootstrapLibDir(lib);
        TestUtils.clearBundleRepositoryRegistry();

        locSvc = (WsLocationAdmin) SharedLocationManager.createLocations(SharedConstants.TEST_DATA_DIR, serverName);
        TestUtils.recursiveClean(locSvc.getServerResource(null));

        BundleRepositoryRegistry.initializeDefaults(serverName, true);

        SymbolRegistry.getRegistry().addStringSymbol("websphere.kernel", "kernelCore-1.0.mf");
        SymbolRegistry.getRegistry().addStringSymbol("websphere.log.provider", "defaultLogging-1.0.mf");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();

        TestUtils.setUtilsInstallDir(null);
        TestUtils.setKernelUtilsBootstrapLibDir(null);
        TestUtils.clearBundleRepositoryRegistry();
    }

    Mockery context = new JUnit4Mockery();

    final Bundle mockBundle = context.mock(Bundle.class);
    final BundleContext mockBundleContext = context.mock(BundleContext.class);
    final ComponentContext mockComponentContext = context.mock(ComponentContext.class);
    final ExecutorService executorService = context.mock(ExecutorService.class);
    final VariableRegistry variableRegistry = context.mock(VariableRegistry.class);
    final MockServiceReference<WsLocationAdmin> mockLocationService = new MockServiceReference<WsLocationAdmin>(locSvc);
    final MockServiceReference<ScheduledExecutorService> mockScheduledExecutorService = new MockServiceReference<ScheduledExecutorService>(context.mock(ScheduledExecutorService.class));
    final RegionDigraph mockDigraph = context.mock(RegionDigraph.class);
    final RuntimeUpdateManager runtimeUpdateManager = context.mock(RuntimeUpdateManager.class);
    final RuntimeUpdateNotification appForceRestart = context.mock(RuntimeUpdateNotification.class, "appForceRestart");
    final RuntimeUpdateNotification featureBundlesResolved = context.mock(RuntimeUpdateNotification.class, "featureBundlesResolved");
    final RuntimeUpdateNotification featureBundlesProcessed = context.mock(RuntimeUpdateNotification.class, "featureBundlesProcessed");
    final RuntimeUpdateNotification featureUpdatesCompleted = context.mock(RuntimeUpdateNotification.class, "featureUpdatesCompleted");
    final Region mockKernelRegion = context.mock(Region.class);
    final TestBundleRevision mockBundleRevision = context.mock(TestBundleRevision.class);
    final TestBundleStartLevel mockBundleStartLevel = context.mock(TestBundleStartLevel.class);
    final TestFrameworkStartLevel testFrameworkStartLevel = new TestFrameworkStartLevel();
    final TestFrameworkWiring testFrameworkWiring = new TestFrameworkWiring();

    FeatureManager fm;
    Provisioner provisioner;

    @Before
    public void setUp() throws Exception {
        fm = new FeatureManager();
        fm.featureChanges.clear();
        fm.onError = OnError.WARN;
        fm.bundleContext = mockBundleContext;
        fm.fwStartLevel = testFrameworkStartLevel;
        fm.setExecutorService(executorService);
        fm.setLocationService(locSvc);
        fm.setVariableRegistry(variableRegistry);
        fm.setDigraph(mockDigraph);
        fm.setRuntimeUpdateManager(runtimeUpdateManager);

        // We have to activate the FeatureManager here so that the component context will be
        // propagated to the AtomicServiceReferences for the locationService and executorService
        try {
            context.checking(new Expectations() {
                {
                    allowing(mockComponentContext).getBundleContext();
                    will(returnValue(mockBundleContext));

                    allowing(mockBundleContext).getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
                    will(returnValue(mockBundle));
                    allowing(mockBundle).getBundleContext();
                    will(returnValue(mockBundleContext));
                    allowing(mockBundleContext).getBundles();
                    will(returnValue(new Bundle[0]));

                    //allow mock calls from the BundleInstallOriginBundleListener <init>
                    allowing(mockBundleContext).getBundle();
                    will(returnValue(mockBundle));
                    one(mockBundle).getDataFile("bundle.origin.cache");
                    //allow the BundleInstallOriginBundleListener to get a ScheduledExecutorService
                    //and schedule a purge for the future
                    one(mockBundleContext).getServiceReference(ScheduledExecutorService.class);
                    will(returnValue(mockScheduledExecutorService));
                    one(mockBundleContext).getService(mockScheduledExecutorService);
                    will(returnValue(mockScheduledExecutorService.getService()));
                    one(mockScheduledExecutorService.getService()).schedule(with(any(BundleInstallOriginBundleListener.class)), with(any(Integer.class)),
                                                                            with(any(TimeUnit.class)));
                    one(mockBundleContext).ungetService(mockScheduledExecutorService);

                    allowing(mockBundleContext).addBundleListener(with(any(BundleListener.class)));

                    allowing(mockBundleContext).getProperty("websphere.os.extension");
                    will(returnValue(null));

                    allowing(mockBundleContext).removeBundleListener(with(any(BundleListener.class)));

                    one(mockBundle).adapt(FrameworkStartLevel.class);
                    will(returnValue(testFrameworkStartLevel));

                    one(mockBundle).adapt(FrameworkWiring.class);
                    will(returnValue(testFrameworkWiring));

                    allowing(mockBundleContext).getProperty(with("com.ibm.ws.liberty.content.request"));
                    will(returnValue(null));

                    allowing(mockBundleContext).getProperty(with("com.ibm.ws.liberty.feature.request"));
                    will(returnValue(null));

                    allowing(mockBundleContext).getProperty(with("com.ibm.ws.kernel.classloading.apiPackagesToHide"));
                    will(returnValue(null));

                    allowing(mockBundleContext).getProperty(with("wlp.process.type"));
                    will(returnValue(BootstrapConstants.LOC_PROCESS_TYPE_SERVER));

                    allowing(mockComponentContext).locateService(with("locationService"), with(any(ServiceReference.class)));
                    will(returnValue(locSvc));
                    allowing(mockComponentContext).locateService(with("executorService"), with(any(ServiceReference.class)));
                    will(returnValue(executorService));
                    allowing(mockComponentContext).locateService(with("variableRegistry"), with(any(ServiceReference.class)));
                    will(returnValue(variableRegistry));

                    allowing(executorService).execute(with(any(Runnable.class)));
                    one(variableRegistry).addVariable(with("feature:usr"), with("${usr.extension.dir}/lib/features/"));

                    allowing(mockBundleContext).getBundles();
                    allowing(mockBundleContext).registerService(with(any(Class.class)), with(any(ServerStarted.class)), with(any(Dictionary.class)));
                    allowing(mockBundleContext).registerService(with(any(String[].class)), with(any(PackageInspectorImpl.class)), with(any(Dictionary.class)));

                    // allow calls to digraph by Provisioner
                    allowing(mockDigraph).getRegion(mockBundle);
                    will(returnValue(mockKernelRegion));
                    allowing(mockKernelRegion).getName();
                    will(returnValue("kernel.region"));
                    allowing(mockDigraph).copy();
                    will(returnValue(mockDigraph));

                    allowing(runtimeUpdateManager).createNotification(RuntimeUpdateNotification.FEATURE_UPDATES_COMPLETED);
                    will(returnValue(featureUpdatesCompleted));
                    allowing(runtimeUpdateManager).createNotification(RuntimeUpdateNotification.APP_FORCE_RESTART);
                    will(returnValue(appForceRestart));
                    allowing(runtimeUpdateManager).createNotification(RuntimeUpdateNotification.FEATURE_BUNDLES_RESOLVED);
                    will(returnValue(featureBundlesResolved));
                    allowing(appForceRestart).setResult(with(any(Boolean.class)));
                    allowing(featureBundlesResolved).setResult(with(any(Boolean.class)));
                    allowing(runtimeUpdateManager).getNotification(RuntimeUpdateNotification.FEATURE_BUNDLES_PROCESSED);
                    will(returnValue(featureBundlesProcessed));
                    allowing(featureBundlesProcessed).waitForCompletion();
                    allowing(featureUpdatesCompleted).setResult(with(any(Boolean.class)));

                    allowing(mockBundleContext).getProperty("wlp.liberty.boot");
                    will(returnValue(null));

                    allowing(mockBundleContext).getDataFile("feature.fix.cache");
                    will(returnValue(File.createTempFile("feature.fix.cache", null)));
                }
            });
            fm.activate(mockComponentContext, new HashMap<String, Object>());
        } catch (Throwable t) {
            outputMgr.failWithThrowable("setUp", t);
        }

        fm.featureRepository = new FeatureRepository();
        fm.bundleCache = new BundleList(fm);

        fm.featureRepository.init();
        fm.bundleCache.init();

        provisioner = new Provisioner(fm, null);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation, this keeps
        // things sane
        outputMgr.resetStreams();

        TestUtils.recursiveClean(locSvc.getServerResource(null));
    }

    @Test(expected = NullPointerException.class)
    public void testUnitializedProvisioner() {
        // No provisioner, should blow up with NPE
        FeatureChange featureChange = new FeatureChange(runtimeUpdateManager, ProvisioningMode.INITIAL_PROVISIONING, new String[] { "notexist" });
        featureChange.createNotifications();
        fm.updateFeatures(locSvc, null, null, featureChange, 1L);
    }

    @Test(expected = IllegalStateException.class)
    public void testUnitializedProvisionerWithFailOnError() {
        final String m = "testUnitializedProvisionerWithFailOnError";
        try {
            fm.onError = OnError.FAIL;

            context.checking(new Expectations() {
                {
                    one(mockBundle).stop();
                }
            });

            // onError is FAIL which means we expect an
            // IllegalStateException
            FeatureChange featureChange = new FeatureChange(runtimeUpdateManager, ProvisioningMode.INITIAL_PROVISIONING, new String[] { "notexist" });
            featureChange.createNotifications();
            fm.updateFeatures(locSvc, null, null, featureChange, 1L);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testUpdateFeaturesNotExistFeature() {
        final String m = "testUpdateFeaturesNotExistFeature";
        try {
            // This shouldn't do anything: no return to test-- make sure it doesn't
            // blow up w/ NPE!
            fm.onError = OnError.WARN;

            String missingFeature = "CWWKF0001E";

            FeatureChange featureChange = new FeatureChange(runtimeUpdateManager, ProvisioningMode.INITIAL_PROVISIONING, new String[] { "notexist", "NotExist" });
            featureChange.createNotifications();
            fm.updateFeatures(locSvc, provisioner, null, featureChange, 1L);
            assertTrue("An error should have been issued about the missing feature", outputMgr.checkForMessages(missingFeature));
            assertTrue("The error message should contain notexist", outputMgr.checkForMessages("notexist"));
            assertFalse("The error message should not contain NotExist", outputMgr.checkForMessages("NotExist"));

            // We put notexist on there twice -- this should condense to ONE error message
            // for only one missing feature
            String err = outputMgr.getCapturedErr();
            int pos = err.indexOf(missingFeature);
            assertTrue("Error output should contain the error message: " + pos, pos > -1);
            pos = err.indexOf(missingFeature, pos + missingFeature.length());
            assertFalse("Error output should not contain a second error message: " + pos + ": " + err, pos > -1);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateFeaturesNotExistFeatureWithFailOnError() {
        final String m = "testUpdateFeaturesNotExistFeatureWithFailOnError";
        try {
            fm.onError = OnError.FAIL;

            context.checking(new Expectations() {
                {
                    one(mockBundle).stop();
                }
            });

            // continueOnError=false means that we expect an IllegalStateException due
            // to the non-existant feature 'notexist'
            FeatureChange featureChange = new FeatureChange(runtimeUpdateManager, ProvisioningMode.INITIAL_PROVISIONING, new String[] { "notexist" });
            featureChange.createNotifications();
            fm.updateFeatures(locSvc, provisioner, null, featureChange, 1L);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testUpdateFeaturesMixedWithFailOnError() {
        final String m = "testUpdateFeaturesMixedWithFailOnError";
        try {
            fm.onError = OnError.FAIL;

            final AtomicBoolean installBundlesCalled = new AtomicBoolean(false);

            Provisioner provisioner = new Provisioner(fm, null) {
                /** Override installBundles to verify that it is not called in this scenario */
                @Override
                public void installBundles(BundleContext bContext,
                                           BundleList bundleList,
                                           BundleInstallStatus installStatus,
                                           int minStartLevel, int defaultStartLevel, int defaultInitialStartLevel,
                                           WsLocationAdmin locSvc) {
                    installBundlesCalled.set(true);
                }
            };

            context.checking(new Expectations() {
                {
                    one(mockBundle).stop();
                }
            });

            // continueOnError=false means that we expect an IllegalStateException due
            // to the non-existant feature 'notexist'; installBundles should not be called
            // even though there is one existing feature 'good' in the list
            FeatureChange featureChange = new FeatureChange(runtimeUpdateManager, ProvisioningMode.INITIAL_PROVISIONING, new String[] { "good", "notexist" });
            featureChange.createNotifications();
            fm.updateFeatures(locSvc, provisioner, null, featureChange, 1L);
            assertFalse("installBundles should not have been called", installBundlesCalled.get());
        } catch (IllegalStateException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * @param bundleList
     * @return
     */
    @SuppressWarnings("unchecked")
    protected List<FeatureResource> getResources(BundleList bundleList) {
        try {
            return (List<FeatureResource>) bListResources.get(bundleList);
        } catch (Exception e) {
            Error err = new AssertionFailedError("Could not obtain the value of FeatureCache.resources");
            err.initCause(e);
            throw err;
        }
    }

    /**
     * Test method
     */
    @Test
    public void testNoEnabledFeatures() {
        final String m = "testNoEnabledFeatures";
        try {
            fm.onError = OnError.WARN;
            FeatureChange featureChange = new FeatureChange(runtimeUpdateManager, ProvisioningMode.INITIAL_PROVISIONING, new String[0]);
            featureChange.createNotifications();
            fm.updateFeatures(locSvc, new Provisioner(fm, null), null, featureChange, 1L);

            // SharedOutputMgr routing trace to standard out
            assertTrue("Assert 'no enabled features' warning", outputMgr.checkForStandardOut("CWWKF0009W"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testActivate() {
        final String m = "testActivate";
        try {
            context.checking(new Expectations() {
                {
                    allowing(mockComponentContext).getBundleContext();
                    will(returnValue(mockBundleContext));

                    allowing(mockBundleContext).getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
                    will(returnValue(mockBundle));
                    allowing(mockBundle).getBundleContext();
                    will(returnValue(mockBundleContext));
                    allowing(mockBundleContext).getBundles();
                    will(returnValue(new Bundle[0]));

                    one(mockBundle).adapt(FrameworkStartLevel.class);
                    will(returnValue(testFrameworkStartLevel));

                    one(mockBundle).adapt(FrameworkWiring.class);
                    will(returnValue(testFrameworkWiring));

                    allowing(executorService).execute(with(any(Runnable.class)));
                    one(variableRegistry).addVariable(with("feature:usr"), with("${usr.extension.dir}/lib/features/"));

                    //allow mock calls from the BundleInstallOriginBundleListener <init>
                    allowing(mockBundleContext).getBundle();
                    will(returnValue(mockBundle));
                    one(mockBundle).getDataFile("bundle.origin.cache");
                    //allow the BundleInstallOriginBundleListener to get a ScheduledExecutorService
                    //and schedule a purge for the future
                    one(mockBundleContext).getServiceReference(ScheduledExecutorService.class);
                    will(returnValue(mockScheduledExecutorService));
                    one(mockBundleContext).getService(mockScheduledExecutorService);
                    will(returnValue(mockScheduledExecutorService.getService()));
                    one(mockScheduledExecutorService.getService()).schedule(with(any(BundleInstallOriginBundleListener.class)), with(any(Integer.class)),
                                                                            with(any(TimeUnit.class)));
                    one(mockBundleContext).ungetService(mockScheduledExecutorService);

                }
            });

            Map<String, Object> componentProps = new HashMap<String, Object>();

            fm.activate(mockComponentContext, componentProps);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testUpdated() {
        final String m = "testUpdated";
        try {
            // Immediate return: no NPE
            fm.updated(null);

            context.checking(new Expectations() {
                {
                    allowing(executorService).execute(with(any(Runnable.class)));
                }
            });

            Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(OnErrorUtil.CFG_KEY_ON_ERROR, OnError.FAIL);
            fm.updated(props);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoadFeatureInclude() {
        final String m = "testLoadFeatureInclude";
        try {
            BundleInstallStatus installStatus = new BundleInstallStatus();
            Result result = FeatureManager.featureResolver.resolveFeatures(fm.featureRepository, noKernelFeatures, Collections.singleton("include"),
                                                                           Collections.<String> emptySet(), false);
            fm.reportErrors(result, Collections.<String> emptyList(), Collections.singleton("include"), installStatus);
            assertTrue("CWWKF0001E error message", outputMgr.checkForStandardErr("CWWKF0001E:"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testUpdate() {
        final String m = "testUpdate";
        try {
            // Add list of empty features
            fm.featureChanges.add(new FeatureChange(runtimeUpdateManager, ProvisioningMode.UPDATE, new String[0]));
            fm.onError = OnError.WARN;

            fm.processFeatureChanges();
            assertTrue("CWWKF0007I start audit should be in output", outputMgr.checkForMessages("CWWKF0007I"));
            assertTrue("CWWKF0008I complete audit", outputMgr.checkForMessages("CWWKF0008I"));

            fm.deactivate(ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED); // no-op
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testFeaturesRequest() {
        final String m = "testFeaturesRequest";
        try {
            // Add list of empty features
            fm.featureChanges.add(new FeatureChange(runtimeUpdateManager, ProvisioningMode.FEATURES_REQUEST, new String[0]));
            fm.onError = OnError.WARN;

            fm.processFeatureChanges();
            assertTrue("CWWKF0007I start audit should be in output", outputMgr.checkForMessages("CWWKF0007I"));
            assertTrue("CWWKF0036I finished gathering a list of required features should be in output", outputMgr.checkForMessages("CWWKF0036I"));
            assertTrue("CWWKF0008I complete audit", outputMgr.checkForMessages("CWWKF0008I"));

            fm.deactivate(ComponentConstants.DEACTIVATION_REASON_BUNDLE_STOPPED); // no-op
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testInstallStatusNull() {
        final String m = "testUpdate";
        try {
            fm.onError = OnError.FAIL;
            fm.checkInstallStatus(null);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testInstallStatusOtherException() {
        final String m = "testInstallStatusOtherException";

        try {
            fm.onError = OnError.FAIL;
            context.checking(new Expectations() {
                {
                    one(mockBundle).stop();
                }
            });

            final BundleInstallStatus iStatus = new BundleInstallStatus();
            iStatus.addOtherException(new Throwable("some other exception"));

            fm.checkInstallStatus(iStatus);
            throw new Throwable("stopFramework not called");
        } catch (IllegalStateException e) {
            assertTrue("CWWKF0004E -- other exception in stderr", outputMgr.checkForStandardErr("CWWKF0004E"));
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testMissingBundleException() {
        final String m = "testMissingBundleException";

        try {
            fm.onError = OnError.FAIL;
            context.checking(new Expectations() {
                {
                    one(mockBundle).stop();
                }
            });

            final BundleInstallStatus iStatus = new BundleInstallStatus();
            iStatus.addMissingBundle(new FeatureResourceImpl("missing", Collections.<String, String> emptyMap(), null, null));

            try {
                fm.checkInstallStatus(iStatus);
                throw new Throwable("stopFramework not called");
            } catch (IllegalStateException e) {
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testInstallStatusInstallException() {
        final String m = "testInstallStatusInstallException";

        try {
            fm.onError = OnError.FAIL;
            context.checking(new Expectations() {
                {
                    one(mockBundle).stop();
                }
            });

            final BundleInstallStatus iStatus = new BundleInstallStatus();
            iStatus.addInstallException("bundle", new Throwable("exception-b1"));
            iStatus.addInstallException("bundle2", new Throwable("exception-b2"));

            try {
                fm.checkInstallStatus(iStatus);
                throw new Throwable("stopFramework not called");
            } catch (IllegalStateException e) {
                assertTrue("CWWKF0003E -- install exception in stderr", outputMgr.checkForStandardErr("CWWKF0003E"));
                // message should be issued once for each bundle install exception
                assertTrue("exception-b1 -- install exception in stderr", outputMgr.checkForStandardErr("exception-b1"));
                assertTrue("exception-b2 -- install exception in stderr", outputMgr.checkForStandardErr("exception-b2"));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testBundleStatusException() {
        final String m = "testBundleStatusException";

        try {
            fm.onError = OnError.FAIL;
            context.checking(new Expectations() {
                {
                    one(mockBundle).stop();
                }
            });

            final Bundle mockBundle2 = context.mock(Bundle.class, "bundle2");

            final BundleLifecycleStatus bStatus = new BundleLifecycleStatus();
            bStatus.addStartException(mockBundle, new Throwable("terrible thing"));
            bStatus.addStartException(mockBundle2, new Throwable("horrible other thing"));

            try {
                fm.checkBundleStatus(bStatus);
                throw new Throwable("stopFramework not called");
            } catch (IllegalStateException e) {
                assertTrue("CWWKF0005E -- exception in stderr", outputMgr.checkForStandardErr("CWWKF0005E"));

                // message should be issued once for each bundle exception
                assertTrue("terrible thing -- exception in stderr", outputMgr.checkForStandardErr("terrible thing"));
                assertTrue("horrible other thing -- exception in stderr", outputMgr.checkForStandardErr("horrible other thing"));
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testLoadFeatureDefinition() {
        final String m = "testLoadFeatureDefinition";

        try {
            BundleInstallStatus installStatus = new BundleInstallStatus();

            // make sure handles just a :
            Result result = FeatureManager.featureResolver.resolveFeatures(fm.featureRepository, noKernelFeatures, Collections.singleton(":"), Collections.<String> emptySet(),
                                                                           false);
            fm.reportErrors(result, Collections.<String> emptyList(), Collections.singleton(":"), installStatus);
            assertTrue("CWWKF0001E error message", outputMgr.checkForStandardErr("CWWKF0001E:"));
            assertTrue("There should be missing features", installStatus.featuresMissing());
            assertTrue("installStatus should contain : as missing feature", installStatus.getMissingFeatures().contains(":"));
            outputMgr.resetStreams();
            installStatus.getMissingFeatures().clear();

            // make sure handles nothing after :
            // CWWKF0001E: A feature definition could not be found for usr:
            result = FeatureManager.featureResolver.resolveFeatures(fm.featureRepository, noKernelFeatures, Collections.singleton("usr:"), Collections.<String> emptySet(), false);
            fm.reportErrors(result, Collections.<String> emptyList(), Collections.singleton("usr:"), installStatus);
            assertTrue("CWWKF0001E error message", outputMgr.checkForStandardErr("CWWKF0001E:"));
            assertTrue("specification of usr:", outputMgr.checkForStandardErr("usr:"));
            assertTrue("installStatus shuold contain : as missing feature", installStatus.getMissingFeatures().contains("usr:"));
            outputMgr.resetStreams();
            installStatus.getMissingFeatures().clear();

            // give it one it should not find
            // CWWKF0001E: A feature definition could not be found for usr:bad
            result = FeatureManager.featureResolver.resolveFeatures(fm.featureRepository, noKernelFeatures, Collections.singleton("usr:bad"), Collections.<String> emptySet(),
                                                                    false);
            fm.reportErrors(result, Collections.<String> emptyList(), Collections.singleton("usr:bad"), installStatus);
            assertTrue("CWWKF0001E error message", outputMgr.checkForStandardErr("CWWKF0001E:"));
            assertTrue("specification of usr:bad", outputMgr.checkForStandardErr("usr:bad"));
            assertTrue("installStatus should contain : as missing feature", installStatus.getMissingFeatures().contains("usr:bad"));

            // now test with a 'core' feature with no ':'
            result = FeatureManager.featureResolver.resolveFeatures(fm.featureRepository, noKernelFeatures, Collections.singleton("coreMissing-1.0"),
                                                                    Collections.<String> emptySet(), false);
            fm.reportErrors(result, Collections.<String> emptyList(), Collections.singleton("coreMissing-1.0"), installStatus);
            assertTrue("CWWKF0001E error message", outputMgr.checkForStandardErr("CWWKF0001E:"));
            assertTrue("specification of coreMissing-1.0", outputMgr.checkForStandardErr("coreMissing-1.0"));
            assertTrue("installStatus should contain : as missing feature", installStatus.getMissingFeatures().contains("coreMissing-1.0"));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testReadWriteCache() throws IOException {
        final String m = "testReadWriteCache";

        WsResource cacheFile = locSvc.getServerWorkareaResource(m);
        final AtomicLong id = new AtomicLong();

        try {
            context.checking(new Expectations() {
                {
                    exactly(2).of(mockBundle).getBundleId();
                    will(returnValue(id.incrementAndGet()));
                }
            });

            if (cacheFile.exists())
                cacheFile.delete();

            assertFalse("Cache file should be a unique temp file (should not exist)", cacheFile.exists());

            InputStream is = TestUtils.createValidFeatureManifestStream("simple.feature-1.0",
                                                                        "simple;version=\"[0.1,0.2)\", simpleTwo;version=\"[2.0, 2.0.100)\"");
            SubsystemFeatureDefinitionImpl definitionImpl = new SubsystemFeatureDefinitionImpl("", is);
            assertEquals("Subsystem should be for core repository (empty string)", "", definitionImpl.getBundleRepositoryType());

            final BundleList list = new BundleList(cacheFile, fm);
            list.addAll(definitionImpl, fm);
            assertEquals("List should have 2 elements", 2, getResources(list).size());

            list.foreach(new FeatureResourceHandler() {
                @Override
                public boolean handle(FeatureResource fr) {
                    String bundleRepositoryType = fr.getBundleRepositoryType();
                    BundleRepositoryHolder bundleRepositoryHolder = fm.getBundleRepositoryHolder(bundleRepositoryType);
                    ContentBasedLocalBundleRepository lbr = bundleRepositoryHolder.getBundleRepository();

                    File f = lbr.selectBundle(null, fr.getSymbolicName(), fr.getVersionRange());
                    WsResource resource = locSvc.asResource(f, f.isFile());

                    list.createAssociation(fr, mockBundle, resource, fr.getStartLevel());
                    return true;
                }
            });

            list.store();
            assertTrue("Cache file should exist after cache is stored", cacheFile.exists());

            BundleList list2 = new BundleList(cacheFile, fm);
            list2.init();
            assertEquals("List should have two elements", 2, getResources(list2).size());

            List<?> res1 = getResources(list);
            List<?> res2 = getResources(list2);
            for (Object o1 : res1) {
                boolean found = false;
                for (Object o2 : res2) {
                    if (match((FeatureResource) o1, (FeatureResource) o2)) {
                        found = true;
                        break;
                    }
                }
                assertTrue("List read from cache should contain element from original list: " + o1, found);
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    private boolean match(FeatureResource o1, FeatureResource o2) {
        System.out.println("o1: " + o1.toString() + " , " + o1.getMatchString() + " , " + o1.getLocation());
        System.out.println("o2: " + o2.toString() + " , " + o2.getMatchString() + " , " + o2.getLocation());

        if (!o1.getMatchString().equals(o2.getMatchString()))
            return false;

        return true;
    }
}