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
package com.ibm.ejb3x.SimpleBindingName.web;

import static org.junit.Assert.fail;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb3x.SimpleBindingName.ejb.SimpleBindingNameHome;

import componenttest.app.FATServlet;

/**
 * Tests that the default ejblocal binding is disabled when a custom binding is defined.
 *
 * Tests a number of combinations of jndi lookups for beans that have a number of combinations of
 * custom bindings defined with the SimpleBindingName element in ibm-ejb-jar-bnd.xml. Based on these rules:
 *
 * 1. You can lookup just ejblocal: local: and local:ejb to get namespace contexts and then exclude them from the lookup.
 *
 * 2. If SimpleBindingName does not have a namespace written in, it will be bound to ejblocal: only, local: and local:ejb
 * are for jndiName element normally
 *
 * 3. If simple-binding-name="ejblocal:<name> nothing changes (with regards to local beans)
 *
 * 4. If simple-binding-name="local:<name> it will only be bound to local: (even though these are 3X beans)
 * - also, the binding will not have ejb stuck in front, the lookup is local:<name> not local:ejb/<name>
 * - or with local: context it is <name> not ejb/<name>
 *
 * 5. If simple-binding-name="local:ejb/<name> it will be bound in local: and local:ejb
 * - the local: lookup will not have double ejb stuck in front
 * - local:ejb/<name>
 * - with local: context: ejb/<name>
 * - with local:ejb context: <name>
 *
 * 6. If you do simple-binding-name="<namespace>:<namespace>:<name> it will ignore up to the innermost namespace.
 * - simple-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/ejbinwar/webejb3x/Stateless2xLocalHome" would use the local:ejb rule above
 *
 * 7. You can do a lookup and chain as many ejblocal: and local: in front as you want.
 *
 */
@SuppressWarnings("serial")
@WebServlet("/SimpleBindingNameTestServlet")
public class SimpleBindingNameTestServlet extends FATServlet {

    /*
     * Tests that the ejblocal: default binding should not have been bound because we
     * have custom bindings.
     */
    @Test
    public void testEJBLocalDefaultDisabled() {
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
     * Tests a bunch of different jndi lookup combinations against a bean
     * Expecting the lookup to pass or fail accordingly
     */
    private void testLookupCombinations(String SimpleBindingName, int beanNum) throws Exception {
        System.out.println("Testing " + SimpleBindingName);

        // default context ejblocal lookups -------------------------------------------------------------
        Context context = new InitialContext();
        String contextString = "Initial";

        // com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        String lookupName = "com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1">
        // simple-binding-name="ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome3">
        lookupName = "ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,3");

        // ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2">
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2");

        // ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4">
        // simple-binding-name="ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6">
        lookupName = "ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "4,6");

        // ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5">
        // simple-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7">
        lookupName = "ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "5,7");

        // ejblocal:local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejblocal:local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("ejblocal:");
        contextString = "ejblocal:";

        // ejblocal: context + com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1">
        // simple-binding-name="ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome3">
        lookupName = "com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,3");

