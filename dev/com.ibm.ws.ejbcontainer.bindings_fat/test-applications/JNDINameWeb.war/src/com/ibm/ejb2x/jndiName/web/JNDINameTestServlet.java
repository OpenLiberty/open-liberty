/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejb2x.jndiName.web;

import static org.junit.Assert.fail;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.jndiName.ejb.JNDIName;
import com.ibm.ejb2x.jndiName.ejb.JNDINameHome;
import com.ibm.ejb2x.jndiName.ejb.JNDINameRemoteHome;
import com.ibm.ejb2x.jndiName.ejb.JNDIRemoteName;

import componenttest.app.FATServlet;

/**
 * Tests that you can lookup the 3 ejb namespace contexts; ejblocal, local, and the
 * local:ejb sub context.
 *
 * Tests that the default ejblocal binding is disabled when a custom binding is defined.
 *
 * Tests a number of combinations of jndi lookups for beans that have a number of combinations of
 * custom bindings defined with the jndiName element in ibm-ejb-jar-bnd.xmi.
 *
 */
@SuppressWarnings("serial")
@WebServlet("/JNDINameTestServlet")
public class JNDINameTestServlet extends FATServlet {

    /*
     * Tests the lookup of the various EJB namespaces from ejblocal:
     */
    @Test
    public void testEJBLocalNamespaceLookup() throws Exception {
        Context ejblocal = null;

        try {
            ejblocal = (Context) new InitialContext().lookup("ejblocal:");
            if (ejblocal == null) {
                fail("Could not lookup ejblocal: naming context");
            }
        } catch (Exception e) {
            fail("Could not lookup ejblocal: naming context");
        }
        try {
            if (ejblocal.lookup("ejblocal:") == null) {
                fail("Could not lookup ejblocal: naming context from ejblocal: naming context");
            }
        } catch (Exception e) {
            fail("Could not lookup ejblocal: naming context from ejblocal: naming context");
        }
        try {
            if (ejblocal.lookup("local:") == null) {
                fail("Could not lookup local: naming context from ejblocal: naming context");
            }
        } catch (Exception e) {
            fail("Could not lookup local: naming context from ejblocal: naming context");
        }
        try {
            if (ejblocal.lookup("local:ejb") == null) {
                fail("Could not lookup local:ejb naming context from ejblocal: naming context");
            }
        } catch (Exception e) {
            fail("Could not lookup local:ejb naming context from ejblocal: naming context");
        }
    }

    /*
     * Tests the lookup of the various EJB namespaces from local:
     */
    @Test
    public void testLocalNamespaceLookup() throws Exception {
        Context local = null;

        try {
            local = (Context) new InitialContext().lookup("local:");
            if (local == null) {
                fail("Could not lookup local: naming context");
            }
        } catch (Exception e) {
            fail("Could not lookup local: naming context");
        }
        try {
            if (local.lookup("ejblocal:") == null) {
                fail("Could not lookup ejblocal: naming context from local: naming context");
            }
        } catch (Exception e) {
            fail("Could not lookup ejblocal: naming context from local: naming context");
        }
        try {
            if (local.lookup("local:") == null) {
                fail("Could not lookup local: naming context from local: naming context");
            }
        } catch (Exception e) {
            fail("Could not lookup local: naming context from local: naming context");
        }
        try {
            if (local.lookup("local:ejb") == null) {
                fail("Could not lookup local:ejb naming context from local: naming context");
            }
        } catch (Exception e) {
            fail("Could not lookup local:ejb naming context from local: naming context");
        }
    }

    /*
     * Tests the lookup of the various EJB namespaces from local:ejb/
     */
    @Test
    public void testLocalEJBNamespaceLookup() throws Exception {
        Context localejb = null;

        try {
            localejb = (Context) new InitialContext().lookup("local:ejb");
            if (localejb == null) {
                fail("Could not lookup local:ejb naming context");
            }
        } catch (Exception e) {
            fail("Could not lookup local:ejb naming context");
        }
        try {
            if (localejb.lookup("ejblocal:") == null) {
                fail("Could not lookup ejblocal: naming context from local:ejb naming context");
            }
        } catch (Exception e) {
            fail("Could not lookup ejblocal: naming context from local:ejb naming context");
        }
        try {
            if (localejb.lookup("local:") == null) {
                fail("Could not lookup local: naming context from local:ejb naming context");
            }
        } catch (Exception e) {
            fail("Could not lookup local: naming context from local:ejb naming context");
        }
        try {
            if (localejb.lookup("local:ejb") == null) {
                fail("Could not lookup local:ejb naming context from local:ejb naming context");
            }
        } catch (Exception e) {
            fail("Could not lookup local:ejb naming context from local:ejb naming context");
        }
    }

