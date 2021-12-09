/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb1x.base.spec.sfr.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb1x.base.spec.sfr.ejb.SFRa;
import com.ibm.ejb1x.base.spec.sfr.ejb.SFRaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SFRemoteImplLifecycleMethodTest (formerly WSTestSFR_IITest)
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>I____ - Bean Implementation;
 * <li>IIA__ - Session Context - IllegalAccess.
 * </ul>
 *
 * <dt>Command options:
 * <dd>
 * <TABLE width="100%">
 * <COL span="1" width="25%" align="left"> <COL span="1" align="left">
 * <TBODY>
 * <TR> <TH>Option</TH> <TH>Description</TH> </TR>
 * <TR> <TD>None</TD>
 * <TD></TD>
 * </TR>
 * </TBODY>
 * </TABLE>
 *
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>iia01 - CMT - constructor
 * <li>iia02 - CMT - set[Session,Entity]Context()
 * <li>iia03 - CMT - unset[Session,Entity]Context()
 * <li>iia04 - CMT - ejbCreate()
 * <li>iia05 - CMT - ejbPostCreate()
 * <li>iia06 - CMT - ejbRemove()
 * <li>iia07 - CMT - ejbActivate()
 * <li>iia08 - CMT - ejbPassivate()
 * <li>iia09 - CMT - business methods()
 * <li>iia10 - CMT - ejbFind()
 * <li>iia11 - CMT - ejbHome()
 * <li>iia12 - CMT - ejbLoad()
 * <li>iia13 - CMT - ejbStore()
 * <li>iia14 - BMT - constructor
 * <li>iia15 - BMT - set[Session,Entity]Context()
 * <li>iia16 - BMT - unset[Session,Entity]Context()
 * <li>iia17 - BMT - ejbCreate()
 * <li>iia18 - BMT - ejbPostCreate()
 * <li>iia19 - BMT - ejbRemove()
 * <li>iia20 - BMT - ejbActivate()
 * <li>iia21 - BMT - ejbPassivate()
 * <li>iia22 - BMT - business methods()
 * <li>iia23 - BMT - ejbFind()
 * <li>iia24 - BMT - ejbHome()
 * <li>iia25 - BMT - ejbLoad()
 * <li>iia26 - BMT - ejbStore()
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/SFRemoteImplLifecycleMethodServlet")
public class SFRemoteImplLifecycleMethodServlet extends FATServlet {
    private final static String CLASS_NAME = SFRemoteImplLifecycleMethodServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb1x/base/spec/sfr/ejb/SFRaBMTHome";
    private final static String ejbJndiName2 = "com/ibm/ejb1x/base/spec/sfr/ejb/SFRaCMTHome";
    private final static String ejbJndiName3 = "com/ibm/ejb1x/base/spec/sfr/ejb/SFRaBMTActivateTranHome";
    private final static String ejbJndiName4 = "com/ibm/ejb1x/base/spec/sfr/ejb/SFRaCMTActivateTranHome";
    private static SFRaHome fhome1;
    private static SFRaHome fhome2;
    private static SFRaHome fhome3;
    private static SFRaHome fhome4;
    private static SFRa fejb1;
    private static SFRa fejb2;

