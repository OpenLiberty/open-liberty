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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.ejb.NoSuchEJBException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb.InitEJBRemote;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb.InitEJBRemoteHome;
import com.ibm.ws.ejbcontainer.remote.fat.xml.sf.ejb.InitRemote;

import componenttest.app.FATServlet;

/**
 * Tests EJB Container support for the Init/ejbCreate methods of CMT
 * Stateful Session EJBs.
 * <p>
 *
 * For this test, 3 different 'styles' of beans will be tested:
 * <ol>
 * <li>A Basic EJB 3.0 bean that only contains a business interface.
 * <li>A Component bean, that contains both a business interface and a EJB 2.1
 * style component interface... and implements SessionBean.
 * <li>A Component View bean, that contains both a business interface and an EJB
 * 2.1 style component interface... but does NOT implement SessionBean.
 * </ol>
 *
 * Sub-tests
 * <ul>
 * <li>testInitializeNoSessionBeanImp - Business Interface: Verify initialize
 * methods may be called, and no init/ejbCreate methods.
 * <li>testCreateMethodsWithSessionBeans - Component Interface: Verify various
 * ejbCreate methods will be called, with overlapping names and parameter
 * signatures.
 * <li>testCreateMethodsWithNoSessionBeans - Component View: Verify various Init
 * methods will be called, with overlapping names and parameter signatures.
 * </ul>
 */
@WebServlet("/InitCMTStatefulXMLRemoteServlet")
public class InitCMTStatefulXMLRemoteServlet extends FATServlet {
    private static final long serialVersionUID = 5377944266555928505L;
    private final static String CLASSNAME = InitCMTStatefulXMLRemoteServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Names of application and module... for lookup.
    private static final String Module = "StatefulXMLRemoteEJB";
    private static final String Application = "StatefulXMLRemoteTest";

    // Names of the beans used for the test... for lookup.
    private static final String BasicBean = "InitBasicCMTBean";
    private static final String CompBean = "InitCompCMTBean";
    private static final String CompViewBean = "InitCompViewCMTBean";

    // Names of the interfaces used for the test
    private static final String InitRemoteInterface = InitRemote.class.getName();
    private static final String InitEJBRemoteHomeInterface = InitEJBRemoteHome.class.getName();

    /**
     * Test looking up and calling initialize methods on an EJB 3.0 CMT Stateful
     * Session EJB Business Interface that does NOT implement SessionBean, to
     * insure the instance is created and initialized without calling an
     * ejbCreate/Init method.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>SFSB lookup creates an instance properly.
     * <li>SFSB initialize method may be called and works properly.
     * <li>2nd lookup creates a new instance, not the first one.
     * <li>SFSB initialize method may be called on 2nd instance.
     * <li>State of 1st SFSB is unaffected by second lookup.
     * <li>Stateful Session beans may be removed via designated remove method.
     * </ol>
     *
     * For each 'lookup'/'initialize' method, the test will assert that an
     * object of the correct type is returned, and that the state of that object
     * is correct.
     * <p>
     *
     * For bean removal, the test will check that the remove method returns
     * successfully, and further attempts to access the bean will result in the
     * correct exception.
     * <p>
     */
    @Test
    public void testSFSBInitializeNoSessionBeanImpXML() throws Exception {

        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        InitRemote bean = null;
        InitRemote beans[] = new InitRemote[2];

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (InitRemote) FATHelper.lookupDefaultBindingEJBJavaApp(
                                                                     InitRemoteInterface, Module, BasicBean);
        beans[0] = bean;
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "InitBasicCMTBean", bean.getString());
        assertEquals("3 ---> SFLSB created with proper int state.", 1, bean.getInt());

        // Call one of the 'initialize' methods, and insure the proper state.
        bean.initializeBasic("Bink Rules!");
        assertEquals("4 ---> SFLSB initialized with proper String state.",
                     "InitBasicCMTBean:initializeBasic:Bink Rules!", bean.getString());
        assertEquals("5 ---> SFLSB initialized with proper int state.", 10001,
                     bean.getInt());

        // Create another instance of the bean by looking up the business
        // interface
        // and insure the bean contains the default state.
        InitRemote bean2 = (InitRemote) FATHelper.lookupDefaultBindingEJBJavaApp(InitRemoteInterface,
                                                                                 Module, BasicBean);
        beans[1] = bean2;
        assertNotNull("6 ---> SFLSB 'lookup' successful.", bean2);
        assertEquals("7 ---> SFLSB created with proper String state.",
                     "InitBasicCMTBean", bean2.getString());
        assertEquals("8 ---> SFLSB created with proper int state.", 1, bean2.getInt());

