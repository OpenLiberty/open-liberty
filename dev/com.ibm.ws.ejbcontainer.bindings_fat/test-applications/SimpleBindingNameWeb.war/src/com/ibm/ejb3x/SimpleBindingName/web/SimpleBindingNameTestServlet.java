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
package com.ibm.ejb3x.SimpleBindingName.web;

import static org.junit.Assert.fail;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb3x.SimpleBindingName.ejb.SimpleBindingName;
import com.ibm.ejb3x.SimpleBindingName.ejb.SimpleBindingNameHome;
import com.ibm.ejb3x.SimpleBindingName.ejb.SimpleBindingNameRemoteHome;
import com.ibm.ejb3x.SimpleBindingName.ejb.SimpleBindingRemoteName;

import componenttest.app.FATServlet;

/**
 * Tests that the default ejblocal binding is disabled when a custom binding is defined.
 *
 * Tests a number of combinations of jndi lookups for beans that have a number of combinations of
 * custom bindings defined with the SimpleBindingName element in ibm-ejb-jar-bnd.xml.
 */
@SuppressWarnings("serial")
@WebServlet("/SimpleBindingNameTestServlet")
public class SimpleBindingNameTestServlet extends FATServlet {

    /*
     * Tests that the ejblocal: default binding should not have been bound because we
     * have custom bindings.
     */
    @Test
    public void testEJBLocalDefaultDisabledSimpleBindingName() {
        try {
            Object bean = new InitialContext().lookup("ejblocal:SimpleBindingNameTestApp/SimpleBindingNameEJB.jar/SimpleBindingName1#com.ibm.ejb3x.SimpleBindingName.ejb.SimpleBindingNameHome");
            if (bean != null) {
                fail("EJBLocal default bindings lookup should not have worked because we have custom bindings");
            }
        } catch (NamingException e) {
            // expected to not work
        }
    }

    /*
     * Tests that the remote default binding should not have been bound because we
     * have custom bindings.
     */
    @Test
    public void testRemoteDefaultDisabledSimpleBindingName() {
        try {
            Object bean = new InitialContext().lookup("ejb/SimpleBindingNameTestApp/SimpleBindingNameEJB.jar/SimpleBindingName4#com.ibm.ejb3x.SimpleBindingName.ejb.SimpleBindingNameRemoteHome");
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
    private void testLookupCombinations(boolean remote, String SimpleBindingName, int beanNum) throws Exception {
        System.out.println("Testing " + SimpleBindingName);

        // default context ejblocal lookups -------------------------------------------------------------
        Context context = new InitialContext();
        String contextString = "Initial";

        // com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome3"> (remote)
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7"> (hybrid)
        String lookupName = "com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "3,7");

        // ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4"> (remote)
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome8"> (hybrid)
        lookupName = "ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "4,8");

        // ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1"> (local)
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5"> (hybrid)
        lookupName = "ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,5");

        // ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2"> (local)
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6"> (hybrid)
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2,6");

        // ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal:local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejblocal:local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("ejblocal:");
        contextString = "ejblocal:";

        // ejblocal: context + com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1"> (local)
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5"> (hybrid)
        lookupName = "com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,5");

        // ejblocal: context + ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2"> (local)
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6"> (hybrid)
        lookupName = "ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2,6");

