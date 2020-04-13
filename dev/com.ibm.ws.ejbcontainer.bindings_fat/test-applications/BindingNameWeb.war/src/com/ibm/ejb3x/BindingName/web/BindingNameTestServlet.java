/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v3.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejb3x.BindingName.web;

import static org.junit.Assert.fail;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb3x.BindingName.ejb.BindingNameIntf;
import com.ibm.ejb3x.BindingName.ejb.RemoteBindingNameIntf;

import componenttest.app.FATServlet;

/**
 * Tests that the default ejblocal binding is disabled when a custom binding is defined.
 *
 * Tests a number of combinations of jndi lookups for beans that have a number of combinations of
 * custom bindings defined with the binding-name element in ibm-ejb-jar-bnd.xml.
 *
 */
@SuppressWarnings("serial")
@WebServlet("/BindingNameTestServlet")
public class BindingNameTestServlet extends FATServlet {

    /*
     * Tests that the ejblocal: default binding should not have been bound because we
     * have custom bindings.
     */
    @Test
    public void testBindingNameDefaultDisabled() {
        try {
            Object bean = new InitialContext().lookup("ejblocal:BindingNameTestApp/BindingNameEJB.jar/BindingName1#com.ibm.ejb3x.BindingName.ejb.BindingNameIntf");
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
    public void testBindingNameRemoteDefaultDisabled() {
        try {
            Object bean = new InitialContext().lookup("ejb/BindingNameTestApp/BindingNameEJB.jar/BindingName5#com.ibm.ejb3x.BindingName.ejb.RemoteBindingNameIntf");
            if (bean != null) {
                fail("Remote default bindings lookup should not have worked because we have custom bindings");
            }
        } catch (NamingException e) {
            // expected to not work
        }
    }

    /*
     * Tests a bunch of different jndi lookup combinations against a bean
     * Expecting the lookup to pass or fail accordingly
     */
    private void testLookupCombinations(boolean remote, String BindingName, int beanNum) throws Exception {
        System.out.println("Testing " + BindingName);

        // default context lookups -------------------------------------------------------------
        Context context = new InitialContext();
        String contextString = "Initial";

        // ejb/BindingNameIntf# should work for
        // binding-name="ejb/BindingNameIntf3" (remote)
        // binding-name="ejb/BindingNameIntf7" (hybrid)
        String lookupName = "ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,7");

        // BindingNameIntf# should work for
        // binding-name="BindingNameIntf4" (remote)
        // binding-name="BindingNameIntf8" (hybrid)
        lookupName = "BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "4,8");

        // ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // ejblocal:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejblocal:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // local:BindingNameIntf# should never work
        lookupName = "local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local:ejb/BindingNameIntf# should never work
        lookupName = "local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // ejblocal:local:BindingNameIntf# should never work
        lookupName = "ejblocal:local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // ejblocal:local:ejb/BindingNameIntf# should never work
        lookupName = "ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // ejblocal:ejblocal:local:ejb/BindingNameIntf# should never work
        lookupName = "ejblocal:ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // ejblocal:local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "ejblocal:local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // ejblocal:local:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejblocal:local:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // local:ejb/ejblocal:BindingNameIntf# should never work
        lookupName = "local:ejb/ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // ejblocal context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("ejblocal:");
        contextString = "ejblocal:";

        // ejblocal: + ejb/BindingNameIntf should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // ejblocal: + BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // ejblocal: + ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // ejblocal: + ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // ejblocal: + ejblocal:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejblocal:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // ejblocal: + local:BindingNameIntf# should never work
        lookupName = "local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // ejblocal: + local:ejb/BindingNameIntf# should never work
        lookupName = "local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // ejblocal: + ejblocal:local:BindingNameIntf# should never work
        lookupName = "ejblocal:local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // ejblocal: + ejblocal:local:ejb/BindingNameIntf# should never work
        lookupName = "ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // ejblocal: + ejblocal:ejblocal:local:ejb/BindingNameIntf# should never work
        lookupName = "ejblocal:ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // ejblocal: + local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // ejblocal: + ejblocal:local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "ejblocal:local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // ejblocal: + ejblocal:local:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejblocal:local:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // ejblocal: + local:ejb/ejblocal:BindingNameIntf# should never work
        lookupName = "local:ejb/ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:");
        contextString = "local:";

        // local: + ejb/BindingNameIntf# should never work
        lookupName = "ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local: + BindingNameIntf# should never work
        lookupName = "BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local: + ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // local: + ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // local: + ejblocal:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejblocal:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // local: + local:BindingNameIntf# should never work
        lookupName = "local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local: + local:ejb/BindingNameIntf# should never work
        lookupName = "local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local: + ejblocal:local:BindingNameIntf# should never work
        lookupName = "ejblocal:local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local: + ejblocal:local:ejb/BindingNameIntf# should never work
        lookupName = "ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local: + ejblocal:ejblocal:local:ejb/BindingNameIntf# should never work
        lookupName = "ejblocal:ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local: + local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // local: + ejblocal:local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "ejblocal:local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // local: + ejblocal:local:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejblocal:local:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // local: + local:ejb/ejblocal:BindingNameIntf# should never work
        lookupName = "local:ejb/ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local:ejb context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:ejb");
        contextString = "local:ejb";

        // local:ejb/ + ejb/BindingNameIntf# should never work
        lookupName = "ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local:ejb/ + BindingNameIntf# should never work
        lookupName = "BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local:ejb/ + ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // local:ejb/ + ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // local:ejb/ + ejblocal:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejblocal:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // local:ejb/ + local:BindingNameIntf# should never work
        lookupName = "local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local:ejb/ + local:ejb/BindingNameIntf# should never work
        lookupName = "local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local:ejb/ + ejblocal:local:BindingNameIntf# should never work
        lookupName = "ejblocal:local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local:ejb/ + ejblocal:local:ejb/BindingNameIntf# should never work
        lookupName = "ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local:ejb/ + ejblocal:ejblocal:local:ejb/BindingNameIntf# should never work
        lookupName = "ejblocal:ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local:ejb/ + local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // local:ejb/ + ejblocal:local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf2" (local)
        // binding-name="ejblocal:BindingNameIntf6" (hybrid)
        lookupName = "ejblocal:local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "2,6");

