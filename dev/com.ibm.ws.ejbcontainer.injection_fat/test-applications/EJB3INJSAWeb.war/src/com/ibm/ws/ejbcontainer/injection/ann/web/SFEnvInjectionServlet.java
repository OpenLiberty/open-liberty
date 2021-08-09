/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.injection.ann.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.EnvInjectionEJBLocal;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.EnvInjectionEJBLocalHome;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.PetStoreEJBLocal;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.PetStoreEJBLocalHome;
import com.ibm.ws.ejbcontainer.injection.ann.ejb.PetStoreLocal;

import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt><b>Test Name:</b> SFEnvInjectionTest .
 *
 * <dt><b>Test Author:</b> Brian Decker / Urrvano Gamez, Jr. <p>
 *
 * <dt><b>Test Description:</b>
 * <dd>Tests EJB Container support for the Basic EJB 3.0
 * Injection of Environment values on Stateful Session beans
 * with no XML (and therefore no env values). <p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>01 testSFLEnvObjFldInjection - Business Interface: Object Field Injection
 * <li>02 testSFLEnvPrimFldInjection - Business Interface: Primitive Field Injection
 * <li>03 testSFLEnvObjMthdInjection - Business Interface: Object Method Injection
 * <li>04 testSFLEnvPrimMthdInjection - Business Interface: Primitive Method Injection
 * <li>05 testSFLCompEnvObjFldInjection - Component Interface: Object Field Injection
 * <li>06 testSFLCompEnvPrimFldInjection - Component Interface: Primitive Field Injection
 * <li>07 testSFLCompEnvObjMthdInjection - Component Interface: Object Method Injection
 * <li>08 testSFLCompEnvPrimMthdInjection - Component Interface: Primitive Method Injection
 * <li>09 testSFLEJBClassLevelInjection - Business Interface: EJB class level ENC injection
 * <li>10 testSFLEJBFieldLevelInjection - Business Interface: EJB field level ENC injection
 * <li>11 testSFLEJBMethodLevelInjection - Business Interface: EJB method level ENC injection
 * <li>12 testSFLCompEJBClassLevelInjection
 * - Component Interface: Component EJB class level ENC injection
 * <li>13 testSFLCompEJBFieldLevelBeanNameInjection
 * - Component Interface: Component EJB field level ENC injection with beanName specified
 * <li>14 testSFLCompEJBMethodLevelInjection
 * - Component Interface: Component EJB method level ENC injection
 * <li>//commented out: test15 - Component Interface: Component EJB field level ENC injection with beanInterface specified that overrides type
 * <li>16 testSFLCompEJBFieldLevelInterfaceInjection
 * - Component Interface: Component EJB field level ENC injection with beanInterface specified
 * <li>17 testSFLCompEJBFieldLevelAutoInjection
 * - Component Interface: Component EJB field level ENC injection no attributes specified (i.e. total autolink)
 * <li>18 testSFL30CompEJBClassLevelInjection
 * - Business Interface: Component EJB class level ENC injection
 * <li>19 testSFL30CompEJBFieldLevelBeanNameInjection
 * - Business Interface: Component EJB field level ENC injection with beanName specified
 * <li>20 testSFL30CompEJBMethodLevelInjection
 * - Business Interface: Component EJB method level ENC injection
 * <li>//commented out: test21 - Business Interface: Component EJB field level ENC injection with beanInterface specified that overrides type
 * <li>22 testSFL30CompEJBFieldLevelInterfaceInjection
 * - Business Interface: Component EJB field level ENC injection with beanInterface specified
 * <li>23 testSFL30CompEJBFieldLevelAutoInjection
 * - Business Interface: Component EJB field level ENC injection no attributes specified (i.e. total autolink)
 * <li>24 testSFL30EJBClassLevelInjection
 * - Business Interface: Business EJB class level ENC injection
 * <li>25 testSFL30EJBFieldLevelBeanNameOverrideInjection
 * - Business Interface: Business EJB field level ENC injection with beanName specified that overrides type
 * <li>26 testSFL30EJBMethodLevelInjection
 * - Business Interface: Business EJB method level ENC injection
 * <li>27 testSFL30EJBFieldLevelInterfaceOverrideInjection
 * - Business Interface: Business EJB field level ENC injection with beanInterface specified that overrides type
 * <li>28 testSFL30EJBFieldLevelBeanNameInjection
 * - Business Interface: Business EJB field level ENC injection with beanName specified
 * <li>29 testSFL30EJBFieldLevelAutoInjection
 * - Business Interface: Business EJB field level ENC injection no attributes specified (i.e. total autolink)
 * <li>30 testSFLComp30EJBClassLevelInjection
 * - Component Interface: Business EJB class level ENC injection
 * <li>31 testSFLComp30EJBFieldLevelInjection
 * - Component Interface: Business EJB field level ENC injection
 * <li>32 testSFLComp30EJBMethodLevelInjection
 * - Component Interface: Business EJB method level ENC injection
 * <li>33 testSFLComp30EJBFieldLevelInterfaceOverrideInjection
 * - Component Interface: Business EJB field level ENC injection with beanInterface specified that overrides type
 * <li>34 testSFLComp30EJBFieldLevelBeanNameInjection
 * - Component Interface: Business EJB field level ENC injection with beanName specified
 * <li>35 testSFLComp30EJBFieldLevelAutoInjection
 * - Component Interface: Business EJB field level ENC injection no attributes specified (i.e. total autolink)
 * <li>--- Cross Module Injection tests: The injected bean is located in a different jar than the referencing bean.
 * <li>36 testSFLCrossModuleAutoLinkInjection
 * - The bean is being injected using the EJB annotation with no attributes specified (i.e. autolink).
 * <li>37 testSFLCrossModuleBeanNameInjection
 * - The bean is being injected using the EJB annotation with the beanName attribute specified.
 * <li>38 testSFLCrossModuleModuleBeanNameInjection
 * - The bean being injected is located in two different jars neither of which contain the referencing
 * bean. The bean will be injected using the EJB annotation with the beanName attribute pointing to one of these
 * specific beans. This should NOT result in an error.
 * <li>39 testSFLCrossModuleDuplicateBeanNameInjection
 * - The bean being injected is located in the same jar as the referencing bean as well as in a different
 * jar than the referencing bean. Here the bean will be injected using the EJB annotation with the beanName attribute
 * specifying the bean located in a different jar.
 * <li>40 testSFLCrossModuleDuplicateAutoInjection
 * - The bean being injected is located in the same jar as the referencing bean as well as in a different
 * jar than the referencing bean. Here the bean will be injected using the EJB annotation with no attributes specified
 * (i.e. pure autolink).
 * <li>41 testSFLCrossModuleDuplicateFailedInjection
 * - Tests that a bean will fail to create if it attempts to inject a bean where the App contains multiple
 * beans with same interface.
 * </ul>
 * <br>Data Sources - None
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/SFEnvInjectionServlet")
public class SFEnvInjectionServlet extends FATServlet {