    /*
     * Tests that the local: default binding should not have been bound because we
     * have custom bindings.
     */
    @Test
    public void testLocalColonDefaultDisabledJNDIName() {
        try {
            Object bean = new InitialContext().lookup("local:ejb/JNDIName1");
            if (bean != null) {
                fail("Local default bindings lookup should not have worked because we have custom bindings");
            }
        } catch (NamingException e) {
            // expected to not work
        }
    }

    /*
     * Tests that the ejblocal: default binding should not have been bound because we
     * have custom bindings.
     */
    @Test
    public void testEJBLocalDefaultDisabledJNDIName() {
        try {
            Object bean = new InitialContext().lookup("ejblocal:ejb/JNDIName1");
            if (bean != null) {
                fail("EJBLocal default bindings lookup should not have worked because we have custom bindings");
            }
        } catch (NamingException e) {
            // expected to not work
        }
    }

    /*
     * Tests that the remote default binding should not have been bound because we have custom bindings.
     *
     */
    @Test
    public void testRemoteDefaultDisabledJNDIName() {
        try {
            Object bean = new InitialContext().lookup("ejb/JNDIName1");
            if (bean != null) {
                fail("remote default bindings lookup should not have worked because we have custom bindings");
            }
        } catch (NamingException e) {
            // expected to not work
        }
    }

    /*
     * Tests a bunch of different jndi lookup combinations against a bean
     * Expecting the lookup to pass or fail accordingly
     */
    private void testLookupCombinations(boolean remote, String jndiName, int beanNum) throws Exception {
        System.out.println("Testing " + jndiName);

        // default context ejblocal lookups -------------------------------------------------------------
        Context context = new InitialContext();
        String contextString = "Initial";

        // com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome3"> (remote bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome7"> (hybrid bean)
        String lookupName = "com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "3,7");

        // ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome4"> (remote bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome8"> (hybrid bean)
        lookupName = "ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "4,8");

        // ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // ejblocal:local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "ejblocal:local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // ejblocal context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("ejblocal:");
        contextString = "ejblocal:";

        // ejblocal: context + com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // ejblocal: context + ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // ejblocal: context + ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // ejblocal: context + ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // ejblocal: context + ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // ejblocal: context + ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // ejblocal: context + local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // default context local lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // local:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "local:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // local:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "local:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // local:local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "local:local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // local:ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "local:ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // local:ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "local:ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // local context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:");
        contextString = "local:";

        // local: context + com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // local: context + ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // local: context + ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // local: context + ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // local: context + ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // local: context + ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // local: context + local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // local: context + local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // local: context + local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // local:ejb context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:ejb");
        contextString = "local:ejb";

        // local:ejb context + com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // local:ejb context + ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // local:ejb context + ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // local:ejb context + ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // local:ejb context + ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // local:ejb context + ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // local:ejb context + local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // local:ejb context + local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // local:ejb context + local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // default context 3 nested lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // ejblocal:local:ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "ejblocal:local:ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");

        // ejblocal:local:ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2"> (local bean)
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6"> (hybrid bean)
        lookupName = "ejblocal:local:ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2,6");

