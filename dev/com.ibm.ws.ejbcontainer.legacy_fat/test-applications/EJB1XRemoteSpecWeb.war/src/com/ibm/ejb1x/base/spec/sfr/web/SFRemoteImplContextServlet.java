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

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb1x.base.spec.sfr.ejb.SFRa;
import com.ibm.ejb1x.base.spec.sfr.ejb.SFRaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SFRemoteImplContextTest (formerly WSTestSFR_IXTest)
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container basic function tests:
 * <ul>
 * <li>I____ - Bean Implementation;
 * <li>IXC__ - EJBContext / EntityContext / SessionContext.
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
 * <li>ixc01 - getEJBObject() needs Java cast not narrowing.
 * <li>ixc02 - getEJBHome()
 * <li>ixc03 - getEJBLocalHome()
 * <li>ixc04 - getEnvironment()
 * <li>ixc05 - getCallerIdentity()
 * <li>ixc06 - getCallerPrincipal()
 * <li>ixc07 - isCallerInRole( Identity )
 * <li>ixc08 - isCallerInRole( String )
 * <li>ixc09 - getUserTransaction() - CMT
 * <li>ixc10 - getUserTransaction() - BMT
 * <li>ixc11 - setRollbackOnly() - CMT
 * <li>ixc12 - setRollbackOnly() - BMT
 * <li>ixc13 - getRollbackOnly() - CMT
 * <li>ixc14 - getRollbackOnly() - BMT
 * <li>ixc15 - getEJBLocalObject()
 * <li>ixc16 - getEJBObject()
 * <li>ixc17 - getPrimaryKey()
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/SFRemoteImplContextServlet")
public class SFRemoteImplContextServlet extends FATServlet {
    private final static String CLASS_NAME = SFRemoteImplContextServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASS_NAME);

    private final static String ejbJndiName1 = "com/ibm/ejb1x/base/spec/sfr/ejb/SFRaBMTHome";
    private final static String ejbJndiName2 = "com/ibm/ejb1x/base/spec/sfr/ejb/SFRaCMTHome";
    private static SFRaHome fhome1;
    private static SFRaHome fhome2;

    private static SFRa fejb1;
    private static SFRa fejb2;

    @PostConstruct
    public void initializeHomes() {
        try {

            fhome1 = FATHelper.lookupRemoteHomeBinding(ejbJndiName1, SFRaHome.class);
            fhome2 = FATHelper.lookupRemoteHomeBinding(ejbJndiName2, SFRaHome.class);
            fejb1 = fhome1.create();
            fejb2 = fhome2.create();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PreDestroy
    public void destroyBeans() {
        try {

            if (fejb1 != null) {
                fejb1.remove();
            }
            if (fejb2 != null) {
                fejb2.remove();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * (ixc01) Test Stateful remote EJBContext.getEJBObject(). <p>
     *
     * Needs Java cast not narrowing. See EJB 2.0 spec section 12.3.12.
     */
    @Test
    public void test1XSFEJBContext_getEJBObject_NoNarrow() throws Exception {
        SFRa ejb1 = fejb1.context_getEJBObject();
        assertNotNull("Cast getEJBObject to SFRa was null.", ejb1);
        String testStr = "ixc01 test string";
        String rtnStr = ejb1.method1(testStr);
        assertEquals("EJB reference after type cast was unexpected value.", testStr, rtnStr);
    }

    /**
     * (ixc02) Test Stateful remote EJBContext.getEJBHome().
     */
    @Test
    public void test1XSFEJBContext_getEJBHome() throws Exception {
        Object o = fejb1.context_getEJBHome();
        if (o instanceof Throwable) {
            fail("Caught unexpected expection : " + o);
        }
        assertNotNull("Session context's getEJBHome was null.", o);
        SFRaHome home1 = (SFRaHome) javax.rmi.PortableRemoteObject.narrow(o, SFRaHome.class);
        assertNotNull("Narrow getEJBHome to SFRaHome was null.", home1);
    }

    /**
     * (ixc03) Test Stateful remote EJBContext.getEJBLocalHome().
     */
    //@Test
    public void test1XSFEJBContext_getEJBLocalHome() throws Exception {
        svLogger.info("This test does not apply to EJB 1.x remote beans.");
    }

    /**
     * (ixc04) Test Stateful remote EJBContext.getEnvironment().
     */
    @Test
    @SkipForRepeat({ EE9_FEATURES })
    public void test1XSFEJBContext_getEnvironment() throws Exception {
        String tempStr = fejb1.context_getEnvironment("value1");
        assertEquals("Get Environment string from context was unexpected value", tempStr, "value of value1");
    }

    /**
     * (ixc05) Test Stateful remote EJBContext.getCallerIdentity().
     */
    @Test
    @SuppressWarnings("deprecation")
    @SkipForRepeat({ EE9_FEATURES })
    public void test1XSFEJBContext_getCallerIdentity() throws Exception {
        Object o = fejb1.context_getCallerIdentity();
        if (o instanceof Throwable) {
            fail("Caught unexpected expection : " + o);
        } else {
            if (o == null) {
                svLogger.info("No principle is asserted. Security disabled.");
            } else {
                if (o instanceof java.security.Identity) {
                    assertNotNull("Prinicple identity should not have been null", o);
                } else {
                    fail("       Unexpected object return from context.getCallerPrincipal=" + o.getClass().getName());
                }
            }
        }
    }

    /**
     * (ixc06) Test Stateful remote EJBContext.getCallerPrincipal().
     */
    @Test
    public void test1XSFEJBContext_getCallerPrincipal() throws Exception {
        Object o = fejb1.context_getCallerPrincipal();
        if (o instanceof Throwable) {
            fail("Caught unexpected expection : " + o);
        } else {
            if (o instanceof String) {
                assertNotNull("Prinicple identity should not have been null", o);
            } else {
                fail("Unexpected object return from context.getCallerPrincipal=" + o.getClass().getName());
            }
        }
    }

    /**
     * (ixc07) Test Stateful remote EJBContext.isCallerInRole( Identity ).
     */
    @Test
    @SuppressWarnings("deprecation")
    @SkipForRepeat({ EE9_FEATURES })
    public void test1XSFEJBContext_isCallerInRole_Identity() throws Exception {
        Object o = fejb1.context_isCallerInRole((java.security.Identity) null);
        if (o instanceof Throwable) {
            if (o instanceof UnsupportedOperationException) {
                svLogger.info("Caught expected " + o.getClass().getName());
            } else {
                fail("Caught unexpected expection : " + o);
            }
        } else {
            fail("Unexpected object return from context.getCallerPrincipal=" + o.getClass().getName());
        }
    }

    /**
     * (ixc08) Test Stateful remote EJBContext.isCallerInRole( String ).
     */
    @Test
    public void test1XSFEJBContext_isCallerInRole_String() throws Exception {
        Object o = fejb1.context_isCallerInRole("Not In Role");
        if (o instanceof Throwable) {
            fail("Caught unexpected expection : " + o);
        } else {
            if (o instanceof Boolean) {
                assertFalse("Test should not have been in role", ((Boolean) o).booleanValue());
            } else {
                fail("Unexpected object return from context.getCallerPrincipal=" + o.getClass().getName());
            }
        }
    }

    /**
     * (ixc09) Test Stateful remote EJBContext.getUserTransaction() - CMT.
     */
    @Test
    public void test1XSFEJBContext_getUserTransaction_CMT() throws Exception {
        Object o = fejb2.context_getUserTransaction();
        if (o instanceof Throwable) {
            if (o instanceof IllegalStateException) {
                svLogger.info("Caught expected " + o.getClass().getName());
            } else {
                fail("Caught unexpected expection : " + o);
            }
        } else {
            fail("Unexpected return from context_getUserTransaction.");
        }
    }

    /**
     * (ixc10) Test Stateful remote EJBContext.getUserTransaction() - BMT.
     */
    @Test
    public void test1XSFEJBContext_getUserTransaction_BMT() throws Exception {
        Object o = fejb1.context_getUserTransaction();
        if (o instanceof Throwable) {
            fail("Caught unexpected expection : " + o);
        } else {
            svLogger.info("Expected return from context_getUserTransaction.");

            if (o instanceof String) {
                assertEquals("Did not return a user transaction type.", o, "com.ibm.ejs.container.UserTransactionWrapper");
            } else {
                fail("Expected String object but got " + o.getClass().getName());
            }
        }
    }

    /**
     * (ixc11) Test Stateful remote EJBContext.setRollbackOnly() - CMT.
     */
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.csi.CSITransactionRolledbackException" })
    public void test1XSFEJBContext_setRollbackOnly_CMT() throws Exception {
        Object o = fejb2.context_setRollbackOnly();
        if (o instanceof Boolean) {
            assertNotNull("Did not successfully called setRollbackOnly().", o);
        } else {
            if (o instanceof Throwable) {
                fail("Caught unexpected expection : " + o);
            } else {
                fail("Unexpected return object returned." + o.getClass().getName());
            }
        }
    }

    /**
     * (ixc12) Test Stateful remote EJBContext.setRollbackOnly() - BMT.
     */
    @Test
    public void test1XSFEJBContext_setRollbackOnly_BMT() throws Exception {
        Object o = fejb1.context_setRollbackOnly();
        if (o instanceof Throwable) {
            if (o instanceof IllegalStateException) {
                svLogger.info("Caught expected " + o.getClass().getName());
            } else {
                fail("Caught unexpected expection : " + o);
            }
        } else {
            fail("Unexpected return from context_setRollbackOnly().");
        }
    }

    /**
     * (ixc13) Test Stateful remote EJBContext.getRollbackOnly() - CMT.
     */
    @Test
    public void test1XSFEJBContext_getRollbackOnly_CMT() throws Exception {
        Object o = fejb2.context_getRollbackOnly();
        if (o instanceof Boolean) {
            assertNotNull("Did not successfully called getRollbackOnly()", o);
        } else {
            if (o instanceof Throwable) {
                fail("Caught unexpected expection : " + o);
            } else {
                fail("Unexpected return object returned." + o.getClass().getName());
            }
        }
    }

    /**
     * (ixc14) Test Stateful remote EJBContext.getRollbackOnly() - BMT.
     */
    @Test
    public void test1XSFEJBContext_getRollbackOnly_BMT() throws Exception {
        Object o = fejb1.context_getRollbackOnly();
        if (o instanceof Throwable) {
            if (o instanceof IllegalStateException) {
                svLogger.info("Caught expected " + o.getClass().getName());
            } else {
                fail("Caught unexpected expection : " + o);
            }
        } else {
            fail("Unexpected return from context_getRollbackOnly().");
        }
    }

    /**
     * (ixc15) Test Stateful remote EJBContext.getEJBLocalObject().
     */
    //@Test
    public void test1XSFEJBContext_getEJBLocalObject() throws Exception {
        svLogger.info("This test does not apply to EJB 1.x remote beans.");
    }

    /**
     * (ixc16) Test Stateful remote EJBContext.getEJBObject().
     */
    @Test
    public void test1XSFEJBContext_getEJBObject() throws Exception {
        Object o = fejb1.context_getEJBObject();
        if (o instanceof Throwable) {
            fail("       Caught unexpected expection : " + o);
        }
        assertNotNull("Session context's getEJBObject was null.", o);
    }

    /**
     * (ixc17) Test Stateful remote EJBContext.getPrimaryKey().
     */
    //@Test
    public void test1XSFEJBContext_getPrimaryKey() throws Exception {
        svLogger.info("This test does not apply to session beans.");
    }
}
