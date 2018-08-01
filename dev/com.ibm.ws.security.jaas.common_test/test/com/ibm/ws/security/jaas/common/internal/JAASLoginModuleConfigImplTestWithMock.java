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
package com.ibm.ws.security.jaas.common.internal;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.authentication.jaas.modules.WSLoginModuleImpl;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.library.Library;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class JAASLoginModuleConfigImplTestWithMock {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule checkOutput = outputMgr;

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final ClassLoadingService classLoadingService = mockery.mock(ClassLoadingService.class);
    private final ClassLoader classLoader = mockery.mock(ClassLoader.class);
    protected final Library sharedLibrary = mockery.mock(Library.class);
    private final Class wsLoginClass = WSLoginModuleImpl.class;
    private final String delegateClassName = wsLoginClass.getName();

    private final JAASLoginModuleConfigImpl jaasLoginModuleConfig = new JAASLoginModuleConfigImpl();

    @Before
    public void setUp() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(classLoadingService).getSharedLibraryClassLoader(sharedLibrary);
                will(returnValue(classLoader));
                allowing(classLoader).loadClass(delegateClassName);
                will(returnValue(wsLoginClass));
            }
        });

        jaasLoginModuleConfig.setClassLoadingSvc(classLoadingService);
    }

    @After
    public void tearDown() throws Exception {
        outputMgr.assertContextStatisfied(mockery);
    }

    @Test
    public void activate_noOptions() throws Exception {
        // Skip test on Java 11 because even the latest version of CGLib has issues when running on Hotspot JVM
        if (!System.getProperty("java.version").startsWith("1."))
            return;

        ModuleConfig moduleConfig = moduleConfig();
        jaasLoginModuleConfig.setClassLoadingSvc(classLoadingService);
        jaasLoginModuleConfig.setSharedLib(sharedLibrary);
        jaasLoginModuleConfig.activate(moduleConfig, Collections.<String, Object> emptyMap());

        assertEquals("userNameAndPassword1", jaasLoginModuleConfig.getId());
//        assertEquals("simpleClass", jaasLoginModuleConfig.getOptions().get("delegate"));
        assertEquals("Did not get expected OPTIONAL control flag",
                     LoginModuleControlFlag.OPTIONAL, jaasLoginModuleConfig.getControlFlag());
        assertEquals("Should have two options", 2, jaasLoginModuleConfig.getOptions().size());
    }

    ModuleConfig moduleConfig() {
        ModuleConfig moduleConfig = new ModuleConfig() {
            private Library sharedLibrary;
            private ClassLoadingService classLoadingService;

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String className() {
                return delegateClassName;
            }

            @Override
            public String controlFlag() {
                return "OPTIONAL";
            }

            @Override
            public String libraryRef() {
                return null;
            }

            @Override
            public String SharedLib_target() {
                return null;
            }

            @Override
            public String options() {
                return null;
            }

            @Override
            public String id() {
                return "userNameAndPassword1";
            }

//            public void setClassLoadingService(ClassLoadingService classLoadingService) {
//                this.classLoadingService = classLoadingService;
//            }
//
//            protected void setSharedLib(Library sharedLibrary) {
//                this.sharedLibrary = sharedLibrary;
//            }

//            public Map<String, Object> processDelegateOptions(Map<String, Object> inOptions, String originalLoginModuleClassName, ClassLoadingService classLoadingService,
//                                                              Library sharedLibrary, boolean jaasConfigFile) {
//                Map<String, Object> options = new HashMap<String, Object>();
//                options.put("delegate", "simpleClass");
//
//                return options;
//            }

        };
        return moduleConfig;
    }

    @Test
    public void activate_withOptions() throws Exception {
        // Skip test on Java 11 because even the latest version of CGLib has issues when running on Hotspot JVM
        if (!System.getProperty("java.version").startsWith("1."))
            return;

        final Map<String, Object> orops = new HashMap<String, Object>();
        orops.put("options.0.option1", "value1");
        orops.put("options.0.option2", "value2");

        ModuleConfig moduleConfig = moduleConfig();
        jaasLoginModuleConfig.setClassLoadingSvc(classLoadingService);
        jaasLoginModuleConfig.setSharedLib(sharedLibrary);
        jaasLoginModuleConfig.activate(moduleConfig, orops);
        assertEquals("userNameAndPassword1", jaasLoginModuleConfig.getId());
        assertEquals(JAASLoginModuleConfigImpl.LOGIN_MODULE_PROXY, jaasLoginModuleConfig.getClassName());
//        assertEquals("simpleClass", jaasLoginModuleConfig.getOptions().get("delegate"));
        assertEquals("Did not get expected OPTIONAL control flag",
                     LoginModuleControlFlag.OPTIONAL, jaasLoginModuleConfig.getControlFlag());
        assertEquals("Should have four options", 4, jaasLoginModuleConfig.getOptions().size());
    }

    @Test
    public void activate_withIncompleteOption() throws Exception {
        final Map<String, Object> orops = new HashMap<String, Object>();

        ModuleConfig moduleConfig = moduleConfig();

        JAASLoginModuleConfigImpl jaasLoginModuleConfig = new JAASLoginModuleConfigImpl();
        jaasLoginModuleConfig.activate(moduleConfig, orops);

        assertEquals("Should have two options", 2, jaasLoginModuleConfig.getOptions().size());
    }

