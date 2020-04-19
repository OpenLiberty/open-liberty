/*******************************************************************************
 * Copyright (c) 2009, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.junit.Test;

// import com.ibm.websphere.ejbcontainer.AmbiguousEJBReferenceException;
import com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.ejb.ComplexNoInterfaceBean;
import com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.ejb.LocalBusiness;
import com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.ejb.LocalComponent;
import com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.ejb.LocalHome;
import com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.ejb.RemoteBusiness;

import componenttest.app.FATServlet;

/**
 * Base test class for testing that the various binding options work properly
 * for each of the bean types that support a No-Interface View. <p>
 *
 * A subclass test should be implemented for each bean type. For each bean
 * type, the following configurations are tested:
 *
 * <ul>
 * <li> A bean that is only exposed through the No-Interface view.
 * <li> A bean that has both a No-Interface view and Local business interface.
 * <li> A bean that has both a No-Interface view and Remote business interface.
 * <li> A bean that has both a No-Interface view and Local component interface.
 * </ul>
 *
 * Then, for all of the above bean configurations, there will be a test
 * variation for each of the following binding configurations:
 *
 * <ul>
 * <li> default bindings
 * <li> component-id bindings
 * <li> simple-binding-name bindings
 * <li> custom bindings
 * </ul>
 *
 * Also, since this abstract class will use the same interfaces for all bean
 * configurations, it is not possible to properly test the short default
 * binding name, as an AmbiguousEJBReferenceException will occur. So, each
 * subclass test must provide the implementation for one additional test
 * that covers the following:
 *
 * <ul>
 * <li> A unique bean class that only has a no interface view with default
 * bindings.
 * </ul>
 **/
@SuppressWarnings("serial")
public abstract class NoInterfaceBindingAbstractServlet extends FATServlet {
    private static final String CLASS_NAME = NoInterfaceBindingAbstractServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Constants used to lookup the various EJB interfaces
    private static final String APP_NAME = "NoInterfaceBndTestApp";
    private static final String MOD_NAME = "NoInterfaceBndBean.jar";

    // Common interface names used by all test variations
    static final String beanInterface = ComplexNoInterfaceBean.class.getName();
    static final String beanLocalInterface = LocalBusiness.class.getName();
    static final String beanRemoteInterface = RemoteBusiness.class.getName();
    static final String beanCompInterface = LocalHome.class.getName();

    // The naming context used to perform lookups
    protected Context ivContext;

    // --------------------------------------------------------------------------
    // Variables that need to be set by each subclass setup() method.
    // --------------------------------------------------------------------------
    protected String beanName; // name of the basic No-Interface EJB
    protected String componentid; // component-id of the basic No-Interface EJB
    protected String simpleName; // simple name of the basic No-Interface EJB
    protected String customPrefix; // custom prefix of the basic No-Interface EJB
    protected boolean supportsComponentInterface = true; // i.e. not singleton

