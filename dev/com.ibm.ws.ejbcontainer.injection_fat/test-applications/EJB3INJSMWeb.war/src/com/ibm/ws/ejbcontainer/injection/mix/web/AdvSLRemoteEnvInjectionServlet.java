// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/AdvSLRemoteEnvInjectionTest.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 2/27/10 16:51:38
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  AdvSLRemoteEnvInjectionTest.java
//
// Source File Description:
//
//     Tests EJB Container support for the Basic EJB 3.0
//     Injection of Environment values on Stateless Session beans.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d439081   EJB3      20070403 bmdecker : New Part
// d446507   EJB3      20070618 tonyei   : Update lookups for new binding format
// d432816.1 EJB3      20070619 schmittm : Uncomment multiple injection target tests
// d423446.3 EJB3      20070910 urrvano  : Updated total test points due to Mike Schmitt's changes for 423446.1
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------

package com.ibm.ws.ejbcontainer.injection.mix.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.servlet.annotation.WebServlet;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.EnvInjectionEJBRemote;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.EnvInjectionEJBRemoteHome;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.EnvInjectionRemote;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> AdvSLRemoteEnvInjectionTest .
 *
 * <dt><b>Test Author:</b> Brian Decker <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the EJB 3.0
 * Injection of Environment values on Stateless Session beans. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSLRAdvEnvObjFldInjection - Business Interface: Object Field Injection
 * <li>testSLRAdvEnvPrimFldInjection - Business Interface: Primitive Field Injection
 * <li>testSLRAdvEnvObjMthdInjection - Business Interface: Object Method Injection
 * <li>testSLRAdvEnvPrimMthdInjection - Business Interface: Primitive Method Injection
 * <li>testSLRAdvCompEnvObjFldInjection - Component Interface: Object Field Injection
 * <li>testSLRAdvCompEnvPrimFldInjection - Component Interface: Primitive Field Injection
 * <li>testSLRAdvCompEnvObjMthdInjection - Component Interface: Object Method Injection
 * <li>testSLRAdvCompEnvPrimMthdInjection - Component Interface: Primitive Method Injection
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/AdvSLRemoteEnvInjectionServlet")
public class AdvSLRemoteEnvInjectionServlet extends FATServlet {
    private static final String CLASS_NAME = AdvSLRemoteEnvInjectionServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String PASSED = "Passed";

    // SimpleBinding name was used for the bean used by this test
    private static final String SimpleBindingPreFix = "com/ibm/ws/ejbcontainer/injection/mix/ejb/";

    // Names of the beans used for the test... for lookup.
    private static final String AdvObjFldBean = "AdvSLEnvInjectObjFld";
    private static final String AdvPrimFldBean = "AdvSLEnvInjectPrimFld";
    private static final String AdvObjMthdBean = "AdvSLEnvInjectObjMthd";
    private static final String AdvPrimMthdBean = "AdvSLEnvInjectPrimMthd";
    private static final String AdvCompObjFldBean = "AdvCompSLEnvInjectObjFld";
    private static final String AdvCompPrimFldBean = "AdvCompSLEnvInjectPrimFld";
    private static final String AdvCompObjMthdBean = "AdvCompSLEnvInjectObjMthd";
    private static final String AdvCompPrimMthdBean = "AdvCompSLEnvInjectPrimMthd";

    // Names of the interfaces used for the test
    private static final String EnvInjectionEJBRemoteHomeInterface = EnvInjectionEJBRemoteHome.class.getName();

    /** Jndi Names of the Bean Homes to use for the test. **/
    private String ivJNDI_SLREnvInjectObjFld = SimpleBindingPreFix + AdvCompObjFldBean +
                                               "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SLREnvInjectPrimFld = SimpleBindingPreFix + AdvCompPrimFldBean +
                                                "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SLREnvInjectObjMthd = SimpleBindingPreFix + AdvCompObjMthdBean +
                                                "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SLREnvInjectPrimMthd = SimpleBindingPreFix + AdvCompPrimMthdBean +
                                                 "#" + EnvInjectionEJBRemoteHomeInterface;