    private static final String PASSED = "Passed";

    // Names of application and module... for lookup.
    private static final String Application = "EJB3INJSATestApp";
    private static final String Module = "EJB3INJSABean";

    // Names of the beans used for the test... for lookup.
    private static final String CompObjFldBean = "CompSFEnvInjectObjFld";
    private static final String CompPrimFldBean = "CompSFEnvInjectPrimFld";
    private static final String CompObjMthdBean = "CompSFEnvInjectObjMthd";
    private static final String CompPrimMthdBean = "CompSFEnvInjectPrimMthd";
    private static final String CompPetStoreBean = "CompPetStore";
    private static final String PetStoreBean = "PetStore";

    // Names of the interfaces used for the test
    private static final String EnvInjectionLocalHomeInterface = EnvInjectionEJBLocalHome.class.getName();
    private static final String PetStoreEJBLocalHomeInterface = PetStoreEJBLocalHome.class.getName();
    private static final String PetStoreLocalInterface = PetStoreLocal.class.getName();

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB with no XML, with Component Interfaces,
     * for fields that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * All of the fields should be null as no injection occurs when a
     * value is not specified in XML. <p>
     *
     * Also test that the values injected into the fields may not be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean with injection may be accessed.
     * <li> String field is null.
     * <li> Character field is null.
     * <li> Byte field is null.
     * <li> Short field is null.
     * <li> Integer field is null.
     * <li> Long field is null.
     * <li> Boolean field is null.
     * <li> Double field is null.
     * <li> Float field is null.
     * <li> String value may not be looked up from global namespace - NameNotFound.
     * <li> Character value may not be looked up from global namespace - NameNotFound.
     * <li> Byte value may not be looked up from global namespace - NameNotFound.
     * <li> Short value may not be looked up from global namespace - NameNotFound.
     * <li> Integer value may not be looked up from global namespace - NameNotFound.
     * <li> Long value may not be looked up from global namespace - NameNotFound.
     * <li> Boolean value may not be looked up from global namespace - NameNotFound.
     * <li> Double value may not be looked up from global namespace - NameNotFound.
     * <li> Float value may not be looked up from global namespace - NameNotFound.
     * <li> String value may not be looked up from session context - IllegalArgument.
     * <li> Character value may not be looked up from session context - IllegalArgument.
     * <li> Byte value may not be looked up from session context - IllegalArgument.
     * <li> Short value may not be looked up from session context - IllegalArgument.
     * <li> Integer value may not be looked up from session context - IllegalArgument.
     * <li> Long value may not be looked up from session context - IllegalArgument.
     * <li> Boolean value may not be looked up from session context - IllegalArgument.
     * <li> Double value may not be looked up from session context - IllegalArgument.
     * <li> Float value may not be looked up from session context - IllegalArgument.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSFLCompEnvObjFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBLocalHome slHome = (EnvInjectionEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(EnvInjectionLocalHomeInterface,
                                                                                                                 Application,
                                                                                                                 Module,
                                                                                                                 CompObjFldBean);
        EnvInjectionEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 29;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }

    /**
     * Test Field injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB with no XML, with Component Interfaces,
     * for fields that are the primitives (char, int, float, etc.). <p>
     *
     * All of the fields should be defaulted as no injection occurs when a
     * value is not specified in XML. <p>
     *
     * Also test that the values injected into the fields may not be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean with injection may be accessed.
     * <li> char value is \u0000.
     * <li> byte value is 0.
     * <li> short value is 0.
     * <li> int value is 0.
     * <li> long value is 0L.
     * <li> boolean value is false.
     * <li> double value is 0.0D.
     * <li> float value is 0.0F.
     * <li> char value may not be looked up from global namespace - NameNotFound.
     * <li> byte value may not be looked up from global namespace - NameNotFound.
     * <li> short value may not be looked up from global namespace - NameNotFound.
     * <li> int value may not be looked up from global namespace - NameNotFound.
     * <li> long value may not be looked up from global namespace - NameNotFound.
     * <li> boolean value may not be looked up from global namespace - NameNotFound.
     * <li> double value may not be looked up from global namespace - NameNotFound.
     * <li> float value may not be looked up from global namespace - NameNotFound.
     * <li> char value may not be looked up from session context - IllegalArgument.
     * <li> byte value may not be looked up from session context - IllegalArgument.
     * <li> short value may not be looked up from session context - IllegalArgument.
     * <li> int value may not be looked up from session context - IllegalArgument.
     * <li> long value may not be looked up from session context - IllegalArgument.
     * <li> boolean value may not be looked up from session context - IllegalArgument.
     * <li> double value may not be looked up from session context - IllegalArgument.
     * <li> float value may not be looked up from session context - IllegalArgument.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSFLCompEnvPrimFldInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBLocalHome slHome = (EnvInjectionEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(EnvInjectionLocalHomeInterface,
                                                                                                                 Application,
                                                                                                                 Module,
                                                                                                                 CompPrimFldBean);
        EnvInjectionEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 26;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB with no XML, with Component Interfaces,
     * for methods that are the Object primitives (String, Integer, Float, etc.). <p>
     *
     * All of the values should be null as no injection occurs when a
     * value is not specified in XML. Likewise, none of the injection
     * methods should be called.<p>
     *
     * Also test that the values injected into the methods may not be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean with injection may be accessed.
     * <li> Injection methods are not called.
     * <li> String field is null.
     * <li> Character field is null.
     * <li> Byte field is null.
     * <li> Short field is null.
     * <li> Integer field is null.
     * <li> Long field is null.
     * <li> Boolean field is null.
     * <li> Double field is null.
     * <li> Float field is null.
     * <li> String value may not be looked up from global namespace - NameNotFound.
     * <li> Character value may not be looked up from global namespace - NameNotFound.
     * <li> Byte value may not be looked up from global namespace - NameNotFound.
     * <li> Short value may not be looked up from global namespace - NameNotFound.
     * <li> Integer value may not be looked up from global namespace - NameNotFound.
     * <li> Long value may not be looked up from global namespace - NameNotFound.
     * <li> Boolean value may not be looked up from global namespace - NameNotFound.
     * <li> Double value may not be looked up from global namespace - NameNotFound.
     * <li> Float value may not be looked up from global namespace - NameNotFound.
     * <li> String value may not be looked up from session context - IllegalArgument.
     * <li> Character value may not be looked up from session context - IllegalArgument.
     * <li> Byte value may not be looked up from session context - IllegalArgument.
     * <li> Short value may not be looked up from session context - IllegalArgument.
     * <li> Integer value may not be looked up from session context - IllegalArgument.
     * <li> Long value may not be looked up from session context - IllegalArgument.
     * <li> Boolean value may not be looked up from session context - IllegalArgument.
     * <li> Double value may not be looked up from session context - IllegalArgument.
     * <li> Float value may not be looked up from session context - IllegalArgument.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSFLCompEnvObjMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBLocalHome slHome = (EnvInjectionEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(EnvInjectionLocalHomeInterface,
                                                                                                                 Application,
                                                                                                                 Module,
                                                                                                                 CompObjMthdBean);
        EnvInjectionEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 30;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * Test Method injection of simple environment entries on an EJB 3.0
     * Stateful Session EJB with no XML, with Component Interfaces,
     * for methods that are the primitives (char, int, float, etc.). <p>
     *
     * All of the values should be defaulted as no injection occurs when a
     * value is not specified in XML. Likewise, none of the injection
     * methods should be called.<p>
     *
     * Also test that the values injected into the methods may not be looked
     * up through both the global namespace and the session context. <p>
     *
     * This test will confirm the following :
     * <ol>
     * <li> Stateful Session bean with injection may be accessed.
     * <li> Injection methods are not called.
     * <li> char value is \u0000.
     * <li> byte value is 0.
     * <li> short value is 0.
     * <li> int value is 0.
     * <li> long value is 0L.
     * <li> boolean value is false.
     * <li> double value is 0.0D.
     * <li> float value is 0.0F.
     * <li> char value may not be looked up from global namespace - NameNotFound.
     * <li> byte value may not be looked up from global namespace - NameNotFound.
     * <li> short value may not be looked up from global namespace - NameNotFound.
     * <li> int value may not be looked up from global namespace - NameNotFound.
     * <li> long value may not be looked up from global namespace - NameNotFound.
     * <li> boolean value may not be looked up from global namespace - NameNotFound.
     * <li> double value may not be looked up from global namespace - NameNotFound.
     * <li> float value may not be looked up from global namespace - NameNotFound.
     * <li> char value may not be looked up from session context - IllegalArgument.
     * <li> byte value may not be looked up from session context - IllegalArgument.
     * <li> short value may not be looked up from session context - IllegalArgument.
     * <li> int value may not be looked up from session context - IllegalArgument.
     * <li> long value may not be looked up from session context - IllegalArgument.
     * <li> boolean value may not be looked up from session context - IllegalArgument.
     * <li> double value may not be looked up from session context - IllegalArgument.
     * <li> float value may not be looked up from session context - IllegalArgument.
     * </ol>
     *
     * And, all but the first (above) will be repeated, to insure injection
     * does NOT occur from the pooled state. <p>
     */
    @Test
    public void testSFLCompEnvPrimMthdInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SL Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        EnvInjectionEJBLocalHome slHome = (EnvInjectionEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(EnvInjectionLocalHomeInterface,
                                                                                                                 Application,
                                                                                                                 Module,
                                                                                                                 CompPrimMthdBean);
        EnvInjectionEJBLocal bean = slHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyEnvInjection(testpoint++));

