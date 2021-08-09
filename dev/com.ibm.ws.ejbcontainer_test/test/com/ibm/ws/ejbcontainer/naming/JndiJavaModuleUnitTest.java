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
import com.ibm.websphere.csi.J2EENameFactory;
import com.ibm.ws.container.service.metadata.internal.J2EENameFactoryImpl;
import com.ibm.ws.container.service.naming.NamingConstants.JavaColonNamespace;
import com.ibm.ws.ejbcontainer.mock.MockEJBJavaColonNamingHelper;
import com.ibm.ws.ejbcontainer.mock.MockModuleMetaData;
import com.ibm.ws.ejbcontainer.osgi.internal.naming.EJBBinding;
import com.ibm.ws.ejbcontainer.osgi.internal.naming.EJBJavaColonNamingHelper;
import com.ibm.ws.runtime.metadata.ModuleMetaData;

/**
 * Tests EJBJavaColonNamingHelper - java:module
 */
@SuppressWarnings("static-method")
public class JndiJavaModuleUnitTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("EJBContainer=all");

    /**
     * Verify correct exception is throw with correct text from message file
     * when there is no component metadata on the thread.
     */
    @Test
    public void testJndiNonJeeThreadExceptionMsg() throws Exception {
        String jndiName = "no_cmd";
        try {
            EJBJavaColonNamingHelper helper = new EJBJavaColonNamingHelper();
            Object instance = helper.getObjectInstance(JavaColonNamespace.MODULE, jndiName);
            fail("getObjectInstance with no CMD on thread should fail : " + instance);
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("No CMD on thread exception message text is null", msgTxt);
            assertTrue("No CMD on thread exception message not correct : " + msgTxt,
                       msgTxt.startsWith("CWWKN0100E: "));
            assertTrue("JNDI name not correct in message: " + msgTxt,
                       msgTxt.contains("java:module/" + jndiName));
        }
    }

    /**
     * Verify correct exception is throw when the ejb lookup
     * is not found.
     */
    @Test
    public void testJndiNoEJBExceptionMsg() throws Exception {
        EJBJavaColonNamingHelper helper = new MockEJBJavaColonNamingHelper(true);

        Object instance = helper.getObjectInstance(JavaColonNamespace.MODULE, "no_ejb");
        assertNull("getObjectInstance with no EJB should return null.", instance);
    }

    /**
     * Verify correct exception is throw when the ejb lookup
     * is not found.
     */
    @Test
    public void testJndiModuleEJBLookup() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithModuleBindings(true);

        Object instance = helper.getObjectInstance(JavaColonNamespace.MODULE, "ejb");
        assertNotNull("EJB instance is null", instance);
        instance = helper.getObjectInstance(JavaColonNamespace.MODULE, "ejb!com.ibm.ws.Interface");
        assertNotNull("EJB instance is null", instance);
    }

    /**
     * Verify a non-local EJB lookup throws exception.
     */
    @Test
    public void testJndiModuleNotLocalEJBLookup() throws Exception {
        MockEJBJavaColonNamingHelper helper = createDefaultHelperWithModuleBindings(true);
        helper.setTestingException(true);

        try {
            helper.getObjectInstance(JavaColonNamespace.MODULE, "notlocal");
            fail("should have gotten exception.");
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("Cannot instantiate object message text is null", msgTxt);
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.startsWith("CNTR4009E: "));
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.contains("notlocal"));
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
     * Verify a home EJB lookup throws exception.
     */
    @Test
    public void testJndiModuleHomeEJBLookup() throws Exception {
        MockEJBJavaColonNamingHelper helper = createDefaultHelperWithModuleBindings(true);
        helper.setTestingException(true);

        try {
            helper.getObjectInstance(JavaColonNamespace.MODULE, "home");
            fail("should have gotten exception.");
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("Cannot instantiate object message text is null", msgTxt);
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.startsWith("CNTR4008E: "));
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.contains("home"));
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
    public void testJndiNoEJBInMapFound() throws Exception {
        MockEJBJavaColonNamingHelper helper = createDefaultHelperWithModuleBindings(true);
        helper.setTestingException(true);

        String jndiName = "cannot_instantiate_object";

        Object instance = helper.getObjectInstance(JavaColonNamespace.MODULE, jndiName);
        assertNull("getObjectInstance should return null.", instance);
    }

    /**
     * Verify correct exception is throw with correct text from message file
     * when an exception occurs obtaining the object instance.
     */
    @Test
    public void testJndiWithFailedInstanciation() throws Exception {
        MockEJBJavaColonNamingHelper helper = createDefaultHelperWithModuleBindings(true);
        helper.setTestingException(true);

        try {
            Object instance = helper.getObjectInstance(JavaColonNamespace.MODULE, "ejbmod2/goodbye!com.ibm.ws.Goodbye");
            assertNull("getObjectInstance with object creation exception should return null.", instance);
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("Cannot instantiate object message text is null", msgTxt);
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.startsWith("CNTR4007E: "));
            assertTrue("Cannot instantiate object message not correct : " + msgTxt,
                       msgTxt.contains("ejbmod2/goodbye!com.ibm.ws.Goodbye"));
            assertFalse("parameters not filled in.", msgTxt.contains("{"));

            J2EEName name = helper.getTargetEJBJ2EEName();
            String matchingMessage = ".*" + name.getComponent() +
                                     ".*" + name.getModule() +
                                     ".*" + name.getApplication() + ".*";
            assertTrue("Incorrect parameters filled in for EJB: " + msgTxt,
                       msgTxt.matches(matchingMessage));
        }
    }

    @Test
    public void testJndiListInstancesNoJ2eeThread() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithModuleBindings(false);

        try {
            Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.MODULE, "");
            assertEquals(0, pairs.size());
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("No CMD on thread exception message text is null", msgTxt);
            assertTrue("No CMD on thread exception message not correct : " + msgTxt,
                       msgTxt.startsWith("CWWKN0100E: "));
            assertTrue("JNDI name not correct in message: " + msgTxt,
                       msgTxt.contains("java:module "));
        }
    }

    @Test
    public void testJndiListInstancesNotModule() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithModuleBindings(false);

        // COMP_ENV not supported by this JavaColonHelper.
        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.COMP_ENV, "");
        assertEquals(0, pairs.size());
    }

    @Test
    public void testJndiListRootContext() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithModuleBindings(true);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.MODULE, "");
        Set<String> remaining = new HashSet<String>(Arrays.asList("hello", "hello!com.ibm.ws.Hello",
                                                                  "goodbye", "goodbye!com.ibm.ws.Goodbye",
                                                                  "home", "notlocal", "ejb",
                                                                  "ejb!com.ibm.ws.Interface"));

        for (NameClassPair pair : pairs) {
            if (!remaining.remove(pair.getName())) {
                fail("Pair name returned more than once: " + pair.getName());
            }
            assertFalse(Context.class.getName().equals(pair.getClassName()));
        }
        assertTrue(remaining.isEmpty());
    }

    @Test
    public void testJndiListSpecific() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithModuleBindings(true);

        try {
            helper.listInstances(JavaColonNamespace.MODULE, "goodbye");
            fail("NotContextException should have been thrown.");
        } catch (NotContextException nce) {
            assertEquals(nce.getMessage(), "java:module/goodbye");
        }
    }

    @Test
    public void testJndiListNoBindings() throws Exception {
        EJBJavaColonNamingHelper helper = new MockEJBJavaColonNamingHelper(true);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.COMP_ENV, "");
        assertEquals(0, pairs.size());
    }

    @Test
    public void testJndiHasObjectModuleNoJ2EEThread() throws Exception {
        try {
            EJBJavaColonNamingHelper helper = createDefaultHelperWithModuleBindings(false);

            helper.hasObjectWithPrefix(JavaColonNamespace.MODULE, "ejbmod1");
            fail("Should have thrown a NamingException");
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("No CMD on thread exception message text is null", msgTxt);
            assertTrue("No CMD on thread exception message not correct : " + msgTxt,
                       msgTxt.startsWith("CWWKN0100E: "));
            assertTrue("JNDI name not correct in message: " + msgTxt,
                       msgTxt.contains("java:module/ejbmod1"));
        }
    }

    @Test
    public void testJndiHasObjectWithPrefixNotFound() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithModuleBindings(true);

        boolean found = helper.hasObjectWithPrefix(JavaColonNamespace.MODULE, "badName");
        assertFalse("Bad return code for object which is not found", found);
    }

    /**
     * @return
     */
    private MockEJBJavaColonNamingHelper createDefaultHelperWithModuleBindings(boolean createCMD) {

        MockEJBJavaColonNamingHelper helper = new MockEJBJavaColonNamingHelper(createCMD);
        ModuleMetaData mmd = null;
        if (createCMD) {
            mmd = helper.getCMD().getModuleMetaData();
        } else {
            J2EENameFactory nameFactory = new J2EENameFactoryImpl();
            J2EEName name = nameFactory.create("app", "mod", "ejb");
            mmd = new MockModuleMetaData(name, null);
        }

        EJBBinding bindings = new EJBBinding(null, "com.ibm.ws.Hello", 0, true);
        helper.addModuleBinding(mmd, "hello", bindings);
        helper.addModuleBinding(mmd, "hello!com.ibm.ws.Hello", bindings);

        bindings = new EJBBinding(null, "com.ibm.ws.Goodbye", 0, true);
        helper.addModuleBinding(mmd, "goodbye", bindings);
        helper.addModuleBinding(mmd, "goodbye!com.ibm.ws.Goodbye", bindings);

        helper.addModuleBinding(mmd, "home", new EJBBinding(null, "com.ibm.ws.Home", -1, false)); // home 
        helper.addModuleBinding(mmd, "notlocal", new EJBBinding(null, "com.ibm.ws.Interface1", 0, false)); // not local
        helper.addModuleBinding(mmd, "ejb", new EJBBinding(null, "com.ibm.ws.Interface2", 0, true));
        helper.addModuleBinding(mmd, "ejb!com.ibm.ws.Interface", new EJBBinding(null, "com.ibm.ws.Interface3", 0, true));
        return helper;
    }
}