    /**
     * Tests the default bindings for beans with the following configurations:
     *
     * <ul>
     * <li> A bean that is only exposed through the No-Interface view.
     * <li> A bean that has both a No-Interface view and Local business interface.
     * <li> A bean that has both a No-Interface view and Remote business interface.
     * <li> A bean that has both a No-Interface view and Local component interface.
     * </ul>
     *
     * For configurations with multiple interfaces, all interfaces are tested. <p>
     *
     * Note : since multiple beans are using the same implementation class,
     * the short default binding should result in an
     * AmbiguousEJBReferenceException. All other long default bindings
     * should work fine.
     **/
    //@Test
    public void testNoInterfaceDefaultBindings() throws Exception {
        String beanNameLocal = beanName + "Local";
        String beanNameRemote = beanName + "Remote";
        String beanNameComp = beanName + "Component";
        ComplexNoInterfaceBean bean = null;
        //ivContext = (Context) new InitialContext().lookup("");
        ivContext = new InitialContext();

        // -----------------------------------------------------------------------
        // Lookup short default No-Interface bean - Ambiguous
        // -----------------------------------------------------------------------
        svLogger.info("Lookup short default No-Interface bean - Ambiguous");
        try {
            bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" + beanInterface);
            fail("Ambiguous No-Interface short default found");
        } catch (NamingException nex) {
            // TODO: Enable when AmbiguousEJBReferenceException support is added (#11441)
            // Throwable cause = nex.getCause();
            // if (cause instanceof AmbiguousEJBReferenceException) {
            // svLogger.info("lookup of short default failed as expected : " +
            // cause.getClass().getName() + " : " +
            // cause.getMessage());
            // } else {
            svLogger.info(nex.getClass().getName() + " : " + nex.getMessage());
            nex.printStackTrace();
            fail("Ambiguous No-Interface short default lookup failed in an " +
                 "unexpected way : " + nex.getClass().getName() + " : " +
                 nex.getMessage());
            // }
        }

        // -----------------------------------------------------------------------
        // Lookup long default of No-Interface bean
        // -----------------------------------------------------------------------
        svLogger.info("Lookup long default of No-Interface bean");
        bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" + APP_NAME + "/" + MOD_NAME +
                                                         "/" + beanName + "#" + beanInterface);

        // Verify the reference is valid by using it....
        assertEquals("long default no-interface reference of bean is invalid", beanName, bean.getBeanName());

