/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingException;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.container.service.naming.NamingConstants;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.injection.mock.MockInjectionBinding;
import com.ibm.ws.injection.mock.MockInjectionJavaColonHelper;
import com.ibm.ws.injectionengine.osgi.internal.naming.InjectionJavaColonHelper;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * Tests InjectionJavaColonHelper.
 */
public class JndiHelperUnitTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    /**
     * Verify correct exception is throw with correct text from message file
     * when there is no CMD on thread.
     */
    @Test
    public void testJndiNonJeeThreadExceptionMsg() {
        String jndiName = "no_cmd";
        try {
            InjectionJavaColonHelper helper = new InjectionJavaColonHelper();
            Object instance = helper.getObjectInstance(JavaColonNamespace.COMP_ENV, jndiName);
            fail("getObjectInstance with no CMD on thread should fail : " + instance);
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("No CMD on thread message text is null", msgTxt);
            assertTrue("No CMD on thread message not correct : " + msgTxt,
                       msgTxt.startsWith("CWNEN1000E: "));
        }
    }

    /**
     * Verify null is returned when there is no injection metadata associated
     * with the CMD on the thread.
     */
    @Test
    public void testJndiNoInjectionMetaDataForCMDNoMMD() throws Exception {
        String jndiName = "no_injection_metadata";
        InjectionJavaColonHelper helper = new MockInjectionJavaColonHelper(null);

        Object instance = helper.getObjectInstance(JavaColonNamespace.COMP_ENV, jndiName);
        assertNull("No injection metadata lookup did not return null : " + instance, instance);
    }

    /**
     * Verify null is returned when there is no injection metadata associated
     * with the CMD or MMD the thread.
     */
    @Test
    public void testJndiNoInjectionMetaDataForCMDOrMMD() throws Exception {
        String jndiName = "no_injection_metadata";
        InjectionJavaColonHelper helper = new MockInjectionJavaColonHelper(null);

        Object instance = helper.getObjectInstance(JavaColonNamespace.COMP_ENV, jndiName);
        assertNull("No injection metadata lookup did not return null : " + instance, instance);
    }

    /**
     * Verify correct object is returned when there is injection metadata
     * associated with the CMD on the thread.
     */
    @Test
    public void testJndiWithInjectionMetaDataForCMD() throws Exception {
        String jndiName = "with_injection_metadata";
        Map<String, InjectionBinding<?>> bindings = new HashMap<String, InjectionBinding<?>>();
        bindings.put(jndiName, new MockInjectionBinding("Hello"));
        InjectionJavaColonHelper helper = new MockInjectionJavaColonHelper(bindings);

        Object instance = helper.getObjectInstance(JavaColonNamespace.COMP_ENV, jndiName);
        assertEquals("Wrong object returned on lookup", "Hello", instance);
    }

    /**
     * Verify correct object is returned when there is injection metadata
     * associated with the MMD on the thread.
     */
    @Test
    public void testJndiWithInjectionMetaDataForMMD() throws Exception {
        String jndiName = "with_injection_metadata";
        Map<String, InjectionBinding<?>> bindings = new HashMap<String, InjectionBinding<?>>();
        bindings.put(jndiName, new MockInjectionBinding("Hello"));
        InjectionJavaColonHelper helper = new MockInjectionJavaColonHelper(bindings);

        Object instance = helper.getObjectInstance(JavaColonNamespace.COMP_ENV, jndiName);
        assertEquals("Wrong object returned on lookup", "Hello", instance);
    }

    private Map<String, InjectionBinding<?>> createBindings(String prefix) throws Exception {
        Map<String, InjectionBinding<?>> bindings = new HashMap<String, InjectionBinding<?>>();
        InjectionBinding<?> binding = new MockInjectionBinding("Hello");
        binding.setInjectionClassType(String.class);
        binding.setJndiName(prefix + "jdbc/hello");
        binding.setObjects(null, new Object());
        bindings.put(prefix + "jdbc/hello", binding);
        InjectionBinding<?> binding2 = new MockInjectionBinding(new Long(7));
        binding2.setInjectionClassType(Long.class);
        binding2.setJndiName(prefix + "jdbc/goodbye");
        binding2.setObjects(null, new Object());
        bindings.put(prefix + "jdbc/goodbye", binding2);
        return bindings;
    }

    private Map<Class<?>, Map<String, InjectionBinding<?>>> createNonCompBindings(NamingConstants.JavaColonNamespace namespace) throws Exception {
        return Collections.<Class<?>, Map<String, InjectionBinding<?>>> singletonMap(Object.class, createBindings(namespace.prefix()));
    }

    @Test
    public void testJndiForJavaComp() throws Exception {
        InjectionJavaColonHelper helper = new MockInjectionJavaColonHelper(NamingConstants.JavaColonNamespace.COMP, createNonCompBindings(NamingConstants.JavaColonNamespace.COMP));
        assertEquals("Hello", helper.getObjectInstance(NamingConstants.JavaColonNamespace.COMP, "jdbc/hello"));
        assertNull(helper.getObjectInstance(NamingConstants.JavaColonNamespace.COMP_ENV, "jdbc/hello"));
        assertNull(helper.getObjectInstance(NamingConstants.JavaColonNamespace.MODULE, "jdbc/hello"));
    }

    @Test
    public void testJndiForJavaModule() throws Exception {
        InjectionJavaColonHelper helper = new MockInjectionJavaColonHelper(NamingConstants.JavaColonNamespace.MODULE, createNonCompBindings(NamingConstants.JavaColonNamespace.MODULE));
        assertNull(helper.getObjectInstance(NamingConstants.JavaColonNamespace.COMP, "jdbc/hello"));
        assertNull(helper.getObjectInstance(NamingConstants.JavaColonNamespace.COMP_ENV, "jdbc/hello"));
        assertEquals("Hello", helper.getObjectInstance(NamingConstants.JavaColonNamespace.MODULE, "jdbc/hello"));
    }

    @Test
    public void testJndiForJavaApp() throws Exception {
        InjectionJavaColonHelper helper = new MockInjectionJavaColonHelper(NamingConstants.JavaColonNamespace.APP, createNonCompBindings(NamingConstants.JavaColonNamespace.APP));
        assertNull(helper.getObjectInstance(NamingConstants.JavaColonNamespace.COMP, "jdbc/hello"));
        assertNull(helper.getObjectInstance(NamingConstants.JavaColonNamespace.COMP_ENV, "jdbc/hello"));
        assertEquals("Hello", helper.getObjectInstance(NamingConstants.JavaColonNamespace.APP, "jdbc/hello"));
    }

    @Test
    public void testJndiForJavaGlobal() throws Exception {
        InjectionJavaColonHelper helper = new MockInjectionJavaColonHelper(NamingConstants.JavaColonNamespace.GLOBAL, createNonCompBindings(NamingConstants.JavaColonNamespace.GLOBAL));
        assertNull(helper.getObjectInstance(NamingConstants.JavaColonNamespace.COMP, "jdbc/hello"));
        assertNull(helper.getObjectInstance(NamingConstants.JavaColonNamespace.COMP_ENV, "jdbc/hello"));
        assertEquals("Hello", helper.getObjectInstance(NamingConstants.JavaColonNamespace.GLOBAL, "jdbc/hello"));
    }

    private void assertExceptionInstanceof(String msg, Throwable t, Class<?> klass) {
        if (!klass.isInstance(t)) {
            AssertionError ae = new AssertionError(msg);
            ae.initCause(t);
            throw ae;
        }
    }

    /**
     * Verify correct exception is throw with correct text from message file
     * when an exception occurs obtaining the object instance.
     */
    @Test
    public void testJndiWithCannotInstantiateObject() throws Exception {
        String jndiName = "cannot_instantiate_object";

        Map<String, InjectionBinding<?>> bindings = new HashMap<String, InjectionBinding<?>>();
        InjectionBinding<?> binding = new MockInjectionBinding(null);
        binding.setJndiName(jndiName);
        bindings.put(jndiName, binding);
        InjectionJavaColonHelper helper = new MockInjectionJavaColonHelper(bindings);

        try {
            Object instance = helper.getObjectInstance(JavaColonNamespace.COMP_ENV, jndiName);
            fail("getObjectInstance with object creation exception should fail : " + instance);
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("Cannot instantiate object message text is null", msgTxt);
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.startsWith("CWNEN1001E: "));
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.contains(jndiName));
            Throwable cause = nex.getCause();
            assertNotNull("Cannot instantiate object has no cause", cause);
            assertExceptionInstanceof("Cannot instantiate object cause not correct : " + cause,
                                      cause, InjectionException.class);
            cause = cause.getCause();
            assertNotNull("Cannot instantiate object has no root cause", cause);
            assertExceptionInstanceof("Cannot instantiate object root cause not correct : " + cause,
                                      cause, NullPointerException.class);
            assertEquals("Cannot instantiate object root cause text wrong",
                         "Expected Test Exception", cause.getMessage());
        }
    }

    @Test
    public void testJndiListInstances() throws Exception {
        String lookupName = "";

        Map<String, InjectionBinding<?>> bindings = createBindings("");
        InjectionJavaColonHelper helper = new MockInjectionJavaColonHelper(bindings);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.COMP_ENV, lookupName);
        for (NameClassPair pair : pairs) {
            assertEquals(Context.class.getName(), pair.getClassName());
            assertEquals("jdbc", pair.getName());
        }
        assertEquals(1, pairs.size());

        pairs = helper.listInstances(JavaColonNamespace.COMP_ENV, "jdbc");
        assertEquals(2, pairs.size());
        for (NameClassPair pair : pairs) {
            if (String.class.getName().equals(pair.getClassName())) {
                assertEquals("hello", pair.getName());
            } else {
                assertEquals(Long.class.getName(), pair.getClassName());
                assertEquals("goodbye", pair.getName());
            }

        }
    }

    @Test
    public void testJndiListNoBindings() throws Exception {
        String lookupName = "";
        InjectionJavaColonHelper helper = new MockInjectionJavaColonHelper(null);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.COMP_ENV, lookupName);
        assertEquals(0, pairs.size());
    }

    @Test
    public void testJndiListAtRoot() throws Exception {
        String lookupName = "";

        Map<String, InjectionBinding<?>> bindings = new HashMap<String, InjectionBinding<?>>();
        InjectionBinding<?> binding = new MockInjectionBinding("Hello");
        binding.setInjectionClassType(String.class);
        binding.setJndiName("hello");
        bindings.put(binding.getJndiName(), binding);
        InjectionJavaColonHelper helper = new MockInjectionJavaColonHelper(bindings);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.COMP_ENV, lookupName);
        assertEquals(1, pairs.size());
        NameClassPair pair = pairs.iterator().next();
        assertEquals(String.class.getName(), pair.getClassName());
        assertEquals("hello", pair.getName());
    }
}