        // Call one of the 'initialize' methods, and insure the proper state.
        bean2.initializeAdv("Tarzan", 255);
        assertEquals("9 ---> SFLSB initialized with proper String state.",
                     "InitBasicCMTBean:initializeAdv:Tarzan", bean2.getString());
        assertEquals("10 --> SFLSB initialized with proper int state.",
                     1000256, bean2.getInt());

        // Insure the proper state of the first bean created
        assertEquals("11 ---> SFLSB initialized with proper String state.",
                     "InitBasicCMTBean:initializeBasic:Bink Rules!", bean.getString());
        assertEquals("12 ---> SFLSB initialized with proper int state.", 10001,
                     bean.getInt());

        // Remove all of the beans created above.
        int numRemoved = 0;
        for (InitRemote rbean : beans) {
            if (rbean != null) {
                rbean.finish("TTFN", 1036);
                try {
                    rbean.getInt();
                } catch (NoSuchEJBException nsejbex) {
                    // Verify the bean really is gone.
                    ++numRemoved;
                }
            }
        }
        assertEquals(
                     "13 --> " + beans.length + " SFLSBs removed successfully.",
                     beans.length, numRemoved);
    }

    /**
     * Test calling create methods on an EJB 3.0 CMT Stateful Session EJB
     * Component Interface Home that implements SessionBean, to insure the
     * proper corresponding ejbCreate method is invoked.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>SFSB default create method works properly.
     * <li>SFSB default create method with 1 parameter works properly.
     * <li>SFSB custom create method with 1 parameter works properly.
     * <li>SFSB custom create method with 1 parameter works properly.
     * <li>SFSB custom create method with 1 parameter of a different type.
     * <li>SFSB custom create method with 2 parameters work properly.
     * <li>Stateful Session beans may be removed via EJBRemoteObject.remove().
     * </ol>
     *
     * For each 'create' method, the test will assert that an object of the
     * correct type is returned, and that the state of that object is correct.
     * <p>
     *
     * This test will confirm the following for the business interface:
     * <ol>
     * <li>SFSB lookup creates an instance properly.
     * <li>SFSB initialize method may be called and works properly.
     * <li>Stateful Session bean may be removed via designated remove method.
     * </ol>
     *
     * For each 'lookup'/'initialize' method, the test will assert that an
     * object of the correct type is returned, and that the state of that object
     * is correct.
     * <p>
     *
     * For bean removal, the test will check that the remove method returns
     * successfully, and further attempts to access the bean will result in the
     * correct exception.
     * <p>
     */
    @Test
    public void testSFSBCreateMethodsWithSessionBeansXML() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        InitEJBRemoteHome sfHome = (InitEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                         InitEJBRemoteHomeInterface, Application, Module,
                                                                                                         CompBean);

        InitEJBRemote bean = null;
        InitEJBRemote beans[] = new InitEJBRemote[6];

        // Invoke default create method, and verify state.
        bean = sfHome.create();
        beans[0] = bean;
        assertNotNull("1 ---> SFLSB 'create()' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "InitCompCMTBean:ejbCreate", bean.getString());
        assertEquals("3 ---> SFLSB created with proper int state.", 11, bean.getInt());

        // Invoke default create method with String parameter, and verify state.
        bean = sfHome.create("Hi Bob!");
        beans[1] = bean;
        assertNotNull("4 ---> SFLSB 'create(String)' successful.", bean);
        assertEquals("5 ---> SFLSB created with proper String state.",
                     "InitCompCMTBean:ejbCreate:Hi Bob!", bean.getString());
        assertEquals("6 ---> SFLSB created with proper int state.", 101, bean.getInt());

        // Invoke custom create method, and verify state.
        bean = sfHome.createDefault();
        beans[2] = bean;
        assertNotNull("7 ---> SFLSB 'createDefault()' successful.", bean);
        assertEquals("8 ---> SFLSB created with proper String state.",
                     "InitCompCMTBean:ejbCreateDefault", bean.getString());
        assertEquals("9 ---> SFLSB created with proper int state.", 1001, bean.getInt());

        // Invoke custom create method with String parameter, and verify state.
        bean = sfHome.createBasic("Hi Scooby!");
        beans[3] = bean;
        assertNotNull("10 --> SFLSB 'createBasic(String)' successful.", bean);
        assertEquals("11 --> SFLSB created with proper String state.",
                     "InitCompCMTBean:ejbCreateBasic:Hi Scooby!", bean.getString());
        assertEquals("12 --> SFLSB created with proper int state.", 10001, bean.getInt());

        // Invoke custom create method with int parameter, and verify state.
        bean = sfHome.createBasic(25);
        beans[4] = bean;
        assertNotNull("13 --> SFLSB 'createBasic(int)' successful.", bean);
        assertEquals("14 --> SFLSB created with proper String state.",
                     "InitCompCMTBean:ejbCreateBasic", bean.getString());
        assertEquals("15 --> SFLSB created with proper int state.", 100026,
                     bean.getInt());

        // Invoke custom create method with 2 parameters, and verify state.
        bean = sfHome.createAdv("Scooby Rocks!", 137);
        beans[5] = bean;
        assertNotNull("16 --> SFLSB 'createAdv(String, int)' successful.", bean);
        assertEquals("17 --> SFLSB created with proper String state.",
                     "InitCompCMTBean:ejbCreateAdv:Scooby Rocks!", bean.getString());
        assertEquals("18 --> SFLSB created with proper int state.", 1000138,
                     bean.getInt());

        // Remove all of the beans created above.
        int numRemoved = 0;
        for (InitEJBRemote rbean : beans) {
            if (rbean != null) {
                rbean.remove();
                try {
                    rbean.getInt();
                } catch (java.rmi.NoSuchObjectException nsoex) {
                    // Verify the bean really is gone.
                    ++numRemoved;
                }
            }
        }
        assertEquals(
                     "19 --> " + beans.length + " SFLSBs removed successfully.",
                     beans.length, numRemoved);

        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        InitRemote bbean = (InitRemote) FATHelper.lookupDefaultBindingEJBJavaApp(InitRemoteInterface,
                                                                                 Module, CompBean);
        assertNotNull("20 --> SFLSB 'lookup' successful.", bbean);
        assertEquals("21 --> SFLSB created with proper String state.",
                     "InitCompCMTBean", bbean.getString());
        assertEquals("22 --> SFLSB created with proper int state.", 1, bbean.getInt());

        // Call one of the 'initialize' methods, and insure the proper state.
        bbean.initializeBasic("Bink Rules!");
        assertEquals("23 --> SFLSB initialized with proper String state.",
                     "InitCompCMTBean:initializeBasic:Bink Rules!", bbean.getString());
        assertEquals("24 --> SFLSB initialized with proper int state.", 10001,
                     bbean.getInt());

        // Remove the bean created above.
        String finalValue = bbean.finish("TTFN", 1036);
        assertEquals(
                     "25 --> SFLSB remove method returned successfully.",
                     "InitCompCMTBean:initializeBasic:Bink Rules!:finish:TTFN:11040",
                     finalValue);

        // Confirm the bean is really gone, and correct exception is returned
        try {
            bbean.getInt();
            fail("26 --> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("26 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }

    }

    /**
     * Test calling create methods on an EJB 3.0 CMT Stateful Session EJB
     * Component Interface Home that does NOT implement SessionBean, to insure
     * the proper corresponding Init method is invoked.
     * <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li>SFSB default create method works properly.
     * <li>SFSB default create method with 1 parameter works properly.
     * <li>SFSB custom create method with 1 parameter works properly.
     * <li>SFSB custom create method with 1 parameter works properly.
     * <li>SFSB custom create method with 1 parameter of a different type.
     * <li>SFSB custom create method with 2 parameters work properly.
     * <li>SFSB custom create method with 1 parameter, that shares an init
     * method with another create works properly.
     * <li>Stateful Session bean may be removed via EJBRemoteObject.remove().
     * </ol>
     *
     * For each 'create' method, the test will assert that an object of the
     * correct type is returned, and that the state of that object is correct.
     * <p>
     *
     * This test will confirm the following for the business interface:
     * <ol>
     * <li>SFSB lookup creates an instance properly.
     * <li>SFSB initialize method may be called and works properly.
     * <li>Stateful Session bean may be removed via designated remove method.
     * </ol>
     *
     * For each 'lookup'/'initialize' method, the test will assert that an
     * object of the correct type is returned, and that the state of that object
     * is correct.
     * <p>
     *
     * For bean removal, the test will check that the remove method returns
     * successfully, and further attempts to access the bean will result in the
     * correct exception.
     * <p>
     */
    @Test
    public void testSFSBCreateMethodsWithNoSessionBeansXML() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        InitEJBRemoteHome sfHome = (InitEJBRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(
                                                                                                         InitEJBRemoteHomeInterface, Application, Module,
                                                                                                         CompViewBean);

        InitEJBRemote bean = null;
        InitEJBRemote beans[] = new InitEJBRemote[7];

        // Invoke default create method, and verify state.
        bean = sfHome.create();
        beans[0] = bean;
        assertNotNull("1 ---> SFLSB 'create()' successful.", bean);
        assertEquals("2 ---> SFLSB created with proper String state.",
                     "InitCompViewCMTBean:initialize", bean.getString());
        assertEquals("3 ---> SFLSB created with proper int state.", 11, bean.getInt());

        // Invoke default create method with String parameter, and verify state.
        bean = sfHome.create("Hi Bob!");
        beans[1] = bean;
        assertNotNull("4 ---> SFLSB 'create(String)' successful.", bean);
        assertEquals("5 ---> SFLSB created with proper String state.",
                     "InitCompViewCMTBean:initialize:Hi Bob!", bean.getString());
        assertEquals("6 ---> SFLSB created with proper int state.", 101, bean.getInt());

        // Invoke custom create method, and verify state.
        bean = sfHome.createDefault();
        beans[2] = bean;
        assertNotNull("7 ---> SFLSB 'createDefault()' successful.", bean);
        assertEquals("8 ---> SFLSB created with proper String state.",
                     "InitCompViewCMTBean:initializeDefault", bean.getString());
        assertEquals("9 ---> SFLSB created with proper int state.", 1001, bean.getInt());

        // Invoke custom create method with String parameter, and verify state.
        bean = sfHome.createBasic("Hi Scooby!");
        beans[3] = bean;
        assertNotNull("10 --> SFLSB 'createBasic(String)' successful.", bean);
        assertEquals("11 --> SFLSB created with proper String state.",
                     "InitCompViewCMTBean:initializeBasic:Hi Scooby!", bean.getString());
        assertEquals("12 --> SFLSB created with proper int state.", 10001, bean.getInt());

        // Invoke custom create method with int parameter, and verify state.
        bean = sfHome.createBasic(25);
        beans[4] = bean;
        assertNotNull("13 --> SFLSB 'createBasic(int)' successful.", bean);
        assertEquals("14 --> SFLSB created with proper String state.",
                     "InitCompViewCMTBean:initializeBasic", bean.getString());
        assertEquals("15 --> SFLSB created with proper int state.", 100026,
                     bean.getInt());

        // Invoke custom create method with 2 parameters, and verify state.
        bean = sfHome.createAdv("Scooby Rocks!", 137);
        beans[5] = bean;
        assertNotNull("16 --> SFLSB 'createAdv(String, int)' successful.", bean);
        assertEquals("17 --> SFLSB created with proper String state.",
                     "InitCompViewCMTBean:ejbCreateAdv:Scooby Rocks!", bean.getString());
        assertEquals("18 --> SFLSB created with proper int state.", 1000138,
                     bean.getInt());

        // Invoke custom create method with 1 parameters that shares
        // the 'init method' with another create, and verify state.
        bean = sfHome.createDup("Apple Butter?");
        beans[6] = bean;
        assertNotNull("19 --> SFLSB 'createDup(String)' successful.", bean);
        assertEquals("20 --> SFLSB created with proper String state.",
                     "InitCompViewCMTBean:initialize:Apple Butter?", bean.getString());
        assertEquals("21 --> SFLSB created with proper int state.", 101, bean.getInt());

        // Remove all of the beans created above.
        int numRemoved = 0;
        for (InitEJBRemote rbean : beans) {
            if (rbean != null) {
                rbean.remove();
                try {
                    rbean.getInt();
                } catch (java.rmi.NoSuchObjectException nsoex) {
                    // Verify the bean really is gone.
                    ++numRemoved;
                }
            }
        }
        assertEquals(
                     "22 --> " + beans.length + " SFLSBs removed successfully.",
                     beans.length, numRemoved);

        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        InitRemote bbean = (InitRemote) FATHelper.lookupDefaultBindingEJBJavaApp(InitRemoteInterface,
                                                                                 Module, CompViewBean);
        assertNotNull("23 --> SFLSB 'lookup' successful.", bbean);
        assertEquals("24 --> SFLSB created with proper String state.",
                     "InitCompViewCMTBean", bbean.getString());
        assertEquals("25 --> SFLSB created with proper int state.", 1, bbean.getInt());

        // Call one of the 'initialize' methods, and insure the proper state.
        bbean.initializeBasic("Bink Rules!");
        assertEquals("26 --> SFLSB initialized with proper String state.",
                     "InitCompViewCMTBean:initializeBasic:Bink Rules!", bbean.getString());
        assertEquals("27 --> SFLSB initialized with proper int state.", 10001,
                     bbean.getInt());

        // Remove the bean created above.
        String finalValue = bbean.finish("TTFN", 1036);
        assertEquals(
                     "28 --> SFLSB remove method returned successfully.",
                     "InitCompViewCMTBean:initializeBasic:Bink Rules!:finish:TTFN:11040",
                     finalValue);

        // Confirm the bean is really gone, and correct exception is returned
        try {
            bbean.getInt();
            fail("29 --> SFLSB was not really removed.");
        } catch (NoSuchEJBException nsejbex) {
            svLogger.info("29 --> SFLSB remove was successful; "
                          + "NoSuchEJBException occured invoking removed bean.");
        }
    }

}
