/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Policy;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityPropagator;
import com.ibm.ws.security.authorization.jacc.internal.DummyPolicy;
import com.ibm.ws.security.authorization.jacc.internal.DummyPolicyConfigurationFactory;

import io.openliberty.security.authorization.jacc.internal.proxy.ProxyTestUtil;
import test.common.SharedOutputManager;

public class PolicyConfigurationManagerTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery context = new JUnit4Mockery();
    private final PolicyConfiguration pc1 = context.mock(PolicyConfiguration.class, "pc1");
    private final ExtendedApplicationInfo ai = context.mock(ExtendedApplicationInfo.class);
    private final ApplicationMetaData amd = context.mock(ApplicationMetaData.class);
    private final J2EEName jen = context.mock(J2EEName.class);
    private final EJBSecurityPropagator esp = context.mock(EJBSecurityPropagator.class);
    private PolicyConfigurationManagerImpl pcm = null;
    private PolicyConfigurationFactory pcf = null;
    private Policy policy = null;

    @Before
    public void setUp() {
        pcf = new DummyPolicyConfigurationFactory(pc1);
        policy = new DummyPolicy();
        pcm = new PolicyConfigurationManagerImpl();
        pcm.initialize(ProxyTestUtil.createPolicyProxy(policy), pcf);
    }

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
    }

    /**
     * Tests containModule method
     * Expected result: returen true if module exist, false otherwise
     */
    @Test
    public void containModule() {
        final String appName = "app";
        final String contextId = "contextId";
        assertFalse(pcm.containModule(appName, contextId));
        pcm.addModule(appName, contextId);
        assertTrue(pcm.containModule(appName, contextId));
    }

    /**
     * Tests removeModule method
     * Expected result: removes module without any error.
     */
    @Test
    public void removeModule() {
        final String appName = "app";
        final String contextId = "contextId";
        final String contextId2 = "contextId2";
        pcm.removeModule(appName, contextId);
        pcm.addModule(appName, contextId);
        pcm.addModule(appName, contextId2);
        pcm.removeModule(appName, contextId);
        assertFalse(pcm.containModule(appName, contextId));
        assertTrue(pcm.containModule(appName, contextId2));
        pcm.removeModule(appName, contextId2);
        assertFalse(pcm.containModule(appName, contextId2));
    }

    /**
     * Tests addEJB method
     * Expected result: no exception
     */
    @Test
    public void addEJB() {
        final String appName = "app";
        final String contextId = "contextId";

        context.checking(new Expectations() {
            {
                one(esp).processEJBRoles(pcf, contextId, pcm);
            }
        });
        pcm.addEJB(appName, contextId);
        assertTrue(pcm.containModule(appName, contextId));

        pcm.setEJBSecurityPropagator(esp);
        pcm.processEJBs(appName);
    }

    /**
     * Tests commitModules method
     * Expected result: invokes commit method
     */
    @Test
    public void commitModulesNull() {
        final String APP_NAME = "applicationName";
        context.checking(new Expectations() {
            {
                exactly(1).of(ai).getDeploymentName();
                will(returnValue(APP_NAME));
            }
        });
        pcm.applicationStarted(ai);
    }

    /**
     * Tests commitModules method
     * Expected result: invokes commit method
     */
    @Test
    public void commitModulesApps() {
        final String APP_NAME = "applicationName";
        try {
            context.checking(new Expectations() {
                {
                    exactly(1).of(ai).getDeploymentName();
                    will(returnValue(APP_NAME));

                    allowing(pc1).linkConfiguration(with(any(PolicyConfiguration.class)));
                    one(pc1).commit();
                }
            });
        } catch (PolicyContextException e) {
            fail("An exception is caught: " + e);
        }
        try {
            pcm.linkConfiguration(APP_NAME, pc1);
        } catch (PolicyContextException e) {
            e.printStackTrace();
            fail("An exception is caught.");
        }
        pcm.applicationStarted(ai);
    }

    /**
     * Tests commitModules method
     * Expected result: invokes commit method and getting Exception
     */
    @Test
    public void commitModulesAppsException() {
        final String APP_NAME = "applicationName";
        try {
            context.checking(new Expectations() {
                {
                    exactly(1).of(ai).getDeploymentName();
                    will(returnValue(APP_NAME));

                    allowing(pc1).linkConfiguration(with(any(PolicyConfiguration.class)));
                    one(pc1).commit();
                    will(throwException(new PolicyContextException()));
                    one(pc1).getContextID();
                    will(throwException(new PolicyContextException()));
                }
            });
        } catch (PolicyContextException pce) {
            pce.printStackTrace();
            fail("An exception is caught.");
        }
        try {
            pcm.linkConfiguration(APP_NAME, pc1);
        } catch (PolicyContextException e) {
            e.printStackTrace();
            fail("An exception is caught.");
        }
        pcm.applicationStarted(ai);
    }

    /**
     * Tests removeModules method
     * Expected result: no error
     */
    @Test
    public void removeModulesApps() {
        final String APP_NAME = "applicationName";
        final String CONTEXT_ID = "contextId#application#module";
        try {
            context.checking(new Expectations() {
                {
                    exactly(1).of(ai).getDeploymentName();
                    will(returnValue(APP_NAME));

                    one(pc1).delete();
                }
            });
        } catch (PolicyContextException e) {
            fail("An exception is caught: " + e);
        }
        pcm.addModule(APP_NAME, CONTEXT_ID);
        pcm.applicationStopped(ai);
    }

    /**
     * Tests applicationStarted and applicationStopped error scenario
     * Expected result: no error
     */
    @Test
    public void applicationStartedAndStoppedError() {
        context.checking(new Expectations() {
            {
                exactly(2).of(ai).getDeploymentName();
                will(returnValue(null));
            }
        });
        pcm.applicationStopped(ai);
        pcm.applicationStarted(ai);
    }

}
