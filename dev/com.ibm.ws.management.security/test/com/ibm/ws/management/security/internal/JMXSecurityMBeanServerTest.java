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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.kernel.boot.jmx.service.MBeanServerPipeline;
import com.ibm.ws.management.security.ManagementSecurityConstants;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authorization.AuthorizationService;

/**
 * Tests the activated logic of JMXSecurityMBeanServer.
 * <p>
 * For tests around the activation of JMXSecurityMBeanServer, see
 * JMXSecurityMBeanServerActivationTest.
 * 
 * @see JMXSecurityMBeanServerActivationTest
 */
@SuppressWarnings("unchecked")
public class JMXSecurityMBeanServerTest {
    private static SharedOutputManager outputMgr;
    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final MBeanServer mbeanServer = mock.mock(MBeanServer.class);
    private final ServiceReference<MBeanServerPipeline> mbeanServerPipelineRef = mock.mock(ServiceReference.class, "mbeanServerPipelineRef");
    private final MBeanServerPipeline mbeanServerPipeline = mock.mock(MBeanServerPipeline.class, "mbeanServerPipeline");
    private final ServiceReference<SecurityService> securityServiceRef = mock.mock(ServiceReference.class, "securityServiceRef");
    private final SecurityService securityService = mock.mock(SecurityService.class, "securityService");
    private final AuthorizationService authorizationService = mock.mock(AuthorizationService.class, "authorizationService");
    private final Sequence pipelineInsertSequence = mock.sequence("pipelineInsertSequence");

    private final JMXSecurityMBeanServer jmxSecurityMBeanServer = new JMXSecurityMBeanServer();
    private final Attribute attribute = new Attribute("TestAttr", "TestAttrValue");
    private final AttributeList attributes = new AttributeList();
    private ObjectName objectName;

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        objectName = new ObjectName("d:object=TestObject");

        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(JMXSecurityMBeanServer.KEY_MBEAN_SERVER_PIPLINE, mbeanServerPipelineRef);
                will(returnValue(mbeanServerPipeline));

                allowing(cc).locateService(JMXSecurityMBeanServer.KEY_SECURITY_SERVICE, securityServiceRef);
                will(returnValue(securityService));

                one(mbeanServerPipeline).contains(jmxSecurityMBeanServer);
                inSequence(pipelineInsertSequence);
                will(returnValue(false));

                one(mbeanServerPipeline).insert(jmxSecurityMBeanServer);