        // ejblocal: context + ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1"> (local)
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5"> (hybrid)
        lookupName = "ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,5");

        // ejblocal: context + ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2"> (local)
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6"> (hybrid)
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2,6");

        // ejblocal: context + ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // default context local lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome#should always fail
        lookupName = "local:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1"> (local)
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5"> (hybrid)
        lookupName = "local:ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,5");

        // local:ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2"> (local)
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6"> (hybrid)
        lookupName = "local:ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2,6");

        // local context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:");
        contextString = "local:";

        // local: context + com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local: context + ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local: context + ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local: context + ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1"> (local)
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5"> (hybrid)
        lookupName = "ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,5");

        // local: context + ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2"> (local)
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6"> (hybrid)
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2,6");

        // local: context + ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local: context + local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local: context + local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local: context + local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:ejb");
        contextString = "local:ejb";

        // local:ejb context + com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1"> (local)
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5"> (hybrid)
        lookupName = "ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,5");

        // local:ejb context + ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2"> (local)
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6"> (hybrid)
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2,6");

        // local:ejb context + ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // default context 3 nested lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // ejblocal:local:ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1"> (local)
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5"> (hybrid)
        lookupName = "ejblocal:local:ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,5");

        // ejblocal:local:ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2"> (local)
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6"> (hybrid)
        lookupName = "ejblocal:local:ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2,6");

        // local:ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");
    }

    /**
     * Helper that tries the actual lookup and asserts conditions based on if
     * it should work or not. this is done by checking what bean is being looked
     * up against a list of beans expected to pass.
     *
     * @param context - the namespace context to look up in. Like InitialContext or ejblocal:
     * @param lookupName - the lookup name to perform
     * @param SimpleBindingName - the simple-binding-name="" name provided for the bean in xmi
     * @param beanNum - which bean we are testing against
     * @param passingCases - list of beans this lookupName should work on
     */
    private void testLookupCombinationsHelper(boolean remote, Context context, String contextString, String lookupName, String SimpleBindingName, String beanNum,
                                              String passingCases) {
        if (remote) {
            testLookupCombinationsHelperRemote(context, contextString, lookupName, SimpleBindingName, beanNum, passingCases);
        } else {
            testLookupCombinationsHelperLocal(context, contextString, lookupName, SimpleBindingName, beanNum, passingCases);
        }
    }

    private void testLookupCombinationsHelperLocal(Context context, String contextString, String lookupName, String SimpleBindingName, String beanNum, String passingCases) {
        try {
            System.out.println("Testing " + lookupName + " with context " + contextString + " against " + SimpleBindingName);
            SimpleBindingNameHome beanHome = (SimpleBindingNameHome) context.lookup(lookupName);
            if (passingCases.contains(beanNum)) {
                if (beanHome == null) {
                    fail("lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
                }
                try {
                    SimpleBindingName bean = beanHome.create();
                    if (bean == null) {
                        fail("home.create() for lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
                    }
                    System.out.println("Got bean, calling method");
                    if (bean.foo() == null) {
                        fail("bean.method() for lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    fail("home.create() or method call for lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + SimpleBindingName + " and context " + contextString);
                }
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                e.printStackTrace(System.out);
                fail("lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        } catch (ClassCastException cce) {
            // For the hybrid beans they might have a remote bound in the lookup string, so we'll get a class cast
            // since we try all the lookup combinations, just ignore it.
            if (passingCases.contains(beanNum)) {
                cce.printStackTrace();
                fail("ClassCastException While performing lookup " + lookupName + " for " + SimpleBindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    private void testLookupCombinationsHelperRemote(Context context, String contextString, String lookupName, String SimpleBindingName, String beanNum, String passingCases) {
        try {
            System.out.println("Testing " + lookupName + " with context " + contextString + " against " + SimpleBindingName);
            SimpleBindingNameRemoteHome beanHome = (SimpleBindingNameRemoteHome) context.lookup(lookupName);
            if (passingCases.contains(beanNum)) {
                if (beanHome == null) {
                    fail("lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
                }
                try {
                    SimpleBindingRemoteName bean = beanHome.create();
                    if (bean == null) {
                        fail("bean.create() for lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
                    }
                    System.out.println("Got bean, calling method");
                    if (bean.foo() == null) {
                        fail("bean.method() for lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
                    }

                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    fail("home.create() or method call for lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + SimpleBindingName + " and context " + contextString);
                }
            }
        } catch (ClassCastException cce) {
            if (passingCases.contains(beanNum)) {
                cce.printStackTrace();
                fail("ClassCastException While narrowing lookup " + lookupName + " for " + SimpleBindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                e.printStackTrace(System.out);
                fail("lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    @Test
    public void testSimpleBindingNameStartsWithCom() throws Exception {
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1">
        testLookupCombinations(false, "simple-binding-name=\"com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1\"", 1);
    }

    @Test
    public void testSimpleBindingNameStartsWithEJB() throws Exception {
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2">
        testLookupCombinations(false, "simple-binding-name=\"ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2\"", 2);
    }

    @Test
    public void testRemoteSimpleBindingNameStartsWithCom() throws Exception {
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome3">
        testLookupCombinations(true, "simple-binding-name=\"com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome3\"", 3);
    }

    @Test
    public void testRemoteSimpleBindingNameStartsWithEJB() throws Exception {
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4">
        testLookupCombinations(true, "simple-binding-name=\"ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4\"", 4);
    }

    @Test
    public void testHybridBindingNameStartsWithComLocalMode() throws Exception {
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5">
        testLookupCombinations(false, "simple-binding-name=\"com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5\"", 5);
    }

    @Test
    public void testHybridBindingNameStartsWithEJBLocalMode() throws Exception {
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6">
        testLookupCombinations(false, "simple-binding-name=\"ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6\"", 6);
    }

    @Test
    public void testHybridBindingNameStartsWithComRemoteMode() throws Exception {
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7">
        testLookupCombinations(true, "simple-binding-name=\"com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7\"", 7);
    }

    @Test
    public void testHybridBindingNameStartsWithEJBRemoteMode() throws Exception {
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome8">
        testLookupCombinations(true, "simple-binding-name=\"ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome8\"", 8);
    }

}
