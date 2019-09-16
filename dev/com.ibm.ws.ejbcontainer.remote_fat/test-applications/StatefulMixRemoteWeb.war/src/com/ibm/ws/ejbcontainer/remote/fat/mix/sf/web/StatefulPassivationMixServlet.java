/*******************************************************************************
 * Copyright (c) 2015, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.remote.fat.mix.sf.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.SerObj;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.StatefulCLInterceptorLocal;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.StatefulCLInterceptorRemote;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.StatefulEmptyLocal;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.StatefulEmptyRemote;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.StatefulPassBMTLocal;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.StatefulPassBMTRemote;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.StatefulPassLocal;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.StatefulPassRemote;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.StaticStatefulRemote;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.SuperStatefulLocal;
import com.ibm.ws.ejbcontainer.remote.fat.mix.sf.ejb.SuperStatefulRemote;

import componenttest.app.FATServlet;

/**
 * Tests passivation/activation of SFSB with various types of serializable and non-serialiable objects and
 * defined by EJB 3.0 spec section 4.2. For each test, remote, local, remote business, and local business interfaces will be called.<p>
 *
 * <dt><b>Test Matrix:</b>
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>test01 - Passivate and activate a basic SFSB which implements serializable.
 * <li>test02 - Passivate and activate a basic SFSB which does not implement serializable.
 * <li>test03 - Passivate and activate a SFSB which has one annotated class level interceptor that does not implement serializable but contains a serializable field
 * <li>test04 - Passivate and activate a serializable SFSB which has two class level interceptors one serializable, one not (one annotated, one XML) containing serialiable fields
 * <li>test05 - Passivate and activate a SFSB which has a default interceptor containing a serializable field
 * <li>test06 - Passivate and activate a SFSB which has a class level and default interceptors containing a serializable field
 *
 * <li>test08 - Passivate an SFSB that contains a static field.
 * <li>test09 - Passivate an interceptor that contains a static field.
 *
 *
 * <li>test17 - Passivate and activate a SFSB with a reference to a SessionContext object
 * <li>test18 - Passivate and activate a SFSB with a reference to a ENC naming context
 * <li>test19 - Passivate and activate a SFSB with a reference to the UserTransaction interface
 * <li>test20 - Passivate and activate a SFSB with a reference to a resource manager connection factory
 * <li>test21 - Passivate and activate a SFSB with a reference javax.ejb.Timer object.
 * <li>test22 - Passivate and activate a SFSB with a serializable object containing a reference to items tested in test11 through test21
 * <li>test23 - Passivate and activate a SFSB with a serializable object that is in a separate jar that is within the library directory of the application archive
 * <li>test24 - Passivate and activate a SFSB is not serializable with 1 serializable interceptor and 1 non-serializable interceptor
 * <li>test25 - Passivate and activate a SFSB which has a base class that is not serializable.
 * <li>test26 - Passivate and activate a non-serializable SFSB which has two class level interceptors one serializable, one not (one annotated, one XML) containing serialiable
 * fields
 * <li>test27 - Passivate and activate a serializable SFSB which has an interceptor which is a superclass of a non-serializable interceptor.
 * <li>test28 - Passivate and activate a serializable SFSB which has an interceptor which is a superclass of a serializable interceptor.
 * </ul>
 */
