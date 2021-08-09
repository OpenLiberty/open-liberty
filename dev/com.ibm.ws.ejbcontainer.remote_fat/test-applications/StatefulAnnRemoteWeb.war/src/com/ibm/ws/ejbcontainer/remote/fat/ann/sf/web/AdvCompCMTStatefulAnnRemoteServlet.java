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
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb.AdvCMTStatefulEJBRemote;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb.AdvCMTStatefulEJBRemoteHome;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb.AdvCMTStatefulRemote;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb.BasicCMTStatefulRemote;
import com.ibm.ws.ejbcontainer.remote.fat.ann.sf.ejb.EJBInjectionRemote;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * Tests EJB Container support for the Compatibility EJB 3.0 Container
 * Managed Stateful Session bean functionality with advanced EJB 3.0
 * configurations: including multiple Business Interfaces.
 * <p>
 *
 * Sub-tests
 * <ul>
 * <li>testTranAttrAdvCMT() - Component Interface: Verify methods with all
 * ContainerManaged Tx Attributes
 * <li>testTranBasicCMT() - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes
 * <li>testAdvCMTInvAndkBizObj() - Component Interface: Verify
 * getInvokedBusinessInterface and getBusinessObject throw correct exception
 * <li>testBasicCMTInvAndkBizObj() - Business Interface: Verify
 * getInvokedBusinessInterface and getBusinessObject work correctly
 * <li>testInjectionCompInt() - Component Interface: Verify EJB field and method
 * injection occurs properly
 * <li>testInjectionBizInt() - Business Interface: Verify EJB field and method
 * injection occurs properly
 * </ul>
 */
@SuppressWarnings("serial")
@WebServlet("/AdvCompCMTStatefulAnnRemoteServlet")
public class AdvCompCMTStatefulAnnRemoteServlet extends FATServlet // F379-549fvtFrw
{
    private final static String CLASSNAME = AdvCompCMTStatefulAnnRemoteServlet.class.getName();

    // Names of application and module... for lookup.
    private static final String Module = "StatefulAnnRemoteEJB";

    private static final String Application = "StatefulAnnRemoteTest";

    // Names of the beans used for the test... for lookup.
    private static final String AdvCompBean = "AdvCompCMTStatefulRemote";

    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Names of the interfaces used for the test
    private static final String BasicCMTStatefulRemoteInterface = BasicCMTStatefulRemote.class.getName();
    private static final String AdvCMTStatefulRemoteInterface = AdvCMTStatefulRemote.class.getName();
    private static final String EJBInjectionRemoteInterface = EJBInjectionRemote.class.getName();

