// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/BasicSFRemoteEnvInjectionTest.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 2/27/10 16:51:42
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  BasicSFRemoteEnvInjectionTest.java
//
// Source File Description:
//
//     Tests EJB Container support for the Basic EJB 3.0
//     Injection of Environment values on Stateful Session beans.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d420547   EJB3      20070110 jrbauer  : New part
// d446507   EJB3      20070618 tonyei   : Update lookups for new binding format
// F896-23013 WAS70    20100215 tkb      : converted to REGR release
// --------- --------- -------- --------- -----------------------------------------

package com.ibm.ws.ejbcontainer.injection.mix.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

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
 * <dt><b>Test Name:</b> BasicSFRemoteEnvInjectionTest .
 *
 * <dt><b>Test Author:</b> Tracy Burroughs / Jeremy Bauer <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Basic EJB 3.0
 * Injection of Environment values on Stateful Session beans. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSFREnvObjFldInjection - Business Interface: Object Field Injection
 * <li>testSFREnvPrimFldInjection - Business Interface: Primitive Field Injection
 * <li>testSFREnvObjMthdInjection - Business Interface: Object Method Injection
 * <li>testSFREnvPrimMthdInjection - Business Interface: Primitive Method Injection
 * <li>testSFRCompEnvObjFldInjection - Component Interface: Object Field Injection
 * <li>testSFRCompEnvPrimFldInjection - Component Interface: Primitive Field Injection
 * <li>testSFRCompEnvObjMthdInjection - Component Interface: Object Method Injection
 * <li>testSFRCompEnvPrimMthdInjection - Component Interface: Primitive Method Injection
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/BasicSFRemoteEnvInjectionServlet")
public class BasicSFRemoteEnvInjectionServlet extends FATServlet {
    private static final String CLASS_NAME = BasicSFRemoteEnvInjectionServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String PASSED = "Passed";

    // SimpleBinding name was used for the bean used by this test
    private static final String SimpleBindingPreFix = "com/ibm/ws/ejbcontainer/injection/mix/ejb/";

    // Names of the beans used for the test... for lookup.
    private static final String BasicObjFldBean = "BasicSFEnvInjectObjFld";
    private static final String BasicPrimFldBean = "BasicSFEnvInjectPrimFld";
    private static final String BasicObjMthdBean = "BasicSFEnvInjectObjMthd";
    private static final String BasicPrimMthdBean = "BasicSFEnvInjectPrimMthd";
    private static final String CompObjFldBean = "CompSFEnvInjectObjFld";
    private static final String CompPrimFldBean = "CompSFEnvInjectPrimFld";
    private static final String CompObjMthdBean = "CompSFEnvInjectObjMthd";
    private static final String CompPrimMthdBean = "CompSFEnvInjectPrimMthd";

    // Names of the interfaces used for the test
    private static final String EnvInjectionEJBRemoteHomeInterface = EnvInjectionEJBRemoteHome.class.getName();

