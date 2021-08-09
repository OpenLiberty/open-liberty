/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.web;

import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_NO_TRANSACTION;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.naming.NameNotFoundException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.AdvCMTStatelessRemote;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.AnnotatedCMTStatelessRemote;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.AnnotatedCMTStatelessRemote2;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.BasicCMTStatelessRemote;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.BasicCMTStatelessRemote2;
import com.ibm.ws.ejbcontainer.remote.ejb3session.sl.ann.ejb.UserTransactionRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> RemoteBusinessInterfaceTest .
 *
 * <dt><b>Test Author:</b> Jim Krueger (remote-ified by Urrvano Gamez, Jr. <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Basic EJB 3.0
 * Container Managed Stateless Session bean functionality with
 * combinations of Remote Business Interfaces. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testTxAttribsOnEJBs - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes. Business interface
 * defined on <code> @Remote </code> on EJB class.
 * <li>testTxAttribsOnInt - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes. Business interface
 * defined with <code> @Remote </code> on interface class
 * itself and on implements clause of EJB class.
 * <li>testTxAttribsOnIntAndPOJOs - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes. Business interface
 * defined with <code> @Remote </code> on interface class
 * and is listed on the implements clause with other pojo interfaces.
 * <li>testTxAttribsOnIntNoPOJOs - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes. Business interface
 * defined with <code> @Remote </code> on interface class
 * and is listed on the implements clause with no other pojo interfaces.
 * <li>testTxAttribsAnnotationsOnInt - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes. Remote Business interface defined via
 * annotation on interface class, with
 * Serializable, Externalizable, and an interface from the
 * javax.ejb package on implements clause of bean class.
 * <li>testTxAttribsNoAnnOnBean - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes. No annotation on bean class,
 * Remote annotation on 2 interface classes, 2 business interfaces
 * specified on implements clause of bean class.
 * <li>testTxAttribsNoIntSpecified - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes. Remote annotation on bean class
 * with no interfaces specified, Remote annotation on interface
 * class which is specified on implements clause of bean class.
 * <li>testTxAttribs1Int1POJO - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes. Remote annotation on bean class
 * with one interface specified, no annotation on interface class,
 * one different POJO interface specified on implements clause
 * of bean class.
 * <li>testTxAttribs2Ints1POJO - Business Interface: Verify methods with all
 * ContainerManaged Tx Attributes. Remote annotation on bean class
 * with two interfaces specified, no annotation on interface class,
 * one different POJO interface specified on implements clause of
 * bean class.
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/RemoteBusinessInterfaceServlet")
public class RemoteBusinessInterfaceServlet extends FATServlet {
    /**
     * Definitions for the logger
     */
    private final static String CLASSNAME = RemoteBusinessInterfaceServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Name of module... for lookup.
    private static final String Module = "StatelessAnnEJB";

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB, (remote) Business Interface, with each of the different Transaction Attributes.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Remote Business interfaces can be defined on annotation on bean class.
     * </ol>
     */
    @Test
    public void testTxAttribsOnEJBs() throws Exception {
        UserTransaction userTran = null;

        try {
            // --------------------------------------------------------------------
            // Locate SL Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            String beanName = "AnnotatedCMTStatelessRemoteBean";
            String interfaceName = BasicCMTStatelessRemote.class.getName();

            BasicCMTStatelessRemote bean = (BasicCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName); // F379-549fvtFrw
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
        } finally {
            if (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)
                userTran.rollback();
        }
    }

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB, (remote) Business Interface, with each of the different Transaction Attributes. The (remote) Business
     * Interface is defined by
     * an annotation on the interface class itself and listed on the implements clause of the bean class.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Remote business interface defined on interface class.
     * </ol>
     */
    @Test
    public void testTxAttribsOnInt() throws Exception {
        UserTransaction userTran = null;

        try {
            String beanName = "AnnotatedCMTStatelessRemoteBean";
            String interfaceName = AnnotatedCMTStatelessRemote.class.getName();
            AnnotatedCMTStatelessRemote bean = (AnnotatedCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName); // F379-549fvtFrw
            assertNotNull("1 ---> SLRSB created successfully.", bean);

            bean.tx_ADefault();
            bean.tx_ARequired();
            bean.tx_ANotSupported();
            bean.tx_ARequiresNew();
            bean.tx_ASupports();
            bean.tx_ANever();
            userTran = FATHelper.lookupUserTransaction();
            svLogger.info("Beginning User Transaction ...");
            userTran.begin();
            bean.tx_AMandatory();
            svLogger.info("Committing User Transaction ...");
            userTran.commit();
        } finally {
            if (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)
                userTran.rollback();
        }
    }

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB, (remote) Business Interface, with each of the different Transaction Attributes. The (remote) Business
     * Interface is defined by
     * an annotation on the interface class itself and listed on the implements clause of the bean class along with another pojo interface.
     *
     * This test will confirm the following :
     * <ol>
     * <li>Non-annotated interfaces on the bean class's implements clause will be ignored when multiple interfaces exist.
     * </ol>
     */
    @Test
    public void testTxAttribsOnIntAndPOJOs() throws Exception {
        UserTransaction userTran = null;

        try {
            // --------------------------------------------------------------------
            // Locate SL Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            String beanName = "TwoIntOnImplStatelessRemoteBean";
            String interfaceName = AnnotatedCMTStatelessRemote.class.getName();
            AnnotatedCMTStatelessRemote bean = (AnnotatedCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName); // F379-549fvtFrw
            assertNotNull("1 ---> SLRSB created successfully.", bean);
            bean.tx_ADefault();
            bean.tx_ARequired();
            bean.tx_ANotSupported();
            bean.tx_ARequiresNew();
            bean.tx_ASupports();
            bean.tx_ANever();
            userTran = FATHelper.lookupUserTransaction();
            svLogger.info("Beginning User Transaction ...");
            userTran.begin();
            bean.tx_AMandatory();
            svLogger.info("Committing User Transaction ...");
            userTran.commit();
        } finally {
            if (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)
                userTran.rollback();
        }
    }

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB, Business Interface, with each of the different Transaction Attributes. The (remote) Business Interface is
     * defined by an
     * annotation on the interface class itself and listed on the implements clause of the bean class.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Remote business interfaces can be defined with an annotation on the interface class.
     * </ol>
     */
    @Test
    public void testTxAttribsOnIntNoPOJOs() throws Exception {
        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        try {
            String beanName = "OneIntOnImplStatelessRemoteBean";
            String interfaceName = AnnotatedCMTStatelessRemote.class.getName();
            AnnotatedCMTStatelessRemote bean = (AnnotatedCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName); // F379-549fvtFrw
            assertNotNull("1 ---> SLRSB created successfully.", bean);
            bean.tx_ADefault();
            bean.tx_ARequired();
            bean.tx_ANotSupported();
            bean.tx_ARequiresNew();
            bean.tx_ASupports();
            bean.tx_ANever();
            userTran = FATHelper.lookupUserTransaction();
            svLogger.info("Beginning User Transaction ...");
            userTran.begin();
            bean.tx_AMandatory();
            svLogger.info("Committing User Transaction ...");
            userTran.commit();
        } finally {
            if (userTran != null && userTran.getStatus() != STATUS_NO_TRANSACTION && userTran.getStatus() != STATUS_COMMITTED)
                userTran.rollback();
        }
    }

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB, (remote) Business Interface, with each of the different Transaction Attributes. The Business Interface is
     * defined by the
     * Remote annotation on the interface class. However, Serializable, Externalizable, and javax.ejb.Handle are also on the implements clause.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Serializable, Externalizable and interfaces from the javax.ejb package will be ignored and not considered to be a business interface.
     * </ol>
     */
    @Test
    public void testTxAttribsAnnotationsOnInt() throws Exception {
        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        String beanName = "OneAnnotatedWithSpecialIntsStatelessRemoteBean";
        String interfaceName = AnnotatedCMTStatelessRemote.class.getName();
        AnnotatedCMTStatelessRemote bean = (AnnotatedCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName); // F379-549fvtFrw
        assertNotNull("1 ---> SLRSB created successfully.", bean);
        bean.tx_ADefault();
        bean.tx_ARequired();
        bean.tx_ANotSupported();
        bean.tx_ARequiresNew();
        bean.tx_ASupports();
        bean.tx_ANever();
        userTran = FATHelper.lookupUserTransaction();
        svLogger.info("Beginning User Transaction ...");
        userTran.begin();
        bean.tx_AMandatory();
        svLogger.info("Committing User Transaction ...");
        userTran.commit();
    }

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB, (remote)Business Interface, with each of the different Transaction Attributes. No annotation on bean
     * class, @Remote annotation
     * on 2 interface classes, 2 business interfaces specified on implements clause of bean class.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Multiple annotated interfaces on the implements clause of a bean class will be considered to be valid remote business interfaces.
     * </ol>
     */
    @Test
    public void testTxAttribsNoAnnOnBean() throws Exception {
        UserTransaction userTran = null;
        String beanName = "TwoAnnotatedIntCMTStatelessRemoteBean";
        String interfaceName = AnnotatedCMTStatelessRemote.class.getName();
        AnnotatedCMTStatelessRemote bean = (AnnotatedCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName); // F379-549fvtFrw
        assertNotNull("1 ---> SLRSB created successfully.", bean);
        bean.tx_ADefault();
        bean.tx_ARequired();
        bean.tx_ANotSupported();
        bean.tx_ARequiresNew();
        bean.tx_ASupports();
        bean.tx_ANever();
        userTran = FATHelper.lookupUserTransaction();
        svLogger.info("Beginning User Transaction ...");
        userTran.begin();
        bean.tx_AMandatory();
        svLogger.info("Committing User Transaction ...");
        userTran.commit();

        // Now check second business interface.
        interfaceName = AnnotatedCMTStatelessRemote2.class.getName();
        AnnotatedCMTStatelessRemote2 bean2 = (AnnotatedCMTStatelessRemote2) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName); // F379-549fvtFrw
        assertNotNull("3 ---> SLRSB created successfully.", bean2);
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
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB, (remote) Business Interface, with each of the different Transaction Attributes. Remote annotation on bean
     * class with no
     * interfaces specified, Remote annotation on interface class which is specified on implements clause of bean class.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>The same remote business interface can be defined via annotations on the bean class and the interface without problems.
     * </ol>
     */
    @Test
    public void testTxAttribsNoIntSpecified() throws Exception {
        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        String beanName = "EmptyRemoteWithAnnotatedIntStatelessRemoteBean";
        String interfaceName = AnnotatedCMTStatelessRemote.class.getName();
        AnnotatedCMTStatelessRemote bean = (AnnotatedCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName); // F379-549fvtFrw
        assertNotNull("1 ---> SLRSB created successfully.", bean);
        bean.tx_ADefault();
        bean.tx_ARequired();
        bean.tx_ANotSupported();
        bean.tx_ARequiresNew();
        bean.tx_ASupports();
        bean.tx_ANever();
        userTran = FATHelper.lookupUserTransaction();
        svLogger.info("Beginning User Transaction ...");
        userTran.begin();
        bean.tx_AMandatory();
        svLogger.info("Committing User Transaction ...");
        userTran.commit();
    }

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB, (remote) Business Interface, with each of the different Transaction Attributes. Remote annotation on bean
     * class with one
     * interface specified, no annotation on interface class, one different POJO interface specified on implements clause of bean class.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Single pojo interfaces on implements clause will be ignored when a remote business interface has been defined on the annotation.
     * </ol>
     */
    @Test
    public void testTxAttribs1Int1POJO() throws Exception {
        UserTransaction userTran = null;

        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        String beanName = "OneNonBusPojoStatelessRemoteBean";
        String interfaceName = AnnotatedCMTStatelessRemote.class.getName();
        AnnotatedCMTStatelessRemote bean = (AnnotatedCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName);
        assertNotNull("1 ---> SLRSB created successfully.", bean);
        bean.tx_ADefault();
        bean.tx_ARequired();
        bean.tx_ANotSupported();
        bean.tx_ARequiresNew();
        bean.tx_ASupports();
        bean.tx_ANever();

        try {
            userTran = FATHelper.lookupUserTransaction();
            svLogger.info("Beginning User Transaction ...");
            userTran.begin();
            bean.tx_AMandatory();
            svLogger.info("Committing User Transaction ...");
            userTran.commit();
            interfaceName = BasicCMTStatelessRemote.class.getName();
            @SuppressWarnings("unused")
            BasicCMTStatelessRemote bean2 = (BasicCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName); // F379-549fvtFrw
            // TODO: Should this be a different error for REMOTE ?
            fail("3 ---> expected NameNotFoundException was not thrown.");
        } catch (NameNotFoundException ex) {
            svLogger.info("3 ---> expected NameNotFoundException occured.");
        }
    }

    /**
     * Test calling methods on a Remote EJB 3.0 CMT Stateless Session EJB, (remote) Business Interface, with each of the different Transaction Attributes. Remote annotation on bean
     * class with two
     * interfaces specified, no annotation on interface class, one different POJO interface specified on implements clause of bean class.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>Multiple business interfaces on a Remote annotation will be usable.
     * <li>Non-annotated pojo interface will not be considered to be a Remote or Local interface if other Remote interfaces are annotated.
     * </ol>
     */
    @Test
    public void testTxAttribs2Ints1POJO() throws Exception {
        String beanName = null;
        String interfaceName = null;
        beanName = "TwoOnAnnotOneOnImplStatelessRemoteBean";
        interfaceName = BasicCMTStatelessRemote.class.getName();
        @SuppressWarnings("unused")
        BasicCMTStatelessRemote bean = (BasicCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName); // F379-549fvtFrw
        svLogger.info("1 ---> BasicCMTStatelessRemote business interface found.");

        // --------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        beanName = "TwoOnAnnotOneOnImplStatelessRemoteBean";
        interfaceName = AdvCMTStatelessRemote.class.getName();
        @SuppressWarnings("unused")
        AdvCMTStatelessRemote bean2 = (AdvCMTStatelessRemote) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName); // F379-549fvtFrw
        svLogger.info("2 ---> AdvCMTStatelessRemote business interface found.");

        try {
            // --------------------------------------------------------------------
            // Locate SL Remote Home/Factory and execute the test
            // --------------------------------------------------------------------
            beanName = "TwoOnAnnotOneOnImplStatelessRemoteBean";
            interfaceName = BasicCMTStatelessRemote2.class.getName();
            @SuppressWarnings("unused")
            BasicCMTStatelessRemote2 bean3 = (BasicCMTStatelessRemote2) FATHelper.lookupDefaultBindingEJBJavaApp(interfaceName, Module, beanName);
            fail("3 ---> expected NameNotFoundException was not thrown.");
        } catch (NameNotFoundException ex) {
            svLogger.info("3 ---> expected NameNotFoundException occured.");
        }
    }

    /**
     * Ensure that an EJB UserTransaction object can be serialized and
     * deserialized by the ORB when passed on a remote interface.
     */
    @Test
    public void testUserTransactionSerialization() throws Exception {
        UserTransactionRemote bean = (UserTransactionRemote) FATHelper.lookupDefaultBindingEJBJavaApp(UserTransactionRemote.class.getName(), Module, "UserTransactionBean");
        bean.test();
    }
}