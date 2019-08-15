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

package com.ibm.ejb2x.base.spec.sll.web;

import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.sll.ejb.SLLa;
import com.ibm.ejb2x.base.spec.sll.ejb.SLLaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SLLocalImplLifecycleMethodTest (formerly WSTestSLL_IITest)
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
@WebServlet("/SLLocalImplLifecycleMethodServlet")
public class SLLocalImplLifecycleMethodServlet extends FATServlet {

    private final static String CLASS_NAME = SLLocalImplLifecycleMethodServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb2x/base/spec/sll/ejb/SLLaBMTHome";
    private final static String ejbJndiName2 = "com/ibm/ejb2x/base/spec/sll/ejb/SLLaCMTHome";
    private SLLaHome fhome1;
    private SLLaHome fhome2;

    @PostConstruct
    private void initializeHomes() {
        try {
            fhome1 = (SLLaHome) FATHelper.lookupLocalHome(ejbJndiName1);
            fhome2 = (SLLaHome) FATHelper.lookupLocalHome(ejbJndiName2);
            //InitialContext cntx = new InitialContext();
            //fhome1 = (SLLaHome) cntx.lookup("java:app/EJB2XSLLocalSpecEJB/SLLaBMT");
            //fhome2 = (SLLaHome) cntx.lookup("java:app/EJB2XSLLocalSpecEJB/SLLaCMT");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (iia01) Test Stateless CMT lifecycle constructor method.
     */
    @Test
    public void testSLLocalCMTLifecycle_constructor() throws Exception {
        SLLa ejb2 = fhome2.create();
        // Exception will be thrown if constructor not called properly
        ejb2.verify_constructor();
    }

    /**
     * (iia02) Test Stateless CMT lifecycle setSessionContext() method.
     */
    @Test
    public void testSLLocalCMTLifecycle_setSessionContext() throws Exception {
        SLLa ejb2 = fhome2.create();
        // Exception will be thrown if setSessionContext not called properly
        ejb2.verify_setContext();
    }

    /**
     * (iia03) Test Stateless CMT lifecycle unsetSessionContext() method.
     */
    //@Test
    public void testSLLocalCMTLifecycle_unsetSessionContext() throws Exception {
        svLogger.info("No specification defined way to validate this for stateless beans.");
    }

    /**
     * (iia04) Test Stateless CMT lifecycle ejbCreate() method.
     */
    @Test
    public void testSLLocalCMTLifecycle_ejbCreate() throws Exception {
        SLLa ejb2 = fhome2.create();
        // Exception will be thrown if ejbCreate not called properly
        ejb2.verify_ejbCreate();
    }

    /**
     * (iia05) Test Stateless CMT lifecycle ejbPostCreate() method.
     */
    //@Test
    public void testSLLocalCMTLifecycle_ejbPostCreate() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia06) Test Stateless CMT lifecycle ejbRemove() method.
     */
    //@Test
    public void testSLLocalCMTLifecycle_ejbRemove() throws Exception {
        svLogger.info("No specification defined way to validate this for stateless beans.");
    }

    /**
     * (iia07) Test Stateless CMT lifecycle ejbActivate() method.
     */
    //@Test
    public void testSLLocalCMTLifecycle_ejbActivate() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia08) Test Stateless CMT lifecycle ejbPassivate() method.
     */
    //@Test
    public void testSLLocalCMTLifecycle_ejbPassivate() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia09) Test Stateless CMT business method.
     */
    @Test
    public void testSLLocalCMTBusinessMethod() throws Exception {
        SLLa ejb2 = fhome2.create();
        String testStr = "Test string.";
        String buf = ejb2.method1(testStr);
        assertEquals("Method call (method1) test was unexpected value.", buf, testStr);
    }

    /**
     * (iia10) Test Stateless CMT lifecycle ejbFind() method.
     */
    //@Test
    public void testSLLocalCMTLifecycle_ejbFind() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia11) Test Stateless CMT lifecycle ejbHome() method.
     */
    //@Test
    public void testSLLocalCMTLifecycle_ejbHome() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia12) Test Stateless CMT lifecycle ejbLoad() method.
     */
    //@Test
    public void testSLLocalCMTLifecycle_ejbLoad() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia13) Test Stateless CMT lifecycle ejbStore() method.
     */
    //@Test
    public void testSLLocalCMTLifecycle_ejbStore() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia14) Test Stateless BMT lifecycle constructor method.
     */
    @Test
    public void testSLLocalBMTLifecycle_constructor() throws Exception {
        SLLa ejb1 = fhome1.create();
        // Exception will be thrown if constructor not called properly
        ejb1.verify_constructor();
    }

    /**
     * (iia15) Test Stateless BMT lifecycle setSessionContext() method.
     */
    @Test
    public void testSLLocalBMTLifecycle_setSessionContext() throws Exception {
        SLLa ejb1 = fhome1.create();
        // Exception will be thrown if setSessionContext not called properly
        ejb1.verify_setContext();
    }

    /**
     * (iia16) Test Stateless BMT lifecycle unsetSessionContext() method.
     */
    //@Test
    public void testSLLocalBMTLifecycle_unsetSessionContext() throws Exception {
        svLogger.info("No specification defined way to validate this for stateless beans.");
    }

    /**
     * (iia17) Test Stateless BMT lifecycle ejbCreate() method.
     */
    @Test
    public void testSLLocalBMTLifecycle_ejbCreate() throws Exception {
        SLLa ejb1 = fhome1.create();
        // Exception will be thrown if ejbCreate not called properly
        ejb1.verify_ejbCreate();
    }

    /**
     * (iia18) Test Stateless BMT lifecycle ejbPostCreate() method.
     */
    //@Test
    public void testSLLocalBMTLifecycle_ejbPostCreate() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia19) Test Stateless BMT lifecycle ejbRemove() method.
     */
    //@Test
    public void testSLLocalBMTLifecycle_ejbRemove() throws Exception {
        svLogger.info("No specification defined way to validate this for stateless beans.");
    }

    /**
     * (iia20) Test Stateless BMT lifecycle ejbActivate() method.
     */
    //@Test
    public void testSLLocalBMTLifecycle_ejbActivate() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia21) Test Stateless BMT lifecycle ejbPassivate() method.
     */
    //@Test
    public void testSLLocalBMTLifecycle_ejbPassivate() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia22) Test Stateless BMT business method.
     */
    @Test
    public void testSLLocalBMTBusinessMethod() throws Exception {
        SLLa ejb1 = fhome1.create();
        String testStr = "Test string.";
        String buf = ejb1.method1(testStr);
        assertEquals("Method call (method1) test returned unexpected value.", buf, testStr);
    }

    /**
     * (iia23) Test Stateless BMT lifecycle ejbFind() method.
     */
    //@Test
    public void testSLLocalBMTLifecycle_ejbFind() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia24) Test Stateless BMT lifecycle ejbHome() method.
     */
    //@Test
    public void testSLLocalBMTLifecycle_ejbHome() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia25) Test Stateless BMT lifecycle ejbLoad() method.
     */
    //@Test
    public void testSLLocalBMTLifecycle_ejbLoad() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia26) Test Stateless BMT lifecycle ejbStore() method.
     */
    //@Test
    public void testSLLocalBMTLifecycle_ejbStore() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }
}
