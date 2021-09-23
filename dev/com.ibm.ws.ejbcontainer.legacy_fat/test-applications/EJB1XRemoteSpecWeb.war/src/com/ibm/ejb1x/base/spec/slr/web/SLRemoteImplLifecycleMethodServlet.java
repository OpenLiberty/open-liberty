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

package com.ibm.ejb1x.base.spec.slr.web;

import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb1x.base.spec.slr.ejb.SLRa;
import com.ibm.ejb1x.base.spec.slr.ejb.SLRaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SLRemoteImplLifecycleMethodTest (formerly WSTestSLR_IITest)
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
@WebServlet("/SLRemoteImplLifecycleMethodServlet")
public class SLRemoteImplLifecycleMethodServlet extends FATServlet {
    private final static String CLASS_NAME = SLRemoteImplLifecycleMethodServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb1x/base/spec/slr/ejb/SLRaBMTHome";
    private final static String ejbJndiName2 = "com/ibm/ejb1x/base/spec/slr/ejb/SLRaCMTHome";
    private static SLRaHome fhome1;
    private static SLRaHome fhome2;
    private static SLRa fejb1;
    private static SLRa fejb2;

    @PostConstruct
    public void initializeHomes() {
        try {

            fhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName1, SLRaHome.class);
            fhome2 = FATHelper.lookupRemoteHomeBinding(ejbJndiName2, SLRaHome.class);
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
     * (iia01) Test Stateless CMT lifecycle constructor method.
     */
    @Test
    public void test1XSLCMTLifecycle_constructor() throws Exception {
        // Exception will be thrown if constructor not called properly
        fejb2.verify_constructor();
    }

    /**
     * (iia02) Test Stateless CMT lifecycle setSessionContext() method.
     */
    @Test
    public void test1XSLCMTLifecycle_setSessionContext() throws Exception {
        // Exception will be thrown if setSessionContext not called properly
        fejb2.verify_setContext();
    }

    /**
     * (iia03) Test Stateless CMT lifecycle unsetSessionContext() method.
     */
    //@Test
    public void test1XSLCMTLifecycle_unsetSessionContext() throws Exception {
        svLogger.info("No specification defined way to validate this for stateless beans.");
    }

    /**
     * (iia04) Test Stateless CMT lifecycle ejbCreate() method.
     */
    @Test
    public void test1XSLCMTLifecycle_ejbCreate() throws Exception {
        // Exception will be thrown if ejbCreate not called properly
        fejb2.verify_ejbCreate();
    }

    /**
     * (iia05) Test Stateless CMT lifecycle ejbPostCreate() method.
     */
    //@Test
    public void test1XSLCMTLifecycle_ejbPostCreate() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia06) Test Stateless CMT lifecycle ejbRemove() method.
     */
    //@Test
    public void test1XSLCMTLifecycle_ejbRemove() throws Exception {
        svLogger.info("No specification defined way to validate this for stateless beans.");
    }

    /**
     * (iia07) Test Stateless CMT lifecycle ejbActivate() method.
     */
    //@Test
    public void test1XSLCMTLifecycle_ejbActivate() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia08) Test Stateless CMT lifecycle ejbPassivate() method.
     */
    //@Test
    public void test1XSLCMTLifecycle_ejbPassivate() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia09) Test Stateless CMT business method.
     */
    @Test
    public void test1XSLCMTBusinessMethod() throws Exception {
        String testStr = "Test string.";
        String buf = fejb2.method1(testStr);
        assertEquals("Method call (method1) test returned unexpected value.", buf, testStr);
    }

    /**
     * (iia10) Test Stateless CMT lifecycle ejbFind() method.
     */
    //@Test
    public void test1XSLCMTLifecycle_ejbFind() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia11) Test Stateless CMT lifecycle ejbHome() method.
     */
    //@Test
    public void test1XSLCMTLifecycle_ejbHome() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia12) Test Stateless CMT lifecycle ejbLoad() method.
     */
    //@Test
    public void test1XSLCMTLifecycle_ejbLoad() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia13) Test Stateless CMT lifecycle ejbStore() method.
     */
    //@Test
    public void test1XSLCMTLifecycle_ejbStore() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia14) Test Stateless BMT lifecycle constructor method.
     */
    @Test
    public void test1XSLBMTLifecycle_constructor() throws Exception {
        // Exception will be thrown if constructor not called properly
        fejb1.verify_constructor();
    }

    /**
     * (iia15) Test Stateless BMT lifecycle setSessionContext() method.
     */
    @Test
    public void test1XSLBMTLifecycle_setSessionContext() throws Exception {
        // Exception will be thrown if setSessionContext not called properly
        fejb1.verify_setContext();
    }

    /**
     * (iia16) Test Stateless BMT lifecycle unsetSessionContext() method.
     */
    //@Test
    public void test1XSLBMTLifecycle_unsetSessionContext() throws Exception {
        svLogger.info("No specification defined way to validate this for stateless beans.");
    }

    /**
     * (iia17) Test Stateless BMT lifecycle ejbCreate() method.
     */
    @Test
    public void test1XSLBMTLifecycle_ejbCreate() throws Exception {
        // Exception will be thrown if ejbCreate not called properly
        fejb1.verify_ejbCreate();
    }

    /**
     * (iia18) Test Stateless BMT lifecycle ejbPostCreate() method.
     */
    //@Test
    public void test1XSLBMTLifecycle_ejbPostCreate() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia19) Test Stateless BMT lifecycle ejbRemove() method.
     */
    //@Test
    public void test1XSLBMTLifecycle_ejbRemove() throws Exception {
        svLogger.info("No specification defined way to validate this for stateless beans.");
    }

    /**
     * (iia20) Test Stateless BMT lifecycle ejbActivate() method.
     */
    //@Test
    public void test1XSLBMTLifecycle_ejbActivate() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia21) Test Stateless BMT lifecycle ejbPassivate() method.
     */
    //@Test
    public void test1XSLBMTLifecycle_ejbPassivate() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia22) Test Stateless BMT business method.
     */
    @Test
    public void test1XSLBMTBusinessMethod() throws Exception {
        String testStr = "Test string.";
        String buf = fejb1.method1(testStr);
        assertEquals("Method call (method1) test returned unexpected value.", buf, testStr);
    }

    /**
     * (iia23) Test Stateless BMT lifecycle ejbFind() method.
     */
    //@Test
    public void test1XSLBMTLifecycle_ejbFind() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia24) Test Stateless BMT lifecycle ejbHome() method.
     */
    //@Test
    public void test1XSLBMTLifecycle_ejbHome() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia25) Test Stateless BMT lifecycle ejbLoad() method.
     */
    //@Test
    public void test1XSLBMTLifecycle_ejbLoad() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }

    /**
     * (iia26) Test Stateless BMT lifecycle ejbStore() method.
     */
    //@Test
    public void test1XSLBMTLifecycle_ejbStore() throws Exception {
        svLogger.info("This test does not apply to Stateless beans.");
    }
}
