/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web;

import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_NO_TRANSACTION;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.AdvCMTStatelessEJB;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.AdvCMTStatelessEJBHome;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.AdvCMTStatelessRemote;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.BasicCMTStatelessRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> AdvCompCMTStatelessRemoteTest .
 *
 * <dt><b>Test Author:</b> Tracy Burroughs (remote-ified by Urrvano Gamez, Jr.)
 * <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Compatibility EJB 3.0 Container Managed Stateless Session bean functionality with advanced EJB 3.0 configurations: including multiple
 * Business Interfaces.
 * (remote version)
 * <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>
 * Sub-tests
 * <ul>
 * <li>testCompIntTxAttribs - (remote) Component Interface: Verify methods with all ContainerManaged Tx Attributes
 * <li>testBizIntTxAttribs - (remote) Business Interface: Verify methods with all ContainerManaged Tx Attributes
 * <li>testCompIntBizGetters - (remote) Component Interface: Verify getInvokedBusinessInterface and getBusinessObject throw correct exception
 * <li>testBizIntBizGetters - (remote) Business Interface: Verify getInvokedBusinessInterface and getBusinessObject work correctly
 * <li>testCompIntInjection - (remote) Component Interface: Verify EJB field and method injection occurrs properly
 * </ul>
 * <br>
 * Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/AdvCompCMTStatelessRemoteServlet")
public class AdvCompCMTStatelessRemoteServlet extends FATServlet {
    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = AdvCompCMTStatelessRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Name of module... for lookup.
    private static final String Module = "StatelessAnnEJB";

    // Names of the beans used for the test... for lookup.
    private static final String AdvCompBean = "AdvCompCMTStatelessRemote";

