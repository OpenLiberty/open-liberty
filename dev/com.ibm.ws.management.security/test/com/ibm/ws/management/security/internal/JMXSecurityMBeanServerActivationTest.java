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
package com.ibm.ws.management.security.internal;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.kernel.boot.jmx.service.MBeanServerPipeline;
import com.ibm.ws.security.SecurityService;

/**
 * Tests the activation flow of the JMXSecurityMBeanServer.
 */
@SuppressWarnings("unchecked")
public class JMXSecurityMBeanServerActivationTest {
    private static SharedOutputManager outputMgr;
    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<MBeanServerPipeline> mbeanServerPipelineRef = mock.mock(ServiceReference.class, "mbeanServerPipelineRef");
    private final MBeanServerPipeline mbeanServerPipeline = mock.mock(MBeanServerPipeline.class, "mbeanServerPipeline");
    private final ServiceReference<SecurityService> securityServiceRef = mock.mock(ServiceReference.class, "securityServiceRef");
    private final SecurityService securityService = mock.mock(SecurityService.class, "securityService");
    private final Sequence pipelineInsertSequence = mock.sequence("pipelineInsertSequence");

    private final JMXSecurityMBeanServer jmxSecurityMBeanServer = new JMXSecurityMBeanServer();;

    @BeforeClass
    public static void setUpBeforeClass() {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
        outputMgr.trace("com.ibm.ws.management.security.*=all");
    }

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(JMXSecurityMBeanServer.KEY_MBEAN_SERVER_PIPLINE, mbeanServerPipelineRef);
                will(returnValue(mbeanServerPipeline));

                allowing(cc).locateService(JMXSecurityMBeanServer.KEY_SECURITY_SERVICE, securityServiceRef);
                will(returnValue(securityService));
            }
        });
        jmxSecurityMBeanServer.setMBeanServerPipeline(mbeanServerPipelineRef);
        jmxSecurityMBeanServer.setSecurityService(securityServiceRef);
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        outputMgr.restoreStreams();
        outputMgr.trace("*=all=disabled");
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activate_inserts() {
        mock.checking(new Expectations() {
            {
                one(mbeanServerPipeline).contains(jmxSecurityMBeanServer);
                will(returnValue(false));

                one(mbeanServerPipeline).insert(jmxSecurityMBeanServer);
            }
        });
        jmxSecurityMBeanServer.activate(cc);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#activate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void activate_doesNotInsert() {
        mock.checking(new Expectations() {
            {
                one(mbeanServerPipeline).contains(jmxSecurityMBeanServer);
                will(returnValue(true));

                never(mbeanServerPipeline).insert(jmxSecurityMBeanServer);
            }
        });
        jmxSecurityMBeanServer.activate(cc);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#deactivate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void deactivate_removes() {
        mock.checking(new Expectations() {
            {
                one(mbeanServerPipeline).contains(jmxSecurityMBeanServer);
                inSequence(pipelineInsertSequence);
                will(returnValue(false));

                one(mbeanServerPipeline).insert(jmxSecurityMBeanServer);
            }
        });
        jmxSecurityMBeanServer.activate(cc);

        mock.checking(new Expectations() {
            {
                one(mbeanServerPipeline).contains(jmxSecurityMBeanServer);
                inSequence(pipelineInsertSequence);
                will(returnValue(true));

                one(mbeanServerPipeline).remove(jmxSecurityMBeanServer);
            }
        });
        jmxSecurityMBeanServer.deactivate(cc);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#deactivate(org.osgi.service.component.ComponentContext)}.
     */
    @Test
    public void deactivate_doesNotRemove() {
        mock.checking(new Expectations() {
            {
                one(mbeanServerPipeline).contains(jmxSecurityMBeanServer);
                inSequence(pipelineInsertSequence);
                will(returnValue(true));

                never(mbeanServerPipeline).insert(jmxSecurityMBeanServer);
            }
        });
        jmxSecurityMBeanServer.activate(cc);

        mock.checking(new Expectations() {
            {
                one(mbeanServerPipeline).contains(jmxSecurityMBeanServer);
                inSequence(pipelineInsertSequence);
                will(returnValue(false));

                never(mbeanServerPipeline).remove(jmxSecurityMBeanServer);
            }
        });
        jmxSecurityMBeanServer.deactivate(cc);
    }

}
