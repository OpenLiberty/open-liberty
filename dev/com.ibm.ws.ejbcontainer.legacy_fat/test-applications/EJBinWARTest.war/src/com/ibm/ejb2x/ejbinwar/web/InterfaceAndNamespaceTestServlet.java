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
package com.ibm.ejb2x.ejbinwar.web;

import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.ejbinwar.intf.BasicLooseStatelessInterfaceRemote;
import com.ibm.ejb2x.ejbinwar.intf.Comp2xViewStatefulRemote;
import com.ibm.ejb2x.ejbinwar.intf.Comp2xViewStatefulRemoteHome;
import com.ibm.ejb2x.ejbinwar.intf.XMLComp2xViewStatefulRemote;
import com.ibm.ejb2x.ejbinwar.intf.XMLComp2xViewStatefulRemoteHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

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
     * This test verifies the following: 1) the 2.x CompView Remote interface, 2)
     * the client can access an ejb using a 2.x remote interface
     */
    @Test
    public void testRemote2xCompViewFromClient() throws Exception {
        svLogger.info("--> Before home lookup...");
        Comp2xViewStatefulRemoteHome remote2xCompViewHome = (Comp2xViewStatefulRemoteHome) FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(Comp2xViewStatefulRemoteHome.class.getName(),
                                                                                                                                                      APPLICATION, WAR_MODULE,
                                                                                                                                                      "Comp2xViewStatefulBean");

        svLogger.info("--> Creating bean...");
        Comp2xViewStatefulRemote remote2xCompViewBean = remote2xCompViewHome.create();

        assertTrue("Remote 2.x component interface was not usable", remote2xCompViewBean.verifyComp2xStatefulLookup());
    }

    /**
     * This test verifies the following: 1) a bean inside the .war can access
     * another ejb inside the .war using a 2.x local interface, 2) Injection of
     * the CompView Bean's home 3) Lookup of the CompView Bean's home from the
     * EJBContext
     */
    @Test
    public void testLocal2xCompViewFromInsideWar() throws Exception {
        svLogger.info("--> Before lookup...");
        BasicLooseStatelessInterfaceRemote looseStateless = (BasicLooseStatelessInterfaceRemote) FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(BasicLooseStatelessInterfaceRemote.class.getName(),
                                                                                                                                                            APPLICATION, WAR_MODULE,
                                                                                                                                                            "LooseStatelessBean");

        assertTrue("Local 2.x component interface was not usable from an EJB in the war", looseStateless.callVerifyComp2xStatefulLocalLookup());
    }

    /**
     * This test verifies the following: 1) a bean inside the .war can access
     * another ejb inside the .war using a 2.x remote interface, 2) Injection of
     * the CompView Bean's home 3) Lookup of the CompView Bean's home from the
     * EJBContext
     */
    @Test
    public void testRemote2xCompViewFromInsideWar() throws Exception {
        svLogger.info("--> Before lookup...");
        BasicLooseStatelessInterfaceRemote looseStateless = (BasicLooseStatelessInterfaceRemote) FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(BasicLooseStatelessInterfaceRemote.class.getName(),
                                                                                                                                                            APPLICATION, WAR_MODULE,
                                                                                                                                                            "LooseStatelessBean");

        svLogger.info("--> looseStateless.callVerifyComp2xStatefulRemoteLookup() = " + looseStateless.callVerifyComp2xStatefulRemoteLookup());

        assertTrue("Remote 2.x component interface was not usable from an EJB in the war", looseStateless.callVerifyComp2xStatefulRemoteLookup());
    }

    /**
     * XML version:
     *
     * This test verifies the following: 1) the 2.x CompView Remote interface, 2)
     * the client can access an ejb using a 2.x remote interface
     */
    @Test
    public void testXMLRemote2xCompViewFromClient() throws Exception {
        svLogger.info("--> Before home lookup...");
        XMLComp2xViewStatefulRemoteHome xmlRemote2xCompViewHome = (XMLComp2xViewStatefulRemoteHome) FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(XMLComp2xViewStatefulRemoteHome.class.getName(),
                                                                                                                                                               APPLICATION,
                                                                                                                                                               WAR_MODULE,
                                                                                                                                                               "XMLComp2xViewStatefulBean");

        svLogger.info("--> Creating bean...");
        XMLComp2xViewStatefulRemote xmlRemote2xCompViewBean = xmlRemote2xCompViewHome.create();

        assertTrue("Remote 2.x component interface was not usable", xmlRemote2xCompViewBean.verifyXMLComp2xStatefulLookup());
    }

    /**
     * XML version:
     *
     * This test verifies the following: 1) a bean inside the .war can access
     * another ejb inside the .war using a 2.x local interface, 2) Injection of
     * the CompView Bean's home 3) Lookup of the CompView Bean's home from the
     * EJBContext
     */
    @Test
    public void testXMLLocal2xCompViewFromInsideWar() throws Exception {
        svLogger.info("--> Before lookup...");
        BasicLooseStatelessInterfaceRemote looseStateless = (BasicLooseStatelessInterfaceRemote) FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(BasicLooseStatelessInterfaceRemote.class.getName(),
                                                                                                                                                            APPLICATION, WAR_MODULE,
                                                                                                                                                            "LooseStatelessBean");

        svLogger.info("--> looseStateless.callVerifyXMLComp2xStatefulLocalLookup() = " + looseStateless.callVerifyXMLComp2xStatefulLocalLookup());

        assertTrue("Local 2.x component interface was not usable from an EJB in the war", looseStateless.callVerifyXMLComp2xStatefulLocalLookup());
    }

    /**
     * XML version:
     *
     * This test verifies the following: 1) a bean inside the .war can access
     * another ejb inside the .war using a 2.x remote interface, 2) Injection of
     * the CompView Bean's home 3) Lookup of the CompView Bean's home from the
     * EJBContext
     */
    @Test
    public void testXMLRemote2xCompViewFromInsideWar() throws Exception {
        svLogger.info("--> Before lookup...");
        BasicLooseStatelessInterfaceRemote looseStateless = (BasicLooseStatelessInterfaceRemote) FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(BasicLooseStatelessInterfaceRemote.class.getName(),
                                                                                                                                                            APPLICATION, WAR_MODULE,
                                                                                                                                                            "LooseStatelessBean");

        svLogger.info("--> looseStateless.callVerifyXMLComp2xStatefulRemoteLookup() = " + looseStateless.callVerifyXMLComp2xStatefulRemoteLookup());

        assertTrue("Remote 2.x component interface was not usable from an EJB in the war", looseStateless.callVerifyXMLComp2xStatefulRemoteLookup());
    }
}
