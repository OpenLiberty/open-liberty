/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejb3x.ComponentIDBnd.web;

import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBnd;
import com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome;
import com.ibm.ejb3x.ComponentIDBnd.ejb.RemoteComponentIDBnd;
import com.ibm.ejb3x.ComponentIDBnd.ejb.RemoteComponentIDBndHome;
import com.ibm.websphere.ejbcontainer.AmbiguousEJBReferenceException;

import componenttest.app.FATServlet;

/**
 * Tests that the default ejblocal binding is disabled when a custom binding is defined.
 *
 * Tests a number of combinations of jndi lookups for beans that have a number of combinations of
 * custom bindings defined with the component-id element in ibm-ejb-jar-bnd.xml.
 *
 * Note: Unlike most other custom binding xml elements, we prepend "ejb/" for remote component-id bindings because
 * component-id is a prepend for default short form bindings
 *
 */
@SuppressWarnings("serial")
@WebServlet("/ComponentIDBndTestServlet")
public class ComponentIDBndTestServlet extends FATServlet {
    private static final String CLASS_NAME = ComponentIDBndTestServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    /*
     * Tests that the ejblocal: default binding should not have been bound because we
     * have custom bindings.
     */
    @Test
    public void testEJBLocalDefaultDisabledForComponentID() {
        try {
            Object bean = new InitialContext().lookup("ejblocal:ComponentIDBndTestApp/ComponentIDBndEJB.jar/ComponentIDBnd1#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome");
            if (bean != null) {
                fail("EJBLocal default bindings lookup should not have worked because we have custom bindings");
            }
        } catch (NamingException e) {
            // expected to not work
        }
    }

    /*
     * Tests that the short default bindings should not be disabled because we are using
     * Component ID.
     *
     * Because of how the EJB.jar is set up, we expect AmbiguousEJBReferenceException
     * when looking up the short default binding name.
     */
    @Test
    public void testEJBLocalShortDefaultNotDisabledForComponentID() {
        try {
            Object bean = new InitialContext().lookup("ejblocal:com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome");
            fail("EJBLocal default short bindings lookup should have resulted in AmbiguousEJBReferenceException");
        } catch (NamingException nex) {
            Throwable cause = nex.getCause();
            if (cause instanceof AmbiguousEJBReferenceException) {
                svLogger.info("lookup of short default failed as expected : " +
                              cause.getClass().getName() + " : " +
                              cause.getMessage());
            } else {
                svLogger.info(nex.getClass().getName() + " : " + nex.getMessage());
                nex.printStackTrace();
                fail("short default lookup failed in an " +
                     "unexpected way : " + nex.getClass().getName() + " : " +
                     nex.getMessage());
            }
        }
    }

    /*
     * Tests that the remote default binding should not have been bound because we
     * have custom bindings.
     */
    @Test
    public void testRemoteDefaultDisabledForComponentID() {
        try {
            Object bean = new InitialContext().lookup("ejb/ComponentIDBndTestApp/ComponentIDBndEJB.jar/ComponentIDBnd3#com.ibm.ejb3x.ComponentIDBnd.ejb.RemoteComponentIDBndHome");
            if (bean != null) {
                fail("Remote default bindings lookup should not have worked because we have custom bindings");
            }
        } catch (NamingException e) {
            // expected to not work
        }
    }

    /*
     * Tests that the short default bindings should not be disabled because we are using
     * Component ID.
     *
     * Because of how the EJB.jar is set up, we expect AmbiguousEJBReferenceException
     * when looking up the short default binding name.
     */
    @Test
    public void testRemoteShortDefaultNotDisabledForComponentID() {
        try {
            Object bean = new InitialContext().lookup("com.ibm.ejb3x.ComponentIDBnd.ejb.RemoteComponentIDBndHome");
            fail("EJB remote default short bindings lookup should have resulted in AmbiguousEJBReferenceException");
        } catch (NamingException nex) {
            Throwable cause = nex.getCause();
            if (cause instanceof AmbiguousEJBReferenceException) {
                svLogger.info("lookup of short default failed as expected : " +
                              cause.getClass().getName() + " : " +
                              cause.getMessage());
            } else {
                svLogger.info(nex.getClass().getName() + " : " + nex.getMessage());
                nex.printStackTrace();
                fail("short default lookup failed in an " +
                     "unexpected way : " + nex.getClass().getName() + " : " +
                     nex.getMessage());
            }
        }
    }

