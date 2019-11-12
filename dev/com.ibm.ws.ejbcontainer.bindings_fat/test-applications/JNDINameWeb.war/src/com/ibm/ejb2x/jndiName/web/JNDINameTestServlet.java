/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import com.ibm.ejb2x.jndiName.ejb.JNDINameHome;

import componenttest.app.FATServlet;

/**
 * Tests that you can lookup the 3 ejb namespace contexts; ejblocal, local, and the
 * local:ejb sub context.
 *
 * Tests that the default ejblocal binding is disabled when a custom binding is defined.
 *
 * Tests a number of combinations of jndi lookups for beans that have a number of combinations of
 * custom bindings defined with the jndiName element in ibm-ejb-jar-bnd.xmi. Based on these rules:
 *
 * 1. You can lookup just ejblocal: local: and local:ejb to get namespace contexts and then exclude them from the lookup.
 *
 * 2. If jndiName does not have a namespace written in, it will be bound to ejblocal:, local: and local:ejb with
 * the caveat that the local: context binding will be preceded with ejb.
 * - in other words jndiName="<name>" can be looked up by first getting local: namespace context
 * - and then looking up ejb/<name> or first getting local:ejb and looking up <name>
 *
 * 3. if the jndiName happens to start with ejb in the <name> the local: lookups will have double ejb
 * - local:ejb/ejb/com/ibm/ejb2x/ejbinwar/webejb2x/Stateless2xLocalHome
 * - with local: context: ejb/ejb/com/ibm/ejb2x/ejbinwar/webejb2x/Stateless2xLocalHome
 * - with local:ejb/ context: ejb/com/ibm/ejb2x/ejbinwar/webejb2x/Stateless2xLocalHome
 *
 * 4. If jndiName="ejblocal:<name> it will only be bound to ejblocal:
 *
 * 5. If jndiName="local:<name> it will only be bound to local:
 * - also, the binding will not have ejb stuck in front, the lookup is local:<name> not local:ejb/<name>
 * - or with local: context it is <name> not ejb/<name>
 *
 * 6. If jndiName="local:ejb/<name> it will be bound in local: and local:ejb
 * - the local: lookup will not have double ejb stuck in front
 * - local:ejb/<name>
 * - with local: context: ejb/<name>
 * - with local:ejb context: <name>
 *
 * 7. If you do jndiName="<namespace>:<namespace>:<name> it will ignore up to the innermost namespace.
 * - jndiName="local:ejblocal:ejb/com/ibm/ejb2x/ejbinwar/webejb2x/Stateless2xLocalHome" is only bound to ejblocal: (using the jndiName="ejblocal: rule above)
 * - jndiName="ejblocal:local:ejb/com/ibm/ejb2x/ejbinwar/webejb2x/Stateless2xLocalHome" would use the local:ejb rule above
 *
 * 8. You can do a lookup and chain as many ejblocal: and local: in front as you want.
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
     * Tests that the ejblocal: default binding should not have been bound because we
     * have custom bindings.
     */
    @Test
    public void testEJBLocalDefaultDisabled() {
        try {
            Object bean = new InitialContext().lookup("ejblocal:JNDINameTestApp/JNDINameEJB.jar/JNDIName1#com.ibm.ejb2x.jndiName.ejb.JNDINameHome");
            if (bean != null) {
                fail("EJBLocal default bindings lookup should not have worked because we have custom bindings");
            }
        } catch (NamingException e) {
            // expected to not work
        }
    }

    /*
     * Tests a bunch of different jndi lookup combinations against a bean
     * Expecting the lookup to pass or fail accordingly
     */
    private void testLookupCombinations(String jndiName, int beanNum) throws Exception {
        System.out.println("Testing " + jndiName);

        // default context ejblocal lookups -------------------------------------------------------------
        Context context = new InitialContext();
        String contextString = "Initial";

        // com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        String lookupName = "com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome3">
        lookupName = "ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,3");

        // ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome4">
        // jndiName="ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome6">
        lookupName = "ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "4,6");

        // ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome5">
        // jndiName="ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome7">
        lookupName = "ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5,7");

        // ejblocal:local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "ejblocal:local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // ejblocal context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("ejblocal:");
        contextString = "ejblocal:";

        // ejblocal: context + com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome3">
        lookupName = "com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,3");

        // ejblocal: context + ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // ejblocal: context + ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // ejblocal: context + ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome3">
        lookupName = "ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,3");

        // ejblocal: context + ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // ejblocal: context + ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome4">
        // jndiName="ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome6">
        lookupName = "local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "4,6");

        // ejblocal: context + local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome5">
        // jndiName="ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome7">
        lookupName = "local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5,7");

        // ejblocal: context + local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // default context local lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome4">
        // jndiName="ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome6">
        lookupName = "local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "4,6");

        // local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome5">
        // jndiName="ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome7">
        lookupName = "local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5,7");

        // local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // local:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome4">
        // jndiName="ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome6">
        lookupName = "local:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "4,6");

        // local:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome5">
        // jndiName="ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome7">
        lookupName = "local:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5,7");

        // local:local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "local:local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // local:ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome3">
        lookupName = "local:ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,3");

        // local:ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "local:ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // local context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:");
        contextString = "local:";

        // local: context + com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome4">
        // jndiName="ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome6">
        lookupName = "com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "4,6");

        // local: context + ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome5">
        // jndiName="ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome7">
        lookupName = "ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5,7");

        // local: context + ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // local: context + ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome3">
        lookupName = "ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,3");

        // local: context + ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // local: context + ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // local: context + local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome4">
        // jndiName="ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome6">
        lookupName = "local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "4,6");

        // local: context + local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome5">
        // jndiName="ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome7">
        lookupName = "local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5,7");

        // local: context + local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // local:ejb context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:ejb");
        contextString = "local:ejb";

        // local:ejb context + com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome5">
        // jndiName="ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome7">
        lookupName = "com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5,7");

        // local:ejb context + ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // local:ejb context + ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // local:ejb context + ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome3">
        lookupName = "ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,3");

        // local:ejb context + ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // local:ejb context + ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "none");

        // local:ejb context + local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome4">
        // jndiName="ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome6">
        lookupName = "local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "4,6");

        // local:ejb context + local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome5">
        // jndiName="ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome7">
        lookupName = "local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5,7");

        // local:ejb context + local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "local:ejb/ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // default context 3 nested lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // ejblocal:local:ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome3">
        lookupName = "ejblocal:local:ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,3");

        // ejblocal:local:ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        lookupName = "ejblocal:local:ejblocal:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "2");

        // local:ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome4">
        // jndiName="ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome6">
        lookupName = "local:ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "4,6");

        // local:ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome# should work for
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        // jndiName="local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome5">
        // jndiName="ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome7">
        lookupName = "local:ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, jndiName, Integer.toString(beanNum), "1,5,7");
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
    private void testLookupCombinationsHelper(Context context, String contextString, String lookupName, String jndiName, String beanNum, String passingCases) {
        try {
            System.out.println("Testing " + lookupName + " with context " + contextString + " against " + jndiName);
            JNDINameHome beanHome = (JNDINameHome) context.lookup(lookupName);
            if (passingCases.contains(beanNum)) {
                if (beanHome == null) {
                    fail("lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
                }
                try {
                    if (beanHome.create() == null) {
                        fail("home.create() for lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
                    }
                } catch (Exception e) {
                    fail("home.create() for lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + jndiName + " and context " + contextString);
                }
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                fail("lookup " + lookupName + " should have worked for " + jndiName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    @Test
    public void testjndiNameStartsWithCom() throws Exception {
        // jndiName="com/ibm/ejb2x/jndiName/ejb/JNDINameHome1">
        testLookupCombinations("jndiName=\"com/ibm/ejb2x/jndiName/ejb/JNDINameHome1\"", 1);
    }

    @Test
    public void testjndiNameStartsWithEJB() throws Exception {
        // jndiName="ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2">
        testLookupCombinations("jndiName=\"ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome2\"", 2);
    }

    @Test
    public void testjndiNameStartsWithEJBLocal() throws Exception {
        // jndiName="ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome3">
        testLookupCombinations("jndiName=\"ejblocal:com/ibm/ejb2x/jndiName/ejb/JNDINameHome3\"", 3);
    }

    @Test
    public void testjndiNameStartsWithLocal() throws Exception {
        // jndiName="local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome4">
        testLookupCombinations("jndiName=\"local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome4\"", 4);
    }

    @Test
    public void testjndiNameStartsWithLocalEJB() throws Exception {
        // jndiName="local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome5">
        testLookupCombinations("jndiName=\"local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome5\"", 5);
    }

    @Test
    public void testjndiNameStartsWithEJBLocalLocal() throws Exception {
        // jndiName="ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome6">
        testLookupCombinations("jndiName=\"ejblocal:local:com/ibm/ejb2x/jndiName/ejb/JNDINameHome6\"", 6);
    }

    @Test
    public void testjndiNameStartsWithEJBLocalLocalEJB() throws Exception {
        // jndiName="ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome7">
        testLookupCombinations("jndiName=\"ejblocal:local:ejb/com/ibm/ejb2x/jndiName/ejb/JNDINameHome7\"", 7);
    }

}
