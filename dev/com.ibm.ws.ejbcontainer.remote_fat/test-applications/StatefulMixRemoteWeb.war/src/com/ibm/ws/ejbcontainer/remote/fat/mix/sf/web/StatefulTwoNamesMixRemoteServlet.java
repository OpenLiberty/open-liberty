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
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.AltCMTStatefulRemote;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.CMTStatefulRemote;

import componenttest.app.FATServlet;

/**
 * Tests that when a stateful session bean is defined with two
 * different ejb-names, one in annotations, the other in XML,
 * both components are created.<p>
 *
 * Sub-tests
 * <ul>
 * <li>test01 - Business Interface: Look up annotation-based component
 * and verify all methods
 * <li>test02 - Business Interface: Look up XML-based component
 * and verify all methods
 * <li>test03 - Business Interface: Look up a component (Alt) using the name
 * of another bean impl class (NoName)
 * <li>test04 - Business Interface: Look up the component that was
 * overridden in test03 (NoName) with a different name (RealNoName)
 * </ul>
 */
@WebServlet("/StatefulTwoNamesMixRemoteServlet")
public class StatefulTwoNamesMixRemoteServlet extends FATServlet {
    private static final long serialVersionUID = -3433351216682045099L;
    private final static String CLASSNAME = StatefulTwoNamesMixRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    /** Strings to use in the lookups for the test. **/
    final String businessInterface = "com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.CMTStatefulRemote";
    final String altBusinessInterface = "com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.AltCMTStatefulRemote";
    final String module = "StatefulMixRemoteEJB";
    final String beanName1 = "CMTStateful1";
    final String beanName2 = "CMTStateful2";
    final String beanName3 = "NoNameCMTStatefulBean";
    final String beanName4 = "RealNoNameBean";

    /**
     * Test calling methods on an EJB 3.0 CMT Stateful Session EJB
     * that was looked up by its annotation-specified name. <p>
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
     */
    @Test
    public void testStatefulTwoNamesMixRemoteServlet_test01() throws Exception {

        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Lookup SFSB by annotation name and execute the test
        // --------------------------------------------------------------------
        CMTStatefulRemote bean1 = (CMTStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface, module, beanName1);
        assertNotNull("1 ---> CMTStatefulRemote1 obtained successfully.", bean1);

        bean1.tx_Default();
        bean1.tx_Required();
        bean1.tx_NotSupported();
        bean1.tx_RequiresNew();
        bean1.tx_Supports();
        bean1.tx_Never();

        try {
            userTran = FATHelper.lookupUserTransaction();

            svLogger.info("Beginning User Transaction ...");
            userTran.begin();

            bean1.tx_Mandatory();

            svLogger.info("Committing User Transaction ...");
            userTran.commit();
        } catch (Exception ex) {
            if (userTran != null &&
                userTran.getStatus() != Status.STATUS_NO_TRANSACTION)
                userTran.rollback();
            throw ex;
        }

    }

    /**
     * Test calling methods on an EJB 3.0 CMT Stateful Session EJB
     * that was looked up by its XML-specified name. <p>
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
     */
    @Test
    public void testStatefulTwoNamesMixRemoteServlet_test02() throws Exception {

        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Lookup SLSB by XML name and execute the test
        // --------------------------------------------------------------------
        CMTStatefulRemote bean2 = (CMTStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface, module, beanName2);
        assertNotNull("1 ---> CMTStatefulRemote2 obtained successfully.", bean2);

        bean2.tx_Default();
        bean2.tx_Required();
        bean2.tx_NotSupported();
        bean2.tx_RequiresNew();
        bean2.tx_Supports();
        bean2.tx_Never();

        try {
            userTran = FATHelper.lookupUserTransaction();

            svLogger.info("Beginning User Transaction ...");
            userTran.begin();

            bean2.tx_Mandatory();

            svLogger.info("Committing User Transaction ...");
            userTran.commit();
        } catch (Exception ex) {
            if (userTran != null &&
                userTran.getStatus() != Status.STATUS_NO_TRANSACTION)
                userTran.rollback();
            throw ex;
        }
    }

    /**
     * Test calling methods on an EJB 3.0 CMT Stateful Session EJB
     * that was looked up by its XML-specified name which is the same
     * as the implementation class of a different EJB. <p>
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
     */
    @Test
    public void testStatefulTwoNamesMixRemoteServlet_test03() throws Exception {

        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Lookup SFSB by XML name and execute the test
        // --------------------------------------------------------------------
        AltCMTStatefulRemote bean3 = (AltCMTStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(altBusinessInterface, module, beanName3);
        assertNotNull("1 ---> NoNameCMTStatefulRemoteBean obtained successfully.", bean3);

        bean3.txDefault();
        bean3.txRequired();
        bean3.txNotSupported();
        bean3.txRequiresNew();
        bean3.txSupports();
        bean3.txNever();

        try {
            userTran = FATHelper.lookupUserTransaction();

            svLogger.info("Beginning User Transaction ...");
            userTran.begin();

            bean3.txMandatory();

            svLogger.info("Committing User Transaction ...");
            userTran.commit();
        } catch (Exception ex) {
            if (userTran != null &&
                userTran.getStatus() != Status.STATUS_NO_TRANSACTION)
                userTran.rollback();
            throw ex;
        }
    }

    /**
     * Test calling methods on an EJB 3.0 CMT Stateful Session EJB
     * whose implementation class was used as an XML-specified name
     * of a different EJB. <p>
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
     */
    @Test
    public void testStatefulTwoNamesMixRemoteServlet_test04() throws Exception {

        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Lookup SFSB by XML name and execute the test
        // --------------------------------------------------------------------
        CMTStatefulRemote bean4 = (CMTStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface, module, beanName4);
        assertNotNull("1 ---> RealNoNameBean obtained successfully.", bean4);

        bean4.tx_Default();;
        bean4.tx_Required();;
        bean4.tx_NotSupported();;
        bean4.tx_RequiresNew();;
        bean4.tx_Supports();;
        bean4.tx_Never();;

        try {
            userTran = FATHelper.lookupUserTransaction();

            svLogger.info("Beginning User Transaction ...");
            userTran.begin();

            bean4.tx_Mandatory();;

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