@WebServlet("/StatefulPassivationMixServlet")
public class StatefulPassivationMixServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    private final static String CLASSNAME = StatefulPassivationMixServlet.class.getName();
    private final static Logger svLogger = Logger.getLogger(CLASSNAME);

    // Names of application and module... for lookup.
    private static final String Module = "StatefulMixRemoteEJB";

    // Names of the beans used for the test... for lookup.
    private static final String SerializableBean = "SerializableStatefulBean";
    private static final String NonSerializableBean = "NonSerializableStatefulBean";
    private static final String StaticStatefulBean = "StaticStateful";
    private static final String SerializableBMTBean = "SerializableBMTStatefulBean";
    private static final String StatefulCLInterceptorBean = "StatefulCLInterceptorBean";
    private static final String StatefulCL2InterceptorBean = "StatefulCL2InterceptorBean";
    private static final String StatefulNonSerCL2InterceptorBean = "StatefulNonSerCL2InterceptorBean";
    private static final String StatefulDefInterceptorBean = "StatefulDefInterceptorBean";
    private static final String StatefulCLDefInterceptorBean = "StatefulCLDefInterceptorBean";
    private static final String StatefulEmptyBean = "StatefulEmptyBean";
    private static final String SuperStatefulNonSerBaseBean = "SuperStatefulNonSerBaseBean";
    private static final String SuperStatefulSerBaseBean = "SuperStatefulSerBaseBean";
    private static final String StatefulCLSuperInterceptorBean = "StatefulCLSuperInterceptorBean";
    private static final String StatefulCLSuperSerInterceptorBean = "StatefulCLSuperSerInterceptorBean";

    // Names of the interfaces used for the test
    private static final String StatefulPassLocalInterface = StatefulPassLocal.class.getName();
    private static final String StatefulPassRemoteInterface = StatefulPassRemote.class.getName();
    private static final String StaticStatefulRemoteInterface = StaticStatefulRemote.class.getName();
    private static final String StatefulCLInterceptorLocal = StatefulCLInterceptorLocal.class.getName();
    private static final String StatefulCLInterceptorRemote = StatefulCLInterceptorRemote.class.getName();
    private static final String StatefulPassBMTLocalInterface = StatefulPassBMTLocal.class.getName();
    private static final String StatefulPassBMTRemoteInterface = StatefulPassBMTRemote.class.getName();
    private static final String StatefulEmptyLocalInterface = StatefulEmptyLocal.class.getName();
    private static final String StatefulEmptyRemoteInterface = StatefulEmptyRemote.class.getName();
    private static final String SuperStatefulLocalInterface = SuperStatefulLocal.class.getName();
    private static final String SuperStatefulRemoteInterface = SuperStatefulRemote.class.getName();

    @Test
    public void testStatefulPassivationMixServlet_test01() throws Exception {

        StatefulPassLocal bean = null;

        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulPassLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassLocalInterface, Module, SerializableBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);

        bean.setIntegerValue(new Integer(10)); // Passivation, activation 1
        bean.setStringValue("Ten"); // Passivation, activation 2
        bean.setSerObjValue(new SerObj("Dias", "TransientDias")); // Passivation, activation 3

        int passCount = bean.getPassivateCount(); // Passivation, activation 4 (passivation count = 4)
        int actCount = bean.getActivateCount(); // Passivation, activation 5 (activation count = 5)

        svLogger.info("Passivation count: " + passCount);
        svLogger.info("Activation count: " + actCount);
        assertEquals("Comparing passivation count ", passCount, 4);
        assertEquals("Comparing activation count ", actCount, 5);

        // If passivation occured, compare the values after the passivation
        Integer intVal = bean.getIntegerValue();
        String strVal = bean.getStringValue();
        SerObj serObjValue = bean.getSerObjValue();

        assertEquals("Comparing Integer value ", new Integer(10), intVal);
        assertEquals("Comparing String value ", "Ten", strVal);
        assertEquals("Comparing String value of SerObj ", serObjValue.getStrVal(), "Dias");
        svLogger.info("Transient value of SerObj: " + serObjValue.getTransStrVal());

        bean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test02() throws Exception {

        StatefulPassRemote bean = null;

        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulPassRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassRemoteInterface, Module, NonSerializableBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);

        bean.setIntegerValue(new Integer(11)); // Passivation, activation 1
        bean.setStringValue("Eleven"); // Passivation, activation 2
        bean.setSerObjValue(new SerObj("Once", "TransientOnce")); // Passivation, activation 3

        int passCount = bean.getPassivateCount(); // Passivation, activation 4 (passivation count = 4)
        int actCount = bean.getActivateCount(); // Passivation, activation 5 (activation count = 5)

        svLogger.info("Passivation count: " + passCount);
        svLogger.info("Activation count: " + actCount);
        assertEquals("Comparing passivation count ", passCount, 4);
        assertEquals("Comparing activation count ", actCount, 5);

        // If passivation occured, compare the values after the passivation
        Integer intVal = bean.getIntegerValue();
        String strVal = bean.getStringValue();
        SerObj serObjValue = bean.getSerObjValue();

        assertEquals("Comparing Integer value ", new Integer(11), intVal);
        assertEquals("Comparing String value ", "Eleven", strVal);
        assertEquals("Comparing String value of SerObj ", serObjValue.getStrVal(), "Once");
        svLogger.info("Transient value of SerObj: " + serObjValue.getTransStrVal());

        bean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test03() throws Exception {

        StatefulCLInterceptorLocal localBean = null;
        StatefulCLInterceptorRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulCLInterceptorRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorRemote, Module, StatefulCLInterceptorBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.interceptorStart("Remote");
        bean.interceptorEnd("Remote");

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulCLInterceptorLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorLocal, Module, StatefulCLInterceptorBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.interceptorStart("Local");
        localBean.interceptorEnd("Local");

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test04() throws Exception {

        StatefulCLInterceptorLocal localBean = null;
        StatefulCLInterceptorRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulCLInterceptorRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorRemote, Module, StatefulCL2InterceptorBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.interceptorStart("Remote");
        bean.interceptorEnd("Remote");

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulCLInterceptorLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorLocal, Module, StatefulCL2InterceptorBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.interceptorStart("Local");
        localBean.interceptorEnd("Local");

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test05() throws Exception {

        StatefulCLInterceptorLocal localBean = null;
        StatefulCLInterceptorRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulCLInterceptorRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorRemote, Module, StatefulDefInterceptorBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.interceptorStart("Remote");
        bean.interceptorEnd("Remote");

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulCLInterceptorLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorLocal, Module, StatefulDefInterceptorBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.interceptorStart("Local");
        localBean.interceptorEnd("Local");

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test06() throws Exception {

        StatefulCLInterceptorLocal localBean = null;
        StatefulCLInterceptorRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulCLInterceptorRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorRemote, Module, StatefulCLDefInterceptorBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.interceptorStart("Remote");
        bean.interceptorEnd("Remote");

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulCLInterceptorLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorLocal, Module, StatefulCLDefInterceptorBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.interceptorStart("Local");
        localBean.interceptorEnd("Local");

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test08() throws Exception {

        StaticStatefulRemote bean = null;

        // --------------------------------------------------------------------
        // Locate SF Local Home/Factory and execute the test
        // --------------------------------------------------------------------

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StaticStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StaticStatefulRemoteInterface, Module, StaticStatefulBean);
        assertNotNull("1 ---> SFLSB 'lookup' successful.", bean);

        String staticStr = bean.getStaticString(); // Passivation, activation 1
        SerObj staticSerObj = bean.getStaticSerObj(); // Passivation, activation 2

        assertEquals("Comparing String value ", "Static", staticStr);
        assertEquals("Comparing String value of SerObj ", staticSerObj.getStrVal(), "ABC");
        svLogger.info("Transient value of SerObj: " + staticSerObj.getTransStrVal());

        int passCount = bean.getPassivateCount(); // Passivation, activation 3 (passivation count = 3)
        int actCount = bean.getActivateCount(); // Passivation, activation 4 (activation count = 4)

        svLogger.info("Passivation count: " + passCount);
        svLogger.info("Activation count: " + actCount);
        assertEquals("Comparing passivation count ", passCount, 3);
        assertEquals("Comparing activation count ", actCount, 4);

        bean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test09() throws Exception {

        StatefulCLInterceptorLocal localBean = null;
        StatefulCLInterceptorRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulCLInterceptorRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorRemote, Module, StatefulNonSerCL2InterceptorBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.interceptorStaticStart("Remote");
        bean.interceptorStaticEnd("Remote");

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulCLInterceptorLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorLocal, Module, StatefulNonSerCL2InterceptorBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.interceptorStaticStart("Local");
        localBean.interceptorStaticEnd("Local");

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test17() throws Exception {

        StatefulPassLocal localBean = null;
        StatefulPassRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulPassRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassRemoteInterface, Module, SerializableBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.checkSessionContextStart();
        bean.checkSessionContextEnd();

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulPassLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassLocalInterface, Module, SerializableBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.checkSessionContextStart();
        localBean.checkSessionContextEnd();

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test18() throws Exception {

        StatefulPassLocal localBean = null;
        StatefulPassRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulPassRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassRemoteInterface, Module, SerializableBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.checkENCEntryStart();
        bean.checkENCEntryEnd();

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulPassLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassLocalInterface, Module, SerializableBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.checkENCEntryStart();
        localBean.checkENCEntryEnd();

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test19() throws Exception {

        StatefulPassBMTLocal localBean = null;
        StatefulPassBMTRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulPassBMTRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassBMTRemoteInterface, Module, SerializableBMTBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.checkUserTranStart();
        bean.checkUserTranEnd();

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulPassBMTLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassBMTLocalInterface, Module, SerializableBMTBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.checkUserTranStart();
        localBean.checkUserTranEnd();

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test21() throws Exception {

        StatefulPassLocal localBean = null;
        StatefulPassRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulPassRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassRemoteInterface, Module, SerializableBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.checkTimerStart();
        bean.checkTimerEnd();

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulPassLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassLocalInterface, Module, SerializableBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.checkTimerStart();
        localBean.checkTimerEnd();

        localBean.finish();
    }

    // TODO: Beef up variation by adding and exercising various references on serialized object, except for DataSource.
    @Test
    public void testStatefulPassivationMixServlet_test22() throws Exception {

        StatefulPassLocal localBean = null;
        StatefulPassRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulPassRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassRemoteInterface, Module, SerializableBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.checkMySerObjStart();
        bean.checkMySerObjEnd();

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulPassRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassRemoteInterface, Module, NonSerializableBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.checkMySerObjStart();
        bean.checkMySerObjEnd();

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulPassLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassLocalInterface, Module, SerializableBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", localBean);

        localBean.checkMySerObjStart();
        localBean.checkMySerObjEnd();

        localBean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulPassLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulPassLocalInterface, Module, NonSerializableBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", localBean);

        localBean.checkMySerObjStart();
        localBean.checkMySerObjEnd();

        localBean.finish();

    }

    @Test
    public void testStatefulPassivationMixServlet_test23() throws Exception {

        StatefulCLInterceptorLocal localBean = null;
        StatefulCLInterceptorRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulCLInterceptorRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorRemote, Module, StatefulNonSerCL2InterceptorBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.interceptorStart("Remote");
        bean.interceptorEnd("Remote");

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulCLInterceptorLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorLocal, Module, StatefulNonSerCL2InterceptorBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.interceptorStart("Local");
        localBean.interceptorEnd("Local");

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test24() throws Exception {

        StatefulEmptyLocal localBean = null;
        StatefulEmptyRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulEmptyRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulEmptyRemoteInterface, Module, StatefulEmptyBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.checkEmptyStart("Remote");
        bean.checkEmptyEnd("Remote");

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulEmptyLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulEmptyLocalInterface, Module, StatefulEmptyBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.checkEmptyStart("Local");
        localBean.checkEmptyEnd("Local");

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test25() throws Exception {

        SuperStatefulLocal localBean = null;
        SuperStatefulRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (SuperStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(SuperStatefulRemoteInterface, Module, SuperStatefulNonSerBaseBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.checkSuperStart();
        bean.checkSuperEnd();

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (SuperStatefulLocal) FATHelper.lookupDefaultBindingEJBJavaApp(SuperStatefulLocalInterface, Module, SuperStatefulNonSerBaseBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.checkSuperStart();
        localBean.checkSuperEnd();

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test26() throws Exception {

        SuperStatefulLocal localBean = null;
        SuperStatefulRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (SuperStatefulRemote) FATHelper.lookupDefaultBindingEJBJavaApp(SuperStatefulRemoteInterface, Module, SuperStatefulSerBaseBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.checkSuperStart();
        bean.checkSuperEnd();

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (SuperStatefulLocal) FATHelper.lookupDefaultBindingEJBJavaApp(SuperStatefulLocalInterface, Module, SuperStatefulSerBaseBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.checkSuperStart();
        localBean.checkSuperEnd();

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test27() throws Exception {

        StatefulCLInterceptorLocal localBean = null;
        StatefulCLInterceptorRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulCLInterceptorRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorRemote, Module, StatefulCLSuperInterceptorBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.interceptorStart("Remote");
        bean.interceptorEnd("Remote");

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulCLInterceptorLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorLocal, Module, StatefulCLSuperInterceptorBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.interceptorStart("Local");
        localBean.interceptorEnd("Local");

        localBean.finish();
    }

    @Test
    public void testStatefulPassivationMixServlet_test28() throws Exception {

        StatefulCLInterceptorLocal localBean = null;
        StatefulCLInterceptorRemote bean = null;

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        bean = (StatefulCLInterceptorRemote) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorRemote, Module, StatefulCLSuperSerInterceptorBean);
        assertNotNull("1 ---> SFLSB remote 'lookup' successful.", bean);

        bean.interceptorStart("Remote");
        bean.interceptorEnd("Remote");

        bean.finish();

        // Create an instance of the bean by looking up the business interface
        // and insure the bean contains the default state.
        localBean = (StatefulCLInterceptorLocal) FATHelper.lookupDefaultBindingEJBJavaApp(StatefulCLInterceptorLocal, Module, StatefulCLSuperSerInterceptorBean);
        assertNotNull("1 ---> SFLSB local 'lookup' successful.", bean);

        localBean.interceptorStart("Local");
        localBean.interceptorEnd("Local");

        localBean.finish();
    }

}
