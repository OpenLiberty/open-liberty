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

import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.XMLDefinedLooseSingletonInterfaceRemote;
import com.ibm.ws.ejbcontainer.bindings.ejbinwar.intf.XMLDefinedStatelessInJarInWarInterfaceRemote;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/BndTestServlet")
public class BndTestServlet extends FATServlet {
    private final static String CLASSNAME = BndTestServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    private final static String APPLICATION = "EJBinWARTestApp";
    private final static String WAR_MODULE = "EJBinWARTest.war";

    /**
     * This test verifies the following: 1) The ibm-ejb-jar-bnd.xml file located
     * in the WEB-INF/ directory of the war is successfully picked up and used by
     * verifying one can lookup the xml defined SLSB using the remote interface
     * defined in the bindings file 2) that the SLSB was able to be defined via
     * XML
     */
    @Test
    public void testRemoteBinding() throws Exception {
        svLogger.info("--> Before SLSB lookup...");

        XMLDefinedStatelessInJarInWarInterfaceRemote xmlSLSBRemote = (XMLDefinedStatelessInJarInWarInterfaceRemote) new InitialContext().lookup("ejb/core/RemoteXMLSLSB");

        svLogger.info("--> Calling the method to verify the bean lookup was successful...");
        assertEquals("Failed to lookup stateless EJB via remote custom binding", "Success", xmlSLSBRemote.verifyLookup("Success"));
    }

    /**
     * This test verifies the following: 1) The ibm-ejb-jar-bnd.xml file located
     * in the WEB-INF/ directory of the .war is successfully picked up and used
     * by verifying one can lookup the XML defined SLSB using the local interface
     * defined in the bindings file - this local lookup is done by a Singleton
     * bean also in the .war 2) that both the SLSB and Singleton were
     * successfully defined via XML
     */
    @Test
    public void testLocalBinding() throws Exception {
        svLogger.info("--> Before Singleton lookup...");

        XMLDefinedLooseSingletonInterfaceRemote xmlSingBean = (XMLDefinedLooseSingletonInterfaceRemote) FATHelper.lookupDefaultEJBLegacyBindingsEJBRemoteInterface(XMLDefinedLooseSingletonInterfaceRemote.class.getName(),
                                                                                                                                                                   APPLICATION,
                                                                                                                                                                   WAR_MODULE,
                                                                                                                                                                   "XMLDefinedSingletonBean");

        svLogger.info("--> Calling the Singleton's method that verifies that the SLSB was able to be successfully looked up using the local interface...");

        assertEquals("Failed to lookup stateless EJB via local custom binding", "Success", xmlSingBean.verifyBNDofSLSBLocal());
    }
}
