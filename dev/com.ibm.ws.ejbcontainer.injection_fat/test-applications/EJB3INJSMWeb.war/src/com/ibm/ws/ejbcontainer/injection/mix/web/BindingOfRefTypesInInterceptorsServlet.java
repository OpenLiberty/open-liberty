/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.mix.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;

import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.fat.rar.core.FVTXAResourceImpl;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTBaseMessageProvider;
import com.ibm.ws.ejbcontainer.fat.rar.message.FVTMessage;
import com.ibm.ws.ejbcontainer.fat.rar.message.MessageListener;
import com.ibm.ws.ejbcontainer.injection.mix.ejbint.AnnotationInjectionInterceptor;
import com.ibm.ws.ejbcontainer.injection.mix.ejbint.AnnotationInjectionInterceptor2;
import com.ibm.ws.ejbcontainer.injection.mix.ejbint.MessageDrivenInjectionBean;
import com.ibm.ws.ejbcontainer.injection.mix.ejbint.StatefulInterceptorInjectionBean;
import com.ibm.ws.ejbcontainer.injection.mix.ejbint.StatefulInterceptorInjectionLocal;
import com.ibm.ws.ejbcontainer.injection.mix.ejbint.StatelessInterceptorInjectionRemote;
import com.ibm.ws.ejbcontainer.injection.mix.ejbint.StatelessInterceptorInjectionRemoteHome;
import com.ibm.ws.ejbcontainer.injection.mix.ejbint.XMLInjectionInterceptor;
import com.ibm.ws.ejbcontainer.injection.mix.ejbint.XMLInjectionInterceptor2;
import com.ibm.ws.ejbcontainer.injection.mix.ejbint.XMLInjectionInterceptor3;
import com.ibm.ws.ejbcontainer.injection.mix.ejbint.XMLInjectionInterceptor4;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;