    // Names of the interfaces used for the test
    private static final String BasicCMTStatelessRemoteInterface = BasicCMTStatelessRemote.class.getName();
    private static final String AdvCMTStatelessRemoteInterface = AdvCMTStatelessRemote.class.getName();
    private static final String AdvCMTStatelessEJBHomeInterface = AdvCMTStatelessEJBHome.class.getName();

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB, (remote) Component Interface, with each of the different Transaction Attributes.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Stateless Session bean may be created.
     * <li>SLSB method with default tx attribute may be called.
     * <li>SLSB method with Required tx attribute may be called.
     * <li>SLSB method with NotSupported tx attribute may be called.
     * <li>SLSB method with RequiresNew tx attribute may be called.
     * <li>SLSB method with Supports tx attribute may be called.
     * <li>SLSB method with Never tx attribute may be called.
     * <li>SLSB method with Mandatory tx attribute may be called.
     * <li>Stateless Session bean may be removed.
     * </ol>
     */
    @Test
    public void testCompIntTxAttribs_AdvCompCMTStatelessRemote() throws Exception {
        UserTransaction userTran = null;

        try {
            // --------------------------------------------------------------------
            // Locate SL Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            AdvCMTStatelessEJBHome slHome = (AdvCMTStatelessEJBHome) FATHelper.lookupDefaultBindingEJBJavaApp(AdvCMTStatelessEJBHomeInterface, Module,
                                                                                                              AdvCompBean);

            AdvCMTStatelessEJB bean = slHome.create();
            assertNotNull("1 ---> SLRSB created successfully.", bean);

            bean.tx_Default();
            bean.tx_Required();
            bean.tx_NotSupported();
            bean.tx_RequiresNew();
            bean.tx_Supports();
            bean.tx_Never();

            userTran = FATHelper.lookupUserTransaction();

            svLogger.info("Beginning User Transaction ...");
            userTran.begin();

            bean.tx_Mandatory();

            svLogger.info("Committing User Transaction ...");
            userTran.commit();
        } finally {
            if (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)
                userTran.rollback();
        }
    }

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB, using multiple (remote) Business Interfaces, with each of the different Transaction Attributes.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Stateless Session bean may be created.
     * <li>SLSB method with default tx attribute may be called.
     * <li>SLSB method with Required tx attribute may be called.
     * <li>SLSB method with NotSupported tx attribute may be called.
     * <li>SLSB method with RequiresNew tx attribute may be called.
     * <li>SLSB method with Supports tx attribute may be called.
     * <li>SLSB method with Never tx attribute may be called.
     * <li>SLSB method with Mandatory tx attribute may be called.
     * <li>Stateless Session bean may be created via 2nd business interface.
     * <li>SLSB method with default tx attribute may be called.
     * <li>SLSB method with Required tx attribute may be called.
     * <li>SLSB method with NotSupported tx attribute may be called.
     * <li>SLSB method with RequiresNew tx attribute may be called.
     * <li>SLSB method with Supports tx attribute may be called.
     * <li>SLSB method with Never tx attribute may be called.
     * <li>SLSB method with Mandatory tx attribute may be called.
     * </ol>
     */
    @Test
    public void testBizIntTxAttribs() throws Exception {
        UserTransaction userTran = null;

        try {
            // --------------------------------------------------------------------
            // Locate SL Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            BasicCMTStatelessRemote bean = (BasicCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(BasicCMTStatelessRemoteInterface, Module, AdvCompBean);
            assertNotNull("1 ---> BasicCMTStatelessRemote obtained successfully.", bean);

            bean.tx_Default();
            bean.tx_Required();
            bean.tx_NotSupported();
            bean.tx_RequiresNew();
            bean.tx_Supports();
            bean.tx_Never();
            userTran = FATHelper.lookupUserTransaction();

            svLogger.info("Beginning User Transaction ...");
            userTran.begin();

            bean.tx_Mandatory();

            svLogger.info("Committing User Transaction ...");
            userTran.commit();

            // --------------------------------------------------------------------
            // Access EJB using 2nd Business Remote Interface and execute test
            // --------------------------------------------------------------------
            AdvCMTStatelessRemote bean2 = (AdvCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(AdvCMTStatelessRemoteInterface, Module, AdvCompBean);
            assertNotNull("9 ---> AdvCMTStatelessRemote obtained successfully.", bean2);

            bean2.tx_Default();
            bean2.adv_Tx_NotSupported();
            bean2.tx_RequiresNew();
            bean2.tx_Supports();
            bean2.tx_Never();

            userTran = FATHelper.lookupUserTransaction();

            svLogger.info("Beginning User Transaction ...");
            userTran.begin();

            bean2.adv_Tx_Mandatory();

            svLogger.info("Committing User Transaction ...");
            userTran.commit();
        } finally {
            if (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)
                userTran.rollback();
        }
    }

    /**
     * Test calling the context methods getInvokedBusinessInterface and getBusinessObject on a Remote EJB 3.0 CMT Stateless Session EJB, (remote) Component Interface.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Stateless Session bean may be created.
     * <li>getInvokedBusinessInterface results in an IllegalStateException.
     * <li>getBusinessObject(null) results in an IllegalStateException.
     * <li>getBusinessObject(Component) results in an IllegalStateException.
     * <li>getBusinessObject(Business) returns a business object.
     * <li>a method may be called on the returned business object.
     * <li>Stateless Session bean may be removed.
     * </ol>
     */
    @Test
    public void testCompIntBizGetters_AdvCompCMTStatelessRemote() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        AdvCMTStatelessEJBHome slHome = (AdvCMTStatelessEJBHome) FATHelper.lookupDefaultBindingEJBJavaApp(AdvCMTStatelessEJBHomeInterface, Module,
                                                                                                          AdvCompBean);

        AdvCMTStatelessEJB bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

        bean.test_getBusinessObject(false);

        bean.remove();
        svLogger.info("4 ---> SLRSB removed successfully.");
    }

    /**
     * Test calling the context methods getInvokedBusinessInterface and getBusinessObject on a Remote EJB 3.0 CMT Stateless Session EJB, (remote) Business Interface.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Stateless Session bean may be created.
     * <li>getInvokedBusinessInterface returns the correct class.
     * <li>getBusinessObject(null) results in an IllegalStateException.
     * <li>getBusinessObject(Component) results in an IllegalStateException.
     * <li>getBusinessObject(Business) returns a business object.
     * <li>a method may be called on the returned business object.
     * </ol>
     */
    @Test
    public void testBizIntBizGetters_AdvCompCMTStatelessRemote() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        BasicCMTStatelessRemote bean = (BasicCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(BasicCMTStatelessRemoteInterface, Module, AdvCompBean);
        assertNotNull("1 ---> SLRSB created successfully.", bean);
        bean.test_getBusinessObject(true);
    }

    /**
     * Test that EJB field and method injection occurs properly on a Remote EJB 3.0 CMT Stateless Session EJB, (remote)Component Interface.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Stateless Session bean may be created.
     * <li>Verify SLSB field injection occurs properly.
     * <li>Verify SLSB field injection does not occur after field cleared in instance.
     * <li>Verify SLSB method injection occurs properly.
     * <li>Verify SLSB method injection does not occur after field cleared in instance.
     * <li>Stateless Session bean may be removed.
     * </ol>
     */
    @Test
    public void testCompIntInjection_AdvCompCMTStatelessRemote() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        AdvCMTStatelessEJBHome slHome = (AdvCMTStatelessEJBHome) FATHelper.lookupDefaultBindingEJBJavaApp(AdvCMTStatelessEJBHomeInterface, Module,
                                                                                                          AdvCompBean);

        AdvCMTStatelessEJB bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

        bean.verifyEJBFieldInjection();

        bean.verifyNoEJBFieldInjection();

        bean.verifyEJBMethodInjection();

        bean.verifyNoEJBMethodInjection();

        bean.remove();
        svLogger.info("16 --> SLRSB removed successfully.");
    }
}