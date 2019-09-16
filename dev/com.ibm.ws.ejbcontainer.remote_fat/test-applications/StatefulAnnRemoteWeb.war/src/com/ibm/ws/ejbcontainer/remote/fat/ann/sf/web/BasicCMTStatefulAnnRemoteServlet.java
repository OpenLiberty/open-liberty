/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.ann.sf.web;

import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb.BasicCMTStatefulRemote;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * Tests EJB Container support for the Basic EJB 3.0 Container Managed
 * Stateful Session bean functionality.
 * <p>
 *
 * Sub-tests
 * <ul>
 * <li>testTxAttribs - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes
 * <li>testBizMethods - Business Interface: Verify getInvokedBusinessInterface
 * and getBusinessObject work correctly
 * </ul>
 */
@SuppressWarnings("serial")
@WebServlet("/BasicCMTStatefulAnnRemoteServlet")
public class BasicCMTStatefulAnnRemoteServlet extends FATServlet // F379-549fvtFrw
{
    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = BasicCMTStatefulAnnRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Names of the beans used for the test... for lookup.
    private static final String BasicBean = "BasicCMTStatefulRemoteBean";

    // Name of module... for lookup.
    private static final String Module = "StatefulAnnRemoteEJB";

    // Names of the interfaces used for the test
    private static final String BasicCMTStatefulRemoteInterface = BasicCMTStatefulRemote.class.getName();

    /**
     * Test calling methods on an EJB 3.0 CMT Stateful Session EJB, Business
     * Interface, with each of the different Transaction Attributes.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Stateful Session bean may be created.
     * <li>SFSB method with default tx attribute may be called.
     * <li>SFSB method with Required tx attribute may be called.
     * <li>SFSB method with NotSupported tx attribute may be called.
     * <li>SFSB method with RequiresNew tx attribute may be called.
     * <li>SFSB method with Supports tx attribute may be called.
     * <li>SFSB method with Never tx attribute may be called.
     * <li>SFSB method with Mandatory tx attribute may be called.
     * </ol>
     */
    @Test
    public void testSFSBTxAttribsAnn() throws Exception {
        UserTransaction userTran = null;
        try {
            // --------------------------------------------------------------------
            // Locate SF Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            BasicCMTStatefulRemote bean = (BasicCMTStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                                                            BasicCMTStatefulRemoteInterface, Module,
                                                                                                            BasicBean);
            assertNotNull("1 ---> SFLSB created successfully.", bean);
            try {
                bean.tx_Default();
                bean.tx_Required();
                bean.tx_NotSupported();
                bean.tx_RequiresNew();
                bean.tx_Supports();
                bean.tx_Never();
                userTran = FATHelper.lookupUserTransaction();
                userTran.begin();
                bean.tx_Mandatory();
                svLogger.info("Committing User Transaction ...");
                userTran.commit();
            } catch (Throwable t) {
                FATHelper.checkForAssertion(t);
            }
        } finally {
            if (userTran != null && userTran.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION && userTran.getStatus() != javax.transaction.Status.STATUS_COMMITTED)
                userTran.rollback();
        }
    }

    /**
     * Test calling the context methods getInvokedBusinessInterface and
     * getBusinessObject on an EJB 3.0 CMT Stateful Session EJB, Business
     * Interface.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Stateful Session bean may be created.
     * <li>getInvokedBusinessInterface returns the correct class.
     * <li>getBusinessObject(null) results in an IllegalStateException.
     * <li>getBusinessObject(Component) results in an IllegalStateException.
     * <li>getBusinessObject(Business) returns a business object.
     * <li>ConcurrentAccessException occurs calling a method on the returned
     * business object.
     * </ol>
     */
    @Test
    @ExpectedFFDC("com.ibm.ejs.container.BeanNotReentrantException")
    public void testSFSBBizMethodsAnn() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        BasicCMTStatefulRemote bean = (BasicCMTStatefulRemote)

        FATHelper.lookupDefaultBindingEJBJavaApp(
                                                 BasicCMTStatefulRemoteInterface, Module,
                                                 BasicBean);
        assertNotNull("1 ---> SFLSB created successfully.", bean);
        try {
            bean.test_getBusinessObject(true);
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }
}