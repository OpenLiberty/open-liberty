/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.client.internal.jaas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.security.jaas.common.JAASChangeNotifier;
import com.ibm.ws.security.jaas.common.JAASConfiguration;
import com.ibm.ws.security.jaas.common.JAASConfigurationFactory;
import com.ibm.ws.security.jaas.common.JAASLoginContextEntry;
import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;
import com.ibm.ws.security.jaas.common.internal.JAASLoginContextEntryImpl;
import com.ibm.ws.security.jaas.common.internal.JAASSecurityConfiguration;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 *
 */
public class JAASClientServiceTest {
    private static SharedOutputManager outputMgr;

    protected final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    protected ComponentContext componentContext = null;
    private final String pid = "pid";
    private final String loginContextEntryID = "myLoginContextEntry";
    private final String jaasLoginModuleID = "myJaasLoginModule";
    private final String[] loginModuleIds = new String[] { pid };

    private final JAASChangeNotifier jaasChangeNotifier = mock.mock(JAASChangeNotifier.class);
    private final ServiceReference<JAASChangeNotifier> jaasChangeNotifierRef = mock.mock(ServiceReference.class, "jaasChangeNotifierRef");

    protected final ServiceReference<JAASLoginContextEntry> jaasLoginContextEntryRef = mock.mock(ServiceReference.class, JAASClientService.KEY_JAAS_LOGIN_CONTEXT_ENTRY + "Ref");

    private final ServiceReference<JAASConfiguration> jaasConfigurationRef = mock.mock(ServiceReference.class, "jaasConfigurationRef");
    private final JAASConfiguration jaasConfiguration = mock.mock(JAASConfiguration.class, "TestJaasConfiguration");

    private final static Map<String, List<AppConfigurationEntry>> jaasConfigurationEntries = new HashMap<String, List<AppConfigurationEntry>>();

    protected final ServiceReference<JAASLoginModuleConfig> jaasLoginModuleConfigRef = mock.mock(ServiceReference.class, JAASClientService.KEY_JAAS_LOGIN_MODULE_CONFIG + "Ref");
    protected final ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig> jaasLoginModuleConfigs = new ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig>(JAASClientService.KEY_JAAS_LOGIN_MODULE_CONFIG
                                                                                                                                                                           + "s");