    @BeforeClass
    public static void setUp() throws Exception {
        svLogger.entering(CLASS_NAME, "setUp");

        // No setup required

        svLogger.exiting(CLASS_NAME, "setUp");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        svLogger.entering(CLASS_NAME, "tearDown");

        // No tearDown required

        svLogger.exiting(CLASS_NAME, "tearDown");
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB, with Business Interfaces only, for fields
     * that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be accessed.
     * <li> String with default value and XML value is the XML value.
     * <li> String with default value and no XML value is the default value.
     * <li> String with no default value and no XML value is null.
     * <li> String with no default value and misnamed XML value is null.
     * <li> Renamed String with no default value and XML value is the XML value.
     * <li> Object with no default value and an XML-defined Integer is the XML value.
     * <li> String with default value and XML value may be looked up from global namespace.
     * <li> String with default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> String of misnamed XML value may be looked up from global namespace.
     * <li> Renamed String with no default value and XML value may be looked up from global namespace.
     * <li> String of original name may be looked up from global namespace.
     * <li> Object with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> Class level resource with name and type may be looked up from global namespace.
     * <li> Second class level resource with name only may be looked up from global namespace.
     * <li> String with default value and XML value may be looked up from session context.
     * <li> String with default value and no XML value may not be looked up from session context.
     * <li> String with no default value and no XML value may not be looked up from session context.
     * <li> String with no default value and misnamed XML value may not be looked up from session context.
     * <li> String of misnamed XML value may be looked up from session context.
     * <li> Renamed String with no default value and XML value may be looked up from session context.
     * <li> String of original name may be looked up from session context.
     * <li> Object with no default value and an XML-defined Integer may be looked up from session context.
     * <li> Class level resource with name and type may be looked up from session context.
     * <li> Second class level resource with name only may be looked up from session context.
     * </ol>
     */
    @Test
    public void testSLRAdvEnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + AdvObjFldBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SLRSB accessed successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (EJBException ex) {
            assertEquals("Expected EJBException test is wrong",
                         "discardInstance", ex.getMessage());
        }

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB, with Business Interfaces only, for fields
     * that are the primitives (char, int, float, etc.). <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be accessed.
     * <li> int with default value and XML value is the XML value.
     * <li> int with default value and no XML value is the default value.
     * <li> int with no default value and no XML value is 0.
     * <li> int with no default value and misnamed XML value is 0.
     * <li> Renamed int with no default value and XML value is the XML value.
     * <li> int with no default value and invalid XML value is 0.
     * <li> float with no default value and an XML-defined Integer is the XML value.
     * <li> int with default value and XML value may be looked up from global namespace.
     * <li> int with default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> int of misnamed XML value may be looked up from global namespace.
     * <li> Renamed int with no default value and XML value may be looked up from global namespace.
     * <li> int of original name may be looked up from global namespace.
     * <li> int with no default value and invalid XML value may not be looked up from global namespace.
     * <li> float with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> int with default value and XML value may be looked up from session context.
     * <li> int with default value and no XML value may not be looked up from session context.
     * <li> int with no default value and no XML value may not be looked up from session context.
     * <li> int with no default value and misnamed XML value may not be looked up from session context.
     * <li> int of misnamed XML value may be looked up from session context.
     * <li> Renamed int with no default value and XML value may be looked up from session context.
     * <li> int of original name may be looked up from session context.
     * <li> int with no default value and invalid XML value may not be looked up from session context.
     * <li> float with no default value and an XML-defined Integer may be looked up from session context.
     * </ol>
     */
    @Test
    public void testSLRAdvEnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + AdvPrimFldBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SLRSB accessed successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (EJBException ex) {
            assertEquals("Expected EJBException test is wrong",
                         "discardInstance", ex.getMessage());
        }

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB, with Business Interfaces only, for methods
     * that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * Also test that the values injected into the methods may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be accessed.
     * <li> Injection methods with values are called (not injected into fields directly).
     * <li> String with default value and XML value is the XML value.
     * <li> String with default value and no XML value is the default value.
     * <li> String with no default value and no XML value is null.
     * <li> String with no default value and misnamed XML value is null.
     * <li> Renamed String with no default value and XML value is the XML value.
     * <li> Object with no default value and an XML-defined Integer is the XML value.
     * <li> String with default value and XML value may be looked up from global namespace.
     * <li> String with default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> String of misnamed XML value may be looked up from global namespace.
     * <li> Renamed String with no default value and XML value may be looked up from global namespace.
     * <li> String of original name may be looked up from global namespace.
     * <li> Object with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> Class level resource with name and type may be looked up from global namespace.
     * <li> Second class level resource with name only may be looked up from global namespace.
     * <li> String with default value and XML value may be looked up from session context.
     * <li> String with default value and no XML value may not be looked up from session context.
     * <li> String with no default value and no XML value may not be looked up from session context.
     * <li> String with no default value and misnamed XML value may not be looked up from session context.
     * <li> String of misnamed XML value may be looked up from session context.
     * <li> Renamed String with no default value and XML value may be looked up from session context.
     * <li> String of original name may be looked up from session context.
     * <li> Object with no default value and an XML-defined Integer may be looked up from session context.
     * <li> Class level resource with name and type may be looked up from session context.
     * <li> Second class level resource with name only may be looked up from session context.
     * </ol>
     */
    @Test
    public void testSLRAdvEnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + AdvObjMthdBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SLRSB accessed successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (EJBException ex) {
            assertEquals("Expected EJBException test is wrong",
                         "discardInstance", ex.getMessage());
        }

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB, with Business Interfaces only, for methods
     * that are the primitives (char, int, float, etc.). <p>
     *
     * Also test that the values injected into the methods may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be accessed.
     * <li> Injection methods with values are called (not injected into fields directly).
     * <li> int with default value and XML value is the XML value.
     * <li> int with default value and no XML value is the default value.
     * <li> int with no default value and no XML value is 0.
     * <li> int with no default value and misnamed XML value is 0.
     * <li> Renamed int with no default value and XML value is the XML value.
     * <li> int with no default value and invalid XML value is 0.
     * <li> float with no default value and an XML-defined Integer is the XML value.
     * <li> int with default value and XML value may be looked up from global namespace.
     * <li> int with default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> int of misnamed XML value may be looked up from global namespace.
     * <li> Renamed int with no default value and XML value may be looked up from global namespace.
     * <li> int of original name may be looked up from global namespace.
     * <li> int with no default value and invalid XML value may not be looked up from global namespace.
     * <li> float with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> int with default value and XML value may be looked up from session context.
     * <li> int with default value and no XML value may not be looked up from session context.
     * <li> int with no default value and no XML value may not be looked up from session context.
     * <li> int with no default value and misnamed XML value may not be looked up from session context.
     * <li> int of misnamed XML value may be looked up from session context.
     * <li> Renamed int with no default value and XML value may be looked up from session context.
     * <li> int of original name may be looked up from session context.
     * <li> int with no default value and invalid XML value may not be looked up from session context.
     * <li> float with no default value and an XML-defined Integer may be looked up from session context.
     * </ol>
     */
    @Test
    public void testSLRAdvEnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + AdvPrimMthdBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SLRSB accessed successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (EJBException ex) {
            assertEquals("Expected EJBException test is wrong",
                         "discardInstance", ex.getMessage());
        }

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB, with Component Interfaces, for fields
     * that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be created.
     * <li> String with default value and XML value is the XML value.
     * <li> String with default value and no XML value is the default value.
     * <li> String with no default value and no XML value is null.
     * <li> String with no default value and misnamed XML value is null.
     * <li> Renamed String with no default value and XML value is the XML value.
     * <li> Object with no default value and an XML-defined Integer is the XML value.
     * <li> String with default value and XML value may be looked up from global namespace.
     * <li> String with default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> String of misnamed XML value may be looked up from global namespace.
     * <li> Renamed String with no default value and XML value may be looked up from global namespace.
     * <li> String of original name may be looked up from global namespace.
     * <li> Object with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> Class level resource with name and type may be looked up from global namespace.
     * <li> Second class level resource with name only may be looked up from global namespace.
     * <li> String with default value and XML value may be looked up from session context.
     * <li> String with default value and no XML value may not be looked up from session context.
     * <li> String with no default value and no XML value may not be looked up from session context.
     * <li> String with no default value and misnamed XML value may not be looked up from session context.
     * <li> String of misnamed XML value may be looked up from session context.
     * <li> Renamed String with no default value and XML value may be looked up from session context.
     * <li> String of original name may be looked up from session context.
     * <li> Object with no default value and an XML-defined Integer may be looked up from session context.
     * <li> Class level resource with name and type may be looked up from session context.
     * <li> Second class level resource with name only may be looked up from session context.
     * </ol>
     */
    @Test
    public void testSLRAdvCompEnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteBinding(ivJNDI_SLREnvInjectObjFld,
                                                                         EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (RemoteException rex) {
            Throwable cause = rex.getCause(); // a nested RemoteException
            cause = cause.getCause(); // a nested EJBException
            assertEquals("Expected RemoteException test is wrong",
                         "discardInstance", cause.getMessage());
        }

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB, with Component Interfaces, for fields
     * that are the primitives (char, int, float, etc.). <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be created.
     * <li> int with default value and XML value is the XML value.
     * <li> int with default value and no XML value is the default value.
     * <li> int with no default value and no XML value is 0.
     * <li> int with no default value and misnamed XML value is 0.
     * <li> Renamed int with no default value and XML value is the XML value.
     * <li> int with no default value and invalid XML value is 0.
     * <li> float with no default value and an XML-defined Integer is the XML value.
     * <li> int with default value and XML value may be looked up from global namespace.
     * <li> int with default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> int of misnamed XML value may be looked up from global namespace.
     * <li> Renamed int with no default value and XML value may be looked up from global namespace.
     * <li> int of original name may be looked up from global namespace.
     * <li> int with no default value and invalid XML value may not be looked up from global namespace.
     * <li> float with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> int with default value and XML value may be looked up from session context.
     * <li> int with default value and no XML value may not be looked up from session context.
     * <li> int with no default value and no XML value may not be looked up from session context.
     * <li> int with no default value and misnamed XML value may not be looked up from session context.
     * <li> int of misnamed XML value may be looked up from session context.
     * <li> Renamed int with no default value and XML value may be looked up from session context.
     * <li> int of original name may be looked up from session context.
     * <li> int with no default value and invalid XML value may not be looked up from session context.
     * <li> float with no default value and an XML-defined Integer may be looked up from session context.
     * </ol>
     */
    @Test
    public void testSLRAdvCompEnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteBinding(ivJNDI_SLREnvInjectPrimFld,
                                                                         EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (RemoteException rex) {
            Throwable cause = rex.getCause(); // a nested RemoteException
            cause = cause.getCause(); // a nested EJBException
            assertEquals("Expected RemoteException test is wrong",
                         "discardInstance", cause.getMessage());
        }

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB, with Component Interfaces, for methods
     * that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * Also test that the values injected into the methods may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be created.
     * <li> Injection methods with values are called (not injected into fields directly).
     * <li> String with default value and XML value is the XML value.
     * <li> String with default value and no XML value is the default value.
     * <li> String with no default value and no XML value is null.
     * <li> String with no default value and misnamed XML value is null.
     * <li> Renamed String with no default value and XML value is the XML value.
     * <li> Object with no default value and an XML-defined Integer is the XML value.
     * <li> String with default value and XML value may be looked up from global namespace.
     * <li> String with default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and no XML value may not be looked up from global namespace.
     * <li> String with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> String of misnamed XML value may be looked up from global namespace.
     * <li> Renamed String with no default value and XML value may be looked up from global namespace.
     * <li> String of original name may be looked up from global namespace.
     * <li> Object with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> Class level resource with name and type may be looked up from global namespace.
     * <li> Second class level resource with name only may be looked up from global namespace.
     * <li> String with default value and XML value may be looked up from session context.
     * <li> String with default value and no XML value may not be looked up from session context.
     * <li> String with no default value and no XML value may not be looked up from session context.
     * <li> String with no default value and misnamed XML value may not be looked up from session context.
     * <li> String of misnamed XML value may be looked up from session context.
     * <li> Renamed String with no default value and XML value may be looked up from session context.
     * <li> String of original name may be looked up from session context.
     * <li> Object with no default value and an XML-defined Integer may be looked up from session context.
     * <li> Class level resource with name and type may be looked up from session context.
     * <li> Second class level resource with name only may be looked up from session context.
     * </ol>
     */
    @Test
    public void testSLRAdvCompEnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteBinding(ivJNDI_SLREnvInjectObjMthd,
                                                                         EnvInjectionEJBRemoteHome.class);

        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (RemoteException rex) {
            Throwable cause = rex.getCause(); // a nested RemoteException
            cause = cause.getCause(); // a nested EJBException
            assertEquals("Expected RemoteException test is wrong",
                         "discardInstance", cause.getMessage());
        }

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateless Session EJB, with Component Interfaces, for methods
     * that are the primitives (char, int, float, etc.). <p>
     *
     * Also test that the values injected into the methods may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be created.
     * <li> Injection methods with values are called (not injected into fields directly).
     * <li> int with default value and XML value is the XML value.
     * <li> int with default value and no XML value is the default value.
     * <li> int with no default value and no XML value is 0.
     * <li> int with no default value and misnamed XML value is 0.
     * <li> Renamed int with no default value and XML value is the XML value.
     * <li> int with no default value and invalid XML value is 0.
     * <li> float with no default value and an XML-defined Integer is the XML value.
     * <li> int with default value and XML value may be looked up from global namespace.
     * <li> int with default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and no XML value may not be looked up from global namespace.
     * <li> int with no default value and misnamed XML value may not be looked up from global namespace.
     * <li> int of misnamed XML value may be looked up from global namespace.
     * <li> Renamed int with no default value and XML value may be looked up from global namespace.
     * <li> int of original name may be looked up from global namespace.
     * <li> int with no default value and invalid XML value may not be looked up from global namespace.
     * <li> float with no default value and an XML-defined Integer may be looked up from global namespace.
     * <li> int with default value and XML value may be looked up from session context.
     * <li> int with default value and no XML value may not be looked up from session context.
     * <li> int with no default value and no XML value may not be looked up from session context.
     * <li> int with no default value and misnamed XML value may not be looked up from session context.
     * <li> int of misnamed XML value may be looked up from session context.
     * <li> Renamed int with no default value and XML value may be looked up from session context.
     * <li> int of original name may be looked up from session context.
     * <li> int with no default value and invalid XML value may not be looked up from session context.
     * <li> float with no default value and an XML-defined Integer may be looked up from session context.
     * </ol>
     */
    @Test
    public void testSLRAdvCompEnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507
        EnvInjectionEJBRemoteHome slHome = FATHelper.lookupRemoteBinding(ivJNDI_SLREnvInjectPrimMthd,
                                                                         EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

        try {
            // discardInstance is used to clear the local bean from the cache
            bean.discardInstance();
            fail("1a --> discardInstance should have thrown exception");
        } catch (RemoteException rex) {
            Throwable cause = rex.getCause(); // a nested RemoteException
            cause = cause.getCause(); // a nested EJBException
            assertEquals("Expected RemoteException test is wrong",
                         "discardInstance", cause.getMessage());
        }

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

}
