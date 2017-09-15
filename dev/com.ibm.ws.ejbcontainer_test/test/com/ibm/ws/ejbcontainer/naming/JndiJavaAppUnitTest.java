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
 * Tests EJBJavaColonNamingHelper - java:app
 */
@SuppressWarnings("static-method")
public class JndiJavaAppUnitTest {
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
            Object instance = helper.getObjectInstance(JavaColonNamespace.APP, jndiName);
            fail("getObjectInstance with no CMD on thread should fail : " + instance);
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("No CMD on thread exception message text is null", msgTxt);
            assertTrue("No CMD on thread exception message not correct : " + msgTxt,
                       msgTxt.startsWith("CWWKN0100E: "));
            assertTrue("JNDI name not correct in message: " + msgTxt,
                       msgTxt.contains("java:app/" + jndiName));
        }
    }

    /**
     * Verify correct exception is throw when the ejb lookup
     * is not found.
     */
    @Test
    public void testJndiNoEJBExceptionMsg() throws Exception {
        EJBJavaColonNamingHelper helper = new MockEJBJavaColonNamingHelper(true);

        Object instance = helper.getObjectInstance(JavaColonNamespace.APP, "no_ejb");
        assertNull("getObjectInstance with no EJB should return null.", instance);
    }

    /**
     * Verify correct exception is throw when the ejb lookup
     * is not found.
     */
    @Test
    public void testJndiAppEJBLookup() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithAppBindings(true);

        Object instance = helper.getObjectInstance(JavaColonNamespace.APP, "mod/ejb");
        assertNotNull("EJB instance is null", instance);
        instance = helper.getObjectInstance(JavaColonNamespace.APP, "mod/ejb!com.ibm.ws.Interface");
        assertNotNull("EJB instance is null", instance);
    }

    /**
     * Verify a non-local EJB lookup throws exception.
     */
    @Test
    public void testJndiAppNotLocalEJBLookup() throws Exception {
        MockEJBJavaColonNamingHelper helper = createDefaultHelperWithAppBindings(true);
        helper.setTestingException(true);

        try {
            helper.getObjectInstance(JavaColonNamespace.APP, "mod/notlocal");
            fail("should have gotten exception.");
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
     * Verify a home EJB lookup throws exception.
     */
    @Test
    public void testJndiAppHomeEJBLookup() throws Exception {
        MockEJBJavaColonNamingHelper helper = createDefaultHelperWithAppBindings(true);
        helper.setTestingException(true);

        try {
            helper.getObjectInstance(JavaColonNamespace.APP, "mod/home");
            fail("should have gotten exception.");
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
     * verify null is returned if the binding is null in the map.
     * No exception in this case.
     */
    @Test
    public void testJndiWithNullBinding() throws Exception {
        MockEJBJavaColonNamingHelper helper = new MockEJBJavaColonNamingHelper(true);

        String jndiName = "cannot_instantiate_object";
        helper.addAppBinding(helper.getMMD(), jndiName, null); // null bindings

        Object instance = helper.getObjectInstance(JavaColonNamespace.APP, jndiName);
        assertNull("getObjectInstance with object creation exception should return null.", instance);

        // Remove the binding
        String[] names = new String[] { jndiName };
        helper.removeAppBindings(helper.getMMD(), Arrays.asList(names));
    }

    /**
     * Verify correct exception is throw with correct text from message file
     * when an exception occurs obtaining the object instance.
     */
    @Test
    public void testJndiNoEJBInMapFound() throws Exception {
        MockEJBJavaColonNamingHelper helper = createDefaultHelperWithAppBindings(true);
        helper.setTestingException(true);

        String jndiName = "cannot_instantiate_object";

        Object instance = helper.getObjectInstance(JavaColonNamespace.APP, jndiName);
        assertNull("getObjectInstance should return null.", instance);
    }

    /**
     * Verify correct exception is throw with correct text from message file
     * when an exception occurs obtaining the object instance.
     */
    @Test
    public void testJndiWithFailedInstanciation() throws Exception {
        MockEJBJavaColonNamingHelper helper = createDefaultHelperWithAppBindings(true);
        helper.setTestingException(true);

        try {
            Object instance = helper.getObjectInstance(JavaColonNamespace.APP, "ejbmod2/goodbye!com.ibm.ws.Goodbye");
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
        EJBJavaColonNamingHelper helper = createDefaultHelperWithAppBindings(false);

        try {
            Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.APP, "");
            assertEquals(0, pairs.size());
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("No CMD on thread exception message text is null", msgTxt);
            assertTrue("No CMD on thread exception message not correct : " + msgTxt,
                       msgTxt.startsWith("CWWKN0100E: "));
            assertTrue("JNDI name not correct in message: " + msgTxt,
                       msgTxt.contains("java:app "));
        }
    }

    @Test
    public void testJndiListInstancesNotAPP() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithAppBindings(false);

        // COMP_ENV not supported by this JavaColonHelper.
        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.COMP_ENV, "");
        assertEquals(0, pairs.size());
    }

    @Test
    public void testJndiListRootContext() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithAppBindings(true);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.APP, "");
        for (NameClassPair pair : pairs) {
            assertEquals(Context.class.getName(), pair.getClassName());

            boolean test = "mod".equals(pair.getName()) ||
                           "ejbmod1".equals(pair.getName()) ||
                           "ejbmod2".equals(pair.getName());
            assertTrue("Module names are not correct.", test);
        }
        assertEquals(3, pairs.size());
    }

    @Test
    public void testJndiListInstances() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithAppBindings(true);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.APP, "mod");
        assertEquals(4, pairs.size());
        for (NameClassPair pair : pairs) {
            if ("ejb".equals(pair.getName())) {
                assertTrue("Name/Class pair not correct: " + pair.getName() + "/" + pair.getClassName(),
                           pair.getClassName().equals("com.ibm.ws.Interface2"));
            } else if ("ejb!com.ibm.ws.Interface".equals(pair.getName())) {
                assertTrue("Name/Class pair not correct: " + pair.getName() + "/" + pair.getClassName(),
                           pair.getClassName().equals("com.ibm.ws.Interface3"));
            } else if ("notlocal".equals(pair.getName())) {
                assertTrue("Name/Class pair not correct: " + pair.getName() + "/" + pair.getClassName(),
                           pair.getClassName().equals("com.ibm.ws.Interface1"));
            } else if ("home".equals(pair.getName())) {
                assertTrue("Name/Class pair not correct: " + pair.getName() + "/" + pair.getClassName(),
                           pair.getClassName().equals("com.ibm.ws.Home"));
            }
            else {
                fail("Name/class pairs are not correct");
            }
        }
    }

    @Test
    public void testJndiListSpecific() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithAppBindings(true);

        try {
            helper.listInstances(JavaColonNamespace.APP, "ejbmod2/goodbye");
            fail("NotContextException should have been thrown.");
        } catch (NotContextException nce) {
            assertEquals(nce.getMessage(), "java:app/ejbmod2/goodbye");
        }
    }

    @Test
    public void testJndiListNoBindings() throws Exception {
        EJBJavaColonNamingHelper helper = new MockEJBJavaColonNamingHelper(true);

        Collection<? extends NameClassPair> pairs = helper.listInstances(JavaColonNamespace.COMP_ENV, "");
        assertEquals(0, pairs.size());
    }

    @Test
    public void testJndiHasObjectAppNoJ2EEThread() throws Exception {
        try {
            EJBJavaColonNamingHelper helper = createDefaultHelperWithAppBindings(false);

            helper.hasObjectWithPrefix(JavaColonNamespace.APP, "ejbmod1");
            fail("Should have thrown a NamingException");
        } catch (NamingException nex) {
            String msgTxt = nex.getMessage();
            assertNotNull("No CMD on thread exception message text is null", msgTxt);
            assertTrue("No CMD on thread exception message not correct : " + msgTxt,
                       msgTxt.startsWith("CWWKN0100E: "));
            assertTrue("JNDI name not correct in message: " + msgTxt,
                       msgTxt.contains("java:app/ejbmod1"));
        }
    }

    @Test
    public void testJndiHasObjectWithPrefixNotFound() throws Exception {
        EJBJavaColonNamingHelper helper = createDefaultHelperWithAppBindings(true);

        boolean found = helper.hasObjectWithPrefix(JavaColonNamespace.APP, "badName");
        assertFalse("Bad return code for object which is not found", found);
    }

    /**
     * @return
     */
    private MockEJBJavaColonNamingHelper createDefaultHelperWithAppBindings(boolean createCMD) {
        MockEJBJavaColonNamingHelper helper = new MockEJBJavaColonNamingHelper(createCMD);

        EJBBinding bindings = new EJBBinding(null, "com.ibm.ws.Hello", 0, true);
        helper.addAppBinding(helper.getMMD(), "ejbmod1/hello", bindings);
        helper.addAppBinding(helper.getMMD(), "ejbmod1/hello!com.ibm.ws.Hello", bindings);

        bindings = new EJBBinding(null, "com.ibm.ws.Goodbye", 0, true);
        helper.addAppBinding(helper.getMMD(), "ejbmod2/goodbye", bindings);
        helper.addAppBinding(helper.getMMD(), "ejbmod2/goodbye!com.ibm.ws.Goodbye", bindings);

        helper.addAppBinding(helper.getMMD(), "mod/home", new EJBBinding(null, "com.ibm.ws.Home", -1, false)); // home 
        helper.addAppBinding(helper.getMMD(), "mod/notlocal", new EJBBinding(null, "com.ibm.ws.Interface1", 0, false)); // not local
        helper.addAppBinding(helper.getMMD(), "mod/ejb", new EJBBinding(null, "com.ibm.ws.Interface2", 0, true));
        helper.addAppBinding(helper.getMMD(), "mod/ejb!com.ibm.ws.Interface", new EJBBinding(null, "com.ibm.ws.Interface3", 0, true));
        return helper;
    }
}