    protected final ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries = new ConcurrentServiceReferenceMap<String, JAASLoginContextEntry>(JAASClientService.KEY_JAAS_LOGIN_CONTEXT_ENTRY
                                                                                                                                                                            + "s");
    protected final JAASConfigurationFactory jaasConfigurationFactory = new JAASConfigurationFactory();
    private final ServiceReference<JAASConfigurationFactory> jaasConfigurationFactoryRef = mock.mock(ServiceReference.class, "Test" + JAASClientService.KEY_JAAS_CONFIG_FACTORY
                                                                                                                             + "Ref");

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        Configuration.setConfiguration(null);
    }

    @Before
    public void setUp() {
        AppConfigurationEntry appConfigurationEntry = null;
        List<AppConfigurationEntry> appConfigurationEntries = new ArrayList<AppConfigurationEntry>();
        appConfigurationEntries.add(appConfigurationEntry);
        jaasConfigurationEntries.put("pid", appConfigurationEntries);

        componentContext = createComponentContextMock();
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
        Configuration.setConfiguration(null);
        mock.assertIsSatisfied();
    }

    @Test
    public void testActivateCreatesConfiguration() {
        final String methodName = "testActivateCreatesConfiguration";
        try {
            createActivatedJAASClientService();
            Configuration jaasConfiguration = Configuration.getConfiguration();
            assertNotNull("There must be a JAAS Configuration", jaasConfiguration);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testActivateCreatesJAASConfiguration() {
        final String methodName = "testActivateCreatesJAASConfiguration";
        try {
            createActivatedJAASClientService();
            Configuration jaasConfiguration = Configuration.getConfiguration();
            assertTrue("The configuration must be a JAASConfiguration", jaasConfiguration instanceof JAASSecurityConfiguration);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testDeactivateRemovesConfiguration() {
        try {
            JAASClientService jaasClientService = createActivatedJAASClientService();
            jaasClientService.deactivate(componentContext);
            Configuration.getConfiguration();
        } catch (Throwable t) {
            try {
                assertTrue(t.toString(), t instanceof java.lang.SecurityException);
                assertEquals("The message must be 'Unable to locate a login configuration'", "Unable to locate a login configuration", t.getMessage());
            } catch (AssertionError err) {
                if (err.getCause() == null) {
                    err.initCause(t);
                }
                throw err;
            }
        }
    }

    //it doesn't make sense to try to test DS functionality using mocks on the component instances.
//    @Test
    public void testGetLoginModules_invalid() throws Exception {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(JAASLoginContextEntry.CFG_KEY_ID, "SYSTEM_WEB_INBOUND");

        final String[] refs = new String[] { "invalid", "hashtable" };
        props.put(JAASLoginContextEntry.CFG_KEY_LOGIN_MODULE_REF, refs);

        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(JAASClientService.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(null));

                allowing(jaasLoginContextEntryRef).getProperty("service.id");
                will(returnValue(1L));

                allowing(jaasLoginContextEntryRef).getProperty("service.ranking");
                will(returnValue(1L));

                allowing(jaasLoginContextEntryRef).getProperty("id");
                will(returnValue("invalidEntry"));

                allowing(jaasLoginContextEntryRef).getProperty("loginModuleRef");
                will(returnValue(refs));
            }
        });

        JAASClientService jaasService = new JAASClientService();

        JAASLoginContextEntryImpl jaasLoginContextEntryImpl = new JAASLoginContextEntryImpl();
        //jaasLoginContextEntryImpl.activate(null, props);
        jaasService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasService.setJaasLoginContextEntry(jaasLoginContextEntryRef);
        jaasService.activate(componentContext, null);

    }

    @Test
    public void activate_triggersConfigReady() throws Exception {
        mock.checking(new Expectations() {
            {
                one(jaasChangeNotifier).notifyListeners();
            }
        });
        createActivatedJAASClientServiceWithNotifier();
    }

    @Test
    public void modified_triggersConfigReady() throws Exception {
        mock.checking(new Expectations() {
            {
                exactly(2).of((jaasChangeNotifier)).notifyListeners();
            }
        });
        JAASClientService jaasClientService = createActivatedJAASClientServiceWithNotifier();
        jaasClientService.modified(null);
    }

    @Test
    public void deactivate_noNotifications() throws Exception {
        mock.checking(new Expectations() {
            {
                one(jaasChangeNotifier).notifyListeners();
            }
        });
        JAASClientService jaasClientService = createActivatedJAASClientServiceWithNotifier();
        jaasClientService.deactivate(componentContext);
        jaasClientService.configReady();
    }

    @Test
    public void unsetJaasChangeNotifier_noNotifications() throws Exception {
        mock.checking(new Expectations() {
            {
                one(jaasChangeNotifier).notifyListeners();
            }
        });
        JAASClientService jaasClientService = createActivatedJAASClientServiceWithNotifier();
        jaasClientService.unsetJaasChangeNotifier(jaasChangeNotifierRef);
        jaasClientService.configReady();
    }

    @Test
    public void verifyErrorMessageForNoLoginModuleRef() throws Exception {
        JAASClientService jaasClientService = new JAASClientService();
        mock.checking(new Expectations() {
            {
                allowing(jaasLoginContextEntryRef).getProperty("id");
                will(returnValue(loginContextEntryID));
                allowing(jaasLoginContextEntryRef).getProperty(JAASLoginContextEntryImpl.CFG_KEY_LOGIN_MODULE_REF);
                will(returnValue(null));
                allowing(jaasLoginContextEntryRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(jaasLoginContextEntryRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(1L));
            }
        });

        jaasClientService.processContextEntry(jaasLoginContextEntryRef);

        //should get error message for invalid loginModuleRef
        String expectedMsg = "CWWKS1167E:.*" + loginContextEntryID + ".*";
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr(expectedMsg));
    }

    @Test
    public void testUnsetSetJaasLoginModuleConfig() throws Exception {
        JAASClientService jaasClientService = createActivatedJAASClientService();
        jaasClientService.unsetJaasLoginModuleConfig(jaasLoginModuleConfigRef);

        assertTrue("The jaasLoginModuleConfigs Map should now be empty.",
                   jaasClientService.jaasLoginModuleConfigs.isEmpty());

        jaasClientService.setJaasLoginModuleConfig(jaasLoginModuleConfigRef);

        assertTrue("The jaasLoginModuleConfigs Map should contain an entry with " + pid, jaasClientService.jaasLoginModuleConfigs.keySet().contains(pid));
    }

    private ComponentContext createComponentContextMock() {
        final ComponentContext componentContextMock = mock.mock(ComponentContext.class, "testComponentContext");
        final BundleContext bundleContextMock = mock.mock(BundleContext.class);
        final Bundle bundle = mock.mock(Bundle.class);
        final BundleWiring wiring = mock.mock(BundleWiring.class);

        final Map<String, Object> someProps = new Hashtable<String, Object>();
        String[] values = { "value1", "value2" };
        someProps.put("otherProps", values);

        mock.checking(new Expectations() {
            {
                allowing(componentContextMock).getBundleContext();
                will(returnValue(bundleContextMock));

                allowing(bundleContextMock).getBundle();
                will(returnValue(bundle));

                allowing(bundle).adapt(BundleWiring.class);
                will(returnValue(wiring));

                allowing(componentContextMock).locateService(JAASConfigurationFactory.KEY_JAAS_CONFIGURATION, jaasConfigurationRef);
                will(returnValue(jaasConfiguration));
            }
        });
        return componentContextMock;
    }

    private JAASClientService createActivatedJAASClientServiceWithNotifier() throws Exception {
        JAASClientService jaasClientService = new JAASClientService();
        jaasClientService.jaasLoginContextEntries = jaasLoginContextEntries;
        jaasClientService.jaasLoginModuleConfigs = jaasLoginModuleConfigs;

        createJAASClientServiceExpectations();

        mock.checking(new Expectations() {
            {

                allowing(jaasLoginModuleConfigRef).getProperty("id");
                will(returnValue(jaasLoginModuleID));

                allowing(componentContext).locateService(JAASClientService.KEY_CHANGE_SERVICE, jaasChangeNotifierRef);
                will(returnValue(jaasChangeNotifier));

            }
        });
        jaasConfigurationFactory.setJAASConfiguration(jaasConfigurationRef);
        jaasConfigurationFactory.activate(componentContext);
        jaasClientService.setJaasLoginModuleConfig(jaasLoginModuleConfigRef);
        jaasClientService.setJaasLoginContextEntry(jaasLoginContextEntryRef);
        jaasClientService.setJaasChangeNotifier(jaasChangeNotifierRef);
        jaasClientService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasClientService.activate(componentContext, null);
        return jaasClientService;
    }

    private JAASClientService createActivatedJAASClientService() throws IOException {
        JAASClientService jaasClientService = new JAASClientService();
        jaasClientService.jaasLoginContextEntries = jaasLoginContextEntries;
        jaasClientService.jaasLoginModuleConfigs = jaasLoginModuleConfigs;

        createJAASClientServiceExpectations();

        jaasConfigurationFactory.setJAASConfiguration(jaasConfigurationRef);
        jaasConfigurationFactory.activate(componentContext);
        jaasClientService.setJaasLoginModuleConfig(jaasLoginModuleConfigRef);
        jaasClientService.setJaasLoginContextEntry(jaasLoginContextEntryRef);
        jaasClientService.setJaasConfigurationFactory(jaasConfigurationFactoryRef);
        jaasClientService.activate(componentContext, null);
        return jaasClientService;
    }

    private void createJAASClientServiceExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(JAASClientService.KEY_JAAS_CONFIG_FACTORY, jaasConfigurationFactoryRef);
                will(returnValue(jaasConfigurationFactory));
                allowing(jaasLoginModuleConfigRef).getProperty(Constants.SERVICE_PID);
                will(returnValue(pid));
                allowing(jaasLoginModuleConfigRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jaasLoginModuleConfigRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(jaasLoginContextEntryRef).getProperty("id");
                will(returnValue(loginContextEntryID));
                allowing(jaasLoginContextEntryRef).getProperty("loginModuleRef");
                will(returnValue(loginModuleIds));
                allowing(jaasLoginContextEntryRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(jaasLoginContextEntryRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(1L));
                allowing(jaasConfiguration).setJaasLoginContextEntries(jaasLoginContextEntries);
//                allowing(jaasConfiguration).setJaasLoginModuleConfigs(jaasLoginModuleConfigs);
                allowing(jaasConfiguration).getEntries();
                will(returnValue(jaasConfigurationEntries));
            }
        });

    }
}
