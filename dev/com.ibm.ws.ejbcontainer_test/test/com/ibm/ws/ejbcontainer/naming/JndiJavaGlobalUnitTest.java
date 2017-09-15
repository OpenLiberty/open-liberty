/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.naming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.NotContextException;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.websphere.csi.J2EEName;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.ejbcontainer.mock.MockEJBJavaColonNamingHelper;
import com.ibm.ws.ejbcontainer.osgi.internal.naming.EJBBinding;
import com.ibm.ws.ejbcontainer.osgi.internal.naming.EJBJavaColonNamingHelper;

/**
 * Tests EJBJavaColonNamingHelper - java:global
 */
@SuppressWarnings("static-method")
public class JndiJavaGlobalUnitTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("EJBContainer=all");

    /**
     * Verify null is returned when the lookup is not in the java:global
     * namespace.
     */
    @Test
    public void testJndiNonJavaColonLookup() throws Exception {
        String jndiName = "not_GLOBAL";
        EJBJavaColonNamingHelper helper = new EJBJavaColonNamingHelper();

        Object instance = helper.getObjectInstance(JavaColonNamespace.COMP_ENV, jndiName);
        assertNull("Non java:global lookup did not return null : " + instance, instance);
    }

    /**
     * Verify correct exception is throw with correct text from message file
     * when there is no component metadata on the thread.
     */
    @Test
    public void testJndiNonJeeThreadExceptionMsg() throws Exception {
        String jndiName = "no_cmd";
        try {
            EJBJavaColonNamingHelper helper = new EJBJavaColonNamingHelper();
            Object instance = helper.getObjectInstance(JavaColonNamespace.GLOBAL, jndiName);
            fail("getObjectInstance with no CMD on thread should fail : " + instance);
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("No CMD on thread exception message text is null", msgTxt);
            assertTrue("No CMD on thread exception message not correct : " + msgTxt,
                       msgTxt.startsWith("CWWKN0100E: "));
            assertTrue("JNDI name not correct in message: " + msgTxt,
                       msgTxt.contains("java:global/" + jndiName));
        }
    }

    /**
     * Verify correct exception is throw when the ejb lookup
     * is not found.
     */
    @Test
    public void testJndiCannotInstanciateException() throws Exception {
        MockEJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);
        helper.setTestingException(true);

        try {
            Object instance = helper.getObjectInstance(JavaColonNamespace.GLOBAL, "ejbmod1/hello");
            fail("Should have thrown exception." + instance);
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("No EJB found message text is null", msgTxt);
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.startsWith("CNTR4007E: "));
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.contains("java:global/ejbmod1/hello"));
            assertFalse("parameters not filled in.", msgTxt.contains("{"));

            J2EEName name = helper.getTargetEJBJ2EEName();
            String matchingMessage = ".*" + name.getComponent() +
                                     ".*" + name.getModule() +
                                     ".*" + name.getApplication() + ".*";
            assertTrue("Incorrect parameters filled in for EJB: " + msgTxt,
                       msgTxt.matches(matchingMessage));
        }
    }

    /**
     * Verify correct exception is throw when the ejb lookup
     * is not found.
     */
    @Test
    public void testJndiGlobalEJBLookup() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);

        Object instance = helper.getObjectInstance(JavaColonNamespace.GLOBAL, "ejbmod1/hello");
        assertNotNull("EJB instance is null", instance);
        instance = helper.getObjectInstance(JavaColonNamespace.GLOBAL, "ejbmod1/hello!com.ibm.ws.Hello");
        assertNotNull("EJB instance is null", instance);
    }

    /**
     * Verify a non-local EJB lookup returns null. Should not happen
     * in Liberty.
     */
    @Test
    public void testJndiGlobalNotLocalEJBLookup() throws Exception {
        MockEJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);
        helper.setTestingException(true);

        try {
            helper.getObjectInstance(JavaColonNamespace.GLOBAL, "mod/notlocal");
            fail("An exception should have been thrown.");
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("Cannot instantiate object message text is null", msgTxt);
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.startsWith("CNTR4009E: "));
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.contains("mod/notlocal"));
            assertFalse("parameters not filled in.", msgTxt.contains("{"));

            J2EEName name = helper.getTargetEJBJ2EEName();
            String matchingMessage = ".*" + name.getComponent() +
                                     ".*" + name.getModule() +
                                     ".*" + name.getApplication() + ".*";
            assertTrue("Incorrect parameters filled in for EJB: " + msgTxt,
                       msgTxt.matches(matchingMessage));
        }
    }

    /**
     * Verify a non-local EJB lookup returns null. Should not happen
     * in Liberty.
     */
    @Test
    public void testJndiGlobalHomeEJBLookup() throws Exception {
        MockEJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);
        helper.setTestingException(true);

        try {
            helper.getObjectInstance(JavaColonNamespace.GLOBAL, "mod/home");
            fail("An exception should have been thrown.");
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("Cannot instantiate object message text is null", msgTxt);
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.startsWith("CNTR4008E: "));
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.contains("mod/home"));
            assertFalse("parameters not filled in.", msgTxt.contains("{"));

            J2EEName name = helper.getTargetEJBJ2EEName();
            String matchingMessage = ".*" + name.getComponent() +
                                     ".*" + name.getModule() +
                                     ".*" + name.getApplication() + ".*";
            assertTrue("Incorrect parameters filled in for EJB: " + msgTxt,
                       msgTxt.matches(matchingMessage));
        }
    }

    /**
     * Verify correct exception is throw with correct text from message file
     * when an exception occurs obtaining the object instance.
     */
    @Test
    public void testJndiWithNullBinding() throws Exception {
        MockEJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);
        String jndiName = "cannot_instantiate_object";
        helper.addGlobalBinding(jndiName, null);
        helper.setTestingException(true);

        Object instance = helper.getObjectInstance(JavaColonNamespace.GLOBAL, jndiName);
        assertNull("getObjectInstance should return null.", instance);
    }

    @Test
    public void testJndiListInstancesNoJ2eeThread() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(false);

        try {
            Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.GLOBAL, "");
            assertEquals(0, pairs.size());
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("No CMD on thread exception message text is null", msgTxt);
            assertTrue("No CMD on thread exception message not correct : " + msgTxt,
                       msgTxt.startsWith("CWWKN0100E: "));
            assertTrue("JNDI name not correct in message: " + msgTxt,
                       msgTxt.contains("java:global "));
        }
    }

    @Test
    public void testJndiListInstancesNotGlobal() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(false);

        // COMP_ENV not supported by this JavaColonHelper.
        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.COMP_ENV, "");
        assertEquals(0, pairs.size());
    }

    @Test
    public void testJndiListRootContext() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.GLOBAL, "");

        Set<String> remaining = new HashSet<String>(Arrays.asList("ejbmod1", "ejbmod2", "mod"));
        for (NameClassPair pair : pairs) {
            assertEquals(Context.class.getName(), pair.getClassName());
            if (!remaining.remove(pair.getName())) {
                fail("Pair name returned more than once: " + pair.getName());
            }
        }
        assertEquals(3, pairs.size());
        assertTrue(remaining.isEmpty());

        String[] names = new String[] { "ejbmod1/hello", "ejbmod1/hello!com.ibm.ws.Hello" };
        helper.removeGlobalBindings(Arrays.asList(names));

        pairs = helper.listInstances(JavaColonNamespace.GLOBAL, "");
        assertEquals(2, pairs.size());
    }

    @Test
    public void testJndiListRoot() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.GLOBAL, "");
        assertEquals(3, pairs.size());

        pairs = helper.listInstances(JavaColonNamespace.GLOBAL, "mod");
        assertEquals(3, pairs.size());
    }

    @Test
    public void testJndiListInstances() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.GLOBAL, "ejbmod1");
        assertEquals(2, pairs.size());
        Set<String> remaining = new HashSet<String>(Arrays.asList("hello", "hello!com.ibm.ws.Hello"));
        for (NameClassPair pair : pairs) {
            if (!remaining.remove(pair.getName())) {
                fail("Pair name returned more than once: " + pair.getName());
            }
            assertEquals("com.ibm.ws.Hello", pair.getClassName());
        }
        assertTrue(remaining.isEmpty());
    }

    @Test
    public void testJndiListInstancesPartialMatch() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.GLOBAL, "ejb");
        assertEquals(0, pairs.size());
    }

    @Test
    public void testJndiListNoBindings() throws Exception {
        EJBJavaColonNamingHelper helper = new MockEJBJavaColonNamingHelper(true);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.COMP_ENV, "");
        assertEquals(0, pairs.size());
    }

    @Test
    public void testJndiHasObjectGlobalNoJ2EEThread() throws Exception {
        try {
            EJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(false);

            helper.hasObjectWithPrefix(JavaColonNamespace.GLOBAL, "ejbmod1");
            fail("Should have thrown a NamingException");
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("No CMD on thread exception message text is null", msgTxt);
            assertTrue("No CMD on thread exception message not correct : " + msgTxt,
                       msgTxt.startsWith("CWWKN0100E: "));
            assertTrue("JNDI name not correct in message: " + msgTxt,
                       msgTxt.contains("java:global/ejbmod1"));
        }
    }

    @Test
    public void testJndiListNotContextException() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);

        try {
            helper.listInstances(JavaColonNamespace.GLOBAL, "ejbmod1/hello");
            fail("NotContextException should have been thrown.");
        } catch (NotContextException nce) {
            assertEquals(nce.getMessage(), "java:global/ejbmod1/hello");
        }
    }

    @Test
    public void testJndiHasObjectWithPrefixNotSupported() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);

        boolean found = helper.hasObjectWithPrefix(JavaColonNamespace.COMP_ENV, "ejbmod1");
        assertFalse("Should not find match for comp/env", found);
    }

    @Test
    public void testJndiHasObjectWithPrefixNotFound() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);

        boolean found = helper.hasObjectWithPrefix(JavaColonNamespace.GLOBAL, "badName");
        assertFalse("Bad return code for object which is not found", found);
    }

    @Test
    public void testJndiHasObjectWithEmptyMap() throws Exception {
        EJBJavaColonNamingHelper helper = new MockEJBJavaColonNamingHelper(true);

        boolean found = helper.hasObjectWithPrefix(JavaColonNamespace.GLOBAL, "");
        assertFalse("JNDI list is empty. Should return false.", found);
    }

    @Test
    public void testJndiHasObjectNotEmptyMap() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithGlobalBindings(true);

        boolean found = helper.hasObjectWithPrefix(JavaColonNamespace.GLOBAL, "");
        assertTrue("JNDI name should have been found.", found);
    }

    /**
     * @return
     */
    private MockEJBJavaColonNamingHelper createDefaultHelperWithGlobalBindings(boolean createCMD) {
        MockEJBJavaColonNamingHelper helper = new MockEJBJavaColonNamingHelper(createCMD);

        EJBBinding bindings = new EJBBinding(null, "com.ibm.ws.Hello", 0, true);
        helper.addGlobalBinding("ejbmod1/hello", bindings);
        helper.addGlobalBinding("ejbmod1/hello!com.ibm.ws.Hello", bindings);

        bindings = new EJBBinding(null, "com.ibm.ws.Goodbye", 0, true);
        helper.addGlobalBinding("ejbmod2/goodbye", bindings);
        helper.addGlobalBinding("ejbmod2/goodbye!com.ibm.ws.Goodbye", bindings);

        helper.addGlobalBinding("mod/home", new EJBBinding(null, "com.ibm.ws.Home", -1, false)); // home
        helper.addGlobalBinding("mod/notlocal", new EJBBinding(null, "com.ibm.ws.Interface1", 0, false)); // not local
        helper.addGlobalBinding("mod/ejb", new EJBBinding(null, "com.ibm.ws.Interface2", 0, true));

        return helper;
    }

}