        // local:ejb/ + ejblocal:local:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejb/BindingNameIntf1" (local)
        // binding-name="ejblocal:ejb/BindingNameIntf5" (hybrid)
        lookupName = "ejblocal:local:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,5");

        // local:ejb/ + local:ejb/ejblocal:BindingNameIntf# should never work
        lookupName = "local:ejb/ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");
    }

    /**
     * Helper that tries the actual lookup and asserts conditions based on if
     * it should work or not. this is done by checking what bean is being looked
     * up against a list of beans expected to pass.
     *
     * @param context - the namespace context to look up in. Like InitialContext or ejblocal:
     * @param lookupName - the lookup name to perform
     * @param BindingName - the binding-name="" name provided for the bean in xmi
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

    private void testLookupCombinationsHelperLocal(Context context, String contextString, String lookupName, String BindingName, String beanNum, String passingCases) {
        try {
            System.out.println("Testing " + lookupName + " with context " + contextString + " against " + BindingName);
            BindingNameIntf beanHome = (BindingNameIntf) context.lookup(lookupName);
            if (passingCases.contains(beanNum)) {
                if (context.lookup(lookupName) == null) {
                    fail("lookup " + lookupName + " should have worked for " + BindingName + " and context " + contextString);
                }
                try {
                    if (beanHome.foo() == null) {
                        fail("bean.foo() for lookup " + lookupName + " should have worked for " + BindingName + " and context " + contextString);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    fail("bean.foo() for lookup " + lookupName + " should have worked for " + BindingName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + BindingName + " and context " + contextString);
                }
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                e.printStackTrace(System.out);
                fail("lookup " + lookupName + " should have worked for " + BindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        } catch (ClassCastException cce) {
            // For the hybrid beans they might have a remote bound in the lookup string, so we'll get a class cast
            // since we try all the lookup combinations, just ignore it.
            if (passingCases.contains(beanNum)) {
                cce.printStackTrace();
                fail("ClassCastException While performing lookup " + lookupName + " for " + BindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    private void testLookupCombinationsHelperRemote(Context context, String contextString, String lookupName, String BindingName, String beanNum, String passingCases) {
        try {
            System.out.println("Testing " + lookupName + " with context " + contextString + " against " + BindingName);
            Object lookup = context.lookup(lookupName);
            RemoteBindingNameIntf beanHome = (RemoteBindingNameIntf) PortableRemoteObject.narrow(lookup, RemoteBindingNameIntf.class);
            if (passingCases.contains(beanNum)) {
                if (context.lookup(lookupName) == null) {
                    fail("lookup " + lookupName + " should have worked for " + BindingName + " and context " + contextString);
                }
                try {
                    if (beanHome.foo() == null) {
                        fail("bean.foo() for lookup " + lookupName + " should have worked for " + BindingName + " and context " + contextString);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    fail("bean.foo() for lookup " + lookupName + " should have worked for " + BindingName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + BindingName + " and context " + contextString);
                }
            }
        } catch (ClassCastException cce) {
            if (passingCases.contains(beanNum)) {
                cce.printStackTrace();
                fail("ClassCastException While narrowing lookup " + lookupName + " for " + BindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                e.printStackTrace(System.out);
                fail("lookup " + lookupName + " should have worked for " + BindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    @Test
    public void testLocalBindingNameStartsWithEJB() throws Exception {
        // binding-name="ejblocal:ejb/BindingNameIntf1"
        testLookupCombinations(false, "binding-name=\"ejblocal:ejb/BindingNameIntf1\"", 1);
    }

    @Test
    public void testLocalBindingNamePlain() throws Exception {
        // binding-name="ejblocal:BindingNameIntf2"
        testLookupCombinations(false, "binding-name=\"ejblocal:BindingNameIntf2\"", 2);
    }

    @Test
    public void testRemoteBindingNameStartsWithEJB() throws Exception {
        // binding-name="ejb/BindingNameIntf3"
        testLookupCombinations(true, "binding-name=\"ejb/BindingNameIntf3\"", 3);
    }

    @Test
    public void testRemoteBindingNamePlain() throws Exception {
        // binding-name="BindingNameIntf4"
        testLookupCombinations(true, "binding-name=\"BindingNameIntf4\"", 4);
    }

    @Test
    public void testHybridLocalBindingNameStartsWithEJB() throws Exception {
        // binding-name="ejblocal:ejb/BindingNameIntf5"
        testLookupCombinations(false, "binding-name=\"ejblocal:ejb/BindingNameIntf5\"", 5);
    }

    @Test
    public void testHybridLocalBindingNamePlain() throws Exception {
        // binding-name="ejblocal:BindingNameIntf6"
        testLookupCombinations(false, "binding-name=\"ejblocal:BindingNameIntf6\"", 6);
    }

    @Test
    public void testHybridRemoteBindingNameStartsWithEJB() throws Exception {
        // binding-name="ejb/BindingNameIntf7"
        testLookupCombinations(true, "binding-name=\"ejb/BindingNameIntf7\"", 7);
    }

    @Test
    public void testHybridRemoteBindingNamePlain() throws Exception {
        // binding-name="BindingNameIntf8"
        testLookupCombinations(true, "binding-name=\"BindingNameIntf8\"", 8);
    }

}