        // local:ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "local:ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // local:ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1"> (local bean)
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5"> (hybrid bean)
        lookupName = "local:ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5");
    }

    /**
     * Helper that tries the actual lookup and asserts conditions based on if
     * it should work or not. this is done by checking what bean is being looked
     * up against a list of beans expected to pass.
     *
     * @param context - the namespace context to look up in. Like InitialContext or ejblocal:
     * @param lookupName - the lookup name to perform
     * @param jndiName - the jndiName="" name provided for the bean in xmi
     * @param beanNum - which bean we are testing against
     * @param passingCases - list of beans this lookupName should work on
     */
    private void testLookupCombinationsHelper(boolean remote, Context context, String contextString, String lookupName, String jndiName, String beanNum, String passingCases) {
        if (remote) {
            testLookupCombinationsHelperRemote(context, contextString, lookupName, jndiName, beanNum, passingCases);
        } else {
            testLookupCombinationsHelperLocal(context, contextString, lookupName, jndiName, beanNum, passingCases);
        }
    }

    private void testLookupCombinationsHelperLocal(Context context, String contextString, String lookupName, String jndiName, String beanNum, String passingCases) {
        try {
            System.out.println("Testing " + lookupName + " with context " + contextString + " against " + jndiName);
            JNDINameHome beanHome = (JNDINameHome) context.lookup(lookupName);
            if (passingCases.contains(beanNum)) {
                if (beanHome == null) {
                    fail("lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
                }
                try {
                    JNDIName bean = beanHome.create();
                    if (beanHome.create() == null) {
                        fail("home.create() for lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
                    }
                    System.out.println("Got bean, calling method");
                    if (bean.foo() == null) {
                        fail("bean.method() for lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    fail("home.create() for lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + jndiName + " and context " + contextString);
                }
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                e.printStackTrace(System.out);
                fail("lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        } catch (ClassCastException cce) {
            // For the hybrid beans they might have a remote bound in the lookup string, so we'll get a class cast
            // since we try all the lookup combinations, just ignore it.
            if (passingCases.contains(beanNum)) {
                cce.printStackTrace();
                fail("ClassCastException While performing lookup " + lookupName + " for " + jndiName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    private void testLookupCombinationsHelperRemote(Context context, String contextString, String lookupName, String jndiName, String beanNum, String passingCases) {
        try {
            System.out.println("Testing " + lookupName + " with context " + contextString + " against " + jndiName);
            JNDINameRemoteHome beanHome = (JNDINameRemoteHome) context.lookup(lookupName);
            if (passingCases.contains(beanNum)) {
                if (beanHome == null) {
                    fail("lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
                }
                try {
                    JNDIRemoteName bean = beanHome.create();
                    if (beanHome.create() == null) {
                        fail("home.create() for lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
                    }
                    System.out.println("Got bean, calling method");
                    if (bean.foo() == null) {
                        fail("bean.method() for lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    fail("home.create() for lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + jndiName + " and context " + contextString);
                }
            }
        } catch (ClassCastException cce) {
            if (passingCases.contains(beanNum)) {
                cce.printStackTrace();
                fail("ClassCastException While narrowing lookup " + lookupName + " for " + jndiName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                e.printStackTrace(System.out);
                fail("lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    @Test
    public void testjndiNameStartsWithCom() throws Exception {
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        testLookupCombinations(false, "jndiName=\"com/ibm/ejb2x/jndiName/ejb/JNDINameHome1\"", 1);
    }

    @Test
    public void testjndiNameStartsWithEJB() throws Exception {
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        testLookupCombinations(false, "jndiName=\"ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2\"", 2);
    }

    @Test
    public void testRemotejndiNameStartsWithCom() throws Exception {
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome3">
        testLookupCombinations(true, "jndiName=\"com/ibm/ejb2x/jndiName/ejb/JNDINameHome3\"", 3);
    }

    @Test
    public void testRemotejndiNameStartsWithEJB() throws Exception {
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome4">
        testLookupCombinations(true, "jndiName=\"ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome4\"", 4);
    }

    @Test
    public void testHybridjndiNameStartsWithComLocalMode() throws Exception {
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome5">
        testLookupCombinations(false, "jndiName=\"com/ibm/ejb2x/jndiName/ejb/JNDINameHome5\"", 5);
    }

    @Test
    public void testHybridjndiNameStartsWithEJBLocalMode() throws Exception {
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6">
        testLookupCombinations(false, "jndiName=\"ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome6\"", 6);
    }

    @Test
    public void testHybridjndiNameStartsWithComRemoteMode() throws Exception {
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome7">
        testLookupCombinations(true, "jndiName=\"com/ibm/ejb2x/jndiName/ejb/JNDINameHome7\"", 7);
    }

    @Test
    public void testHybridjndiNameStartsWithEJBRemoteMode() throws Exception {
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome8">
        testLookupCombinations(true, "jndiName=\"ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome8\"", 8);
    }

}
