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
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.CMTStatelessEJB;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.CMTStatelessEJBHome;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.CMTStatelessRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> CompCMTStatelessRemoteTest
 *
 * <dt><b>Test Author:</b> Urrvano Gamez, Jr.
 * <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Compatibility EJB 3.0 Container
 * Managed Stateless Session bean functionality. (remote version)
 * <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>
 * Sub-tests
 * <ul>
 * <li>testCompIntTxAttr - (Remote) Component Interface: Verify methods with all
 * ContainerManaged Tx Attributes
 * <li>testBizIntTxAttr - (Remote) Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes
 * <li>testCompIntTxAttr - (Remote) Component Interface: Verify methods with all
 * ContainerManaged Tx Attributes. NOTE the bean used here does not have any
 * business interfaces (i.e. it doesn't use the Remote annotation, just the
 * RemoteHome annotation).
 * </ul>
 * <br>
 * Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/CompCMTStatelessRemoteServlet")
public class MixCompCMTStatelessRemoteServlet extends FATServlet {
    private static final String CLASS_NAME = MixCompCMTStatelessRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    @EJB(beanName = "CompCMTStatelessRemote")
    CMTStatelessEJBHome slHome;

    @EJB(beanName = "CompNoBizCMTStatelessRemote")
    CMTStatelessEJBHome slHomeNoBiz;

    @EJB(beanName = "CompCMTStatelessRemote")
    CMTStatelessRemote bizBean;

    /** Strings to use in the lookups for the test. **/
    final String businessInterface = "com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.CMTStatelessRemote";
    final String application = "EJB3StatelessMixTestApp";
    final String module = "EJB3SLMBean.jar";
    final String beanName = "CompCMTStatelessRemote";
    final String noBizBeanName = "CompNoBizCMTStatelessRemote";
    final String COMPCMTSL_Home = CMTStatelessEJBHome.class.getName();
    final String COMPCMTNoBizSL_Home = CMTStatelessEJBHome.class.getName();

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB,
     * (remote) Component View Interface, with each of the different Transaction
     * Attributes.
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

        CMTStatelessEJB bean = slHome.create();
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

        bean.remove();
        svLogger.info("9 ---> SLRSB removed successfully.");
    }

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB,
     * (remote) Business Interface, with each of the different Transaction
     * Attributes.
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

        assertNotNull("1 ---> SLRSB created successfully.", bizBean);

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
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB,
     * (remote) Component Interface, with each of the different Transaction
     * Attributes. This bean does not have any Business Interfaces defined.
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

        CMTStatelessEJB bean = slHomeNoBiz.create();
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

        bean.remove();
        svLogger.info("9 ---> SLRSB removed successfully.");
    }
}