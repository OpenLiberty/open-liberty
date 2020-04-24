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
package com.ibm.ws.security.authentication.tai.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.library.Library;

import test.common.SharedOutputManager;

@SuppressWarnings("unchecked")
public class InterceptorConfigImplTest {
    private static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final String KEY_INTERCEPTORS = "interceptors";
    static final String KEY_ID = "id";
    static final String KEY_CLASS_NAME = "className";
    static final String KEY_ENABLED = "enabled";
    static final String KEY_INVOKE_BEFORE_SSO = "invokeBeforeSSO";
    static final String KEY_INVOKE_AFTER_SSO = "invokeAfterSSO";
    static final String KEY_DISABLE_LTPA_COOKIE = "disableLtpaCookie";

    static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";
    static final String KEY_SHARED_LIB = "sharedLib";
    static final String KEY_LIBRARY_REF = "libraryRef";
    static final String CFG_KEY_PROPERTIES_PID = "propertiesRef";;
    static final String KEY_PROPERTIES = "properties";

    final ServiceReference<ConfigurationAdmin> configAdminRef = mock.mock(ServiceReference.class, "configAdmin");
    protected final ConfigurationAdmin configAdmin = mock.mock(ConfigurationAdmin.class);
    protected final Library sharedLibrary = mock.mock(Library.class);
    final ComponentContext cc = mock.mock(ComponentContext.class);
    protected final org.osgi.service.cm.Configuration config = mock.mock(org.osgi.service.cm.Configuration.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void contructorWithNull() throws Exception {
        final Map<String, Object> noProps = new Hashtable<String, Object>();
        InterceptorConfigImpl interceptorCfg = new InterceptorConfigImpl(noProps);
        assertNull("id should be null", interceptorCfg.getId());
        assertNull("TAI instance should be null", interceptorCfg.getInterceptorInstance(null));
    }

    @Test
    public void testProcessInterceptorConfigWithFalse() throws Exception {
        final Map<String, Object> props = createInterceptorDefaultProps(false);
        InterceptorConfigImpl interceptorCfg = new InterceptorConfigImpl(props);
        assertFalse("invoke before SSO is false", interceptorCfg.isInvokeBeforeSSO());
        assertFalse("invoke after SSO is false", interceptorCfg.isInvokeAfterSSO());
    }

    @Test
    public void testProcessInterceptorConfigWithTrue() throws Exception {
        final Map<String, Object> props = createInterceptorDefaultProps(true);
        mock.checking(new Expectations() {
            {
                allowing(cc).getBundleContext();
                allowing(cc).locateService(KEY_CONFIGURATION_ADMIN, configAdminRef);
                will(returnValue(configAdmin));

                allowing(configAdmin).listConfigurations("(service.pid=properties)");
                will(returnValue(new Configuration[] { config }));

                allowing(config).getProperties();
                will(returnValue(null));
            }
        });
        InterceptorConfigImpl interceptorCfg = new InterceptorConfigImpl(props);
        interceptorCfg.setConfigurationAdmin(configAdminRef);
        interceptorCfg.activate(cc, props);
        assertEquals("id is id", KEY_ID, interceptorCfg.getId());
        assertTrue("invoke before SSO is true", interceptorCfg.isInvokeBeforeSSO());
        assertTrue("invoke after SSO is true", interceptorCfg.isInvokeAfterSSO());
    }

    @Test
    public void testProcessInterceptorProperties() throws Exception {
        final Dictionary<String, String> properties = new Hashtable<String, String>();
        properties.put("prop1", "value1");
        properties.put("prop2", "value2");

        mock.checking(new Expectations() {
            {
                allowing(cc).getBundleContext();

                allowing(cc).locateService(KEY_CONFIGURATION_ADMIN, configAdminRef);
                will(returnValue(configAdmin));

                one(configAdmin).listConfigurations("(service.pid=propertiesRef)");
                will(returnValue(new Configuration[] { config }));
                one(configAdmin).getConfiguration("propertiesRef", "");
                will(returnValue(config));

                one(config).getProperties();
                will(returnValue(properties));
            }
        });

        final Map<String, Object> taiProps = createInterceptorDefaultProps(true);
        taiProps.put(CFG_KEY_PROPERTIES_PID, "properties");
        taiProps.put(CFG_KEY_PROPERTIES_PID, CFG_KEY_PROPERTIES_PID);
        InterceptorConfigImpl interceptorCfg = new InterceptorConfigImpl();
        interceptorCfg.setConfigurationAdmin(configAdminRef);
        interceptorCfg.activate(cc, taiProps);
        Properties prps = interceptorCfg.getProperties();
        assertEquals("We have two properties", 2, prps.size());
        assertEquals("value1", prps.getProperty("prop1"));
        assertEquals("value2", prps.getProperty("prop2"));
    }

    /**
     * @param value
     * @return
     */
    private Map<String, Object> createInterceptorDefaultProps(Boolean value) {
        final Map<String, Object> props = new Hashtable<String, Object>();
        props.put(KEY_ID, "id");
        props.put(KEY_INTERCEPTORS, "interceptors");
        props.put(KEY_CLASS_NAME, "className");
        props.put(KEY_ENABLED, value);
        props.put(KEY_INVOKE_BEFORE_SSO, value);
        props.put(KEY_INVOKE_AFTER_SSO, value);
        props.put(KEY_DISABLE_LTPA_COOKIE, value);
        props.put(KEY_LIBRARY_REF, "libraryRef");
        return props;
    }
}