        // ejblocal: context + ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2">
        lookupName = "ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2");

        // ejblocal: context + ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1">
        // simple-binding-name="ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome3">
        lookupName = "ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,3");

        // ejblocal: context + ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2">
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2");

        // ejblocal: context + ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4">
        // simple-binding-name="ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6">
        lookupName = "local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "4,6");

        // ejblocal: context + local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5">
        // simple-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7">
        lookupName = "local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "5,7");

        // ejblocal: context + local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // default context local lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4">
        // simple-binding-name="ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6">
        lookupName = "local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "4,6");

        // local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5">
        // simple-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7">
        lookupName = "local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "5,7");

        // local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4">
        // simple-binding-name="ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6">
        lookupName = "local:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "4,6");

        // local:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5">
        // simple-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7">
        lookupName = "local:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "5,7");

        // local:local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1">
        // simple-binding-name="ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome3">
        lookupName = "local:ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,3");

        // local:ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2">
        lookupName = "local:ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2");

        // local context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:");
        contextString = "local:";

        // local: context + com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4">
        // simple-binding-name="ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6">
        lookupName = "com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "4,6");

        // local: context + ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5">
        // simple-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7">
        lookupName = "ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "5,7");

        // local: context + ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local: context + ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1">
        // simple-binding-name="ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome3">
        lookupName = "ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,3");

        // local: context + ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2">
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2");

        // local: context + ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local: context + local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4">
        // simple-binding-name="ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6">
        lookupName = "local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "4,6");

        // local: context + local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5">
        // simple-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7">
        lookupName = "local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "5,7");

        // local: context + local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:ejb");
        contextString = "local:ejb";

        // local:ejb context + com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5">
        // simple-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7">
        lookupName = "com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "5,7");

        // local:ejb context + ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1">
        // simple-binding-name="ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome3">
        lookupName = "ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,3");

        // local:ejb context + ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2">
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2");

        // local:ejb context + ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4">
        // simple-binding-name="ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6">
        lookupName = "local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "4,6");

        // local:ejb context + local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5">
        // simple-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7">
        lookupName = "local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "5,7");

        // local:ejb context + local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "none");

        // default context 3 nested lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // ejblocal:local:ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1">
        // simple-binding-name="ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome3">
        lookupName = "ejblocal:local:ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "1,3");

        // ejblocal:local:ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2">
        lookupName = "ejblocal:local:ejblocal:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "2");

        // local:ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4">
        // simple-binding-name="ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6">
        lookupName = "local:ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "4,6");

        // local:ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome# should work for
        // simple-binding-name="local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5">
        // simple-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7">
        lookupName = "local:ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, SimpleBindingName, Integer.toString(beanNum), "5,7");
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
    private void testLookupCombinationsHelper(Context context, String contextString, String lookupName, String SimpleBindingName, String beanNum, String passingCases) {
        try {
            System.out.print("Testing " + lookupName + " with context " + contextString + " against " + SimpleBindingName);
            SimpleBindingNameHome beanHome = (SimpleBindingNameHome) context.lookup(lookupName);
            if (passingCases.contains(beanNum)) {
                if (beanHome == null) {
                    fail("lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
                }
                try {
                    if (beanHome.create() == null) {
                        fail("home.create() for lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
                    }
                } catch (Exception e) {
                    fail("home.create() for lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + SimpleBindingName + " and context " + contextString);
                }
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                fail("lookup " + lookupName + " should have worked for " + SimpleBindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    @Test
    public void testSimpleBindingNameStartsWithCom() throws Exception {
        // simple-binding-name="com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1">
        testLookupCombinations("simple-binding-name=\"com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome1\"", 1);
    }

    @Test
    public void testSimpleBindingNameStartsWithEJB() throws Exception {
        // simple-binding-name="ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2">
        testLookupCombinations("simple-binding-name=\"ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome2\"", 2);
    }

    @Test
    public void testSimpleBindingNameStartsWithEJBLocal() throws Exception {
        // simple-binding-name="ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome3">
        testLookupCombinations("simple-binding-name=\"ejblocal:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome3\"", 3);
    }

    @Test
    public void testSimpleBindingNameStartsWithLocal() throws Exception {
        // simple-binding-name="local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4">
        testLookupCombinations("simple-binding-name=\"local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome4\"", 4);
    }

    @Test
    public void testSimpleBindingNameStartsWithLocalEJB() throws Exception {
        // simple-binding-name="local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5">
        testLookupCombinations("simple-binding-name=\"local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome5\"", 5);
    }

    @Test
    public void testSimpleBindingNameStartsWithEJBLocalLocal() throws Exception {
        // simple-binding-name="ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6">
        testLookupCombinations("simple-binding-name=\"ejblocal:local:com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome6\"", 6);
    }

    @Test
    public void testSimpleBindingNameStartsWithEJBLocalLocalEJB() throws Exception {
        // simple-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7">
        testLookupCombinations("simple-binding-name=\"ejblocal:local:ejb/com/ibm/ejb3x/SimpleBindingName/ejb/SimpleBindingNameHome7\"", 7);
    }

}
