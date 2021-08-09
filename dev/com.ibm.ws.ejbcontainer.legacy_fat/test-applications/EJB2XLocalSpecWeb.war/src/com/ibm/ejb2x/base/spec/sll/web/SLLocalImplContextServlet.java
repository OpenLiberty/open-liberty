/*******************************************************************************
 * Copyright (c) 2002, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ejb2x.base.spec.sll.web;

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ejb2x.base.spec.sll.ejb.SLLa;
import com.ibm.ejb2x.base.spec.sll.ejb.SLLaHome;
import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>SLLocalImplContextTest (formerly WSTestSLL_IXTest)
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
@WebServlet("/SLLocalImplContextServlet")
public class SLLocalImplContextServlet extends FATServlet {

    private final static String CLASS_NAME = SLLocalImplContextServlet.class.getName();
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
     * (ixc01) Test Stateless local EJBContext.getEJBObject(). <p>
     *
     * Needs Java cast not narrowing. See EJB 2.0 spec section 12.3.12.
     */
    @Test
    public void testSLLocalEJBContext_getEJBObject_NoNarrow() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object o = ejb1.context_getEJBObject();
        if (o instanceof Throwable) {
            if (o instanceof IllegalStateException) {
                svLogger.info("Caught expected " + o.getClass().getName());
            } else {
                fail("Caught unexpected expection : " + o);
            }
        } else {
            fail("Unexpected return from context.getEJBObject : " + o);
        }
    }

    /**
     * (ixc02) Test Stateless local EJBContext.getEJBHome().
     */
    @Test
    public void testSLLocalEJBContext_getEJBHome() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object o = ejb1.context_getEJBHome();
        if (o instanceof Throwable) {
            if (o instanceof IllegalStateException) {
                svLogger.info("Caught expected " + o.getClass().getName());
            } else {
                fail("Caught unexpected expection : " + o);
            }
        } else {
            fail("Unexpected return from context.getEJBHome : " + o);
        }
    }

    /**
     * (ixc03) Test Stateless local EJBContext.getEJBLocalHome().
     */
    @Test
    public void testSLLocalEJBContext_getEJBLocalHome() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object o = ejb1.context_getEJBLocalHome();
        if (o instanceof Throwable) {
            fail("aught unexpected expection : " + o);
        }
        assertNotNull("Session context's getEJBLocalHome was null.", o);
        SLLaHome home1 = (SLLaHome) o;
        assertNotNull("Cast getEJBLocalHome to SLLaHome was null.", home1);
    }

    /**
     * (ixc04) Test Stateless local EJBContext.getEnvironment().
     */
    @Test
    @SkipForRepeat({ EE9_FEATURES })
    public void testSLLocalEJBContext_getEnvironment() throws Exception {
        SLLa ejb1 = fhome1.create();
        String tempStr = ejb1.context_getEnvironment("value1");
        assertEquals("Get Environment string from context was unexpected value", tempStr, "value of value1");
    }

    /**
     * (ixc05) Test Stateless local EJBContext.getCallerIdentity().
     */
    @Test
    @SuppressWarnings("deprecation")
    @SkipForRepeat({ EE9_FEATURES })
    public void testSLLocalEJBContext_getCallerIdentity() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object o = ejb1.context_getCallerIdentity();
        if (o instanceof Throwable) {
            fail("Caught unexpected expection : " + o);
        } else {
            if (o == null) {
                svLogger.info("No principle is asserted. Security disabled.");
            } else {
                if (o instanceof java.security.Identity) {
                    assertNotNull("Prinicple identity should not have been null", o);
                } else {
                    fail("Unexpected object return from context.getCallerPrincipal=" + o.getClass().getName());
                }
            }
        }
    }

    /**
     * (ixc06) Test Stateless local EJBContext.getCallerPrincipal().
     */
    @Test
    public void testSLLocalEJBContext_getCallerPrincipal() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object o = ejb1.context_getCallerPrincipal();
        if (o instanceof Throwable) {
            fail("Caught unexpected expection : " + o);
        } else {
            if (o instanceof String) {
                assertNotNull("Context.getCallerPrincipal should not have been null", o);
            } else {
                fail("Unexpected object return from context.getCallerPrincipal=" + o.getClass().getName());
            }
        }
    }

    /**
     * (ixc07) Test Stateless local EJBContext.isCallerInRole( Identity ).
     */
    @Test
    @SuppressWarnings("deprecation")
    @SkipForRepeat({ EE9_FEATURES })
    public void testSLLocalEJBContext_isCallerInRole_Identity() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object o = ejb1.context_isCallerInRole((java.security.Identity) null);
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
     * (ixc08) Test Stateless local EJBContext.isCallerInRole( String ).
     */
    @Test
    public void testSLLocalEJBContext_isCallerInRole_String() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object o = ejb1.context_isCallerInRole("Not In Role");
        if (o instanceof Throwable) {
            fail("Caught unexpected expection : " + o);
        } else {
            if (o instanceof Boolean) {
                assertFalse("Test not In role should not have returned true", ((Boolean) o).booleanValue());
            } else {
                fail("Unexpected object return from context.getCallerPrincipal=" + o.getClass().getName());
            }
        }
    }

    /**
     * (ixc09) Test Stateless local EJBContext.getUserTransaction() - CMT.
     */
    @Test
    public void testSLLocalEJBContext_getUserTransaction_CMT() throws Exception {
        SLLa ejb2 = fhome2.create();
        Object o = ejb2.context_getUserTransaction();
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
     * (ixc10) Test Stateless local EJBContext.getUserTransaction() - BMT.
     */
    @Test
    public void testSLLocalEJBContext_getUserTransaction_BMT() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object o = ejb1.context_getUserTransaction();
        if (o instanceof Throwable) {
            fail("Caught unexpected expection : " + o);
        } else {
            assertNotNull("Expected return from context_getUserTransaction was null.", o);

            if (o instanceof String) {
                assertEquals("Did not return a user transaction type.", o, "com.ibm.ejs.container.UserTransactionWrapper");
            } else {
                fail("Expected String object but got " + o.getClass().getName());
            }
        }
    }

    /**
     * (ixc11) Test Stateless local EJBContext.setRollbackOnly() - CMT.
     */
    @Test
    @ExpectedFFDC("com.ibm.websphere.csi.CSITransactionRolledbackException")
    public void testSLLocalEJBContext_setRollbackOnly_CMT() throws Exception {
        SLLa ejb2 = fhome2.create();
        Object o = ejb2.context_setRollbackOnly();
        if (o instanceof Boolean) {
            assertNotNull("Did not successfully call setRollbackOnly().", o);
        } else {
            if (o instanceof Throwable) {
                fail("Caught unexpected expection : " + o);
            } else {
                fail("Unexpected return object returned." + o.getClass().getName());
            }
        }
    }

    /**
     * (ixc12) Test Stateless local EJBContext.setRollbackOnly() - BMT.
     */
    @Test
    public void testSLLocalEJBContext_setRollbackOnly_BMT() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object o = ejb1.context_setRollbackOnly();
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
     * (ixc13) Test Stateless local EJBContext.getRollbackOnly() - CMT.
     */
    @Test
    public void testSLLocalEJBContext_getRollbackOnly_CMT() throws Exception {
        SLLa ejb2 = fhome2.create();
        Object o = ejb2.context_getRollbackOnly();
        if (o instanceof Boolean) {
            svLogger.info("Successfully called getRollbackOnly() with return value=" + ((Boolean) o).booleanValue());
        } else {
            if (o instanceof Throwable) {
                fail("Caught unexpected expection : " + o);
            } else {
                fail("Unexpected return object returned." + o.getClass().getName());
            }
        }
    }

    /**
     * (ixc14) Test Stateless local EJBContext.getRollbackOnly() - BMT.
     */
    @Test
    public void testSLLocalEJBContext_getRollbackOnly_BMT() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object o = ejb1.context_getRollbackOnly();
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
     * (ixc15) Test Stateless local EJBContext.getEJBLocalObject().
     */
    @Test
    public void testSLLocalEJBContext_getEJBLocalObject() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object o = ejb1.context_getEJBLocalObject();
        if (o instanceof Throwable) {
            fail("Caught unexpected expection : " + o);
        }
        assertNotNull("Session context's getEJBLocalObject was null.", o);
        assertTrue("EJBLocalObject was not instanceof SLLa", (o instanceof SLLa));
    }

    /**
     * (ixc16) Test Stateless local EJBContext.getEJBObject().
     */
    @Test
    public void testSLLocalEJBContext_getEJBObject() throws Exception {
        SLLa ejb1 = fhome1.create();
        Object o = ejb1.context_getEJBObject();
        if (o instanceof Throwable) {
            if (o instanceof IllegalStateException) {
                svLogger.info("Caught expected " + o.getClass().getName());
            } else {
                fail("Caught unexpected expection : " + o);
            }
        } else {
            fail("Unexpected return from context.getEJBLocalObject : " + o);
        }
    }

    /**
     * (ixc17) Test Stateless local EJBContext.getPrimaryKey().
     */
    //@Test
    public void testSLLocalEJBContext_getPrimaryKey() throws Exception {
        svLogger.info("This test does not apply to session beans.");
    }
}
