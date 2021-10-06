/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ambiguous.web;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import com.ibm.ambiguous.ejb.AmbiguousName;
import com.ibm.ambiguous.ejb.AmbiguousNameHome;
import com.ibm.ambiguous.ejb.AmbiguousNameRemoteHome;
import com.ibm.ambiguous.ejb.AmbiguousRemoteName;
import com.ibm.websphere.ejbcontainer.AmbiguousEJBReferenceException;

import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/AmbiguousTestServlet")
public class AmbiguousTestServlet extends FATServlet {
    private static final Logger svLogger = Logger.getLogger(AmbiguousTestServlet.class.getName());

    public void testAmbiguousOnErrorWarn() throws Exception {
        // lookup should have ambiguous exception

        // local:
        lookupLocal("local:ejb/ejb/AmbiguousBindingHome", true);

        // ejblocal:
        lookupLocal("ejblocal:ejb/AmbiguousBindingHome", true);

        // remote
        lookupRemote("ejb/AmbiguousBindingHome", true);

    }

    public void testAmbiguousOnErrorIgnore() throws Exception {
        // lookup should have first bean

        // local:
        lookupLocal("local:ejb/ejb/AmbiguousBindingHome", false);

        // ejblocal:
        lookupLocal("ejblocal:ejb/AmbiguousBindingHome", false);

        // remote
        lookupRemote("ejb/AmbiguousBindingHome", false);
    }

    private void lookupLocal(String lookup, boolean isAmbiguous) throws Exception {
        try {
            AmbiguousNameHome beanHome = (AmbiguousNameHome) new InitialContext().lookup(lookup);
            if (isAmbiguous) {
                fail("lookup of " + lookup + " should have been ambiguous");
            }
            AmbiguousName bean = beanHome.create();
            assertTrue("Lookup provided the wrong bean", bean.foo().equals("AmbiguousNameBean.toString()"));
        } catch (NamingException nex) {
            if (isAmbiguous) {
                Throwable cause = nex.getCause();
                if (cause instanceof AmbiguousEJBReferenceException) {
                    svLogger.info("lookup of " + lookup + " failed as expected : " +
                                  cause.getClass().getName() + " : " +
                                  cause.getMessage());
                } else {
                    svLogger.info(nex.getClass().getName() + " : " + nex.getMessage());
                    nex.printStackTrace();
                    fail("lookup of " + lookup + " failed in an " +
                         "unexpected way : " + nex.getClass().getName() + " : " +
                         nex.getMessage());
                }
            } else {
                svLogger.info(nex.getClass().getName() + " : " + nex.getMessage());
                nex.printStackTrace();
                fail("lookup of " + lookup + " failed in an " +
                     "unexpected way : " + nex.getClass().getName() + " : " +
                     nex.getMessage());
            }

        }
    }

    // isAmbiguous = should we get AmbiguousEJBReferenceException or just the first bean
    private void lookupRemote(String lookup, boolean isAmbiguous) throws Exception {
        try {
            AmbiguousNameRemoteHome beanHome = (AmbiguousNameRemoteHome) new InitialContext().lookup(lookup);
            if (isAmbiguous) {
                fail("lookup of " + lookup + " should have been ambiguous");
            }
            AmbiguousRemoteName bean = beanHome.create();
            assertTrue("Lookup provided the wrong bean", bean.foo().equals("AmbiguousNameBean.toString()"));
        } catch (NamingException nex) {
            if (isAmbiguous) {
                Throwable cause = nex.getCause();
                if (cause instanceof AmbiguousEJBReferenceException) {
                    svLogger.info("lookup of " + lookup + " failed as expected : " +
                                  cause.getClass().getName() + " : " +
                                  cause.getMessage());
                } else {
                    svLogger.info(nex.getClass().getName() + " : " + nex.getMessage());
                    nex.printStackTrace();
                    fail("lookup of " + lookup + " failed in an " +
                         "unexpected way : " + nex.getClass().getName() + " : " +
                         nex.getMessage());
                }
            } else {
                svLogger.info(nex.getClass().getName() + " : " + nex.getMessage());
                nex.printStackTrace();
                fail("lookup of " + lookup + " failed in an " +
                     "unexpected way : " + nex.getClass().getName() + " : " +
                     nex.getMessage());
            }

        }
    }

}
