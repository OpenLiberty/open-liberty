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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.AltCMTStatelessLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.BasicAnnotLocal;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.mix.ejb.CMTStatelessLocal;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> StatelessTwoNamesTest .
 *
 * <dt><b>Test Author:</b> Brian Decker
 * <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests that when a stateless session bean is defined with two different
 * ejb-names, one in annotations, the other in XML, both components are created.
 * <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>
 * Sub-tests
 * <ul>
 * <li>testBizIntLookupAnnBasedComp - Business Interface: Look up
 * annotation-based component and verify all methods
 * <li>testBizIntLookupXmlBasedComp - Business Interface: Look up XML-based
 * component and verify all methods
 * <li>testBizIntLookupCompUsingBeanImplClassName - Business Interface: Look up
 * a component (Alt) using the name of another bean impl class (NoName)
 * <li>testBizIntLookupCompUsingBeanImplClassName2 - Business Interface: Look up
 * the component that was overridden in
 * testBizIntLookupCompUsingBeanImplClassName (NoName) with a different name
 * (RealNoName)
 * <li>testBizIntMixedSTL - Business Interface: Test mixed stateless bean,
 * annotated with Stateless and no session-type in the XML.
 * </ul>
 * <br>
 * Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/StatelessTwoNamesServlet")
public class StatelessTwoNamesServlet extends FATServlet {
    private static final String CLASS_NAME = StatelessTwoNamesServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    /** Strings to use in the lookups for the test. **/
    final String businessInterface = CMTStatelessLocal.class.getName();
    final String altBusinessInterface = AltCMTStatelessLocal.class.getName();
    final String annotBusinessInterface = BasicAnnotLocal.class.getName();
    final String module = "StatelessMixEJB";
    final String beanName1 = "CMTStatelessLocal1";
    final String beanName2 = "CMTStatelessLocal2";
    final String beanName3 = "NoNameCMTStatelessLocalBean";
    final String beanName4 = "RealNoNameBean";
    final String beanName5 = "BasicAnnotStateless";

    /**
     * Test calling methods on an EJB 3.0 CMT Stateless Session EJB that was
     * looked up by its annotation-specified name.
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
        // Lookup SLSB by annotation name and execute the test
        // --------------------------------------------------------------------
        CMTStatelessLocal bean1 = (CMTStatelessLocal) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface, module, beanName1);
        assertNotNull("1 ---> CMTStatelessLocal1 obtained successfully.", bean1);

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
     * Test calling methods on an EJB 3.0 CMT Stateless Session EJB that was
     * looked up by its XML-specified name.
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
        // Lookup SLSB by XML name and execute the test
        // --------------------------------------------------------------------
        CMTStatelessLocal bean2 = (CMTStatelessLocal) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface, module, beanName2);
        assertNotNull("1 ---> CMTStatelessLocal2 obtained successfully.", bean2);

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
     * Test calling methods on an EJB 3.0 CMT Stateless Session EJB that was
     * looked up by its XML-specified name which is the same as the
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
        // Lookup SLSB by XML name and execute the test
        // --------------------------------------------------------------------
        AltCMTStatelessLocal bean3 = (AltCMTStatelessLocal) FATHelper.lookupDefaultBindingEJBJavaApp(altBusinessInterface, module, beanName3);
        assertNotNull("1 ---> NoNameCMTStatelessLocalBean obtained successfully.", bean3);

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
     * Test calling methods on an EJB 3.0 CMT Stateless Session EJB whose
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
        // Lookup SLSB by XML name and execute the test
        // --------------------------------------------------------------------
        CMTStatelessLocal bean4 = (CMTStatelessLocal) FATHelper.lookupDefaultBindingEJBJavaApp(businessInterface, module, beanName4);
        assertNotNull("1 ---> RealNoNameBean obtained successfully.", bean4);

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

    /**
     * Test calling methods on an EJB 3.0 CMT Stateless Session EJB with
     * Stateless annotation and no session-type in XML.
     */
    @Test
    public void testBizIntMixedSTL() throws Exception {
        // --------------------------------------------------------------------
        // Lookup SFSB by XML name and execute the test
        // --------------------------------------------------------------------
        BasicAnnotLocal bean5 = (BasicAnnotLocal) FATHelper.lookupDefaultBindingEJBJavaApp(annotBusinessInterface, module, beanName5);
        assertNotNull("1 ---> BasicAnnotLocal obtained successfully.", bean5);
        assertEquals("2 ---> Verifying method result.", bean5.getString(), "success");
    }
}