        // Repeat - to verify no injection from pooled state
        testpoint = 27;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.verifyNoEnvInjection(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * This test calls a CompEJB A which has CompEJB B injected into its environment
     * at a class level. CompEJB A looks up CompEJB B's home from its session context,
     * uses that home to create a bean and then calls a method on the bean.
     */
    @Test
    public void testSFLCompEJBClassLevelInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreEJBLocalHome sfHome = (PetStoreEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreEJBLocalHomeInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         CompPetStoreBean);
        PetStoreEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getDogClsComp(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * This test calls a CompEJB A which has CompEJB B injected into its environment
     * at the field level. This EJB injection specifies a value for the beanName
     * attribute. CompEJB A uses the injected CompEJB B's home to create
     * a bean and then calls a method on the bean. Next, CompEJB A looks up CompEJB
     * B's home from its session context, uses that home to create a bean and
     * then calls a method on the bean.
     */
    @Test
    public void testSFLCompEJBFieldLevelBeanNameInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreEJBLocalHome sfHome = (PetStoreEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreEJBLocalHomeInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         CompPetStoreBean);
        PetStoreEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getDogFldComp2(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * This test calls a CompEJB A which has CompEJB B injected into its environment
     * at the method level. CompEJB A uses the injected CompEJB B's home to create
     * a bean and then calls a method on the bean. Next, CompEJB A looks up CompEJB
     * B's home from its session context, uses that home to create a bean and
     * then calls a method on the bean.
     */
    @Test
    public void testSFLCompEJBMethodLevelInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreEJBLocalHome sfHome = (PetStoreEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreEJBLocalHomeInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         CompPetStoreBean);
        PetStoreEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getCatMthdComp(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * This test calls a CompEJB A which has CompEJB B injected into its environment
     * at the field level. This EJB injection specifies a value for the beanInterface
     * attribute. CompEJB A uses the injected CompEJB B's home to create
     * a bean and then calls a method on the bean. Next, CompEJB A looks up CompEJB
     * B's home from its session context, uses that home to create a bean and
     * then calls a method on the bean.
     */
    @Test
    public void testSFLCompEJBFieldLevelInterfaceInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreEJBLocalHome sfHome = (PetStoreEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreEJBLocalHomeInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         CompPetStoreBean);
        PetStoreEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getCatFldComp(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * Tests auto-link (no attributes specified in the EJB injection annotation).
     *
     * This test calls a CompEJB A which has CompEJB B injected into its environment
     * at the field level. CompEJB A uses the injected CompEJB B's home to create
     * a bean and then calls a method on the bean. Next, CompEJB A looks up CompEJB
     * B's home from its session context, uses that home to create a bean and
     * then calls a method on the bean.
     */
    @Test
    public void testSFLCompEJBFieldLevelAutoInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreEJBLocalHome sfHome = (PetStoreEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreEJBLocalHomeInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         CompPetStoreBean);
        PetStoreEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getDogFldComp(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * This test calls a 3.0 EJB A which has CompEJB B injected into its environment
     * at a class level. 3.0 EJB A looks up CompEJB B's home from its session context,
     * uses that home to create a bean and then calls a method on the bean.
     */
    @Test
    public void testSFL30CompEJBClassLevelInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------
        PetStoreLocal bean = (PetStoreLocal) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreLocalInterface,
                                                                                         Application,
                                                                                         Module,
                                                                                         PetStoreBean);
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getDogClsComp(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }

    /**
     * This test calls a 3.0 EJB A which has CompEJB B injected into its environment
     * at the field level. This EJB injection specifies a value for the beanName
     * attribute. 3.0 EJB A uses the injected CompEJB B's home to create
     * a bean and then calls a method on the bean. Next, 3.0 EJB A looks up CompEJB
     * B's home from its session context, uses that home to create a bean and
     * then calls a method on the bean.
     */
    @Test
    public void testSFL30CompEJBFieldLevelBeanNameInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreLocal bean = (PetStoreLocal) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreLocalInterface,
                                                                                         Application,
                                                                                         Module,
                                                                                         PetStoreBean);
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getDogFldComp2(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }

    /**
     * This test calls a 3.0 EJB A which has CompEJB B injected into its environment
     * at the method level. 3.0 EJB A uses the injected CompEJB B's home to create
     * a bean and then calls a method on the bean. Next, 3.0 EJB A looks up CompEJB
     * B's home from its session context, uses that home to create a bean and
     * then calls a method on the bean.
     */
    @Test
    public void testSFL30CompEJBMethodLevelInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreLocal bean = (PetStoreLocal) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreLocalInterface,
                                                                                         Application,
                                                                                         Module,
                                                                                         PetStoreBean);
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getCatMthdComp(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }

    /**
     * This test calls a 3.0 EJB A which has CompEJB B injected into its environment
     * at the field level. This EJB injection specifes a value for the beanInterface
     * attribute. 3.0 EJB A uses the injected CompEJB B's home to create
     * a bean and then calls a method on the bean. Next, 3.0 EJB A looks up CompEJB
     * B's home from its session context, uses that home to create a bean and
     * then calls a method on the bean.
     */
    @Test
    public void testSFL30CompEJBFieldLevelInterfaceInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreLocal bean = (PetStoreLocal) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreLocalInterface,
                                                                                         Application,
                                                                                         Module,
                                                                                         PetStoreBean);

        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getCatFldComp(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }

    /**
     * Tests auto-link (no attributes specified in the EJB injection annotation).
     *
     * This test calls a 3.0 EJB A which has CompEJB B injected into its environment
     * at the field level. 3.0 EJB A uses the injected CompEJB B's home to create
     * a bean and then calls a method on the bean. Next, 3.0 EJB A looks up CompEJB
     * B's home from its session context, uses that home to create a bean and
     * then calls a method on the bean.
     */
    @Test
    public void testSFL30CompEJBFieldLevelAutoInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreLocal bean = (PetStoreLocal) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreLocalInterface,
                                                                                         Application,
                                                                                         Module,
                                                                                         PetStoreBean);
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getDogFldComp(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.finish();
    }

    /**
     * This test calls a CompEJB A which has 3.0 EJB B injected into its environment
     * at the class level. CompEJB A looks up the 3.0 EJB B's ENC (specified when it
     * was injected) and creates an instance of the bean and then calls a method on
     * the bean.
     */
    @Test
    public void testSFLComp30EJBClassLevelInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreEJBLocalHome sfHome = (PetStoreEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreEJBLocalHomeInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         CompPetStoreBean);
        PetStoreEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getDogCls(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * Calls a bean that does a field level EJB injection with attribute
     * beanName=Dog on AnimalLocal dogLikeAnimal2 to verify that
     * we get dog like responses from the bean and not generic animal responses.
     * This bean then calls the favToy() and checks for expected results.
     * Next, this method looks up the stateful bean using an injected session context
     * that is using the default ENC JNDI entry created at the field level injection
     * and then calls this bean's whoAmI() method and returns the results.
     */
    @Test
    public void testSFLComp30EJBFieldLevelInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreEJBLocalHome sfHome = (PetStoreEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreEJBLocalHomeInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         CompPetStoreBean);
        PetStoreEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getDogLikeAnml2(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * This test calls a CompEJB A which has 3.0 EJB B injected into its environment
     * at the method level. CompEJB A uses the injected 3.0 EJB B and calls a method
     * on the bean. Next, 3.0 EJB A looks up 3.0 EJB B from its session context and
     * then calls a method on the bean.
     */
    @Test
    public void testSFLComp30EJBMethodLevelInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreEJBLocalHome sfHome = (PetStoreEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreEJBLocalHomeInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         CompPetStoreBean);
        PetStoreEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getCatMthd(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * Calls a bean that does a field level EJB injection with attribute
     * beanInterface=DogLocal on AnimalLocal dogLikeAnimal to verify that
     * we get dog like responses from the bean and not generic animal responses.
     * This bean then calls the favToy() and checks for expected results.
     * Next, this method looks up the stateful bean using an injected session context
     * that is using the default ENC JNDI entry created at the field level injection
     * and then calls this bean's whoAmI() method and returns the results.
     */
    @Test
    public void testSFLComp30EJBFieldLevelInterfaceOverrideInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreEJBLocalHome sfHome = (PetStoreEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreEJBLocalHomeInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         CompPetStoreBean);
        PetStoreEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getDogLikeAnml(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * This test calls a CompEJB A which has a 3.0 EJB B injected into its environment
     * at the field level. A beanName attribute is specified in the EJB injection
     * annotation. CompEJB A uses the injected 3.0 EJB B and calls a method on the bean.
     * Next, CompEJB A looks up 3.0 EJB B from its session context and then calls a
     * method on the bean.
     */
    @Test
    public void testSFLComp30EJBFieldLevelBeanNameInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreEJBLocalHome sfHome = (PetStoreEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreEJBLocalHomeInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         CompPetStoreBean);
        PetStoreEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getCatFld(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

    /**
     * Tests auto-link (no attributes specified in the EJB injection annotation).
     *
     * This test calls a CompEJB A which has 3.0 EJB B injected into its environment
     * at the field level. CompEJB A uses the injected 3.0 EJB B and calls a method
     * on the bean. Next, CompEJB A looks up 3.0 EJB B from its session context, and
     * then calls a method on the bean.
     */
    @Test
    public void testSFLComp30EJBFieldLevelAutoInjection() throws Exception {
        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        PetStoreEJBLocalHome sfHome = (PetStoreEJBLocalHome) FATHelper.lookupDefaultBindingEJBJavaGlobal(PetStoreEJBLocalHomeInterface,
                                                                                                         Application,
                                                                                                         Module,
                                                                                                         CompPetStoreBean);
        PetStoreEJBLocal bean = sfHome.create();
        assertNotNull("1 ---> SFLSB created successfully.", bean);

        int testpoint = 2;
        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getDogFld(testpoint++));

        // Clean up the instance if no failures occurred. If an error occurred,
        // then the bean would have already been discarded per the spec.
        bean.remove();
    }

}