    /*
     * Tests a bunch of different jndi lookup combinations against a bean
     * Expecting the lookup to pass or fail accordingly
     */
    private void testLookupCombinations(boolean remote, String componentIDBindingName, int beanNum) throws Exception {
        System.out.println("Testing " + componentIDBindingName);

        // default context ejblocal lookups -------------------------------------------------------------
        Context context = new InitialContext();
        String contextString = "Initial";

        // NOTE: % = bean number since the # symbol is in the actual lookup

        // MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        String lookupName = "MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // ejbMyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "ejbMyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="MyEJB4"/> (remote)
        // component-id="MyEJB8"/> (hybrid)
        lookupName = "ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "4,8");

        // ejb/ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="ejb/MyEJB3"/> (remote)
        // component-id="ejb/MyEJB7"/> (hybrid)
        lookupName = "ejb/ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "3,7");

        // ejblocal:MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="MyEJB2"/> (local)
        // component-id="MyEJB6"/> (hybrid)
        lookupName = "ejblocal:MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "2,6");

        // ejblocal:ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="ejb/MyEJB1"/> (local)
        // component-id="ejb/MyEJB5"/> (hybrid)
        lookupName = "ejblocal:ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "1,5");

        // local:MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local:ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local:ejbMyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:ejbMyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // ejblocal context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("ejblocal:");
        contextString = "ejblocal:";

        // ejblocal: context + MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="MyEJB2"/> (local)
        // component-id="MyEJB6"/> (hybrid)
        lookupName = "MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "2,6");

        // ejblocal: context + ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="ejb/MyEJB1"/> (local)
        // component-id="ejb/MyEJB5"/> (hybrid)
        lookupName = "ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "1,5");

        // ejblocal: context + ejblocal:MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="MyEJB2"/> (local)
        // component-id="MyEJB6"/> (hybrid)
        lookupName = "ejblocal:MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "2,6");

        // ejblocal: context + ejblocal:ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="ejb/MyEJB1"/> (local)
        // component-id="ejb/MyEJB5"/> (hybrid)
        lookupName = "ejblocal:ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "1,5");

        // ejblocal: context + local:MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:ejbMyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:ejbMyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // ejblocal: context + local:ejb/MyEJB%/#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:ejb/MyEJB" + beanNum + "/";
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:");
        contextString = "local:";

        // local: context + MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local: context + ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local: context + ejblocal:MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="MyEJB2"/> (local)
        // component-id="MyEJB6"/> (hybrid)
        lookupName = "ejblocal:MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "2,6");

        // local: context + ejblocal:ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="ejb/MyEJB1"/> (local)
        // component-id="ejb/MyEJB5"/> (hybrid)
        lookupName = "ejblocal:ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "1,5");

        // local: context + local:MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local: context + local:ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local: context + local:ejbMyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:ejbMyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local:ejb context lookups -------------------------------------------------------------
        context = (Context) new InitialContext().lookup("local:ejb");
        contextString = "local:ejb";

        // local:ejb/ context + MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local:ejb/ context + ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local:ejb/ context + ejblocal:MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="MyEJB2"/> (local)
        // component-id="MyEJB6"/> (hybrid)
        lookupName = "ejblocal:MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "2,6");

        // local:ejb/ context + ejblocal:ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="ejb/MyEJB1"/> (local)
        // component-id="ejb/MyEJB5"/> (hybrid)
        lookupName = "ejblocal:ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "1,5");

        // local:ejb/ context + local:MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local:ejb/ context + local:ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local:ejb/ context + local:ejbMyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "local:ejbMyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // default context 3 nested lookups -------------------------------------------------------------
        context = new InitialContext();
        contextString = "Initial";

        // ejblocal:local:ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should never work
        lookupName = "ejblocal:local:ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "none");

        // local:ejblocal:MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="MyEJB2"/> (local)
        // component-id="MyEJB6"/> (hybrid)
        lookupName = "local:ejblocal:MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "2,6");

        // local:ejblocal:ejb/MyEJB%#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome should work for
        // component-id="ejb/MyEJB1"/> (local)
        // component-id="ejb/MyEJB5"/> (hybrid)
        lookupName = "local:ejblocal:ejb/MyEJB" + beanNum;
        testLookupCombinationsHelper(remote, context, contextString, lookupName, componentIDBindingName, Integer.toString(beanNum), "1,5");
    }

