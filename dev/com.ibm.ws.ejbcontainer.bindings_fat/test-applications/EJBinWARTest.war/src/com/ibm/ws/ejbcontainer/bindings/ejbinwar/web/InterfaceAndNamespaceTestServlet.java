/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.ejbinwar.web;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.BasicLooseStatelessInterfaceRemote;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.BasicSingletonInJarInWarInterfaceRemote;

import componenttest.app.FATServlet;

/**
 * The tests in this class cover both the exposure of bean interfaces
 * (local and remote) and the ability to lookup beans using the
 * component namespace or the broader global namespace. Some tests do
 * both while others only test one or the other.
 */
@SuppressWarnings("serial")
@WebServlet("/InterfaceAndNamespaceTestServlet")
public class InterfaceAndNamespaceTestServlet extends FATServlet {
    private final static String CLASSNAME = InterfaceAndNamespaceTestServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private final static String APPLICATION = "EJBinWARTestApp";
    private final static String WAR_MODULE = "EJBinWARTest.war";

    /**
     * This test verifies the following: 1) A singleton bean inside an ejb.jar
     * inside the .war can successfully lookup another bean (the local interface)
     * in the .war's WEB-INF\classes dir via the component namespace (java:comp\)
     *
     */
    @Test
    public void testLocalJavaCompLookup() throws Exception {
        svLogger.info("--> Before lookup...");
        BasicSingletonInJarInWarInterfaceRemote singletonInJarInWar = (BasicSingletonInJarInWarInterfaceRemote) FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(BasicSingletonInJarInWarInterfaceRemote.class.getName(),
                                                                                                                                                                           APPLICATION,
                                                                                                                                                                           WAR_MODULE,
                                                                                                                                                                           "SingletonInJarInWarBean");

        svLogger.info("--> singletonInJarInWar.callVerifyLookup() = " + singletonInJarInWar.callVerifyLookup());

        assertTrue("Failed to lookup the local interface of a bean inside a .war from another bean inside the .war using the java:comp namespace",
                   singletonInJarInWar.callVerifyLookup());
    }

    /**
     * This test verifies the following: 1) A stateless bean inside the
     * WEB-INF/classes dir inside the .war can successfully lookup another bean
     * (the remote interface) in the .war's WEB-INF\classes dir via the component
     * namespace (java:comp\)
     *
     */
    @Test
    public void testRemoteJavaCompLookup() throws Exception {
        svLogger.info("--> Before lookup...");
        BasicLooseStatelessInterfaceRemote looseStateless = (BasicLooseStatelessInterfaceRemote) FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(BasicLooseStatelessInterfaceRemote.class.getName(),
                                                                                                                                                            APPLICATION, WAR_MODULE,
                                                                                                                                                            "LooseStatelessBean");

        svLogger.info("--> looseStateless.callVerifyRemoteJavaCompLookup() = " + looseStateless.callVerifyRemoteJavaCompLookup());

        assertTrue("Failed to lookup the remote interface of a bean inside a .war from another bean inside the .war using the java:comp namespace",
                   looseStateless.callVerifyRemoteJavaCompLookup());
    }

    /**
     * This test verifies the following: 1) A stateless bean inside the
     * WEB-INF/classes dir inside the .war can successfully lookup (the local
     * interface) of a standalone bean outside of the .war via the global local
     * namespace (i.e. ejblocal:)
     *
     */
    @Test
    public void testLocalGlobalLookup() throws Exception {
        svLogger.info("--> Before lookup...");
        BasicLooseStatelessInterfaceRemote looseStateless = (BasicLooseStatelessInterfaceRemote) FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(BasicLooseStatelessInterfaceRemote.class.getName(),
                                                                                                                                                            APPLICATION, WAR_MODULE,
                                                                                                                                                            "LooseStatelessBean");

        svLogger.info("--> looseStateless.callVerifyGlobalLocalLookup() = " + looseStateless.callVerifyGlobalLocalLookup());

        assertTrue("Failed to lookup the local interface of a standalone bean outside of the .war from a bean inside the .war using the global namespace (i.e. ejblocal:)",
                   looseStateless.callVerifyGlobalLocalLookup());
    }

    /**
     * This test verifies the following: 1) A stateless bean inside the
     * WEB-INF/classes dir inside the .war can successfully lookup (the remote
     * interface) of a standalone bean outside of the .war via the global remote
     * namespace (i.e. ejb/)
     *
     */
    @Test
    public void testRemoteGlobalLookup() throws Exception {
        svLogger.info("--> Before lookup...");
        BasicLooseStatelessInterfaceRemote looseStateless = (BasicLooseStatelessInterfaceRemote) FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(BasicLooseStatelessInterfaceRemote.class.getName(),
                                                                                                                                                            APPLICATION, WAR_MODULE,
                                                                                                                                                            "LooseStatelessBean");

        svLogger.info("--> looseStateless.callVerifyGlobalRemoteLookup() = " + looseStateless.callVerifyGlobalRemoteLookup());

        assertTrue("Failed to lookup the remote interface of a standalone bean outside of the .war from a bean inside the .war using the remote global namespace (i.e. ejb/)",
                   looseStateless.callVerifyGlobalRemoteLookup());
    }
}
