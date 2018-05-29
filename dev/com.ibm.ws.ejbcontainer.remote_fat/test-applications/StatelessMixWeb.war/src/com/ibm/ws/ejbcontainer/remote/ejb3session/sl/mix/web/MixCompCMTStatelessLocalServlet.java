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
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.web;

import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.CMTStatelessEJBLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.CMTStatelessEJBLocalHome;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.CMTStatelessLocal;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> CompCMTStatelessLocalTest .
 *
 * <dt><b>Test Author:</b> Brian Decker
 * <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Compatibility EJB 3.0 Container
 * Managed Stateless Session bean functionality.
 * <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>
 * Sub-tests
 * <ul>
 * <li>testCompIntTxAttr - Component Interface: Verify methods with all
 * ContainerManaged Tx Attributes
 * <li>testBizIntTxAttr - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes
 * <li>testCompIntTxAttrNoBizInt - Component Interface: Verify methods with all
 * ContainerManaged Tx Attributes. NOTE the bean used here does not have any
 * business interfaces (i.e. it doesn't use the Local annotation, just the
 * LocalHome annotation).
 * </ul>
 * <br>
 * Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/CompCMTStatelessLocalServlet")
public class MixCompCMTStatelessLocalServlet extends FATServlet {
    private static final String CLASS_NAME = MixCompCMTStatelessLocalServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB(beanName = "CompCMTStatelessLocal")
    CMTStatelessEJBLocalHome slHome;

    @EJB(beanName = "CompNoBizCMTStatelessLocal")
    CMTStatelessEJBLocalHome slHomeNoBiz;

    @EJB(beanName = "CompCMTStatelessLocal")
    CMTStatelessLocal bizBean;

    /**
     *
     * Test calling methods on an EJB 3.0 CMT Stateless Session EJB, Component
     * Interface, with each of the different Transaction Attributes.
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
    public void testCompIntTxAttr() throws Exception {
        UserTransaction userTran = null;

        CMTStatelessEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SLSB created successfully.", bean);

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
        svLogger.info("9 ---> SLSB removed successfully.");
    }

    /**
     * Test calling methods on an EJB 3.0 CMT Stateless Session EJB, Business
     * Interface, with each of the different Transaction Attributes.
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
    public void testBizIntTxAttr() throws Exception {
        UserTransaction userTran = null;

        assertNotNull("1 ---> SLSB created successfully.", bizBean);

        bizBean.tx_Default();
        bizBean.tx_Required();
        bizBean.tx_NotSupported();
        bizBean.tx_RequiresNew();
        bizBean.tx_Supports();
        bizBean.tx_Never();

        userTran = FATHelper.lookupUserTransaction();
        svLogger.info("Beginning User Transaction ...");
        userTran.begin();
        bizBean.tx_Mandatory();
        svLogger.info("Committing User Transaction ...");
        userTran.commit();
    }

    /**
     * Test calling methods on an EJB 3.0 CMT Stateless Session EJB, Component
     * Interface, with each of the different Transaction Attributes.
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
    public void testCompIntTxAttrNoBizInt() throws Exception {
        UserTransaction userTran = null;

        CMTStatelessEJBLocal bean = slHomeNoBiz.create();
        assertNotNull("1 ---> SLSB created successfully.", bean);
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
        svLogger.info("9 ---> SLSB removed successfully.");
    }
}