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
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.web;

import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.CMTStatefulEJBRemote;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.CMTStatefulEJBRemoteHome;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.CMTStatefulRemote;

import componenttest.app.FATServlet;

/**
 * Tests EJB Container support for the Compatibility EJB 3.0
 * Container Managed Stateful Session bean functionality. <p>
 *
 * Sub-tests
 * <ul>
 * <li>test01 - Remote Component Interface: Verify methods with all
 * ContainerManaged Tx Attributes
 * <li>test02 - Remote Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes
 * </ul>
 */
@WebServlet("/CompCMTStatefulMixRemoteServlet")
public class CompCMTStatefulMixRemoteServlet extends FATServlet {
    private static final long serialVersionUID = -4724460222267878215L;
    private final static String CLASSNAME = CompCMTStatefulMixRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Strings to use in the lookups for the test. **/
    final String businessInterface = "com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.CMTStatefulRemote";
    final String homeInterface = CMTStatefulEJBRemoteHome.class.getName();
    final String module = "StatefulMixRemoteEJB";
    private static final String Application = "StatefulMixRemoteTest";
    final String beanName = "CompCMTStateful";

    /**
     * Test calling methods on an EJB 3.0 CMT Stateful Session EJB,
     * Component Interface, with each of the different Transaction
     * Attributes. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean may be created.
     * <li> SFSB method with default tx attribute may be called.
     * <li> SFSB method with Required tx attribute may be called.
     * <li> SFSB method with NotSupported tx attribute may be called.
     * <li> SFSB method with RequiresNew tx attribute may be called.
     * <li> SFSB method with Supports tx attribute may be called.
     * <li> SFSB method with Never tx attribute may be called.
     * <li> SFSB method with Mandatory tx attribute may be called.
     * <li> Stateful Session bean may be removed.
     * </ol>
     *
     * @throws Exception
     */
    @Test
    public void testCompCMTStatefulMixRemoteServlet_test01() throws Exception {
        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        CMTStatefulEJBRemoteHome sfHome = (CMTStatefulEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(homeInterface, Application, module, beanName);
        CMTStatefulEJBRemote bean = sfHome.create();
        assertNotNull("1 ---> SFSB created successfully.", bean);

        bean.tx_Default();
        bean.tx_Required();
        bean.tx_NotSupported();
        bean.tx_RequiresNew();
        bean.tx_Supports();
        bean.tx_Never();

        try {
            userTran = FATHelper.lookupUserTransaction();

            svLogger.info("Beginning User Transaction ...");
            userTran.begin();

            bean.tx_Mandatory();

            svLogger.info("Committing User Transaction ...");
            userTran.commit();
        } catch (Exception ex) {
            if (userTran != null &&
                userTran.getStatus() != Status.STATUS_NO_TRANSACTION)
                userTran.rollback();
            throw ex;
        }

        bean.remove();
        svLogger.info("9 ---> SFSB removed successfully.");

    }

    /**
     * Test calling methods on an EJB 3.0 CMT Stateful Session EJB,
     * Business Interface, with each of the different Transaction
     * Attributes. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean may be created.
     * <li> SFSB method with default tx attribute may be called.
     * <li> SFSB method with Required tx attribute may be called.
     * <li> SFSB method with NotSupported tx attribute may be called.
     * <li> SFSB method with RequiresNew tx attribute may be called.
     * <li> SFSB method with Supports tx attribute may be called.
     * <li> SFSB method with Never tx attribute may be called.
     * <li> SFSB method with Mandatory tx attribute may be called.
     * </ol>
     *
     * @throws Exception
     */
    @Test
    public void testCompCMTStatefulMixRemoteServlet_test02() throws Exception {

        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        CMTStatefulRemote bean = (CMTStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface, module, beanName);
        assertNotNull("1 ---> SFSB created successfully.", bean);

        bean.tx_Default();
        bean.tx_Required();
        bean.tx_NotSupported();
        bean.tx_RequiresNew();
        bean.tx_Supports();
        bean.tx_Never();

        try {
            userTran = FATHelper.lookupUserTransaction();

            svLogger.info("Beginning User Transaction ...");
            userTran.begin();

            bean.tx_Mandatory();

            svLogger.info("Committing User Transaction ...");
            userTran.commit();
        } catch (Exception ex) {
            if (userTran != null &&
                userTran.getStatus() != Status.STATUS_NO_TRANSACTION)
                userTran.rollback();
            throw ex;
        }
    }
}
