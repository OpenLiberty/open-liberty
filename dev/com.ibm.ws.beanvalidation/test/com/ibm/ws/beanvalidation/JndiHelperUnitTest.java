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
package com.ibm.ws.beanvalidation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.NamingException;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.beanvalidation.mock.MockComponentMetaData;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * Tests BeanValidationJavaColonHelper.
 */
public class JndiHelperUnitTest {
    private static SharedOutputManager outputMgr;

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor: 
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
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
    }

    /**
     * Verify null is returned when the lookup is not in the java:comp/env
     * namespace.
     */
    @Test
    public void testJndiNonJavaColonLookup() {
        final String m = "testJndiNonJavaColonLookup";
        String jndiName = "not_COMP_ENV";
        try {
            BeanValidationJavaColonHelper helper = new BeanValidationJavaColonHelper();
            Object instance = helper.getObjectInstance(JavaColonNamespace.GLOBAL, jndiName);
            assertNull("Non java:comp/env lookup did not return null : " + instance, instance);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Verify correct exception is throw with correct text from message file
     * when there is no CMD on thread.
     */
    //    @Test
    public void testJndiNonJeeThreadExceptionMsg() {
        final String m = "testJndiNonJeeThreadExceptionMsg";
        String jndiName = "no_cmd";
        try {
            BeanValidationJavaColonHelper helper = new BeanValidationJavaColonHelper();
            Object instance = helper.getObjectInstance(JavaColonNamespace.COMP_ENV, jndiName);
            fail("getObjectInstance with no CMD on thread should fail : " + instance);
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("No CMD on thread message text is null", msgTxt);
            assertTrue("No CMD on thread message not correct : " + msgTxt,
                       msgTxt.startsWith("CWNEN1000E: "));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Verify a Validator is returned when lookup of java:comp/Validator
     * and there is CMD on the thread.
     */
    //    @Test
    public void testJndiCompValidator() {
        final String m = "testJndiCompValidator";
        String jndiName = "Validator";
        try {
            ComponentMetaDataAccessorImpl cmda = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
            ComponentMetaData cmd = new MockComponentMetaData(null, null);
            BeanValidationJavaColonHelper helper = new BeanValidationJavaColonHelper();

            try {
                cmda.beginContext(cmd);
                Object instance = helper.getObjectInstance(JavaColonNamespace.COMP, jndiName);
                assertTrue("Not an instance of Validator : " + instance, instance instanceof Validator);
            } finally {
                cmda.endContext();
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Verify a ValidatorFactory is returned when lookup of java:comp/ValidatorFactory
     * and there is CMD on the thread.
     */
    //    @Test
    public void testJndiCompValidatorFactory() {
        final String m = "testJndiCompValidatorFactory";
        String jndiName = "ValidatorFactory";
        try {
            ComponentMetaDataAccessorImpl cmda = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor();
            ComponentMetaData cmd = new MockComponentMetaData(null, null);
            BeanValidationJavaColonHelper helper = new BeanValidationJavaColonHelper();

            try {
                cmda.beginContext(cmd);
                Object instance = helper.getObjectInstance(JavaColonNamespace.COMP, jndiName);
                assertTrue("Not an instance of ValidatorFactory : " + instance, instance instanceof ValidatorFactory);
            } finally {
                cmda.endContext();
            }
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
