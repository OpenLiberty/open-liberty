/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.BasicCMTStatelessEJBLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.BasicCMTStatelessEJBLocalHome;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> CompCMTStatelessLocalTest .
 *
 * <dt><b>Test Author:</b> Tracy Burroughs
 * <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Compatibility EJB 3.0 Container Managed Stateless Session bean functionality.
 * <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>
 * Sub-tests
 * <ul>
 * <li>testCompIntTxAttribs - Component Interface: Verify methods with all ContainerManaged Tx Attributes
 * <li>testBizIntTxAttribs - Business Interface: Verify methods with all ContainerManaged Tx Attributes
 * <li>testCompIntBizGetters - Component Interface: Test calling the context methods getInvokedBusinessInterface and getBusinessObject
 * <li>testBizIntBizGetters - Business Interface: Test calling the context methods getInvokedBusinessInterface and getBusinessObject
 * </ul>
 * <br>
 * Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/CompCMTStatelessLocalServlet")
public class CompCMTStatelessLocalServlet extends FATServlet {
    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = CompCMTStatelessLocalServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Name of module... for lookup.
    private static final String Module = "StatelessAnnEJB";

    // Names of the beans used for the test... for lookup.
    private static final String CompBean = "CompCMTStatelessLocal";

    // Names of the interfaces used for the test
    private static final String BasicCMTStatelessEJBLocalHomeInterface = BasicCMTStatelessEJBLocalHome.class.getName();

    /**
     * Test calling methods on an EJB 3.0 CMT Stateless Session EJB, Component Interface, with each of the different Transaction Attributes.
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
    public void testCompIntTxAttribs() throws Exception {
        UserTransaction userTran = null;

        try {
            // --------------------------------------------------------------------
            // Locate SL Local Home/Factory and execute the test
            // --------------------------------------------------------------------
            BasicCMTStatelessEJBLocalHome slHome = (BasicCMTStatelessEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaApp(BasicCMTStatelessEJBLocalHomeInterface, Module,
                                                                                                                            CompBean);
            BasicCMTStatelessEJBLocal bean = slHome.create();
            assertNotNull("1 ---> SLLSB created successfully.", bean);

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

            bean.remove();
            svLogger.info("9 ---> SLLSB removed successfully.");
        } finally {
            if (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)
                userTran.rollback();
        }
    }

    /**
     * Test calling the context methods getInvokedBusinessInterface and getBusinessObject on an EJB 3.0 CMT Stateless Session EJB, Component Interface.
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
    public void testCompIntBizGetters() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        BasicCMTStatelessEJBLocalHome slHome = (BasicCMTStatelessEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaApp(BasicCMTStatelessEJBLocalHomeInterface, Module, CompBean);
        BasicCMTStatelessEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        bean.test_getBusinessObject(false);

        bean.remove();
        svLogger.info("4 ---> SLLSB removed successfully.");
    }
}