                allowing(securityService).getAuthorizationService();
                will(returnValue(authorizationService));
            }
        });

        jmxSecurityMBeanServer.setMBeanServer(mbeanServer);
        jmxSecurityMBeanServer.setMBeanServerPipeline(mbeanServerPipelineRef);
        jmxSecurityMBeanServer.setSecurityService(securityServiceRef);
        jmxSecurityMBeanServer.activate(cc);
    }

    @After
    public void tearDown() {
        mock.checking(new Expectations() {
            {
                one(mbeanServerPipeline).contains(jmxSecurityMBeanServer);
                inSequence(pipelineInsertSequence);
                will(returnValue(true));

                one(mbeanServerPipeline).remove(jmxSecurityMBeanServer);
            }
        });
        jmxSecurityMBeanServer.deactivate(cc);
        jmxSecurityMBeanServer.unsetMBeanServerPipeline(mbeanServerPipelineRef);
        jmxSecurityMBeanServer.unsetSecurityService(securityServiceRef);

        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() {
        outputMgr.restoreStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#getPriority()}.
     */
    @Test
    public void getPriority() {
        assertEquals("Must return MAXINT",
                     Integer.MAX_VALUE, jmxSecurityMBeanServer.getPriority());
    }

    /**
     * 
     */
    private void checkAuthorizationFailedMessageLogged() {
        assertTrue("Authorization failed message was not logged",
                   outputMgr.checkForStandardOut("CWWKX0001A"));
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#getAttribute(javax.management.ObjectName, java.lang.String)}.
     */
    @Test
    public void getAttribute_authorized() throws Exception {
        final String attribute = "someAttr";
        mock.checking(new Expectations() {
            {
                one(authorizationService).isAuthorized(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                       jmxSecurityMBeanServer.requiredRoles,
                                                       null);
                will(returnValue(true));

                one(mbeanServer).getAttribute(objectName, attribute);
            }
        });

        jmxSecurityMBeanServer.getAttribute(objectName, attribute);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#getAttribute(javax.management.ObjectName, java.lang.String)}.
     */
    @Test
    public void getAttribute_notAuthorized() throws Exception {
        final String attribute = "someAttr";
        mock.checking(new Expectations() {
            {
                one(authorizationService).isAuthorized(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                       jmxSecurityMBeanServer.requiredRoles,
                                                       null);
                will(returnValue(false));

                never(mbeanServer);
            }
        });

        try {

            try {
                jmxSecurityMBeanServer.getAttribute(objectName, attribute);
                fail("Expected authorization failed exception not thrown");
            } catch (SecurityException se) {
                checkAuthorizationFailedMessageLogged();
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#getAttributes(javax.management.ObjectName, java.lang.String[])}.
     */
    @Test
    public void getAttributes_authorized() throws Exception {
        final String[] attributess = new String[] { "someAttr" };
        mock.checking(new Expectations() {
            {
                one(authorizationService).isAuthorized(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                       jmxSecurityMBeanServer.requiredRoles,
                                                       null);
                will(returnValue(true));

                one(mbeanServer).getAttributes(objectName, attributess);
            }
        });

        jmxSecurityMBeanServer.getAttributes(objectName, attributess);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#getAttributes(javax.management.ObjectName, java.lang.String[])}.
     */
    @Test
    public void getAttributes_notAuthorized() throws Exception {
        final String[] attributess = new String[] { "someAttr" };
        mock.checking(new Expectations() {
            {
                one(authorizationService).isAuthorized(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                       jmxSecurityMBeanServer.requiredRoles,
                                                       null);
                will(returnValue(false));

                never(mbeanServer);
            }
        });

        try {
            try {
                jmxSecurityMBeanServer.getAttributes(objectName, attributess);
                fail("Expected authorization failed exception not thrown");
            } catch (SecurityException se) {
                checkAuthorizationFailedMessageLogged();
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#setAttribute(javax.management.ObjectName, javax.management.Attribute)}.
     */
    @Test
    public void setAttribute_authorized() throws Exception {
        mock.checking(new Expectations() {
            {
                one(authorizationService).isAuthorized(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                       jmxSecurityMBeanServer.requiredRoles,
                                                       null);
                will(returnValue(true));

                one(mbeanServer).setAttribute(objectName, attribute);
            }
        });

        jmxSecurityMBeanServer.setAttribute(objectName, attribute);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#setAttribute(javax.management.ObjectName, javax.management.Attribute)}.
     */
    @Test
    public void setAttribute_notAuthorized() throws Exception {
        mock.checking(new Expectations() {
            {
                one(authorizationService).isAuthorized(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                       jmxSecurityMBeanServer.requiredRoles,
                                                       null);
                will(returnValue(false));

                never(mbeanServer);
            }
        });

        try {
            try {
                jmxSecurityMBeanServer.setAttribute(objectName, attribute);
                fail("Expected authorization failed exception not thrown");
            } catch (SecurityException se) {
                checkAuthorizationFailedMessageLogged();
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }

    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#setAttributes(javax.management.ObjectName, javax.management.AttributeList)}.
     */
    @Test
    public void setAttributes_authorized() throws Exception {
        mock.checking(new Expectations() {
            {
                one(authorizationService).isAuthorized(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                       jmxSecurityMBeanServer.requiredRoles,
                                                       null);
                will(returnValue(true));

                one(mbeanServer).setAttributes(objectName, attributes);
            }
        });

        jmxSecurityMBeanServer.setAttributes(objectName, attributes);
    }

    /**
     * Test method for {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#setAttributes(javax.management.ObjectName, javax.management.AttributeList)}.
     */
    @Test
    public void setAttributes_notAuthorized() throws Exception {
        mock.checking(new Expectations() {
            {
                one(authorizationService).isAuthorized(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                       jmxSecurityMBeanServer.requiredRoles,
                                                       null);
                will(returnValue(false));

                never(mbeanServer);
            }
        });

        try {
            try {
                jmxSecurityMBeanServer.setAttributes(objectName, attributes);
                fail("Expected authorization failed exception not thrown");
            } catch (SecurityException se) {
                checkAuthorizationFailedMessageLogged();
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }

    }

    /**
     * Test method for
     * {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#invoke(javax.management.ObjectName, java.lang.String, java.lang.Object[], java.lang.String[])}.
     */
    @Test
    public void invoke_authorized() throws Exception {
        final String operationName = "testOperation";
        final Object[] params = new Object[] {};
        final String[] signature = new String[] {};

        mock.checking(new Expectations() {
            {
                one(authorizationService).isAuthorized(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                       jmxSecurityMBeanServer.requiredRoles,
                                                       null);
                will(returnValue(true));

                one(mbeanServer).invoke(objectName, operationName, params, signature);
            }
        });

        jmxSecurityMBeanServer.invoke(objectName, operationName, params, signature);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.management.security.internal.JMXSecurityMBeanServer#invoke(javax.management.ObjectName, java.lang.String, java.lang.Object[], java.lang.String[])}.
     */
    @Test
    public void invoke_notAuthorized() throws Exception {
        final String operationName = "testOperation";
        final Object[] params = new Object[] {};
        final String[] signature = new String[] {};

        mock.checking(new Expectations() {
            {
                one(authorizationService).isAuthorized(ManagementSecurityConstants.ADMIN_RESOURCE_NAME,
                                                       jmxSecurityMBeanServer.requiredRoles,
                                                       null);
                will(returnValue(false));

                never(mbeanServer);
            }
        });

        try {
            try {
                jmxSecurityMBeanServer.invoke(objectName, operationName, params, signature);
                fail("Expected authorization failed exception not thrown");
            } catch (SecurityException se) {
                checkAuthorizationFailedMessageLogged();
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }
}
