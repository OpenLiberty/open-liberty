// /I/ /W/ /G/ /U/   <-- CMVC Keywords, replace / with %
// 1.1 REGR/ws/code/ejbcontainer.test/src/com/ibm/ws/ejbcontainer/injection/mix/SuperEnvInjectionTest.java, WAS.ejbcontainer.fvt, WAS855.REGR, cf141822.02 2/27/10 16:51:53
//
// IBM Confidential OCO Source Material
// 5724-I63, 5724-H88 (C) COPYRIGHT International Business Machines Corp. 2006, 2010
//
// The source code for this program is not published or otherwise divested
// of its trade secrets, irrespective of what has been deposited with the
// U.S. Copyright Office.
//
// Module  :  BasicSLEnvInjectionTest.java
//
// Source File Description:
//
//     Tests EJB Container support for the superclass env injections.
//
// Change Activity:
//
// Reason    Version   Date     Userid    Change Description
// --------- --------- -------- --------- -----------------------------------------
// d435060   EJB3      20060717 kabecker : New Part
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
import com.ibm.ws.ejbcontainer.injection.mix.ejb.SuperEnvInjectionEJBLocal;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.SuperEnvInjectionEJBLocalHome;
import com.ibm.ws.ejbcontainer.injection.mix.ejb.SuperEnvInjectionLocal;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> SuperEnvInjectionTest .
 *
 * <dt><b>Test Author:</b> Katie Becker <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Basic EJB 3.0
 * Injection of Environment values on superclasses of beans. <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testSLSuperClassInjection - Test superclasses of SL Session Bean
 * <li>testSFSuperClassInjection - Test superclasses of SF Session Bean
 * <li>testSLCompSuperClassInjection - Test superclasses of Component SL Session Bean
 * <li>testSFCompSuperClassInjection - Test superclasses of Component SF Session Bean
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/SuperEnvInjectionServlet")
public class SuperEnvInjectionServlet extends FATServlet {
    private static final String CLASS_NAME = SuperEnvInjectionServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static final String PASSED = "Passed";

    // SimpleBinding name was used for the bean used by this test
    private static final String SimpleBindingPreFix = "com/ibm/ws/ejbcontainer/injection/mix/ejb/";

    // Names of application and module... for lookup.
    private static final String Application = "EJB3INJSMTestApp";
    private static final String Module = "EJB3INJSMBean.jar";

    // Names of the beans used for the test... for lookup.
    private static final String SuperSLBean = "SuperSLEnvInject";
    private static final String SuperSFBean = "SuperSFEnvInject";

    // Names of the interfaces used for the test
    private static final String SuperInjectionLocalInterface = SuperEnvInjectionLocal.class.getName();

    /** Jndi Names of the Bean Homes to use for the test. **/
    private String ivJNDI_SuperCompSLEnvInject = SimpleBindingPreFix + "SuperCompSLEnvInjectHome";
    private String ivJNDI_SuperCompSFEnvInject = SimpleBindingPreFix + "SuperCompSFEnvInjectHome";

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
     * Test injection to a superclass of a SL Session Bean using both
     * xml and annotations. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Environment entry injection will occur in the superclass of a Stateless Session bean.
     * <li> Injection will occur in a superclass's public, private, or protected fields or methods.
     * <li> Injection will occur in the superclass of a superclass of a bean.
     * <li> Injection will occur with the use of both a specified name and a defaulted name, where the
     * <li> defaulted name is in the form of BEAN_CLASS/fieldName
     * <li> Injection will occur in a superclass when <injection-target> is used.
     * </ol>
     *
     */
    @Test
    public void testSLSuperClassInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SuperEnvInjectionLocal bean = (SuperEnvInjectionLocal) FATHelper.lookupDefaultBindingEJBLocalInterface(SuperInjectionLocalInterface,
                                                                                                               Application,
                                                                                                               Module,
                                                                                                               SuperSLBean);
        assertNotNull("1 ---> SLLSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

    /**
     * Test injection to a superclass of a SF Session Bean using both
     * xml and annotations. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Environment entry injection will occur in the superclass of a Stateful Session bean.
     * <li> Injection will occur in a superclass's public, private, or protected fields or methods.
     * <li> Injection will occur in the superclass of a superclass of a bean.
     * <li> Injection will occur with the use of both a specified name and a defaulted name, where the
     * <li> defaulted name is in the form of BEAN_CLASS/fieldName
     * <li> Injection will occur in a superclass when <injection-target> is used.
     * </ol>
     *
     */
    @Test
    public void testSFSuperClassInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SuperEnvInjectionLocal bean = (SuperEnvInjectionLocal) FATHelper.lookupDefaultBindingEJBLocalInterface(SuperInjectionLocalInterface,
                                                                                                               Application,
                                                                                                               Module,
                                                                                                               SuperSFBean);
        assertNotNull("1 ---> SFLSB accessed successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results", PASSED, bean.verifyEnvInjection(testpoint++));

    }

    /**
     * Test injection to a superclass of a SL Session Bean with Component Interfaces using both
     * xml and annotations. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Environment entry injection will occur in the superclass of a Stateless Session bean.
     * <li> Injection will occur in a superclass's public, private, or protected fields or methods.
     * <li> Injection will occur in the superclass of a superclass of a bean.
     * <li> Injection will occur with the use of both a specified name and a defaulted name, where the
     * <li> defaulted name is in the form of BEAN_CLASS/fieldName
     * <li> Injection will occur in a superclass when <injection-target> is used.
     * </ol>
     *
     */
    @Test
    public void testSLCompSuperClassInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SuperEnvInjectionEJBLocalHome slHome = (SuperEnvInjectionEJBLocalHome) FATHelper.lookupLocalBinding(ivJNDI_SuperCompSLEnvInject);
        SuperEnvInjectionEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SLLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

    /**
     * Test injection to a superclass of a SF Session Bean with Component Interfaces using both
     * xml and annotations. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Environment entry injection will occur in the superclass of a Stateful Session bean.
     * <li> Injection will occur in a superclass's public, private, or protected fields or methods.
     * <li> Injection will occur in the superclass of a superclass of a bean.
     * <li> Injection will occur with the use of both a specified name and a defaulted name, where the
     * <li> defaulted name is in the form of BEAN_CLASS/fieldName
     * <li> Injection will occur in a superclass when <injection-target> is used.
     * </ol>
     *
     */
    @Test
    public void testSFCompSuperClassInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        SuperEnvInjectionEJBLocalHome slHome = (SuperEnvInjectionEJBLocalHome) FATHelper.lookupLocalBinding(ivJNDI_SuperCompSFEnvInject);
        SuperEnvInjectionEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));
    }

}
