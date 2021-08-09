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

import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.AltCMTStatelessRemote;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.CMTStatelessRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> RemoteStatelessTwoNamesTest .
 *
 * <dt><b>Test Author:</b> Urrvano Gamez, Jr.
 * <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests that when a remote stateless session bean is defined with two
 * different ejb-names, one in annotations, the other in XML, both components
 * are created.
 * <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>
 * Sub-tests
 * <ul>
 * <li>testBizIntLookupAnnBasedComp - (remote) Business Interface: Look up
 * annotation-based component and verify all methods
 * <li>testBizIntLookupXmlBasedComp - (remote) Business Interface: Look up
 * XML-based component and verify all methods
 * <li>testBizIntLookupCompUsingBeanImplClassName - (remote) Business Interface:
 * Look up a component (Alt) using the name of another bean impl class (NoName)
 * <li>testBizIntLookupCompUsingBeanImplClassName2 - (remote) Business
 * Interface: Look up the component that was overridden in
 * testBizIntLookupCompUsingBeanImplClassName (NoName) with a different name
 * (RealNoName)
 * </ul>
 * <br>
 * Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/RemoteStatelessTwoNamesServlet")
public class RemoteStatelessTwoNamesServlet extends FATServlet {
    private static final String CLASS_NAME = RemoteStatelessTwoNamesServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    final String businessInterface = CMTStatelessRemote.class.getName();
    final String altBusinessInterface = AltCMTStatelessRemote.class.getName();
    final String module = "StatelessMixEJB";
    final String beanName1 = "CMTStatelessRemote1";
    final String beanName2 = "CMTStatelessRemote2";
    final String beanName3 = "NoNameCMTStatelessRemoteBean";
    final String beanName4 = "RemoteRealNoNameBean";

    /**
     * Test calling methods on a remote EJB 3.0 CMT Stateless Session EJB that
     * was looked up by its annotation-specified name.
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
    public void testBizIntLookupAnnBasedComp() throws Exception {
        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Lookup SLRSB by annotation name and execute the test
        // --------------------------------------------------------------------
        CMTStatelessRemote bean1 = (CMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface, module, beanName1);
        assertNotNull("1 ---> CMTStatelessRemote1 obtained successfully.", bean1);

        bean1.tx_Default();
        bean1.tx_Required();
        bean1.tx_NotSupported();
        bean1.tx_RequiresNew();
        bean1.tx_Supports();
        bean1.tx_Never();

        userTran = FATHelper.lookupUserTransaction();
        svLogger.info("Beginning User Transaction ...");
        userTran.begin();
        bean1.tx_Mandatory();
        svLogger.info("Committing User Transaction ...");
        userTran.commit();
    }

    /**
     * Test calling methods on a remote EJB 3.0 CMT Stateless Session EJB that
     * was looked up by its XML-specified name.
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
    public void testBizIntLookupXmlBasedComp() throws Exception {
        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Lookup SLRSB by XML name and execute the test
        // --------------------------------------------------------------------
        CMTStatelessRemote bean2 = (CMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface, module, beanName2);
        assertNotNull("1 ---> CMTStatelessRemote2 obtained successfully.", bean2);

        bean2.tx_Default();
        bean2.tx_Required();
        bean2.tx_NotSupported();
        bean2.tx_RequiresNew();
        bean2.tx_Supports();
        bean2.tx_Never();

        userTran = FATHelper.lookupUserTransaction();
        svLogger.info("Beginning User Transaction ...");
        userTran.begin();
        bean2.tx_Mandatory();
        svLogger.info("Committing User Transaction ...");
        userTran.commit();
    }

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB that
     * was looked up by its XML-specified name which is the same as the
     * implementation class of a different EJB.
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
    //@Test
    // TODO: 175480 - Re-enable when merge issue is fixed
    public void testBizIntLookupCompUsingBeanImplClassName() throws Exception {
        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Lookup SLRSB by XML name and execute the test
        // --------------------------------------------------------------------
        AltCMTStatelessRemote bean3 = (AltCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(altBusinessInterface, module, beanName3);
        assertNotNull("1 ---> NoNameCMTStatelessRemoteBean obtained successfully.", bean3);

        bean3.txDefault();
        bean3.txRequired();
        bean3.txNotSupported();
        bean3.txRequiresNew();
        bean3.txSupports();
        bean3.txNever();

        userTran = FATHelper.lookupUserTransaction();
        svLogger.info("Beginning User Transaction ...");
        userTran.begin();
        bean3.txMandatory();
        svLogger.info("Committing User Transaction ...");
        userTran.commit();
    }

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB whose
     * implementation class was used as an XML-specified name of a different
     * EJB.
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
    //@Test
    // TODO: 175480 - Re-enable when merge issue is fixed
    public void testBizIntLookupCompUsingBeanImplClassName2() throws Exception {
        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Lookup SLRSB by XML name and execute the test
        // --------------------------------------------------------------------
        CMTStatelessRemote bean4 = (CMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface, module, beanName4);
        assertNotNull("1 ---> RemoteRealNoNameBean obtained successfully.", bean4);

        bean4.tx_Default();
        bean4.tx_Required();
        bean4.tx_NotSupported();
        bean4.tx_RequiresNew();
        bean4.tx_Supports();
        bean4.tx_Never();

        userTran = FATHelper.lookupUserTransaction();
        svLogger.info("Beginning User Transaction ...");
        userTran.begin();
        bean4.tx_Mandatory();
        svLogger.info("Committing User Transaction ...");
        userTran.commit();
    }
}