    /**
     * Helper that tries the actual lookup and asserts conditions based on if
     * it should work or not. this is done by checking what bean is being looked
     * up against a list of beans expected to pass.
     *
     * @param context - the namespace context to look up in. Like InitialContext or ejblocal:
     * @param lookupName - the lookup name to perform
     * @param componentIDName - the component-id="" binding provided for the bean in xml
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

    private void testLookupCombinationsHelperLocal(Context context, String contextString, String lookupName, String componentIDName, String beanNum, String passingCases) {
        try {
            lookupName = lookupName + "#com.ibm.ejb3x.ComponentIDBnd.ejb.ComponentIDBndHome";
            System.out.println("Testing " + lookupName + " with context " + contextString + " against " + componentIDName);
            ComponentIDBndHome beanHome = (ComponentIDBndHome) context.lookup(lookupName);
            if (passingCases.contains(beanNum)) {
                if (beanHome == null) {
                    fail("lookup " + lookupName + " should have worked for " + componentIDName + " and context " + contextString);
                }
                try {
                    ComponentIDBnd bean = beanHome.create();
                    if (beanHome.create() == null) {
                        fail("home.create() for lookup " + lookupName + " should have worked for " + componentIDName + " and context " + contextString);
                    }
                    System.out.println("Got bean, calling method");
                    if (bean.foo() == null) {
                        fail("bean.method() for lookup " + lookupName + " should have worked for " + componentIDName + " and context " + contextString);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    fail("home.create() for lookup " + lookupName + " should have worked for " + componentIDName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + componentIDName + " and context " + contextString);
                }
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                e.printStackTrace(System.out);
                fail("lookup " + lookupName + " should have worked for " + componentIDName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        } catch (ClassCastException cce) {
            // For the hybrid beans they might have a remote bound in the lookup string, so we'll get a class cast
            // since we try all the lookup combinations, just ignore it.
            if (passingCases.contains(beanNum)) {
                cce.printStackTrace();
                fail("ClassCastException While performing lookup " + lookupName + " for " + componentIDName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    private void testLookupCombinationsHelperRemote(Context context, String contextString, String lookupName, String componentIDName, String beanNum, String passingCases) {
        try {
            lookupName = lookupName + "#com.ibm.ejb3x.ComponentIDBnd.ejb.RemoteComponentIDBndHome";
            System.out.println("Testing " + lookupName + " with context " + contextString + " against " + componentIDName);
            RemoteComponentIDBndHome beanHome = (RemoteComponentIDBndHome) context.lookup(lookupName);
            if (passingCases.contains(beanNum)) {
                if (beanHome == null) {
                    fail("lookup " + lookupName + " should have worked for " + componentIDName + " and context " + contextString);
                }
                try {
                    RemoteComponentIDBnd bean = beanHome.create();
                    if (beanHome.create() == null) {
                        fail("home.create() for lookup " + lookupName + " should have worked for " + componentIDName + " and context " + contextString);
                    }
                    System.out.println("Got bean, calling method");
                    if (bean.foo() == null) {
                        fail("bean.method() for lookup " + lookupName + " should have worked for " + componentIDName + " and context " + contextString);
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    fail("home.create() for lookup " + lookupName + " should have worked for " + componentIDName + " and context " + contextString);
                }
            } else {
                if (beanHome != null) {
                    fail("lookup " + lookupName + " should have failed for " + componentIDName + " and context " + contextString);
                }
            }
        } catch (ClassCastException cce) {
            if (passingCases.contains(beanNum)) {
                cce.printStackTrace();
                fail("ClassCastException While narrowing lookup " + lookupName + " for " + componentIDName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        } catch (NamingException e) {
            if (passingCases.contains(beanNum)) {
                e.printStackTrace(System.out);
                fail("lookup " + lookupName + " should have worked for " + componentIDName + " and context " + contextString);
            } else {
                // expected to fail in other cases
            }
        }
    }

    @Test
    public void testLocalComponentIDBindingNameStartsWithEJB() throws Exception {
        //component-id="ejb/MyEJB1"/>
        testLookupCombinations(false, "component-id=\"ejb/MyEJB1\"", 1);
    }

    @Test
    public void testComponentIDBindingNameStartsWithMyEJBSlash() throws Exception {
        //component-id="MyEJB2/"/>
        testLookupCombinations(false, "component-id=\"MyEJB2/\"", 2);
    }

    @Test
    public void testRemoteComponentIDBindingNameStartsWithEJB() throws Exception {
        //component-id="ejb/MyEJB3"/>
        testLookupCombinations(true, "component-id=\"ejb/MyEJB3\"", 3);
    }

    @Test
    public void testRemoteComponentIDBindingNameStartsWithMyEJB() throws Exception {
        //component-id="MyEJB4"/>
        testLookupCombinations(true, "component-id=\"MyEJB4\"", 4);
    }

    @Test
    public void testHybridLocalComponentIDBindingNameStartsWithEJB() throws Exception {
        //component-id="ejb/MyEJB5"/>
        testLookupCombinations(false, "component-id=\"ejb/MyEJB5\"", 5);
    }

    @Test
    public void testHybridLocalComponentIDBindingNameStartsWithMyEJB() throws Exception {
        //component-id="MyEJB6"/>
        testLookupCombinations(false, "component-id=\"MyEJB6\"", 6);
    }

    @Test
    public void testHybridRemoteComponentIDBindingNameStartsWithEJB() throws Exception {
        //component-id="ejb/MyEJB7"/>
        testLookupCombinations(true, "component-id=\"ejb/MyEJB7\"", 7);
    }

    @Test
    public void testHybridRemoteComponentIDBindingNameStartsWithMyEJB() throws Exception {
        //component-id="MyEJB8"/>
        testLookupCombinations(true, "component-id=\"MyEJB8\"", 8);
    }

    /*
     * ComponentID should be disabled because we have a different specific binding
     */

    @Test
    public void testLocalComponentIDBindingNameWithSpecificBinding() throws Exception {
        //component-id="MyEJB9"/>
        testLookupCombinations(false, "component-id=\"MyEJB9\"", 9);
    }

    @Test
    public void testRemoteComponentIDBindingNameWithSpecificBinding() throws Exception {
        //component-id="MyEJB9"/>
        testLookupCombinations(true, "component-id=\"MyEJB9\"", 9);
    }

}
