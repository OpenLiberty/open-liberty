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
package com.ibm.ejb3x.HomeBindingName.web;

import static org.junit.Assert.fail;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb3x.HomeBindingName.ejb.LocalHomeBindingName;
import com.ibm.ejb3x.HomeBindingName.ejb.LocalHomeBindingNameHome;
import com.ibm.ejb3x.HomeBindingName.ejb.RemoteHomeBindingName;
import com.ibm.ejb3x.HomeBindingName.ejb.RemoteHomeBindingNameHome;

import componenttest.app.FATServlet;

/**
 * Tests that the default ejblocal binding is disabled when a custom binding is defined.
 *
 * Tests a number of combinations of jndi lookups for beans that have a number of combinations of
 * custom bindings defined with the local-home-binding-name and remote-home-binding element in ibm-ejb-jar-bnd.xml.
 *
 */
@SuppressWarnings("serial")
@WebServlet("/HomeBindingNameTestServlet")
public class HomeBindingNameTestServlet extends FATServlet {

    /*
     * Tests that the ejblocal: default binding should not have been bound because we
     * have custom bindings.
     */
    @Test
    public void testEJBLocalDefaultDisabledLocalHomeBinding() {
        try {
            Object bean = new InitialContext().lookup("ejblocal:HomeBindingNameTestApp/HomeBindingNameEJB.jar/HomeBindingName1#com.ibm.ejb3x.HomeBindingName.ejb.LocalHomeBindingNameHome");
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
    public void testDefaultDisabledRemoteHomeBinding() {
        try {
            Object bean = new InitialContext().lookup("ejb/HomeBindingNameTestApp/HomeBindingNameEJB.jar/HomeBindingName3#com.ibm.ejb3x.HomeBindingName.ejb.RemoteHomeBindingNameHome");
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
    private void testLookupCombinations(boolean remote, String localHomeBindingName, int beanNum) throws Exception {
        System.out.println("Testing " + localHomeBindingName);

        // default context ejblocal lookups -------------------------------------------------------------
        Context context = new InitialContext();
        String contextString = "Initial";

        // com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // remote-home-binding-name="com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome3">
        String lookupName = "com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "3");

        // ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // remote-home-binding-name="ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome4">
        lookupName = "ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "4");

        // ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome1">
        lookupName = "ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1");

        // ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome2"/>
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal:local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejblocal:local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("ejblocal:");
        contextString = "ejblocal:";

        // ejblocal: context + com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome1">
        lookupName = "com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1");

        // ejblocal: context + ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome2"/>
        lookupName = "ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // ejblocal: context + ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome1">
        lookupName = "ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1");

        // ejblocal: context + ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome2"/>
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // ejblocal: context + ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // default context local lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome1">
        lookupName = "local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1");

        // local:ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome2"/>
        lookupName = "local:ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // local context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:");
        contextString = "local:";

        // local: context + com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local: context + ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local: context + ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local: context + ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome1">
        lookupName = "ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1");

        // local: context + ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome2"/>
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // local: context + ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local: context + local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local: context + local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local: context + local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:ejb");
        contextString = "local:ejb";

        // local:ejb context + com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome1">
        lookupName = "ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1");

        // local:ejb context + ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome2"/>
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // local:ejb context + ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // default context 3 nested lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome1">
        lookupName = "ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1");

        // ejblocal:local:ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome2"/>
        lookupName = "ejblocal:local:ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // local:ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "local:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome# should always fail
        lookupName = "ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");
    }

    /**
     * Helper that tries the actual lookup and asserts conditions based on if
     * it should work or not. this is done by checking what bean is being looked
     * up against a list of beans expected to pass.
     *
     * @param context - the namespace context to look up in. Like InitialContext or ejblocal:
     * @param lookupName - the lookup name to perform
     * @param SimpleBindingName - the local-home-binding-name="" name provided for the bean in xmi
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

    private void testLookupCombinationsHelperLocal(Context context, String contextString, String lookupName, String homeBindingName, String beanNum, String passingCases) {
        try {
            System.out.println("Testing " + lookupName + " with context " + contextString + " against " + homeBindingName);
            LocalHomeBindingNameHome beanHome = (LocalHomeBindingNameHome) context.lookup(lookupName);
            if (passingCases.contains(beanNum)) {
                if (beanHome == null) {
                    fail("lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
                }
                try {
                    LocalHomeBindingName bean = beanHome.create();
                    if (beanHome.create() == null) {
                        fail("home.create() for lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
                    }
                    System.out.println("Got bean, calling method");
                    if (bean.foo() == null) {
                        fail("bean.method() for lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    fail("home.create() for lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + homeBindingName + " and context " + contextString);
                }
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                e.printStackTrace(System.out);
                fail("lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        } catch (ClassCastException cce) {
            // For the hybrid beans they might have a remote bound in the lookup string, so we'll get a class cast
            // since we try all the lookup combinations, just ignore it.
            if (passingCases.contains(beanNum)) {
                cce.printStackTrace();
                fail("ClassCastException While performing lookup " + lookupName + " for " + homeBindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    private void testLookupCombinationsHelperRemote(Context context, String contextString, String lookupName, String homeBindingName, String beanNum, String passingCases) {
        try {
            System.out.println("Testing " + lookupName + " with context " + contextString + " against " + homeBindingName);
            RemoteHomeBindingNameHome beanHome = (RemoteHomeBindingNameHome) context.lookup(lookupName);
            if (passingCases.contains(beanNum)) {
                if (beanHome == null) {
                    fail("lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
                }
                try {
                    RemoteHomeBindingName bean = beanHome.create();
                    if (beanHome.create() == null) {
                        fail("home.create() for lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
                    }
                    System.out.println("Got bean, calling method");
                    if (bean.foo() == null) {
                        fail("bean.method() for lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    fail("home.create() for lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + homeBindingName + " and context " + contextString);
                }
            }
        } catch (ClassCastException cce) {
            if (passingCases.contains(beanNum)) {
                cce.printStackTrace();
                fail("ClassCastException While narrowing lookup " + lookupName + " for " + homeBindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                e.printStackTrace(System.out);
                fail("lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    @Test
    public void testLocalHomeBindingNameStartsWithCom() throws Exception {
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome1"/>
        testLookupCombinations(false, "local-home-binding-name=\"ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome1\"", 1);
    }

    @Test
    public void testLocalHomeBindingNameStartsWithEJB() throws Exception {
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome2"/>
        testLookupCombinations(false, "local-home-binding-name=\"ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome2\"", 2);
    }

    @Test
    public void testRemoteHomeBindingNameStartsWithCom() throws Exception {
        // remote-home-binding-name="com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome3"/>
        testLookupCombinations(true, "remote-home-binding-name=\"com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome3\"", 3);
    }

    @Test
    public void testRemoteHomeBindingNameStartsWithEJB() throws Exception {
        // remote-home-binding-name="ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome4"/>
        testLookupCombinations(true, "remote-home-binding-name=\"ejb/com/ibm/ejb3x/HomeBindingName/ejb/HomeBindingNameHome4\"", 4);
    }
}