    @PostConstruct
    public void initializeHomes() {
        try {

            fhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName1, SFRaHome.class);
            fhome2 = FATHelper.lookupRemoteHomeBinding(ejbJndiName2, SFRaHome.class);
            fhome3 = FATHelper.lookupRemoteHomeBinding(ejbJndiName3, SFRaHome.class);
            fhome4 = FATHelper.lookupRemoteHomeBinding(ejbJndiName4, SFRaHome.class);
            fejb1 = fhome1.create();
            fejb2 = fhome2.create();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroyBeans() {
        try {

            if (fejb1 != null)
                fejb1.remove();
            if (fejb2 != null)
                fejb2.remove();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (iia01) Test Stateful CMT lifecycle constructor method.
     */
    @Test
    public void test1XSFCMTLifecycle_constructor() throws Exception {
        // Exception will be thrown if constructor not called properly
        fejb2.verify_constructor();
    }

    /**
     * (iia02) Test Stateful CMT lifecycle setSessionContext() method.
     */
    @Test
    public void test1XSFCMTLifecycle_setSessionContext() throws Exception {
        // Exception will be thrown if setSessionContext not called properly
        fejb2.verify_setContext();
    }

    /**
     * (iia03) Test Stateful CMT lifecycle unsetSessionContext() method.
     */
    //@Test
    public void test1XSFCMTLifecycle_unsetSessionContext() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia04) Test Stateful CMT lifecycle ejbCreate() method.
     */
    @Test
    public void test1XSFCMTLifecycle_ejbCreate() throws Exception {
        // Exception will be thrown if ejbCreate not called properly
        fejb2.verify_ejbCreate(false); // no argument create

        // Also verify ejbCreate with arguments
        SFRa ejb = fhome2.create(true, (byte) 9, 'C', (short) 0, 0, 0, (float) 0.0, 0.0, "String stringValue");
        assertNotNull("Create EJB was null.", ejb);

        ejb.verify_ejbCreate(true);
        ejb.remove();
    }

    /**
     * (iia05) Test Stateful CMT lifecycle ejbPostCreate() method.
     */
    //@Test
    public void test1XSFCMTLifecycle_ejbPostCreate() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia06) Test Stateful CMT lifecycle ejbRemove() method.
     */
    @Test
    public void test1XSFCMTLifecycle_ejbRemove() throws Exception {
        SFRa ejb1 = fhome2.create();
        assertNotNull("Create EJB was null.", ejb1);
        SFRa ejb2 = fhome2.create();
        assertNotNull("Create EJB was null.", ejb2);

        ejb1.remove();
        ejb1 = null;
        ejb2.verify_ejbRemove();
        ejb2.remove();
    }

    /**
     * (iia07) Test Stateful CMT lifecycle ejbActivate() method.
     */
    @Test
    public void test1XSFCMTLifecycle_ejbActivate() throws Exception {
        SFRa ejb = fhome4.create();
        assertNotNull("Create EJB was null.", ejb);

        ejb.verify_ejbActivate();
        ejb.remove();
    }

    /**
     * (iia08) Test Stateful CMT lifecycle ejbPassivate() method.
     */
    @Test
    public void test1XSFCMTLifecycle_ejbPassivate() throws Exception {
        SFRa ejb = fhome4.create(true, (byte) 9, 'C', (short) 0, 0, 0, (float) 0.0, 0.0, "String stringValue");
        assertNotNull("Create EJB was null.", ejb);

        ejb.verify_ejbPassivate();
        ejb.remove();
    }

    /**
     * (iia09) Test Stateful CMT business method.
     */
    @Test
    public void test1XSFCMTBusinessMethod() throws Exception {
        String testStr = "Test string.";
        String buf = fejb2.method1(testStr);
        assertEquals("Method call (method1) test was unexpected value.", buf, testStr);
    }

    /**
     * (iia10) Test Stateful CMT lifecycle ejbFind() method.
     */
    //@Test
    public void test1XSFCMTLifecycle_ejbFind() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia11) Test Stateful CMT lifecycle ejbHome() method.
     */
    //@Test
    public void test1XSFCMTLifecycle_ejbHome() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia12) Test Stateful CMT lifecycle ejbLoad() method.
     */
    //@Test
    public void test1XSFCMTLifecycle_ejbLoad() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia13) Test Stateful CMT lifecycle ejbStore() method.
     */
    //@Test
    public void test1XSFCMTLifecycle_ejbStore() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia14) Test Stateful BMT lifecycle constructor method.
     */
    @Test
    public void test1XSFBMTLifecycle_constructor() throws Exception {
        // Exception will be thrown if constructor not called properly
        fejb1.verify_constructor();
    }

    /**
     * (iia15) Test Stateful BMT lifecycle setSessionContext() method.
     */
    @Test
    public void test1XSFBMTLifecycle_setSessionContext() throws Exception {
        // Exception will be thrown if setSessionContext not called properly
        fejb1.verify_setContext();
    }

    /**
     * (iia16) Test Stateful BMT lifecycle unsetSessionContext() method.
     */
    //@Test
    public void test1XSFBMTLifecycle_unsetSessionContext() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia17) Test Stateful BMT lifecycle ejbCreate() method.
     */
    @Test
    public void test1XSFBMTLifecycle_ejbCreate() throws Exception {
        // Exception will be thrown if ejbCreate not called properly
        fejb1.verify_ejbCreate(false); // no argument create

        // Also verify ejbCreate with arguments
        SFRa ejb = fhome1.create(true, (byte) 9, 'C', (short) 0, 0, 0, (float) 0.0, 0.0, "String stringValue");
        assertNotNull("Create EJB was null.", ejb);

        ejb.verify_ejbCreate(true);
        ejb.remove();
    }

    /**
     * (iia18) Test Stateful BMT lifecycle ejbPostCreate() method.
     */
    //@Test
    public void test1XSFBMTLifecycle_ejbPostCreate() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia19) Test Stateful BMT lifecycle ejbRemove() method.
     */
    @Test
    public void test1XSFBMTLifecycle_ejbRemove() throws Exception {
        SFRa ejb1 = fhome1.create();
        assertNotNull("Create EJB was null.", ejb1);
        SFRa ejb2 = fhome1.create();
        assertNotNull("Create EJB was null.", ejb2);

        ejb1.remove();
        ejb1 = null;
        ejb2.verify_ejbRemove();
        ejb2.remove();
    }

    /**
     * (iia20) Test Stateful BMT lifecycle ejbActivate() method.
     */
    @Test
    @AllowedFFDC("java.lang.IllegalStateException")
    public void test1XSFBMTLifecycle_ejbActivate() throws Exception {
        SFRa ejb = fhome3.create();
        assertNotNull("Create EJB was null.", ejb);

        try {
            ejb.verify_ejbActivate();
            ejb.remove();
            fail("    ejbActivate was called unexpectedly.");
        } catch (RemoteException rex) {
            // EJB 1.x BMT does not use a local tran... thus never passivate
            // at the end of a tran
            Throwable cause = rex.getCause().getCause();
            if (!(cause instanceof IllegalStateException)) {
                throw new RuntimeException("Incorrect cause : " + cause, cause);
            }
            svLogger.info("Caught expected IllegalSateException");
        }

        ejb = fhome3.create();
        assertNotNull("   Create EJB.", ejb);
        ejb.getBooleanValue();
        try {
            ejb.verify_ejbActivate();
            ejb.remove();
            fail("    ejbActivate was called unexpectedly.");
        } catch (RemoteException rex) {
            // EJB 1.x BMT does not use a local tran... thus never passivate
            // at the end of a tran
            Throwable cause = rex.getCause().getCause();
            if (!(cause instanceof IllegalStateException)) {
                throw new RuntimeException("Incorrect cause : " + cause, cause);
            }
            svLogger.info("Caught expected IllegalSateException");
        }
    }

    /**
     * (iia21) Test Stateful BMT lifecycle ejbPassivate() method.
     */
    @Test
    @AllowedFFDC("java.lang.IllegalStateException")
    public void test1XSFBMTLifecycle_ejbPassivate() throws Exception {
        SFRa ejb = fhome3.create(true, (byte) 9, 'C', (short) 0, 0, 0, (float) 0.0, 0.0, "String stringValue");
        assertNotNull("Create EJB was null.", ejb);

        try {
            ejb.verify_ejbPassivate();
            ejb.remove();
            fail("    ejbPassivate was called unexpectedly.");
        } catch (RemoteException rex) {
            // EJB 1.x BMT does not use a local tran... thus never passivate
            // at the end of a tran
            Throwable cause = rex.getCause().getCause();
            if (!(cause instanceof IllegalStateException)) {
                throw new RuntimeException("Incorrect cause : " + cause, cause);
            }
            svLogger.info("Caught expected IllegalSateException");
        }

        ejb = fhome3.create(true, (byte) 9, 'C', (short) 0, 0, 0, (float) 0.0, 0.0, "String stringValue");
        assertNotNull("   Create EJB.", ejb);
        ejb.getBooleanValue();
        try {
            ejb.verify_ejbPassivate();
            ejb.remove();
            fail("    ejbPassivate was called unexpectedly.");
        } catch (RemoteException rex) {
            // EJB 1.x BMT does not use a local tran... thus never passivate
            // at the end of a tran
            Throwable cause = rex.getCause().getCause();
            if (!(cause instanceof IllegalStateException)) {
                throw new RuntimeException("Incorrect cause : " + cause, cause);
            }
            svLogger.info("Caught expected IllegalSateException");
        }
    }

    /**
     * (iia22) Test Stateful BMT business method.
     */
    @Test
    public void test1XSFBMTBusinessMethod() throws Exception {
        String testStr = "Test string.";
        String buf = fejb1.method1(testStr);
        assertEquals("Method call (method1) test was unexpected value.", buf, testStr);
    }

    /**
     * (iia23) Test Stateful BMT lifecycle ejbFind() method.
     */
    //@Test
    public void test1XSFBMTLifecycle_ejbFind() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia24) Test Stateful BMT lifecycle ejbHome() method.
     */
    //@Test
    public void test1XSFBMTLifecycle_ejbHome() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia25) Test Stateful BMT lifecycle ejbLoad() method.
     */
    //@Test
    public void test1XSFBMTLifecycle_ejbLoad() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia26) Test Stateful BMT lifecycle ejbStore() method.
     */
    //@Test
    public void test1XSFBMTLifecycle_ejbStore() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }
}
