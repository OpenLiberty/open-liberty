/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.configtests.ejb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.ejb.EJBException;
import javax.ejb.LocalHome;
import javax.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Provides a mechanism for a servlet located in a different module to verify java:global,
 * java:app, and java:module lookups from within the current enterprise bean module.
 */
@LocalHome(JavaColonLookupLocalHome.class)
@Stateless
public class JavaColonLookupBean {

    private InitialContext ctx = null;

    public void lookupJavaNamespaces(boolean bindToJavaGlobal, boolean bindToRemote) {
        try {
            ctx = new InitialContext();

            // remote java:global
            ConfigTestsRemoteEJB rbean = lookupRemoteJavaNamespace(bindToJavaGlobal && bindToRemote,
                                                                   "java:global/ConfigTestsTestApp/ConfigTestsEJB/ConfigTestsTestBean!com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsRemoteHome");
            if (bindToJavaGlobal && bindToRemote) {
                assertNotNull("9 ---> ConfigTestsRemoteEJB java:global lookup did not succeed.", rbean);
                String rstr = rbean.getString();
                assertEquals("10 ---> getString() returned unexpected value", "Success", rstr);
            } else {
                assertNull("11 --> ConfigTestsRemoteEJB java:global lookup should have failed.", rbean);
            }

            // remote java:app
            ConfigTestsRemoteEJB rbean2 = lookupRemoteJavaNamespace(bindToJavaGlobal && bindToRemote,
                                                                    "java:app/ConfigTestsEJB/ConfigTestsTestBean!com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsRemoteHome");
            if (bindToJavaGlobal && bindToRemote) {
                assertNotNull("12 ---> ConfigTestsRemoteEJB java:app lookup did not succeed.", rbean);
                String rstr = rbean2.getString();
                assertEquals("13 ---> getString() returned unexpected value", "Success", rstr);
            } else {
                assertNull("14 --> ConfigTestsRemoteEJB java:app lookup should have failed.", rbean);
            }

            // remote java:module
            ConfigTestsRemoteEJB rbean3 = lookupRemoteJavaNamespace(bindToJavaGlobal && bindToRemote,
                                                                    "java:module/ConfigTestsTestBean!com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsRemoteHome");
            if (bindToJavaGlobal && bindToRemote) {
                assertNotNull("15 ---> ConfigTestsRemoteEJB java:module lookup did not succeed.", rbean);
                String rstr = rbean3.getString();
                assertEquals("16 ---> getString() returned unexpected value", "Success", rstr);
            } else {
                assertNull("17 --> ConfigTestsRemoteEJB java:module lookup should have failed.", rbean);
            }

            // local java:global
            ConfigTestsLocalEJB bean = lookupLocalJavaNamespace(bindToJavaGlobal,
                                                                "java:global/ConfigTestsTestApp/ConfigTestsEJB/ConfigTestsTestBean!com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsLocalHome");
            if (bindToJavaGlobal) {
                assertNotNull("18 ---> ConfigTestsLocalEJB short default lookup did not succeed.", bean);
                String str = bean.getString();
                assertEquals("19 ---> getString() returned unexpected value", "Success", str);
            } else {
                assertNull("20 --> ConfigTestsLocalEJB lookup should have failed.", bean);
            }

            // local java:app
            ConfigTestsLocalEJB bean2 = lookupLocalJavaNamespace(bindToJavaGlobal,
                                                                 "java:app/ConfigTestsEJB/ConfigTestsTestBean!com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsLocalHome");
            if (bindToJavaGlobal) {
                assertNotNull("21 ---> ConfigTestsLocalEJB short default lookup did not succeed.", bean);
                String str = bean2.getString();
                assertEquals("22 ---> getString() returned unexpected value", "Success", str);
            } else {
                assertNull("23 --> ConfigTestsLocalEJB lookup should have failed.", bean);
            }

            // local java:module
            ConfigTestsLocalEJB bean3 = lookupLocalJavaNamespace(bindToJavaGlobal,
                                                                 "java:module/ConfigTestsTestBean!com.ibm.ws.ejbcontainer.remote.configtests.ejb.ConfigTestsLocalHome");
            if (bindToJavaGlobal) {
                assertNotNull("24 ---> ConfigTestsLocalEJB short default lookup did not succeed.", bean);
                String str = bean3.getString();
                assertEquals("25 ---> getString() returned unexpected value", "Success", str);
            } else {
                assertNull("26 --> ConfigTestsLocalEJB lookup should have failed.", bean);
            }
        } catch (EJBException ejbex) {
            throw ejbex;
        } catch (Exception ex) {
            throw new EJBException("unexpected exception occurred", ex);
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
                System.out.println("Caught expected exception : " + e);
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
                System.out.println("Caught expected exception : " + e);
            }
        }
        return null;
    }
}
