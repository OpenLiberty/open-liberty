/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.spec.sfl.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.sfl.ejb.SFLa;
import com.ibm.ejb2x.base.spec.sfl.ejb.SFLaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SFLocalImplLifecycleMethodTest (formerly WSTestSFL_IITest)
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
@WebServlet("/SFLocalImplLifecycleMethodServlet")
public class SFLocalImplLifecycleMethodServlet extends FATServlet {

    private final static String CLASS_NAME = SFLocalImplLifecycleMethodServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/sfl/ejb/SFLaBMTHome";
    private final static String ejbJndiName2 = "com/ibm/ejb2x/base/spec/sfl/ejb/SFLaCMTHome";
    private final static String ejbJndiName3 = "com/ibm/ejb2x/base/spec/sfl/ejb/SFLaBMTActivateTranHome";
    private final static String ejbJndiName4 = "com/ibm/ejb2x/base/spec/sfl/ejb/SFLaCMTActivateTranHome";
    private SFLaHome fhome1;
    private SFLaHome fhome2;
    private SFLaHome fhome3;
    private SFLaHome fhome4;

    @PostConstruct
    private void initializeHomes() {
        try {
            fhome1 = (SFLaHome) FATHelper.lookupLocalHome(ejbJndiName1);
            fhome2 = (SFLaHome) FATHelper.lookupLocalHome(ejbJndiName2);
            fhome3 = (SFLaHome) FATHelper.lookupLocalHome(ejbJndiName3);
            fhome4 = (SFLaHome) FATHelper.lookupLocalHome(ejbJndiName4);
            //InitialContext cntx = new InitialContext();
            //fhome1 = (SFLaHome) cntx.lookup("java:app/EJB2XSFLocalSpecEJB/SFLaBMT");
            //fhome2 = (SFLaHome) cntx.lookup("java:app/EJB2XSFLocalSpecEJB/SFLaCMT");
            //fhome3 = (SFLaHome) cntx.lookup("java:app/EJB2XSFLocalSpecEJB/SFLaBMTActivateTran");
            //fhome4 = (SFLaHome) cntx.lookup("java:app/EJB2XSFLocalSpecEJB/SFLaCMTActivateTran");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (iia01) Test Stateful CMT lifecycle constructor method.
     */
    @Test
    public void testSFLocalCMTLifecycle_constructor() throws Exception {
        SFLa ejb2 = fhome2.create();
        // Exception will be thrown if constructor not called properly
        ejb2.verify_constructor();
        ejb2.remove();
    }

    /**
     * (iia02) Test Stateful CMT lifecycle setSessionContext() method.
     */
    @Test
    public void testSFLocalCMTLifecycle_setSessionContext() throws Exception {
        SFLa ejb2 = fhome2.create();
        // Exception will be thrown if setSessionContext not called properly
        ejb2.verify_setContext();
        ejb2.remove();
    }

    /**
     * (iia03) Test Stateful CMT lifecycle unsetSessionContext() method.
     */
    //@Test
    public void testSFLocalCMTLifecycle_unsetSessionContext() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia04) Test Stateful CMT lifecycle ejbCreate() method.
     */
    @Test
    public void testSFLocalCMTLifecycle_ejbCreate() throws Exception {
        SFLa ejb2 = fhome2.create();
        // Exception will be thrown if ejbCreate not called properly
        ejb2.verify_ejbCreate(false); // no argument create

        // Also verify ejbCreate with arguments
        SFLa ejb = fhome2.create(true, (byte) 9, 'C', (short) 0, 0, 0, (float) 0.0, 0.0, "String stringValue");
        assertNotNull("Create EJB was null.", ejb);

        ejb.verify_ejbCreate(true);
        ejb.remove();
        ejb2.remove();
    }

    /**
     * (iia05) Test Stateful CMT lifecycle ejbPostCreate() method.
     */
    //@Test
    public void testSFLocalCMTLifecycle_ejbPostCreate() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia06) Test Stateful CMT lifecycle ejbRemove() method.
     */
    @Test
    public void testSFLocalCMTLifecycle_ejbRemove() throws Exception {
        SFLa ejb1 = fhome2.create();
        assertNotNull("Create EJB was null.", ejb1);
        SFLa ejb2 = fhome2.create();
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
    public void testSFLocalCMTLifecycle_ejbActivate() throws Exception {
        SFLa ejb = fhome4.create();
        assertNotNull("Create EJB was null.", ejb);

        ejb.verify_ejbActivate();
        ejb.remove();
    }

    /**
     * (iia08) Test Stateful CMT lifecycle ejbPassivate() method.
     */
    @Test
    public void testSFLocalCMTLifecycle_ejbPassivate() throws Exception {
        SFLa ejb = fhome4.create(true, (byte) 9, 'C', (short) 0, 0, 0, (float) 0.0, 0.0, "String stringValue");
        assertNotNull("Create EJB was null.", ejb);

        ejb.verify_ejbPassivate();
        ejb.remove();
    }

    /**
     * (iia09) Test Stateful CMT business method.
     */
    @Test
    public void testSFLocalCMTBusinessMethod() throws Exception {
        SFLa ejb2 = fhome2.create();
        String testStr = "Test string.";
        String buf = ejb2.method1(testStr);
        assertEquals("Method call (method1) test was unexpected value.", buf, testStr);
        ejb2.remove();
    }

    /**
     * (iia10) Test Stateful CMT lifecycle ejbFind() method.
     */
    //@Test
    public void testSFLocalCMTLifecycle_ejbFind() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia11) Test Stateful CMT lifecycle ejbHome() method.
     */
    //@Test
    public void testSFLocalCMTLifecycle_ejbHome() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia12) Test Stateful CMT lifecycle ejbLoad() method.
     */
    //@Test
    public void testSFLocalCMTLifecycle_ejbLoad() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia13) Test Stateful CMT lifecycle ejbStore() method.
     */
    //@Test
    public void testSFLocalCMTLifecycle_ejbStore() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia14) Test Stateful BMT lifecycle constructor method.
     */
    @Test
    public void testSFLocalBMTLifecycle_constructor() throws Exception {
        SFLa ejb1 = fhome1.create();
        // Exception will be thrown if constructor not called properly
        ejb1.verify_constructor();
        ejb1.remove();
    }

    /**
     * (iia15) Test Stateful BMT lifecycle setSessionContext() method.
     */
    @Test
    public void testSFLocalBMTLifecycle_setSessionContext() throws Exception {
        SFLa ejb1 = fhome1.create();
        // Exception will be thrown if setSessionContext not called properly
        ejb1.verify_setContext();
        ejb1.remove();
    }

    /**
     * (iia16) Test Stateful BMT lifecycle unsetSessionContext() method.
     */
    //@Test
    public void testSFLocalBMTLifecycle_unsetSessionContext() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia17) Test Stateful BMT lifecycle ejbCreate() method.
     */
    @Test
    public void testSFLocalBMTLifecycle_ejbCreate() throws Exception {
        SFLa ejb1 = fhome1.create();
        // Exception will be thrown if ejbCreate not called properly
        ejb1.verify_ejbCreate(false); // no argument create

        // Also verify ejbCreate with arguments
        SFLa ejb = fhome1.create(true, (byte) 9, 'C', (short) 0, 0, 0, (float) 0.0, 0.0, "String stringValue");
        assertNotNull("Create EJB was null.", ejb);

        ejb.verify_ejbCreate(true);
        ejb.remove();
    }

    /**
     * (iia18) Test Stateful BMT lifecycle ejbPostCreate() method.
     */
    //@Test
    public void testSFLocalBMTLifecycle_ejbPostCreate() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia19) Test Stateful BMT lifecycle ejbRemove() method.
     */
    @Test
    public void testSFLocalBMTLifecycle_ejbRemove() throws Exception {
        SFLa ejb1 = fhome1.create();
        assertNotNull("Create EJB was null.", ejb1);
        SFLa ejb2 = fhome1.create();
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
    public void testSFLocalBMTLifecycle_ejbActivate() throws Exception {
        SFLa ejb = fhome3.create();
        assertNotNull("Create EJB was null.", ejb);

        ejb.verify_ejbActivate();
        ejb.remove();
    }

    /**
     * (iia21) Test Stateful BMT lifecycle ejbPassivate() method.
     */
    @Test
    public void testSFLocalBMTLifecycle_ejbPassivate() throws Exception {
        SFLa ejb = fhome3.create(true, (byte) 9, 'C', (short) 0, 0, 0, (float) 0.0, 0.0, "String stringValue");
        assertNotNull("Create EJB was null.", ejb);

        ejb.verify_ejbPassivate();
        ejb.remove();
    }

    /**
     * (iia22) Test Stateful BMT business method.
     */
    @Test
    public void testSFLocalBMTBusinessMethod() throws Exception {
        SFLa ejb1 = fhome1.create();
        String testStr = "Test string.";
        String buf = ejb1.method1(testStr);
        assertEquals("Method call (method1) test was unexpected value.", buf, testStr);
        ejb1.remove();
    }

    /**
     * (iia23) Test Stateful BMT lifecycle ejbFind() method.
     */
    //@Test
    public void testSFLocalBMTLifecycle_ejbFind() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia24) Test Stateful BMT lifecycle ejbHome() method.
     */
    //@Test
    public void testSFLocalBMTLifecycle_ejbHome() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia25) Test Stateful BMT lifecycle ejbLoad() method.
     */
    //@Test
    public void testSFLocalBMTLifecycle_ejbLoad() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }

    /**
     * (iia26) Test Stateful BMT lifecycle ejbStore() method.
     */
    //@Test
    public void testSFLocalBMTLifecycle_ejbStore() throws Exception {
        svLogger.info("This test does not apply to Stateful beans.");
    }
}
