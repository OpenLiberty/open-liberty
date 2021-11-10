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
package com.ibm.ws.ejbcontainer.bindings.configtests.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.ejb.LocalHome;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 */
@LocalHome(JavaColonLookupLocalHome.class)
@Stateless
public class JavaColonLookupBean {

    private InitialContext ctx = null;

    public void lookupJavaNamespaces(boolean bindToJavaGlobal) throws Exception {
        ctx = new InitialContext();

        // remote java:global
        ConfigTestsRemoteEJB rbean = lookupRemoteJavaNamespace(bindToJavaGlobal,
                                                               "java:global/ConfigTestsTestApp/ConfigTestsEJB/ConfigTestsTestBean!com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteHome");
        if (bindToJavaGlobal) {
            assertNotNull("9 ---> ConfigTestsRemoteEJB java:global lookup did not succeed.", rbean);
            String rstr = rbean.getString();
            assertEquals("10 ---> getString() returned unexpected value", "Success", rstr);
        } else {
            assertNull("11 --> ConfigTestsRemoteEJB java:global lookup should have failed.", rbean);
        }

        // remote java:app
        ConfigTestsRemoteEJB rbean2 = lookupRemoteJavaNamespace(bindToJavaGlobal,
                                                                "java:app/ConfigTestsEJB/ConfigTestsTestBean!com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteHome");
        if (bindToJavaGlobal) {
            assertNotNull("12 ---> ConfigTestsRemoteEJB java:app lookup did not succeed.", rbean);
            String rstr = rbean2.getString();
            assertEquals("13 ---> getString() returned unexpected value", "Success", rstr);
        } else {
            assertNull("14 --> ConfigTestsRemoteEJB java:app lookup should have failed.", rbean);
        }

        // remote java:module
        ConfigTestsRemoteEJB rbean3 = lookupRemoteJavaNamespace(bindToJavaGlobal,
                                                                "java:module/ConfigTestsTestBean!com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsRemoteHome");
        if (bindToJavaGlobal) {
            assertNotNull("15 ---> ConfigTestsRemoteEJB java:module lookup did not succeed.", rbean);
            String rstr = rbean3.getString();
            assertEquals("16 ---> getString() returned unexpected value", "Success", rstr);
        } else {
            assertNull("17 --> ConfigTestsRemoteEJB java:module lookup should have failed.", rbean);
        }

        // local java:global
        ConfigTestsLocalEJB bean = lookupLocalJavaNamespace(bindToJavaGlobal,
                                                            "java:global/ConfigTestsTestApp/ConfigTestsEJB/ConfigTestsTestBean!com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalHome");
        if (bindToJavaGlobal) {
            assertNotNull("18 ---> ConfigTestsLocalEJB short default lookup did not succeed.", bean);
            String str = bean.getString();
            assertEquals("19 ---> getString() returned unexpected value", "Success", str);
        } else {
            assertNull("20 --> ConfigTestsLocalEJB lookup should have failed.", bean);
        }

        // local java:app
        ConfigTestsLocalEJB bean2 = lookupLocalJavaNamespace(bindToJavaGlobal,
                                                             "java:app/ConfigTestsEJB/ConfigTestsTestBean!com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalHome");
        if (bindToJavaGlobal) {
            assertNotNull("21 ---> ConfigTestsLocalEJB short default lookup did not succeed.", bean);
            String str = bean2.getString();
            assertEquals("22 ---> getString() returned unexpected value", "Success", str);
        } else {
            assertNull("23 --> ConfigTestsLocalEJB lookup should have failed.", bean);
        }

        // local java:module
        ConfigTestsLocalEJB bean3 = lookupLocalJavaNamespace(bindToJavaGlobal,
                                                             "java:module/ConfigTestsTestBean!com.ibm.ws.ejbcontainer.bindings.configtests.ejb.ConfigTestsLocalHome");
        if (bindToJavaGlobal) {
            assertNotNull("24 ---> ConfigTestsLocalEJB short default lookup did not succeed.", bean);
            String str = bean3.getString();
            assertEquals("25 ---> getString() returned unexpected value", "Success", str);
        } else {
            assertNull("26 --> ConfigTestsLocalEJB lookup should have failed.", bean);
        }
    }

    private ConfigTestsRemoteEJB lookupRemoteJavaNamespace(boolean shouldWork, String lookupString) throws Exception {
        try {
            ConfigTestsRemoteHome home = (ConfigTestsRemoteHome) ctx.lookup(lookupString);
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("ConfigTestsRemoteEJB Java Namespace lookup didn't work: " + lookupString);
            } else {
                //expected
            }
        }
        return null;
    }

    private ConfigTestsLocalEJB lookupLocalJavaNamespace(boolean shouldWork, String lookupString) throws Exception {
        try {
            ConfigTestsLocalHome home = (ConfigTestsLocalHome) ctx.lookup(lookupString);
            return home.create();
        } catch (NamingException e) {
            if (shouldWork) {
                e.printStackTrace(System.out);
                fail("ConfigTestsLocalEJB Java Namespace lookup didn't work: " + lookupString);
            } else {
                //expected
            }
        }
        return null;
    }
}