/**
 * <dl>
 * <dt><b>Test Name:</b> BindingOfRefTypesInInterceptorsTest.
 *
 * <dt><b>EJB 3 core specification sections tested:</b>
 *
 * <dd>The focus of this testcase is to verify injection into fields of an EJB 3
 * interceptor class occurs when either the @EJB or @Resource annotation is
 * used to annotate a field of an EJB 3 interceptor class. Alternatively,
 * <injection-target> stanza in the ejb-jar.xml file can be used for either a
 * <ejb-ref>, <ejb-local-ref>, <resource-ref>, <resource-env-ref>, or an
 * <message-destination-ref> stanza. Each of these reference type stanzas can
 * occur in either a <session>, <message-driven>, or <interceptor> stanza found in
 * the ejb-jar.xml file. Each of these possibilities must be tested.
 * <p>
 * In addition to the above, an explicit binding for each of the reference types
 * can be found in the ibm-ejb-jar-bnd.xml file. The <ejb-ref>, <ejb-local-ref>,
 * <resource-ref>, <resource-env-ref>, and <message-destination-ref> stanza in
 * the ibm-ejb-jar-bnd.xml file are used to provide the binding for each of the
 * reference type. These binding stanzas can occur either within a <session>,
 * <message-driven>, or a <interceptor> stanza in the ibm-ejb-jar-bnd.xml file.
 * All possibilities must be tested to ensure all EJB container code paths are tested.
 *
 * <dt><b>Test Description:</b>
 * <dt><b>Test Matrix:</b>
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>testAnnotationInjectionInterceptor - verify injection into an interceptor class that uses @EJB
 * and @Resource and bindings for each reference type is in <interceptor> stanza in
 * the ibm-ejb-jar-bnd.xml file. The interceptor class is bound to a session bean.
 * <li>testAnnotationInjectionInterceptor2 - verify injection into an interceptor class that uses @EJB
 * and @Resource and bindings for each reference type is in <session> stanza in
 * the ibm-ejb-jar-bnd.xml file. The interceptor class is bound to a session bean.
 * <li>testXMLInjectionInterceptor - verify injection into an interceptor class fields where <injection-target>
 * for each reference type is specified in a <interceptor> stanza in ejb-jar.xml file and bindings
 * for each reference type is in <interceptor> stanza in the ibm-ejb-jar-bnd.xml file.
 * The interceptor class is bound to a session bean.
 * <li>testXMLInjectionInterceptor2 - verify injection into an interceptor class fields where <injection-target>
 * for each reference type is specified in <interceptor> stanza in ejb-jar.xml file and bindings
 * for each reference type is in <session> stanza in the ibm-ejb-jar-bnd.xml file.
 * The interceptor class is bound to a session bean.
 * <li>testXMLInjectionInterceptor3 - verify injection into an interceptor class fields where <injection-target>
 * for each reference type is specified in a <session> stanza in ejb-jar.xml file and bindings
 * for each reference type is in <interceptor> stanza in the ibm-ejb-jar-bnd.xml file.
 * The interceptor class is bound to a session bean.
 * <li>testXMLInjectionInterceptor4 - verify injection into an interceptor class fields where <injection-target>
 * for each reference type is specified in <session> stanza in ejb-jar.xml file and bindings
 * for each reference type is in <session> stanza in the ibm-ejb-jar-bnd.xml file.
 * The interceptor class is bound to a session bean.
 * <li>testMDBAnnotationInjectionInterceptor - verify injection into an interceptor class that uses @EJB
 * and @Resource and bindings for each reference type is in <interceptor> stanza in
 * the ibm-ejb-jar-bnd.xml file. The interceptor class is bound to a message-driven bean.
 * <li>testMDBAnnotationInjectionInterceptor2 - verify injection into an interceptor class that uses @EJB
 * and @Resource and bindings for each reference type is in message-driven> stanza in
 * the ibm-ejb-jar-bnd.xml file. The interceptor class is bound to a message-driven bean.
 * <li>testMDBXMLInjectionInterceptor - verify injection into an interceptor class fields where <injection-target>
 * for each reference type is specified in a <interceptor> stanza in ejb-jar.xml file and bindings
 * for each reference type is in <interceptor> stanza in the ibm-ejb-jar-bnd.xml file.
 * The interceptor class is bound to a message-driven bean.
 * <li>testMDBXMLInjectionInterceptor2 - verify injection into an interceptor class fields where <injection-target>
 * for each reference type is specified in <interceptor> stanza in ejb-jar.xml file and bindings
 * for each reference type is in <message-driven> stanza in the ibm-ejb-jar-bnd.xml file.
 * The interceptor class is bound to a message-driven bean.
 * <li>testMDBXMLInjectionInterceptor3 - verify injection into an interceptor class fields where <injection-target>
 * for each reference type is specified in a <message-driven> stanza in ejb-jar.xml file and bindings
 * for each reference type is in <interceptor> stanza in the ibm-ejb-jar-bnd.xml file.
 * The interceptor class is bound to a message-driven bean.
 * <li>testMDBXMLInjectionInterceptor4 - verify injection into an interceptor class fields where <injection-target>
 * for each reference type is specified in <message-driven> stanza in ejb-jar.xml file and bindings
 * for each reference type is in <message-driven> stanza in the ibm-ejb-jar-bnd.xml file.
 * The interceptor class is bound to a message-driven bean.
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/BindingOfRefTypesInInterceptorsServlet")
public class BindingOfRefTypesInInterceptorsServlet extends FATServlet {

    private static final String PASSED = "Passed";

    private static final String JNDI_NAME = "session/StatefulInterceptorInjectionBean/StatefulInterceptorInjectionLocal";

    // Default binding name for home is
    // ejb/EJB3INJSMTestApp/EJB3INJINTMBean.jar/StatelessInterceptorInjectionBean#com.ibm.ws.ejbcontainer.injection.mix.ejbint.StatelessInterceptorInjectionRemoteHome
    private static final String Application = "EJB3INJSMTestApp";
    private static final String Module = "EJB3INJINTMBean";
    private static final String CompBean = "StatelessInterceptorInjectionBean";
    private static final String StatelessInterceptorInjectionEJBHomeInterface = StatelessInterceptorInjectionRemoteHome.class.getName();

    private FVTBaseMessageProvider baseProvider = null;

    public void prepareTRA() throws Exception {
        if (baseProvider == null) {
            baseProvider = (FVTBaseMessageProvider) new InitialContext().lookup("java:comp/env/BaseMessageProvider");
            System.out.println("Looked up BaseMessageProvider");
            baseProvider.setResourceAdapter("java:comp/env/FAT_TRA_DS");
        }
    }

    /**
     * Verify injection into a field of an interceptor class when the @EJB
     * or @Resource annotation is used to annotate the interceptor
     * class field and the interceptor class is bound to a SFSB.
     * The bindings for each of the possible reference types
     * is within the <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file
     * for the {@link AnnotationInjectionInterceptor} class.
     */
    @Test
    public void testAnnotationInjectionInterceptor() throws Exception {
        StatefulInterceptorInjectionLocal bean = (StatefulInterceptorInjectionLocal) FATHelper.lookupLocalBinding(JNDI_NAME);

        assertEquals("EJB method did not return expected results",
                     "AII_PASSED", bean.getAnnotationInterceptorResults());

        bean.finish();
    }

    /**
     * Verify injection into a field of an interceptor class when an
     * <injection-target> is used inside of a <ejb-ref>, <ejb-local-ref>,
     * <resource-ref>, <resource-env-ref>, and <message-destination-ref> stanza
     * that is within a <interceptor> stanza that is in the ejb-jar.xml file
     * for the {@link XMLInjectionInterceptor} class and the interceptor class
     * is bound to a SFSB. The bindings for each of these
     * reference type is inside of a <interceptor> stanza in the
     * ibm-ejb-jar-bnd.xml binding file.
     */
    @Test
    public void testXMLInjectionInterceptor() throws Exception {
        StatefulInterceptorInjectionLocal bean = (StatefulInterceptorInjectionLocal) FATHelper.lookupLocalBinding(JNDI_NAME);

        assertEquals("EJB method did not return expected results",
                     "XII_PASSED", bean.getXMLInterceptorResults());

        bean.finish();
    }

    /**
     * Verify injection into a field of an interceptor class when an
     * <injection-target> is used inside of a <ejb-ref>, <ejb-local-ref>,
     * <resource-ref>, <resource-env-ref>, and <message-destination-ref> stanza
     * that is within a <session> stanza that is in the ejb-jar.xml file
     * for the {@link StatefulInterceptorInjectionBean} class. The
     * <injection-target-class> is the {@link XMLInjectionInterceptor2}
     * interceptor class. The bindings for each of these reference type is inside of a
     * <session> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Test
    public void testXMLInjectionInterceptor2() throws Exception {
        StatefulInterceptorInjectionLocal bean = (StatefulInterceptorInjectionLocal) FATHelper.lookupLocalBinding(JNDI_NAME);

        assertEquals("EJB method did not return expected results",
                     "XII2_PASSED", bean.getXMLInterceptor2Results());

        bean.finish();
    }

    /**
     * Verify injection into a field of an interceptor class when an
     * <injection-target> is used inside of a <ejb-ref>, <ejb-local-ref>,
     * <resource-ref>, <resource-env-ref>, and <message-destination-ref> stanza
     * that is within a <session> stanza that is in the ejb-jar.xml file
     * for the {@link StatefulInterceptorInjectionBean} class. The
     * <injection-target-class> is the {@link XMLInjectionInterceptor3}
     * interceptor class. The bindings for each of these reference type is inside of a
     * <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Test
    public void testXMLInjectionInterceptor3() throws Exception {
        StatefulInterceptorInjectionLocal bean = (StatefulInterceptorInjectionLocal) FATHelper.lookupLocalBinding(JNDI_NAME);

        assertEquals("EJB method did not return expected results",
                     "XII3_PASSED", bean.getXMLInterceptor3Results());

        bean.finish();
    }

    /**
     * Verify injection into a field of an interceptor class when an
     * <injection-target> is used inside of a <ejb-ref>, <ejb-local-ref>,
     * <resource-ref>, <resource-env-ref>, and <message-destination-ref> stanza
     * that is within a <session> stanza that is in the ejb-jar.xml file
     * for the {@link StatefulInterceptorInjectionBean} class. The
     * <injection-target-class> is the {@link XMLInjectionInterceptor4}
     * interceptor class. The bindings for each of these reference type is inside of a
     * <session> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Test
    public void testXMLInjectionInterceptor4() throws Exception {
        StatefulInterceptorInjectionLocal bean = (StatefulInterceptorInjectionLocal) FATHelper.lookupLocalBinding(JNDI_NAME);

        assertEquals("EJB method did not return expected results",
                     "XII4_PASSED", bean.getXMLInterceptor4Results());

        bean.finish();
    }

    /**
     * Verify injection into a field of an interceptor class when the @EJB
     * or @Resource annotation is used to annotate the interceptor
     * class field. The bindings for each of the possible reference types
     * is within the within a <session> stanza that is in the ejb-jar.xml file
     * for the {@link StatefulInterceptorInjectionBean} class. The
     * <injection-target-class> is the {@link AnnotationInjectionInterceptor2}
     * interceptor class.
     */
    @Test
    public void testAnnotationInjectionInterceptor2() throws Exception {
        StatefulInterceptorInjectionLocal bean = (StatefulInterceptorInjectionLocal) FATHelper.lookupLocalBinding(JNDI_NAME);

        assertEquals("EJB method did not return expected results",
                     "AII2_PASSED", bean.getAnnotationInterceptor2Results());

        bean.finish();
    }

    /**
     * Verify injection into a field that is annotated with @Resource to ref to
     * a datasource resource. There are 2 different datasources. One to test the
     * scenario where the ibm-ejb-jar-bnd.xml binding file has a resource-ref binding
     * that uses authentication-alias. The other datasource has a resource-ref binding that
     * uses the custom-login-configuration properties. The bindings for each
     * of these reference type is inside of an <interceptor> stanza in the
     * ibm-ejb-jar-bnd.xml binding file.
     */
    @Test
    @SkipForRepeat({ EmptyAction.ID, EE7FeatureReplacementAction.ID, EE8FeatureReplacementAction.ID, JakartaEE9Action.ID, JakartaEE10Action.ID })
    public void testAnnotationDSInjectionInterceptor() throws Exception {
        //------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        StatelessInterceptorInjectionRemoteHome slHome = (StatelessInterceptorInjectionRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(StatelessInterceptorInjectionEJBHomeInterface,
                                                                                                                                                     Application, Module, CompBean);
        StatelessInterceptorInjectionRemote bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getAnnotationDSInterceptorResults());
    }

    /**
     * Verify injection into a field that is a field of an interceptor class
     * when <injection-target> is used inside of a <resource-ref> stanza that
     * is within a <interceptor> stanza that appears in the ejb-jar.xml file
     * of the EJB 3 module.
     * There are 2 different resource ref injections, each is for a datasource.
     * One datasource resource ref is to test the scenario where the
     * ibm-ejb-jar-bnd.xml binding file has a resource-ref binding that uses
     * authentication-alias. The other datasource has a resource-ref binding that
     * uses the custom-login-configuration properties. The bindings for each
     * of these reference type is inside of an <interceptor> stanza in the
     * ibm-ejb-jar-bnd.xml binding file.
     */
    @Test
    @SkipForRepeat({ EmptyAction.ID, EE7FeatureReplacementAction.ID, EE8FeatureReplacementAction.ID, JakartaEE9Action.ID, JakartaEE10Action.ID })
    public void testXMLDSInjectionInterceptor() throws Exception {
        // ------------------------------------------------------------------
        // Locate SL Remote Home/Factory and execute the test
        // --------------------------------------------------------------------
        StatelessInterceptorInjectionRemoteHome slHome = (StatelessInterceptorInjectionRemoteHome) FATHelper.lookupDefaultBindingsEJBRemoteInterface(StatelessInterceptorInjectionEJBHomeInterface,
                                                                                                                                                     Application, Module, CompBean);
        StatelessInterceptorInjectionRemote bean = slHome.create();
        assertNotNull("1 ---> SLRSB created successfully.", bean);

        assertEquals("EJB method did not return expected results",
                     PASSED, bean.getXMLDSInterceptorResults());
    }

    /**
     * Verify injection into a field of an interceptor class when the @EJB
     * or @Resource annotation is used to annotate the interceptor
     * class field and the interceptor class is bound to an MDB.
     * The bindings for each of the possible reference types
     * is within the <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file
     * for the {@link AnnotationInjectionInterceptor} class.
     */
    @Test
    public void testMDBAnnotationInjectionInterceptor() throws Exception {
        prepareTRA();

        String deliveryID = null;
        try {
            // Ensure baseProvider is not null before starting each test
            assertNotNull("BaseMessageProvider is null.", baseProvider);

            // construct a FVTMessage
            MessageDrivenInjectionBean.svResults = null;

            FVTMessage message = new FVTMessage();
            message.addTestResult("MessageDrivenInjectionBean", 102);
            message.addXAResource("MessageDrivenInjectionBean", 102, new FVTXAResourceImpl());

            // Add a option B transacted delivery to another instance.
            Method m = MessageListener.class.getMethod("onStringMessage", new Class[] { String.class });
            message.addDelivery("MessageDrivenInjectionBean", FVTMessage.BEFORE_DELIVERY, m, 102);
            message.add("MessageDrivenInjectionBean", "message", m, 102);
            message.addDelivery("MessageDrivenInjectionBean", FVTMessage.AFTER_DELIVERY, m, 102);
            message.addRelease("MessageDrivenInjectionBean", 102);

            //addInfo( message.introspectSelf() );

            deliveryID = "testMDBAnnotationInjectionInterceptor";
            baseProvider.sendDirectMessage(deliveryID, message);
            deliveryID = null;

            String results = MessageDrivenInjectionBean.svResults;
            assertEquals("EJB method did not return expected results",
                         "AII_PASSED;Passed : onStringMessage", results);
        } finally {
            MessageDrivenInjectionBean.svResults = null;
            if (deliveryID != null) {
                baseProvider.releaseDeliveryId(deliveryID);
            }
        }
    }

    /**
     * Verify injection into a field of an interceptor class when the @EJB
     * or @Resource annotation is used to annotate the interceptor
     * class field. The interceptor class is bound to an MDB. The bindings
     * for each of the possible reference types is within the within
     * a <message-driven> stanza that is in the ejb-jar.xml file
     * for the {@link MessageDrivenInjectionBean} class. The
     * <injection-target-class> is the {@link AnnotationInjectionInterceptor2}
     * interceptor class.
     */
    @Test
    public void testMDBAnnotationInjectionInterceptor2() throws Exception {
        prepareTRA();

        String deliveryID = null;
        try {
            // Ensure baseProvider is not null before starting each test
            assertNotNull("BaseMessageProvider is null.", baseProvider);

            // construct a FVTMessage
            MessageDrivenInjectionBean.svResults = null;

            FVTMessage message = new FVTMessage();
            message.addTestResult("MessageDrivenInjectionBean");

            // Add a option B transacted delivery to another instance.
            Method m = javax.jms.MessageListener.class.getMethod("onMessage", new Class[] { javax.jms.Message.class });
            message.addDelivery("MessageDrivenInjectionBean", FVTMessage.BEFORE_DELIVERY, m);
            message.add("MessageDrivenInjectionBean", "message", m);
            message.addDelivery("MessageDrivenInjectionBean", FVTMessage.AFTER_DELIVERY, m);

            //addInfo( message.introspectSelf() );

            deliveryID = "testMDBAnnotationInjectionInterceptor2";
            baseProvider.sendDirectMessage(deliveryID, message);

            String results = MessageDrivenInjectionBean.svResults;
            assertEquals("EJB method did not return expected results",
                         "AII2_PASSED;Passed : onMessage", results);
        } finally {
            MessageDrivenInjectionBean.svResults = null;
            if (deliveryID != null) {
                baseProvider.releaseDeliveryId(deliveryID);
            }
        }
    }

    /**
     * Verify injection into a field of an interceptor class when an
     * <injection-target> is used inside of a <ejb-ref>, <ejb-local-ref>,
     * <resource-ref>, <resource-env-ref>, and <message-destination-ref> stanza
     * that is within a <interceptor> stanza that is in the ejb-jar.xml file
     * for the {@link XMLInjectionInterceptor} class. The interceptor class is
     * bound to an MDB and the bindings for each of these
     * reference type is inside of a <interceptor> stanza in the
     * ibm-ejb-jar-bnd.xml binding file.
     */
    @Test
    public void testMDBXMLInjectionInterceptor() throws Exception {
        prepareTRA();

        String deliveryID = null;
        try {
            // Ensure baseProvider is not null before starting each test
            assertNotNull("BaseMessageProvider is null.", baseProvider);

            // construct a FVTMessage
            MessageDrivenInjectionBean.svResults = null;

            FVTMessage message = new FVTMessage();
            message.addTestResult("MessageDrivenInjectionBean");

            // Add a option B transacted delivery to another instance.
            Method m = MessageListener.class.getMethod("onIntegerMessage", new Class[] { Integer.class });
            message.addDelivery("MessageDrivenInjectionBean", FVTMessage.BEFORE_DELIVERY, m);
            message.add("MessageDrivenInjectionBean", "1", m);
            message.addDelivery("MessageDrivenInjectionBean", FVTMessage.AFTER_DELIVERY, m);

            //addInfo( message.introspectSelf() );

            deliveryID = "testMDBXMLInjectionInterceptor";
            baseProvider.sendDirectMessage(deliveryID, message);

            String results = MessageDrivenInjectionBean.svResults;
            assertEquals("EJB method did not return expected results",
                         "XII_PASSED;Passed : onIntegerMessage", results);
        } finally {
            MessageDrivenInjectionBean.svResults = null;
            if (deliveryID != null) {
                baseProvider.releaseDeliveryId(deliveryID);
            }
        }
    }

    /**
     * Verify injection into a field of an interceptor class when an
     * <injection-target> is used inside of a <ejb-ref>, <ejb-local-ref>,
     * <resource-ref>, <resource-env-ref>, and <message-destination-ref> stanza
     * that is within a <message-driven> stanza that is in the ejb-jar.xml file
     * for the {@link MessageDrivenInjectionBean} class. The
     * <injection-target-class> is the {@link XMLInjectionInterceptor2}
     * interceptor class. The bindings for each of these reference type is inside of a
     * <message-driven> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Test
    public void testMDBXMLInjectionInterceptor2() throws Exception {
        prepareTRA();

        String deliveryID = null;
        try {
            // Ensure baseProvider is not null before starting each test
            assertNotNull("BaseMessageProvider is null.", baseProvider);

            // construct a FVTMessage
            MessageDrivenInjectionBean.svResults = null;

            FVTMessage message = new FVTMessage();
            message.addTestResult("MessageDrivenInjectionBean");

            // Add a option B transacted delivery to another instance.
            Method m = MessageListener.class.getMethod("onGetTimestamp", new Class[] { String.class });
            message.addDelivery("MessageDrivenInjectionBean", FVTMessage.BEFORE_DELIVERY, m);
            message.add("MessageDrivenInjectionBean", "message2", m);
            message.addDelivery("MessageDrivenInjectionBean", FVTMessage.AFTER_DELIVERY, m);

            //addInfo( message.introspectSelf() );

            deliveryID = "testMDBXMLInjectionInterceptor2";
            baseProvider.sendDirectMessage(deliveryID, message);

            // For this test, all class level interceptors will be called as well
            // as the method level interceptor (a duplicate).  However, only XII2
            // will add results, since it is checking for onGetTimestamp.

            String results = MessageDrivenInjectionBean.svResults;
            assertEquals("EJB method did not return expected results",
                         "XII2_PASSED;XII2_PASSED;Passed : onGetTimestamp", results);
        } finally {
            MessageDrivenInjectionBean.svResults = null;
            if (deliveryID != null) {
                baseProvider.releaseDeliveryId(deliveryID);
            }
        }
    }

    /**
     * Verify injection into a field of an interceptor class when an
     * <injection-target> is used inside of a <ejb-ref>, <ejb-local-ref>,
     * <resource-ref>, <resource-env-ref>, and <message-destination-ref> stanza
     * that is within a <message-driven> stanza that is in the ejb-jar.xml file
     * for the {@link MessageDrivenInjectionBean} class. The
     * <injection-target-class> is the {@link XMLInjectionInterceptor3}
     * interceptor class. The bindings for each of these reference type is inside of a
     * <interceptor> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Test
    public void testMDBXMLInjectionInterceptor3() throws Exception {
        prepareTRA();

        String deliveryID = null;
        try {
            // Ensure baseProvider is not null before starting each test
            assertNotNull("BaseMessageProvider is null.", baseProvider);

            // construct a FVTMessage
            MessageDrivenInjectionBean.svResults = null;

            FVTMessage message = new FVTMessage();
            message.addTestResult("MessageDrivenInjectionBean");

            // Add a option B transacted delivery to another instance.
            Method m = MessageListener.class.getMethod("onCreateDBEntryNikki", new Class[] { String.class });
            message.addDelivery("MessageDrivenInjectionBean", FVTMessage.BEFORE_DELIVERY, m);
            message.add("MessageDrivenInjectionBean", "message3", m);
            message.addDelivery("MessageDrivenInjectionBean", FVTMessage.AFTER_DELIVERY, m);

            //addInfo( message.introspectSelf() );

            deliveryID = "testMDBXMLInjectionInterceptor3";
            baseProvider.sendDirectMessage(deliveryID, message);

            // For this test, all class level interceptors will be called as well
            // as the method level interceptor (a duplicate).  However, only XII3
            // will add results, since it is checking for onCreateDBEntryNikki.

            String results = MessageDrivenInjectionBean.svResults;
            assertEquals("EJB method did not return expected results",
                         "XII3_PASSED;XII3_PASSED;Passed : onCreateDBEntryNikki", results);
        } finally {
            MessageDrivenInjectionBean.svResults = null;
            if (deliveryID != null) {
                baseProvider.releaseDeliveryId(deliveryID);
            }
        }
    }

    /**
     * Verify injection into a field of an interceptor class when an
     * <injection-target> is used inside of a <ejb-ref>, <ejb-local-ref>,
     * <resource-ref>, <resource-env-ref>, and <message-destination-ref> stanza
     * that is within a <message-driven> stanza that is in the ejb-jar.xml file
     * for the {@link MessageDrivenInjectionBean} class. The
     * <injection-target-class> is the {@link XMLInjectionInterceptor4}
     * interceptor class. The bindings for each of these reference type is inside of a
     * <message-driven> stanza in the ibm-ejb-jar-bnd.xml binding file.
     */
    @Test
    public void testMDBXMLInjectionInterceptor4() throws Exception {
        prepareTRA();

        String deliveryID = null;
        try {
            // Ensure baseProvider is not null before starting each test
            assertNotNull("BaseMessageProvider is null.", baseProvider);

            // construct a FVTMessage
            MessageDrivenInjectionBean.svResults = null;

            FVTMessage message = new FVTMessage();
            message.addTestResult("MessageDrivenInjectionBean");

            // Add a option B transacted delivery to another instance.
            Method m = MessageListener.class.getMethod("onCreateDBEntryZiyad", new Class[] { String.class });
            message.addDelivery("MessageDrivenInjectionBean", FVTMessage.BEFORE_DELIVERY, m);
            message.add("MessageDrivenInjectionBean", "1", m);
            message.addDelivery("MessageDrivenInjectionBean", FVTMessage.AFTER_DELIVERY, m);

            //addInfo( message.introspectSelf() );

            deliveryID = "testMDBXMLInjectionInterceptor4";
            baseProvider.sendDirectMessage(deliveryID, message);

            // For this test, all class level interceptors will be called as well
            // as the method level interceptor (a duplicate).  However, only XII4
            // will add results, since it is checking for onCreateDBEntryZiyad.

            String results = MessageDrivenInjectionBean.svResults;
            assertEquals("EJB method did not return expected results",
                         "XII4_PASSED;XII4_PASSED;Passed : onCreateDBEntryZiyad", results);
        } finally {
            MessageDrivenInjectionBean.svResults = null;
            if (deliveryID != null) {
                baseProvider.releaseDeliveryId(deliveryID);
            }
        }
    }

}
