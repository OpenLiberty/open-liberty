/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
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

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.ejbcontainer.bindings.noInterface.bnd.ejb.BasicNoInterfaceSFBean;

/**
 * Test that the various binding options work properly for Stateful beans with
 * a No-Interface View. <p>
 *
 * The following configurations of Stateful Session beans are tested:
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
 * Also, since the parent abstract class will use the same interfaces for all
 * bean configurations, it is not possible to properly test the short default
 * binding name, as an AmbiguousEJBReferenceException will occur. So, this
 * subclass test must provide the implementation for one additional test
 * that covers the following:
 *
 * <ul>
 * <li> A unique bean class that only has a no interface view with default
 * bindings.
 * </ul>
 */
@SuppressWarnings("serial")
@WebServlet("/NoInterfaceBindingStatefulServlet")
public class NoInterfaceBindingStatefulServlet extends NoInterfaceBindingAbstractServlet {
    private static final String CLASS_NAME = NoInterfaceBindingStatefulServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Constants used to lookup the various EJB interfaces
    private static final String APP_NAME = "NoInterfaceBndTestApp";
    private static final String MOD_NAME = "NoInterfaceBndBean.jar";

    @PostConstruct
    protected void setUp() {
        svLogger.info("> " + CLASS_NAME + ".setUp()");

        beanName = "NoInterfaceSF";
        componentid = "com/ibm/ws/ejbcontainer/bindings/noInterface/bnd/comp/sf";
        simpleName = "com/ibm/ws/ejbcontainer/bindings/noInterface/bnd/NoInterfaceSF";
        customPrefix = "com/ibm/ws/ejbcontainer/bindings/noInterface/bnd/sf";

        svLogger.info("< " + CLASS_NAME + ".setUp()");
    }

    /**
     * Tests that a unique bean implementation that is only exposed through
     * the No-Interface view may be looked up by either the short or long
     * default binding name. <p>
     *
     * Since this subclass test will need to provide a specific implementation
     * class.... and it would be best to call a method on that class, it
     * is left up to this subclass to fully implement this variation.
     */
    @Test
    public void testUniqueNoInterfaceDefaultBindings_NoInterfaceBindingStateful() throws Exception {
        String beanName = BasicNoInterfaceSFBean.class.getSimpleName();
        String beanInterface = BasicNoInterfaceSFBean.class.getName();

        ivContext = (Context) new InitialContext().lookup("");

        // Lookup short default of basic No-Interface bean - should find
        BasicNoInterfaceSFBean bbean = (BasicNoInterfaceSFBean) ivContext.lookup("ejblocal:" + beanInterface);

        // Verify the reference is valid by using it....
        assertEquals("short default no-interface reference of bean is invalid", beanName, bbean.getBeanName());

        // Lookup long default of basic No-Interface bean - should find
        bbean = (BasicNoInterfaceSFBean) ivContext.lookup("ejblocal:" + APP_NAME + "/" + MOD_NAME + "/" + beanName + "#" + beanInterface);

        // Verify the reference is valid by using it....
        assertEquals("long default no-interface reference of bean is invalid", beanName, bbean.getBeanName());
    }
}