    private static final String AdvCMTStatefulEJBRemoteHomeInterface = AdvCMTStatefulEJBRemoteHome.class.getName();

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
    public void testSFSBTranAttrAdvCMTAnn() throws Exception {
        UserTransaction userTran = null;
        try {
            // --------------------------------------------------------------------
            // Locate SF Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            AdvCMTStatefulEJBRemoteHome sfHome = (AdvCMTStatefulEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                                                 AdvCMTStatefulEJBRemoteHomeInterface, Application,
                                                                                                                                 Module, AdvCompBean);
            AdvCMTStatefulEJBRemote bean = sfHome.create();
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
                userTran.commit();
                bean.remove();
                svLogger.info("9 ---> SFLSB removed successfully.");
            } catch (Throwable t) {
                FATHelper.checkForAssertion(t);
            }
        } finally {
            if (userTran != null && userTran.getStatus() != javax.transaction.Status.STATUS_NO_TRANSACTION && userTran.getStatus() != javax.transaction.Status.STATUS_COMMITTED)
                userTran.rollback();
        }
    }

    /**
     * Test calling methods on an EJB 3.0 CMT Stateful Session EJB, using
     * multiple Business Interfaces, with each of the different Transaction
     * Attributes.
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
     * <li>Stateful Session bean may be created via 2nd business interface.
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
    public void testSFSBTranBasicCMTAnn() throws Exception {
        UserTransaction userTran = null;
        try {
            // --------------------------------------------------------------------
            // Locate SF Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            BasicCMTStatefulRemote bean = (BasicCMTStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                                                            BasicCMTStatefulRemoteInterface, Module,
                                                                                                            AdvCompBean);
            assertNotNull("1 ---> BasicCMTStatefulRemote obtained successfully.",
                          bean);
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
                userTran.commit();
            } catch (Throwable t) {
                FATHelper.checkForAssertion(t);
            }
            // --------------------------------------------------------------------
            // Access EJB using 2nd Business Remote Interface and execute test
            // --------------------------------------------------------------------
            AdvCMTStatefulRemote bean2 = (AdvCMTStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                                                         AdvCMTStatefulRemoteInterface, Module,
                                                                                                         AdvCompBean);
            assertNotNull("9 ---> AdvCMTStatefulRemote obtained successfully.",
                          bean2);
            try {
                bean2.tx_Default();
                bean2.adv_Tx_NotSupported();
                bean2.tx_RequiresNew();
                bean2.tx_Supports();
                bean2.tx_Never();
                userTran = FATHelper.lookupUserTransaction();
                userTran.begin();
                bean2.adv_Tx_Mandatory();
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
    public void testSFSBAdvCMTInvAndkBizObjAnn() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        AdvCMTStatefulEJBRemoteHome sfHome = (AdvCMTStatefulEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                                             AdvCMTStatefulEJBRemoteHomeInterface,
                                                                                                                             Application, Module, AdvCompBean);
        AdvCMTStatefulEJBRemote bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);
        try {
            bean.test_getBusinessObject(false);
            bean.remove();
            svLogger.info("7 ---> SFLSB removed successfully.");
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
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
    public void testSFSBBasicCMTInvAndkBizObjAnn() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        BasicCMTStatefulRemote bean = (BasicCMTStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                                                        BasicCMTStatefulRemoteInterface, Module,
                                                                                                        AdvCompBean);
        assertNotNull("1 ---> SFLSB created successfully.", bean);
        try {
            bean.test_getBusinessObject(true);
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    /**
     * Test that EJB field and method injection occurs properly on an EJB 3.0
     * CMT Stateful Session EJB, Component Interface.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Stateful Session bean may be created.
     * <li>Verify SLSB field injection occurs properly.
     * <li>Verify SLSB field injection does not occur after field cleared in
     * instance.
     * <li>Verify SLSB method injection occurs properly.
     * <li>Verify SLSB method injection does not occur after field cleared in
     * instance.
     * <li>Stateful Session bean may be removed.
     * </ol>
     */
    @Test
    public void testSFSBInjectionCompIntAnn() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        AdvCMTStatefulEJBRemoteHome slHome = (AdvCMTStatefulEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                                             AdvCMTStatefulEJBRemoteHomeInterface,
                                                                                                                             Application, Module, AdvCompBean);
        AdvCMTStatefulEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);
        try {
            bean.verifyEJBFieldInjection();
            bean.verifyNoEJBFieldInjection();
            bean.verifyEJBMethodInjection();
            bean.verifyNoEJBMethodInjection();
            bean.remove();
            svLogger.info("16 --> SLLSB removed successfully.");
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

    /**
     * Test that EJB field and method injection occurs properly on an EJB 3.0
     * CMT Stateful Session EJB, Business Interface.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Stateful Session bean may be created.
     * <li>Verify SLSB field injection occurs properly.
     * <li>Verify SLSB field injection does not occur after field cleared in
     * instance.
     * <li>Verify SLSB method injection occurs properly.
     * <li>Verify SLSB method injection does not occur after field cleared in
     * instance.
     * </ol>
     */
    @Test
    public void testSFSBInjectionBizIntAnn() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        EJBInjectionRemote bean = (EJBInjectionRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                                                EJBInjectionRemoteInterface, Module,
                                                                                                AdvCompBean);
        assertNotNull("1 ---> SLLSB created successfully.", bean);
        try {
            bean.verifyEJBFieldInjection();
            bean.verifyNoEJBFieldInjection();
            bean.verifyEJBMethodInjection();
            bean.verifyNoEJBMethodInjection();
        } catch (Throwable t) {
            FATHelper.checkForAssertion(t);
        }
    }

}
