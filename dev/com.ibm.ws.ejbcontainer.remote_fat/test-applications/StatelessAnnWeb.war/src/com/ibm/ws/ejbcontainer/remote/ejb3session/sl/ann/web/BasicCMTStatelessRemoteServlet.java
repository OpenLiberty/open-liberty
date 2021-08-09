/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.BasicCMTStatelessRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> BasicCMTStatelessRemoteTest .
 *
 * <dt><b>Test Author:</b> Tracy Burroughs (remote-ified by Urrvano Gamez, Jr.)
 * <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Basic EJB 3.0 Container Managed Stateless Session bean functionality.(remote version)
 * <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>
 * Sub-tests
 * <ul>
 * <li>testTxAttribs - (remote) Business Interface: Verify methods with all ContainerManaged Tx Attributes
 * <li>testBizGetters - (remote) Business Interface: test getBusinessObject
 * </ul>
 * <br>
 * Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/BasicCMTStatelessRemoteServlet")
public class BasicCMTStatelessRemoteServlet extends FATServlet {
    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = BasicCMTStatelessRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Name of module... for lookup.
    private static final String Module = "StatelessAnnEJB";

    // Names of the beans used for the test... for lookup.
    private static final String BasicBean = "BasicCMTStatelessRemoteBean";

    // Names of the interfaces used for the test
    private static final String BasicCMTStatelessRemoteInterface = BasicCMTStatelessRemote.class.getName();

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB, (remote) Business Interface, with each of the different Transaction Attributes.
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
     * </ol>
     */
    @Test
    public void testTxAttribs() throws Exception {
        UserTransaction userTran = null;

        try {
            // --------------------------------------------------------------------
            // Locate SL Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            BasicCMTStatelessRemote bean = (BasicCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(BasicCMTStatelessRemoteInterface, Module, BasicBean);
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
    public void testBizGetters() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        BasicCMTStatelessRemote bean = (BasicCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(BasicCMTStatelessRemoteInterface, Module, BasicBean);
        assertNotNull("1 ---> SLRSB created successfully.", bean);

        bean.test_getBusinessObject(true);
    }
}