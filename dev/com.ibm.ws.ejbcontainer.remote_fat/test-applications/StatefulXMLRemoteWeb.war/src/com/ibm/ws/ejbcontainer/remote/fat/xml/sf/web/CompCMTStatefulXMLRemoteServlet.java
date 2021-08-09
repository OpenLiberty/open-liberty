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
package com.ibm.ws.ejbcontainer.remote.fat.xml.sf.web;

import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb.BasicCMTStatefulEJBRemote;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb.BasicCMTStatefulEJBRemoteHome;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb.BasicCMTStatefulRemote;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * Tests EJB Container support for the Compatibility EJB 3.0 Container
 * Managed Stateful Session bean functionality.
 * <p>
 *
 * Sub-tests
 * <ul>
 * <li>testTxAttribsCompInt - Component Interface: Verify methods with all
 * ContainerManaged Tx Attributes
 * <li>testTxAttribsBizInt - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes
 * <li>testBizGettersCompInt - Component Interface: test getBusinessObject
 * <li>testBizGettersBizInt - Business Interface: test getBusinessObject
 * <li>testTxAttribsCompNoBiz - Component Interface: Verify methods with all
 * ContainerManaged Tx Attributes !!!NOTE!!! The bean used here does NOT have
 * business interfaces (i.e. no Remote annotation, jut RemoteHome annotation)
 * </ul>
 */
@WebServlet("/CompCMTStatefulXMLRemoteServlet")
public class CompCMTStatefulXMLRemoteServlet extends FATServlet {

    private static final long serialVersionUID = -4131880426640389973L;
    private final static String CLASSNAME = CompCMTStatefulXMLRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Names of the beans used for the test... for lookup.
    private static final String CompBean = "CompCMTStatefulRemote";
    private static final String CompNoBizBean = "CompNoBizCMTStatefulRemote";

    // Names of application and module... for lookup.
    private static final String Module = "StatefulXMLRemoteEJB";
    private static final String Application = "StatefulXMLRemoteTest";

    // Names of the interfaces used for the test
    private static final String BasicCMTStatefulRemoteInterface = BasicCMTStatefulRemote.class.getName();
    private static final String BasicCMTStatefulEJBRemoteHomeInterface = BasicCMTStatefulEJBRemoteHome.class.getName();

    /**
     * Test calling methods on an EJB 3.0 CMT Stateful Session EJB, Component
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
     * <li>Stateful Session bean may be removed.
     * </ol>
     */
    @Test
    public void testSFSBTxAttribsCompIntXML() throws Exception {
        UserTransaction userTran = null;
        try {
            // --------------------------------------------------------------------
            // Locate SF Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            BasicCMTStatefulEJBRemoteHome sfHome = (BasicCMTStatefulEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                                                     BasicCMTStatefulEJBRemoteHomeInterface,
                                                                                                                                     Application, Module, CompBean);

            BasicCMTStatefulEJBRemote bean = sfHome.create();
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
            userTran.commit();
            bean.remove();
            svLogger.info("9 ---> SFLSB removed successfully.");
        } finally {
            if (userTran != null && userTran.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION && userTran.getStatus() != javax.transaction.Status.STATUS_COMMITTED)
                userTran.rollback();
        }
    }

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
    public void testSFSBTxAttribsBizIntXML() throws Exception {
        UserTransaction userTran = null;
        try {
            // --------------------------------------------------------------------
            // Locate SF Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            BasicCMTStatefulRemote bean = (BasicCMTStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                                                            BasicCMTStatefulRemoteInterface, Module,
                                                                                                            CompBean);
            assertNotNull("1 ---> SFSB created successfully.", bean);
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
            if (userTran != null && userTran.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION && userTran.getStatus() != javax.transaction.Status.STATUS_COMMITTED)
                userTran.rollback();
        }
    }

    /**
     * Test calling the context methods getInvokedBusinessInterface and
     * getBusinessObject on an EJB 3.0 CMT Stateful Session EJB, Component
     * Interface.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Stateful Session bean may be created.
     * <li>getInvokedBusinessInterface results in an IllegalStateException.
     * <li>getBusinessObject(null) results in an IllegalStateException.
     * <li>getBusinessObject(Component) results in an IllegalStateException.
     * <li>getBusinessObject(Business) returns a business object.
     * <li>ConcurrentAccessException occurs calling a method on the returned
     * business object.
     * <li>Stateful Session bean may be removed.
     * </ol>
     */
    @Test
    @ExpectedFFDC("com.ibm.ejs.container.BeanNotReentrantException")
    public void testSFSBBizGettersCompIntXML() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        BasicCMTStatefulEJBRemoteHome sfHome = (BasicCMTStatefulEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                                                 BasicCMTStatefulEJBRemoteHomeInterface,
                                                                                                                                 Application, Module, CompBean);

        BasicCMTStatefulEJBRemote bean = sfHome.create();
        assertNotNull("1 ---> SFSB created successfully.", bean);
        try {
            bean.test_getBusinessObject(false);
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
        bean.remove();
        svLogger.info("4 ---> SFSB removed successfully.");

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
    public void testSFSBBizGettersBizIntXML() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        BasicCMTStatefulRemote bean = (BasicCMTStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                                                        BasicCMTStatefulRemoteInterface, Module,
                                                                                                        CompBean);
        assertNotNull("1 ---> SFSB created successfully.", bean);
        bean.test_getBusinessObject(true);
    }

    /**
     * Test calling methods on an EJB 3.0 CMT Stateful Session EJB, Component
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
     * <li>Stateful Session bean may be removed.
     * </ol>
     */
    @Test
    public void testSFSBTxAttribsCompNoBizXML() throws Exception {
        UserTransaction userTran = null;
        try {
            // --------------------------------------------------------------------
            // Locate SF Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            BasicCMTStatefulEJBRemoteHome sfHome = (BasicCMTStatefulEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                                                     BasicCMTStatefulEJBRemoteHomeInterface,
                                                                                                                                     Application, Module, CompNoBizBean);
            BasicCMTStatefulEJBRemote bean = sfHome.create();
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
            userTran.commit();
        } finally {
            if (userTran != null && userTran.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION && userTran.getStatus() != javax.transaction.Status.STATUS_COMMITTED)
                userTran.rollback();
        }
    }
}