//    @Test
//    public void modify() throws Exception {
//        final Configuration config = mock.mock(Configuration.class, "config");
//        final Dictionary<String, String> configProps = new Hashtable<String, String>();
//        configProps.put("option1", "value1");
//        configProps.put("option2", "value2");
//
//        mock.checking(new Expectations() {
//            {
//                one(cc).locateService(JAASLoginModuleConfigImpl.KEY_CONFIGURATION_ADMIN, configAdminRef);
//                will(returnValue(configAdmin));
//
//                one(configAdmin).listConfigurations("(service.pid=options)");
//                will(returnValue(new Configuration[] { config }));
//
//                one(configAdmin).getConfiguration("options");
//                will(returnValue(config));
//
//                one(config).getProperties();
//                will(returnValue(configProps));
//            }
//        });
//
//        Map<String, Object> props = new HashMap<String, Object>();
//        props.put(JAASLoginModuleConfigImpl.CFG_KEY_ID, "userNameAndPassword1");
//        props.put(JAASLoginModuleConfigImpl.CFG_KEY_CLASSNAME, "simpleClass");
//        props.put(JAASLoginModuleConfigImpl.CFG_KEY_CONTROL_FLAG, "OPTIONAL");
//        props.put(JAASLoginModuleConfigImpl.CFG_KEY_OPTION_PID, "options");
//
//        JAASLoginModuleConfigImpl jaasLoginModuleConfig = new JAASLoginModuleConfigImpl();
//        jaasLoginModuleConfig.setConfigurationAdmin(configAdminRef);
//        jaasLoginModuleConfig.activate(cc, props);
//
//        assertEquals("userNameAndPassword1", jaasLoginModuleConfig.getId());
//        assertEquals("simpleClass", jaasLoginModuleConfig.getOptions().get("delegate"));
//        assertEquals("Did not get expected OPTIONAL control flag",
//                     LoginModuleControlFlag.OPTIONAL, jaasLoginModuleConfig.getControlFlag());
//        assertEquals("Should have three options", 5, jaasLoginModuleConfig.getOptions().size());
//
//        final Configuration config3 = mock.mock(Configuration.class, "config3");
//        final Dictionary<String, String> config3Props = new Hashtable<String, String>();
//        config3Props.put("option3", "value3");
//        mock.checking(new Expectations() {
//            {
//                one(configAdmin).listConfigurations("(service.pid=options)");
//                will(returnValue(new Configuration[] { config3 }));
//
//                one(config3).getProperties();
//                will(returnValue(config3Props));
//
//                one(configAdmin).getConfiguration("options");
//                will(returnValue(config3));
//
//            }
//        });
//
//        Map<String, Object> newProps = new HashMap<String, Object>();
//        newProps.put(JAASLoginModuleConfigImpl.CFG_KEY_ID, "mytoken");
//        newProps.put(JAASLoginModuleConfigImpl.CFG_KEY_CLASSNAME, "tokenClass");
//        newProps.put(JAASLoginModuleConfigImpl.CFG_KEY_CONTROL_FLAG, "REQUIRED");
//        newProps.put(JAASLoginModuleConfigImpl.CFG_KEY_OPTION_PID, "options");
//
//        jaasLoginModuleConfig.modified(newProps);
//        assertEquals("mytoken", jaasLoginModuleConfig.getId());
//        assertEquals(JAASLoginModuleConfigImpl.LOGIN_MODULE_PROXY, jaasLoginModuleConfig.getClassName());
//        assertEquals("tokenClass", jaasLoginModuleConfig.getOptions().get(JAASLoginModuleConfigImpl.DELEGATE));
//        assertEquals("Did not get expected REQUIRED control flag",
//                     LoginModuleControlFlag.REQUIRED, jaasLoginModuleConfig.getControlFlag());
//        assertEquals("Should have two options", 4, jaasLoginModuleConfig.getOptions().size());
//    }

}