        // -----------------------------------------------------------------------
        // Lookup long default of No-Interface bean with Local
        // -----------------------------------------------------------------------
        svLogger.info("Lookup long default of No-Interface bean with Local");
        bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" + APP_NAME + "/" + MOD_NAME +
                                                         "/" + beanNameLocal + "#" + beanInterface);

        // Verify the reference is valid by using it....
        assertEquals("long default no-interface reference of bean with local is invalid", beanNameLocal, bean.getBeanName());

        LocalBusiness lbean = (LocalBusiness) ivContext.lookup("ejblocal:" + APP_NAME + "/" + MOD_NAME +
                                                               "/" + beanNameLocal + "#" + beanLocalInterface);

        // Verify the reference is valid by using it....
        assertTrue("long default local reference of bean with local is invalid", lbean.localMethod("default test").contains(beanNameLocal));

        // -----------------------------------------------------------------------
        // Lookup long default of No-Interface bean with Remote
        // -----------------------------------------------------------------------
        svLogger.info("Lookup long default of No-Interface bean with Remote");
        bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" + APP_NAME + "/" + MOD_NAME +
                                                         "/" + beanNameRemote + "#" + beanInterface);

        // Verify the reference is valid by using it....
        assertEquals("long default no-interface reference of bean with remote is invalid", beanNameRemote, bean.getBeanName());

        RemoteBusiness rbean = (RemoteBusiness) ivContext.lookup("ejb/" + APP_NAME + "/" + MOD_NAME +
                                                                 "/" + beanNameRemote + "#" + beanRemoteInterface);

        // Verify the reference is valid by using it....
        assertTrue("long default remote reference of bean with remote is invalid", rbean.remoteMethod("default test").contains(beanNameRemote));

        // -----------------------------------------------------------------------
        // Lookup long default of No-Interface bean with Component
        // -----------------------------------------------------------------------
        if (supportsComponentInterface) {
            svLogger.info("Lookup long default of No-Interface bean with Component");
            bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" + APP_NAME + "/" + MOD_NAME +
                                                             "/" + beanNameComp + "#" + beanInterface);

            // Verify the reference is valid by using it....
            assertEquals("long default no-interface reference of bean with component is invalid", beanNameComp, bean.getBeanName());

            LocalHome lhome = (LocalHome) ivContext.lookup("ejblocal:" + APP_NAME + "/" + MOD_NAME +
                                                           "/" + beanNameComp + "#" + beanCompInterface);

            // Verify the reference is valid by using it....
            LocalComponent cbean = lhome.create();
            assertTrue("long default component reference of bean with component is invalid", cbean.localMethod("default test").contains(beanNameComp));
        }
    }

    /**
     * Tests the component-id bindings for beans with the following
     * configurations:
     *
     * <ul>
     * <li> A bean that is only exposed through the No-Interface view.
     * <li> A bean that has both a No-Interface view and Local business interface.
     * <li> A bean that has both a No-Interface view and Remote business interface.
     * <li> A bean that has both a No-Interface view and Local component interface.
     * </ul>
     *
     * For configurations with multiple interfaces, all interfaces are tested. <p>
     **/
    @Test
    public void testNoInterfaceComponentIdBindings() throws Exception {
        String beanNameLocal = beanName + "Local";
        String beanNameRemote = beanName + "Remote";
        String beanNameComp = beanName + "Component";
        String componentidLocal = componentid + "l";
        String componentidRemote = componentid + "r";
        String componentidComp = componentid + "c";
        ComplexNoInterfaceBean bean = null;
        // NOTE: old lookup does not work, JNDI issue raised: #9099
        //ivContext = (Context) new InitialContext().lookup("");
        ivContext = new InitialContext();

        // -----------------------------------------------------------------------
        // Lookup with component-id of No-Interface bean
        // -----------------------------------------------------------------------
        svLogger.info("Lookup with component-id of No-Interface bean");
        bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" +
                                                         componentid +
                                                         "#" + beanInterface);

        // Verify the reference is valid by using it....
        assertEquals("component-id no-interface reference of bean is invalid", beanName, bean.getBeanName());

        // -----------------------------------------------------------------------
        // Lookup with component-id of No-Interface bean with Local
        // -----------------------------------------------------------------------
        svLogger.info("Lookup with component-id of No-Interface bean with Local");
        bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" +
                                                         componentidLocal +
                                                         "#" + beanInterface);

        // Verify the reference is valid by using it....
        assertEquals("component-id no-interface reference of bean with local is invalid", beanNameLocal, bean.getBeanName());

        LocalBusiness lbean = (LocalBusiness) ivContext.lookup("ejblocal:" + componentidLocal +
                                                               "#" + beanLocalInterface);

        // Verify the reference is valid by using it....
        assertTrue("component-id local reference of bean with local is invalid", lbean.localMethod("default test").contains(beanNameLocal));

        // -----------------------------------------------------------------------
        // Lookup with component-id of No-Interface bean with Remote
        // -----------------------------------------------------------------------
        svLogger.info("Lookup with component-id of No-Interface bean with Remote");
        bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" +
                                                         componentidRemote +
                                                         "#" + beanInterface);

        // Verify the reference is valid by using it....
        assertEquals("component-id no-interface reference of bean with remote is invalid", beanNameRemote, bean.getBeanName());

        RemoteBusiness rbean = (RemoteBusiness) ivContext.lookup("ejb/" + componentidRemote +
                                                                 "#" + beanRemoteInterface);

        // Verify the reference is valid by using it....
        assertTrue("component-id remote reference of bean with remote is invalid", rbean.remoteMethod("default test").contains(beanNameRemote));

        // -----------------------------------------------------------------------
        // Lookup with component-id of No-Interface bean with Component
        // -----------------------------------------------------------------------
        if (supportsComponentInterface) {
            svLogger.info("Lookup with component-id of No-Interface bean with Component");
            bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" +
                                                             componentidComp +
                                                             "#" + beanInterface);

            // Verify the reference is valid by using it....
            assertEquals("component-id no-interface reference of bean with component is invalid", beanNameComp, bean.getBeanName());

            LocalHome lhome = (LocalHome) ivContext.lookup("ejblocal:" + componentidComp +
                                                           "#" + beanCompInterface);

            // Verify the reference is valid by using it....
            LocalComponent cbean = lhome.create();
            assertTrue("component-id component reference of bean with component is invalid", cbean.localMethod("default test").contains(beanNameComp));
        }
    }

    /**
     * Tests the simple-binding-name bindings for beans with the following
     * configurations:
     *
     * <ul>
     * <li> A bean that is only exposed through the No-Interface view.
     * <li> A bean that has both a No-Interface view and Local business interface.
     * <li> A bean that has both a No-Interface view and Remote business interface.
     * <li> A bean that has both a No-Interface view and Local component interface.
     * </ul>
     *
     * For configurations with multiple interfaces, all interfaces are tested. <p>
     *
     * Note: For beans with multiple 'local' interfaces, #interface must
     * be appended to the lookup string. For beans without multiple
     * local interfaces, just the simple name is used. The No-Interface
     * view is considered a 'local' interface.
     **/
    @Test
    public void testNoInterfaceSimpleBindings() throws Exception {
        String beanNameLocal = beanName + "Local";
        String beanNameRemote = beanName + "Remote";
        String beanNameComp = beanName + "Component";
        String simpleNameLocal = simpleName + "Local";
        String simpleNameRemote = simpleName + "Remote";
        String simpleNameComp = simpleName + "Component";
        ComplexNoInterfaceBean bean = null;
        // NOTE: old lookup does not work, JNDI issue raised: #9099
        //ivContext = (Context) new InitialContext().lookup("");
        ivContext = new InitialContext();

        // -----------------------------------------------------------------------
        // Lookup with simple-binding-name of No-Interface bean
        // -----------------------------------------------------------------------
        svLogger.info("Lookup with simple-binding-name of No-Interface bean");
        bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" +
                                                         simpleName);

        // Verify the reference is valid by using it....
        assertEquals("simple-binding-name no-interface reference of bean is invalid", beanName, bean.getBeanName());

        // -----------------------------------------------------------------------
        // Lookup with simple-binding-name of No-Interface bean with Local
        // -----------------------------------------------------------------------
        svLogger.info("Lookup with simple-binding-name of No-Interface bean with Local");
        bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" +
                                                         simpleNameLocal +
                                                         "#" + beanInterface);

        // Verify the reference is valid by using it....
        assertEquals("simple-binding-name no-interface reference of bean with local is invalid", beanNameLocal, bean.getBeanName());

        LocalBusiness lbean = (LocalBusiness) ivContext.lookup("ejblocal:" + simpleNameLocal +
                                                               "#" + beanLocalInterface);

        // Verify the reference is valid by using it....
        assertTrue("simple-binding-name local reference of bean with local is invalid", lbean.localMethod("default test").contains(beanNameLocal));

        // -----------------------------------------------------------------------
        // Lookup with simple-binding-name of No-Interface bean with Remote
        // -----------------------------------------------------------------------
        svLogger.info("Lookup with simple-binding-name of No-Interface bean with Remote");
        bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" +
                                                         simpleNameRemote);

        // Verify the reference is valid by using it....
        assertEquals("simple-binding-name no-interface reference of bean with remote is invalid", beanNameRemote, bean.getBeanName());

        RemoteBusiness rbean = (RemoteBusiness) ivContext.lookup(simpleNameRemote);

        // Verify the reference is valid by using it....
        assertTrue("simple-binding-name remote reference of bean with remote is invalid", rbean.remoteMethod("default test").contains(beanNameRemote));

        // -----------------------------------------------------------------------
        // Lookup with simple-binding-name of No-Interface bean with Component
        // -----------------------------------------------------------------------
        if (supportsComponentInterface) {
            svLogger.info("Lookup with simple-binding-name of No-Interface bean with Component");
            bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" +
                                                             simpleNameComp +
                                                             "#" + beanInterface);

            // Verify the reference is valid by using it....
            assertEquals("simple-binding-name no-interface reference of bean with component is invalid", beanNameComp, bean.getBeanName());

            LocalHome lhome = (LocalHome) ivContext.lookup("ejblocal:" + simpleNameComp +
                                                           "#" + beanCompInterface);

            // Verify the reference is valid by using it....
            LocalComponent cbean = lhome.create();
            assertTrue("simple-binding-name component reference of bean with component is invalid", cbean.localMethod("default test").contains(beanNameComp));
        }
    }

    /**
     * Tests the custom bindings for beans with the following
     * configurations:
     *
     * <ul>
     * <li> A bean that is only exposed through the No-Interface view.
     * <li> A bean that has both a No-Interface view and Local business interface.
     * <li> A bean that has both a No-Interface view and Remote business interface.
     * <li> A bean that has both a No-Interface view and Local component interface.
     * </ul>
     *
     * For configurations with multiple interfaces, all interfaces are tested. <p>
     **/
    @Test
    public void testNoInterfaceCustomBindings() throws Exception {
        String beanNameLocal = beanName + "Local";
        String beanNameRemote = beanName + "Remote";
        String beanNameComp = beanName + "Component";
        String customPrefixLocal = customPrefix + "l";
        String customPrefixRemote = customPrefix + "r";
        String customPrefixComp = customPrefix + "c";
        ComplexNoInterfaceBean bean = null;
        // NOTE: old lookup does not work, JNDI issue raised: #9099
        //ivContext = (Context) new InitialContext().lookup("");
        ivContext = new InitialContext();

        // -----------------------------------------------------------------------
        // Lookup with custom of No-Interface bean
        // -----------------------------------------------------------------------
        svLogger.info("Lookup with custom of No-Interface bean");
        bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" +
                                                         customPrefix + "/" +
                                                         beanName);

        // Verify the reference is valid by using it....
        assertEquals("custom no-interface reference of bean is invalid", beanName, bean.getBeanName());

        // -----------------------------------------------------------------------
        // Lookup with custom of No-Interface bean with Local
        // -----------------------------------------------------------------------
        svLogger.info("Lookup with custom of No-Interface bean with Local");
        bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" +
                                                         customPrefixLocal +
                                                         "/" + beanName);

        // Verify the reference is valid by using it....
        assertEquals("custom no-interface reference of bean with local is invalid", beanNameLocal, bean.getBeanName());

        LocalBusiness lbean = (LocalBusiness) ivContext.lookup("ejblocal:" + customPrefixLocal +
                                                               "/" + beanNameLocal);

        // Verify the reference is valid by using it....
        assertTrue("custom local reference of bean with local is invalid", lbean.localMethod("default test").contains(beanNameLocal));

        // -----------------------------------------------------------------------
        // Lookup with custom of No-Interface bean with Remote
        // -----------------------------------------------------------------------
        svLogger.info("Lookup with custom of No-Interface bean with Remote");
        bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" +
                                                         customPrefixRemote +
                                                         "/" + beanName);

        // Verify the reference is valid by using it....
        assertEquals("custom no-interface reference of bean with remote is invalid", beanNameRemote, bean.getBeanName());

        RemoteBusiness rbean = (RemoteBusiness) ivContext.lookup(customPrefixRemote + "/" + beanNameRemote);

        // Verify the reference is valid by using it....
        assertTrue("custom remote reference of bean with remote is invalid", rbean.remoteMethod("default test").contains(beanNameRemote));

        // -----------------------------------------------------------------------
        // Lookup with custom of No-Interface bean with Component
        // -----------------------------------------------------------------------
        if (supportsComponentInterface) {
            svLogger.info("Lookup with custom of No-Interface bean with Component");
            bean = (ComplexNoInterfaceBean) ivContext.lookup("ejblocal:" +
                                                             customPrefixComp +
                                                             "/" + beanName);

            // Verify the reference is valid by using it....
            assertEquals("custom no-interface reference of bean with component is invalid", beanNameComp, bean.getBeanName());

            LocalHome lhome = (LocalHome) ivContext.lookup("ejblocal:" + customPrefixComp +
                                                           "/" + beanNameComp);

            // Verify the reference is valid by using it....
            LocalComponent cbean = lhome.create();
            assertTrue("custom component reference of bean with component is invalid", cbean.localMethod("default test").contains(beanNameComp));
        }
    }
}