    /** Jndi Names of the Bean Homes to use for the test. **/
    private String ivJNDI_SFREnvInjectObjFld = SimpleBindingPreFix + CompObjFldBean +
                                               "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SFREnvInjectPrimFld = SimpleBindingPreFix + CompPrimFldBean +
                                                "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SFREnvInjectObjMthd = SimpleBindingPreFix + CompObjMthdBean +
                                                "#" + EnvInjectionEJBRemoteHomeInterface;
    private String ivJNDI_SFREnvInjectPrimMthd = SimpleBindingPreFix + CompPrimMthdBean +
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
     * Stateful Session EJB, with Business Interfaces only, for fields
     * that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be accessed.
     * <li> String field injected properly from environment entry.
     * <li> Character field injected properly from environment entry.
     * <li> Byte field injected properly from environment entry.
     * <li> Short field injected properly from environment entry.
     * <li> Integer field injected properly from environment entry.
     * <li> Long field injected properly from environment entry.
     * <li> Boolean field injected properly from environment entry.
     * <li> Double field injected properly from environment entry.
     * <li> Float field injected properly from environment entry.
     * <li> String value may be looked up from global namespace.
     * <li> Character value may be looked up from global namespace.
     * <li> Byte value may be looked up from global namespace.
     * <li> Short value may be looked up from global namespace.
     * <li> Integer value may be looked up from global namespace.
     * <li> Long value may be looked up from global namespace.
     * <li> Boolean value may be looked up from global namespace.
     * <li> Double value may be looked up from global namespace.
     * <li> Float value may be looked up from global namespace.
     * <li> String value may be looked up from session context.
     * <li> Character value may be looked up from session context.
     * <li> Byte value may be looked up from session context.
     * <li> Short value may be looked up from session context.
     * <li> Integer value may be looked up from session context.
     * <li> Long value may be looked up from session context.
     * <li> Boolean value may be looked up from session context.
     * <li> Double value may be looked up from session context.
     * <li> Float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the cached state. <p>
     */
    @Test
    public void testSFREnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + BasicObjFldBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SFLSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 29;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Business Interfaces only, for fields
     * that are the primitives (char, int, float, etc.). <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be accessed.
     * <li> char field injected properly from environment entry.
     * <li> byte field injected properly from environment entry.
     * <li> short field injected properly from environment entry.
     * <li> int field injected properly from environment entry.
     * <li> long field injected properly from environment entry.
     * <li> boolean field injected properly from environment entry.
     * <li> double field injected properly from environment entry.
     * <li> float field injected properly from environment entry.
     * <li> char value may be looked up from global namespace.
     * <li> byte value may be looked up from global namespace.
     * <li> short value may be looked up from global namespace.
     * <li> int value may be looked up from global namespace.
     * <li> long value may be looked up from global namespace.
     * <li> boolean value may be looked up from global namespace.
     * <li> double value may be looked up from global namespace.
     * <li> float value may be looked up from global namespace.
     * <li> char value may be looked up from session context.
     * <li> byte value may be looked up from session context.
     * <li> short value may be looked up from session context.
     * <li> int value may be looked up from session context.
     * <li> long value may be looked up from session context.
     * <li> boolean value may be looked up from session context.
     * <li> double value may be looked up from session context.
     * <li> float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does not occur from the cached state. <p>
     */
    @Test
    public void testSFREnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + BasicPrimFldBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SFLSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 26;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Business Interfaces only, for methods
     * that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * Also test that the values injected into the methods may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be accessed.
     * <li> Injection methods are called (not injected into fields directly).
     * <li> String method injected properly from environment entry.
     * <li> Character method injected properly from environment entry.
     * <li> Byte method injected properly from environment entry.
     * <li> Short method injected properly from environment entry.
     * <li> Integer method injected properly from environment entry.
     * <li> Long method injected properly from environment entry.
     * <li> Boolean method injected properly from environment entry.
     * <li> Double method injected properly from environment entry.
     * <li> Float method injected properly from environment entry.
     * <li> String value may be looked up from global namespace.
     * <li> Character value may be looked up from global namespace.
     * <li> Byte value may be looked up from global namespace.
     * <li> Short value may be looked up from global namespace.
     * <li> Integer value may be looked up from global namespace.
     * <li> Long value may be looked up from global namespace.
     * <li> Boolean value may be looked up from global namespace.
     * <li> Double value may be looked up from global namespace.
     * <li> Float value may be looked up from global namespace.
     * <li> String value may be looked up from session context.
     * <li> Character value may be looked up from session context.
     * <li> Byte value may be looked up from session context.
     * <li> Short value may be looked up from session context.
     * <li> Integer value may be looked up from session context.
     * <li> Long value may be looked up from session context.
     * <li> Boolean value may be looked up from session context.
     * <li> Double value may be looked up from session context.
     * <li> Float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does no occur from the cached state. <p>
     */
    @Test
    public void testSFREnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + BasicObjMthdBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SFLSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 30;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Business Interfaces only, for methods
     * that are the primitives (char, int, float, etc.). <p>
     *
     * Also test that the values injected into the methods may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be accessed.
     * <li> Injection methods are called (not injected into fields directly).
     * <li> char method injected properly from environment entry.
     * <li> byte method injected properly from environment entry.
     * <li> short method injected properly from environment entry.
     * <li> int method injected properly from environment entry.
     * <li> long method injected properly from environment entry.
     * <li> boolean method injected properly from environment entry.
     * <li> double method injected properly from environment entry.
     * <li> float method injected properly from environment entry.
     * <li> char value may be looked up from global namespace.
     * <li> byte value may be looked up from global namespace.
     * <li> short value may be looked up from global namespace.
     * <li> int value may be looked up from global namespace.
     * <li> long value may be looked up from global namespace.
     * <li> boolean value may be looked up from global namespace.
     * <li> double value may be looked up from global namespace.
     * <li> float value may be looked up from global namespace.
     * <li> char value may be looked up from session context.
     * <li> byte value may be looked up from session context.
     * <li> short value may be looked up from session context.
     * <li> int value may be looked up from session context.
     * <li> long value may be looked up from session context.
     * <li> boolean value may be looked up from session context.
     * <li> double value may be looked up from session context.
     * <li> float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does no occur from the cached state. <p>
     */
    @Test
    public void testSFREnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionRemote bean = FATHelper.lookupRemoteBinding(SimpleBindingPreFix + BasicPrimMthdBean,
                                                                EnvInjectionRemote.class);
        assertNotNull("1 ---> SFLSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 27;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Component Interfaces, for fields
     * that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be created.
     * <li> String field injected properly from environment entry.
     * <li> Character field injected properly from environment entry.
     * <li> Byte field injected properly from environment entry.
     * <li> Short field injected properly from environment entry.
     * <li> Integer field injected properly from environment entry.
     * <li> Long field injected properly from environment entry.
     * <li> Boolean field injected properly from environment entry.
     * <li> Double field injected properly from environment entry.
     * <li> Float field injected properly from environment entry.
     * <li> String value may be looked up from global namespace.
     * <li> Character value may be looked up from global namespace.
     * <li> Byte value may be looked up from global namespace.
     * <li> Short value may be looked up from global namespace.
     * <li> Integer value may be looked up from global namespace.
     * <li> Long value may be looked up from global namespace.
     * <li> Boolean value may be looked up from global namespace.
     * <li> Double value may be looked up from global namespace.
     * <li> Float value may be looked up from global namespace.
     * <li> String value may be looked up from session context.
     * <li> Character value may be looked up from session context.
     * <li> Byte value may be looked up from session context.
     * <li> Short value may be looked up from session context.
     * <li> Integer value may be looked up from session context.
     * <li> Long value may be looked up from session context.
     * <li> Boolean value may be looked up from session context.
     * <li> Double value may be looked up from session context.
     * <li> Float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does not occur from the cached state. <p>
     */
    @Test
    public void testSFRCompEnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBRemoteHome sfHome = FATHelper.lookupRemoteBinding(ivJNDI_SFREnvInjectObjFld,
                                                                         EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 29;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Component Interfaces, for fields
     * that are the primitives (char, int, float, etc.). <p>
     *
     * Also test that the values injected into the fields may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be created.
     * <li> char field injected properly from environment entry.
     * <li> byte field injected properly from environment entry.
     * <li> short field injected properly from environment entry.
     * <li> int field injected properly from environment entry.
     * <li> long field injected properly from environment entry.
     * <li> boolean field injected properly from environment entry.
     * <li> double field injected properly from environment entry.
     * <li> float field injected properly from environment entry.
     * <li> char value may be looked up from global namespace.
     * <li> byte value may be looked up from global namespace.
     * <li> short value may be looked up from global namespace.
     * <li> int value may be looked up from global namespace.
     * <li> long value may be looked up from global namespace.
     * <li> boolean value may be looked up from global namespace.
     * <li> double value may be looked up from global namespace.
     * <li> float value may be looked up from global namespace.
     * <li> char value may be looked up from session context.
     * <li> byte value may be looked up from session context.
     * <li> short value may be looked up from session context.
     * <li> int value may be looked up from session context.
     * <li> long value may be looked up from session context.
     * <li> boolean value may be looked up from session context.
     * <li> double value may be looked up from session context.
     * <li> float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does not occur from the cached state. <p>
     */
    @Test
    public void testSFRCompEnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 updated lookup for the new binding format
        EnvInjectionEJBRemoteHome sfHome = FATHelper.lookupRemoteBinding(ivJNDI_SFREnvInjectPrimFld,
                                                                         EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 26;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Component Interfaces, for methods
     * that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * Also test that the values injected into the methods may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be created.
     * <li> Injection methods are called (not injected into fields directly).
     * <li> String method injected properly from environment entry.
     * <li> Character method injected properly from environment entry.
     * <li> Byte method injected properly from environment entry.
     * <li> Short method injected properly from environment entry.
     * <li> Integer method injected properly from environment entry.
     * <li> Long method injected properly from environment entry.
     * <li> Boolean method injected properly from environment entry.
     * <li> Double method injected properly from environment entry.
     * <li> Float method injected properly from environment entry.
     * <li> String value may be looked up from global namespace.
     * <li> Character value may be looked up from global namespace.
     * <li> Byte value may be looked up from global namespace.
     * <li> Short value may be looked up from global namespace.
     * <li> Integer value may be looked up from global namespace.
     * <li> Long value may be looked up from global namespace.
     * <li> Boolean value may be looked up from global namespace.
     * <li> Double value may be looked up from global namespace.
     * <li> Float value may be looked up from global namespace.
     * <li> String value may be looked up from session context.
     * <li> Character value may be looked up from session context.
     * <li> Byte value may be looked up from session context.
     * <li> Short value may be looked up from session context.
     * <li> Integer value may be looked up from session context.
     * <li> Long value may be looked up from session context.
     * <li> Boolean value may be looked up from session context.
     * <li> Double value may be looked up from session context.
     * <li> Float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does not occur from the cached state. <p>
     */
    @Test
    public void testSFRCompEnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507 update lookup for new bindings
        EnvInjectionEJBRemoteHome sfHome = FATHelper.lookupRemoteBinding(ivJNDI_SFREnvInjectObjMthd,
                                                                         EnvInjectionEJBRemoteHome.class);

        EnvInjectionEJBRemote bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 30;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB, with Component Interfaces, for methods
     * that are the primitives (char, int, float, etc.). <p>
     *
     * Also test that the values injected into the methods may be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateless Session bean with injection may be created.
     * <li> Injection methods are called (not injected into fields directly).
     * <li> char method injected properly from environment entry.
     * <li> byte method injected properly from environment entry.
     * <li> short method injected properly from environment entry.
     * <li> int method injected properly from environment entry.
     * <li> long method injected properly from environment entry.
     * <li> boolean method injected properly from environment entry.
     * <li> double method injected properly from environment entry.
     * <li> float method injected properly from environment entry.
     * <li> char value may be looked up from global namespace.
     * <li> byte value may be looked up from global namespace.
     * <li> short value may be looked up from global namespace.
     * <li> int value may be looked up from global namespace.
     * <li> long value may be looked up from global namespace.
     * <li> boolean value may be looked up from global namespace.
     * <li> double value may be looked up from global namespace.
     * <li> float value may be looked up from global namespace.
     * <li> char value may be looked up from session context.
     * <li> byte value may be looked up from session context.
     * <li> short value may be looked up from session context.
     * <li> int value may be looked up from session context.
     * <li> long value may be looked up from session context.
     * <li> boolean value may be looked up from session context.
     * <li> double value may be looked up from session context.
     * <li> float value may be looked up from session context.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does not occur from the cached state. <p>
     */
    @Test
    public void testSFRCompEnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        //446507
        EnvInjectionEJBRemoteHome sfHome = FATHelper.lookupRemoteBinding(ivJNDI_SFREnvInjectPrimMthd,
                                                                         EnvInjectionEJBRemoteHome.class);
        EnvInjectionEJBRemote bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from cached state
        testpoint = 27;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));
    }

}
