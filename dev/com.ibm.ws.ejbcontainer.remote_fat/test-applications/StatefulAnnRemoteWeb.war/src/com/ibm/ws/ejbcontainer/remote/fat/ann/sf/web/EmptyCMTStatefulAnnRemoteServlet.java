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

import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/EmptyCMTStatefulAnnRemoteServlet")
public class EmptyCMTStatefulAnnRemoteServlet extends FATServlet {

    private final static String CLASSNAME = EmptyCMTStatefulAnnRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Names of the beans used for the test... for lookup.
    private static final String EmptyBean = "EmptyCMTStatefulRemoteBean";

    // Names of the interfaces used for the test
    private static final String BasicCMTStatefulRemoteInterface = BasicCMTStatefulRemote.class.getName();

    // Names of application and module... for lookup.
    private static final String Module = "StatefulAnnRemoteEJB";

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
    public void testSFSBEmptyTxAttribsAnn() throws Exception {
        UserTransaction userTran = null;
        try {
            // --------------------------------------------------------------------
            // Locate SF Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            BasicCMTStatefulRemote bean = (BasicCMTStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                                                            BasicCMTStatefulRemoteInterface, Module,
                                                                                                            EmptyBean);
            assertNotNull("1 ---> SFLSB created successfully.", bean);
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
        } finally {
            if (userTran != null && userTran.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION && userTran.getStatus() != javax.transaction.Status.STATUS_COMMITTED)
                userTran.rollback();
        }
    }

}
