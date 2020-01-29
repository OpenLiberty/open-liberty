/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb3x.BindingName.ejb.BindingNameIntf;

import componenttest.app.FATServlet;

/**
 * Tests that the default ejblocal binding is disabled when a custom binding is defined.
 *
 * Tests a number of combinations of jndi lookups for beans that have a number of combinations of
 * custom bindings defined with the BindingName element in ibm-ejb-jar-bnd.xml. Based on these rules:
 *
 * 1. You can lookup just ejblocal: local: and local:ejb to get namespace contexts and then exclude them from the lookup.
 *
 * 2. If BindingName does not have a namespace written in, it will be bound to ejblocal: only, local: and local:ejb
 * are for BindingName element normally
 *
 * 3. If binding-name="ejblocal:<name> nothing changes (with regards to local beans)
 *
 * 4. If binding-name="local:<name> it will only be bound to local: (even though these are 3X beans)
 * - also, the binding will not have ejb stuck in front, the lookup is local:<name> not local:ejb/<name>
 * - or with local: context it is <name> not ejb/<name>
 *
 * 5. If binding-name="local:ejb/<name> it will be bound in local: and local:ejb
 * - the local: lookup will not have double ejb stuck in front
 * - local:ejb/<name>
 * - with local: context: ejb/<name>
 * - with local:ejb context: <name>
 *
 * 6. If you do binding-name="<namespace>:<namespace>:<name> it will ignore up to the innermost namespace.
 * - binding-name="ejblocal:local:ejb/com/ibm/ejb3x/ejbinwar/webejb3x/Stateless2xLocalHome" would use the local:ejb rule above
 *
 * 7. You can do a lookup and chain as many ejblocal: and local: in front as you want.
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
            Object bean = new InitialContext().lookup("ejblocal:BindingNameTestApp/BindingNameEJB.jar/BindingName1#com.ibm.ejb3x.BindingName.ejb.BindingNameHome");
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
    private void testLookupCombinations(String BindingName, int beanNum) throws Exception {
        System.out.println("Testing " + BindingName);

        // default context lookups -------------------------------------------------------------
        Context context = new InitialContext();
        String contextString = "Initial";

        // ejb/BindingNameIntf# should always fail
        String lookupName = "ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // BindingNameIntf# should always fail
        lookupName = "BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // ejblocal:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejblocal:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // local:BindingNameIntf# should work for
        // binding-name="ejblocal:local:BindingNameIntf4"
        lookupName = "local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "4");

        // local:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // ejblocal:local:BindingNameIntf# should work for
        // binding-name="ejblocal:local:BindingNameIntf4"
        lookupName = "ejblocal:local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "4");

        // ejblocal:local:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // ejblocal:ejblocal:local:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "ejblocal:ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // ejblocal:local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "ejblocal:local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // ejblocal:local:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejblocal:local:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // local:ejb/ejblocal:BindingNameIntf# should never work
        lookupName = "local:ejb/ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // ejblocal context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("ejblocal:");
        contextString = "ejblocal:";

        // ejblocal: + ejb/BindingNameIntf should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // ejblocal: + BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // ejblocal: + ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // ejblocal: + ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // ejblocal: + ejblocal:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejblocal:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // ejblocal: + local:BindingNameIntf# should work for
        // binding-name="ejblocal:local:BindingNameIntf4"
        lookupName = "local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "4");

        // ejblocal: + local:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // ejblocal: + ejblocal:local:BindingNameIntf# should work for
        // binding-name="ejblocal:local:BindingNameIntf4"
        lookupName = "ejblocal:local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "4");

        // ejblocal: + ejblocal:local:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // ejblocal: + ejblocal:ejblocal:local:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "ejblocal:ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // ejblocal: + local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // ejblocal: + ejblocal:local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "ejblocal:local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // ejblocal: + ejblocal:local:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejblocal:local:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // ejblocal: + local:ejb/ejblocal:BindingNameIntf# should never work
        lookupName = "local:ejb/ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:");
        contextString = "local:";

        // local: + ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // local: + BindingNameIntf# should work for
        // binding-name="ejblocal:local:BindingNameIntf4"
        lookupName = "BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "4");

        // local: + ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // local: + ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // local: + ejblocal:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejblocal:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // local: + local:BindingNameIntf# should work for
        // binding-name="ejblocal:local:BindingNameIntf4"
        lookupName = "local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "4");

        // local: + local:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // local: + ejblocal:local:BindingNameIntf# should work for
        // binding-name="ejblocal:local:BindingNameIntf4"
        lookupName = "ejblocal:local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "4");

        // local: + ejblocal:local:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // local: + ejblocal:ejblocal:local:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "ejblocal:ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // local: + local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // local: + ejblocal:local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "ejblocal:local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // local: + ejblocal:local:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejblocal:local:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // local: + local:ejb/ejblocal:BindingNameIntf# should never work
        lookupName = "local:ejb/ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local:ejb context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:ejb");
        contextString = "local:ejb";

        // local:ejb/ + ejb/BindingNameIntf# should never work
        lookupName = "ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");

        // local:ejb/ + BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // local:ejb/ + ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // local:ejb/ + ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // local:ejb/ + ejblocal:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejblocal:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // local:ejb/ + local:BindingNameIntf# should work for
        // binding-name="ejblocal:local:BindingNameIntf4"
        lookupName = "local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "4");

        // local:ejb/ + local:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // local:ejb/ + ejblocal:local:BindingNameIntf# should work for
        // binding-name="ejblocal:local:BindingNameIntf4"
        lookupName = "ejblocal:local:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "4");

        // local:ejb/ + ejblocal:local:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // local:ejb/ + ejblocal:ejblocal:local:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        lookupName = "ejblocal:ejblocal:local:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "3,5");

        // local:ejb/ + local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // local:ejb/ + ejblocal:local:ejblocal:BindingNameIntf# should work for
        // binding-name="ejblocal:BindingNameIntf7"
        lookupName = "ejblocal:local:ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "7");

        // local:ejb/ + ejblocal:local:ejblocal:ejb/BindingNameIntf# should work for
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        lookupName = "ejblocal:local:ejblocal:ejb/BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "1,2,6");

        // local:ejb/ + local:ejb/ejblocal:BindingNameIntf# should never work
        lookupName = "local:ejb/ejblocal:BindingNameIntf" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, BindingName, Integer.toString(beanNum), "none");
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
    private void testLookupCombinationsHelper(Context context, String contextString, String lookupName, String BindingName, String beanNum, String passingCases) {
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
                    fail("bean.foo() for lookup " + lookupName + " should have worked for " + BindingName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + BindingName + " and context " + contextString);
                }
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                fail("lookup " + lookupName + " should have worked for " + BindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    @Test
    public void testBindingNameEJBLocal() throws Exception {
        // binding-name="ejblocal:ejblocal:ejb/BindingNameIntf1"
        testLookupCombinations("binding-name=\"ejblocal:ejblocal:ejb/BindingNameIntf1\"", 1);
    }

    @Test
    public void testBindingNameEJB() throws Exception {
        // binding-name="ejblocal:ejb/BindingNameIntf2"
        testLookupCombinations("binding-name=\"ejblocal:ejb/BindingNameIntf2\"", 2);
    }

    @Test
    public void testBindingNameLocalEJB() throws Exception {
        // binding-name="ejblocal:local:ejb/BindingNameIntf3"
        testLookupCombinations("binding-name=\"ejblocal:local:ejb/BindingNameIntf3\"", 3);
    }

    @Test
    public void testBindingNameLocal() throws Exception {
        // binding-name="ejblocal:local:BindingNameIntf4"
        testLookupCombinations("binding-name=\"ejblocal:local:BindingNameIntf4\"", 4);
    }

    @Test
    public void testBindingNameEJBLocalLocalEJB() throws Exception {
        // binding-name="ejblocal:ejblocal:local:ejb/BindingNameIntf5"
        testLookupCombinations("binding-name=\"ejblocal:ejblocal:local:ejb/BindingNameIntf5\"", 5);
    }

    @Test
    public void testBindingNameEJBLocalLocalEJBLocal() throws Exception {
        // binding-name="ejblocal:local:ejblocal:ejb/BindingNameIntf6"
        testLookupCombinations("binding-name=\"ejblocal:local:ejblocal:ejb/BindingNameIntf6\"", 6);
    }

    @Test
    public void testBindingNamePlain() throws Exception {
        // binding-name="ejblocal:BindingNameIntf7"
        testLookupCombinations("binding-name=\"ejblocal:BindingNameIntf7\"", 7);
    }

}
