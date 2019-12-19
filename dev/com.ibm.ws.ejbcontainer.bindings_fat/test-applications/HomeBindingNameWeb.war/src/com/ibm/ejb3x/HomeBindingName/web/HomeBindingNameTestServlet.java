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
package com.ibm.ejb3x.HomeBindingName.web;

import static org.junit.Assert.fail;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb3x.HomeBindingName.ejb.LocalHomeBindingNameHome;

import componenttest.app.FATServlet;

/**
 * Tests that the default ejblocal binding is disabled when a custom binding is defined.
 *
 * Tests a number of combinations of jndi lookups for beans that have a number of combinations of
 * custom bindings defined with the local-home-binding-name element in ibm-ejb-jar-bnd.xml. Based on these rules:
 *
 * 1. You can lookup just ejblocal: local: and local:ejb to get namespace contexts and then exclude them from the lookup.
 *
 * 2. local-home-binding-name validates that it starts with ejblocal:, we throw an error if it does not.
 *
 * 3. If local-home-binding-name="ejblocak:local:<name> it will only be bound to local: (even though these are 3X beans)
 * - also, the binding will not have ejb stuck in front, the lookup is local:<name> not local:ejb/<name>
 * - or with local: context it is <name> not ejb/<name>
 *
 * 4. If local-home-binding-name="ejblocal:local:ejb/<name> it will be bound in local: and local:ejb
 * - the local: lookup will not have double ejb stuck in front
 * - local:ejb/<name>
 * - with local: context: ejb/<name>
 * - with local:ejb context: <name>
 *
 * 5. If you do local-home-binding-name="<namespace>:<namespace>:<name> it will ignore up to the innermost namespace.
 * (even though we validate that it starts with ejblocal:)
 * - local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/ejbinwar/webejb3x/Stateless2xLocalHome" would use the local:ejb rule above
 *
 * 6. You can do a lookup and chain as many ejblocal: and local: in front as you want.
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
    public void testEJBLocalDefaultDisabled() {
        try {
            Object bean = new InitialContext().lookup("ejblocal:HomeBindingNameTestApp/HomeBindingNameEJB.jar/LocalHomeBindingName1#com.ibm.ejb3x.HomeBindingName.ejb.LocalHomeBindingNameHome");
            if (bean != null) {
                fail("EJBLocal default bindings lookup should not have worked because we have custom bindings");
            }
        } catch (NamingException e) {
            // expected to not work
        }
    }

    // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome1"/>
    // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome2"/>
    // local-home-binding-name="ejblocal:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome3"/>
    // local-home-binding-name="ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome4"/>
    // local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5"/>
    // local-home-binding-name="ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome6"/>
    // local-home-binding-name="ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome7"/>

    /*
     * Tests a bunch of different jndi lookup combinations against a bean
     * Expecting the lookup to pass or fail accordingly
     */
    private void testLookupCombinations(String localHomeBindingName, int beanNum) throws Exception {
        System.out.println("Testing " + localHomeBindingName);

        // default context ejblocal lookups -------------------------------------------------------------
        Context context = new InitialContext();
        String contextString = "Initial";

        // com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        String lookupName = "com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome1">
        // local-home-binding-name="ejblocal:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome3"/>
        // local-home-binding-name="ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome6"/>
        lookupName = "ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1,3,6");

        // ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome2"/>
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome4"/>
        lookupName = "ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "4");

        // ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5"/>
        // local-home-binding-name="ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome7"/>
        lookupName = "ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "5,7");

        // ejblocal:local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "ejblocal:local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("ejblocal:");
        contextString = "ejblocal:";

        // ejblocal: context + com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome1">
        // local-home-binding-name="ejblocal:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome3"/>
        // local-home-binding-name="ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome6"/>
        lookupName = "com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1,3,6");

        // ejblocal: context + ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome2"/>
        lookupName = "ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // ejblocal: context + ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome1">
        // local-home-binding-name="ejblocal:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome3"/>
        // local-home-binding-name="ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome6"/>
        lookupName = "ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1,3,6");

        // ejblocal: context + ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome2"/>
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // ejblocal: context + ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome4"/>
        lookupName = "local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "4");

        // ejblocal: context + local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5"/>
        // local-home-binding-name="ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome7"/>
        lookupName = "local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "5,7");

        // ejblocal: context + local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // default context local lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome4"/>
        lookupName = "local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "4");

        // local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5"/>
        // local-home-binding-name="ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome7"/>
        lookupName = "local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "5,7");

        // local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome4"/>
        lookupName = "local:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "4");

        // local:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5"/>
        lookupName = "local:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "4");

        // local:local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "local:local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome1">
        // local-home-binding-name="ejblocal:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome3"/>
        // local-home-binding-name="ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome6"/>
        lookupName = "local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1,3,6");

        // local:ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome2"/>
        lookupName = "local:ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // local context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:");
        contextString = "local:";

        // local: context + com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome4"/>
        lookupName = "com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "4");

        // local: context + ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5"/>
        // local-home-binding-name="ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome7"/>
        lookupName = "ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "5,7");

        // local: context + ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local: context + ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome1">
        // local-home-binding-name="ejblocal:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome3"/>
        // local-home-binding-name="ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome6"/>
        lookupName = "ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1,3,6");

        // local: context + ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome2"/>
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // local: context + ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local: context + local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome4"/>
        lookupName = "local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "4");

        // local: context + local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5"/>
        // local-home-binding-name="ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome7"/>
        lookupName = "local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "5,7");

        // local: context + local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:ejb");
        contextString = "local:ejb";

        // local:ejb context + com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5"/>
        // local-home-binding-name="ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome7"/>
        lookupName = "com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "5,7");

        // local:ejb context + ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome1">
        // local-home-binding-name="ejblocal:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome3"/>
        // local-home-binding-name="ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome6"/>
        lookupName = "ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1,3,6");

        // local:ejb context + ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome2"/>
        lookupName = "ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // local:ejb context + ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "ejblocal:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // local:ejb context + local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome4"/>
        lookupName = "local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "4");

        // local:ejb context + local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5"/>
        // local-home-binding-name="ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome7"/>
        lookupName = "local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "5,7");

        // local:ejb context + local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should always fail
        lookupName = "local:ejb/ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "none");

        // default context 3 nested lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome1">
        // local-home-binding-name="ejblocal:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome3"/>
        // local-home-binding-name="ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome6"/>
        lookupName = "ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "1,3,6");

        // ejblocal:local:ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome2"/>
        lookupName = "ejblocal:local:ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "2");

        // local:ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome4"/>
        lookupName = "local:ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "4");

        // local:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5"/>
        // local-home-binding-name="ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome7"/>
        lookupName = "local:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "5,7");

        // ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome# should work for
        // local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5"/>
        // local-home-binding-name="ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome7"/>
        lookupName = "ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome" + beanNum;
        testLookupCombinationsHelper(context, contextString, lookupName, localHomeBindingName, Integer.toString(beanNum), "5,7");
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
    private void testLookupCombinationsHelper(Context context, String contextString, String lookupName, String homeBindingName, String beanNum, String passingCases) {
        try {
            System.out.println("Testing " + lookupName + " with context " + contextString + " against " + homeBindingName);
            LocalHomeBindingNameHome beanHome = (LocalHomeBindingNameHome) context.lookup(lookupName);
            if (passingCases.contains(beanNum)) {
                if (beanHome == null) {
                    fail("lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
                }
                try {
                    if (beanHome.create() == null) {
                        fail("home.create() for lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
                    }
                } catch (Exception e) {
                    fail("home.create() for lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + homeBindingName + " and context " + contextString);
                }
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                fail("lookup " + lookupName + " should have worked for " + homeBindingName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    @Test
    public void testLocalHomeBindingNameStartsWithCom() throws Exception {
        // local-home-binding-name="ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome1"/>
        testLookupCombinations("local-home-binding-name=\"ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome1\"", 1);
    }

    @Test
    public void testLocalHomeBindingNameStartsWithEJB() throws Exception {
        // local-home-binding-name="ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome2"/>
        testLookupCombinations("local-home-binding-name=\"ejblocal:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome2\"", 2);
    }

    @Test
    public void testLocalHomeBindingNameNestedEJBLocal() throws Exception {
        // local-home-binding-name="ejblocal:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome3"/>
        testLookupCombinations("local-home-binding-name=\"ejblocal:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome3\"", 3);
    }

    @Test
    public void testLocalHomeBindingNameNestedLocal() throws Exception {
        // local-home-binding-name="ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome4"/>
        testLookupCombinations("local-home-binding-name=\"ejblocal:local:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome4\"", 4);
    }

    @Test
    public void testLocalHomeBindingNameNestedLocalEJB() throws Exception {
        // local-home-binding-name="ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5"/>
        testLookupCombinations("local-home-binding-name=\"ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome5\"", 5);
    }

    // local-home-binding-name="ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome6"/>
    @Test
    public void testLocalHomeBindingNameNestedLocalEJBLocal() throws Exception {
        // local-home-binding-name="ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome6"/>
        testLookupCombinations("local-home-binding-name=\"ejblocal:local:ejblocal:com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome6\"", 6);
    }

    @Test
    public void testLocalHomeBindingNameDoubleEJBLocalLocal() throws Exception {
        // local-home-binding-name="ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome7"/>
        testLookupCombinations("local-home-binding-name=\"ejblocal:ejblocal:local:ejb/com/ibm/ejb3x/HomeBindingName/ejb/LocalHomeBindingNameHome7\"", 7);
    }

}
