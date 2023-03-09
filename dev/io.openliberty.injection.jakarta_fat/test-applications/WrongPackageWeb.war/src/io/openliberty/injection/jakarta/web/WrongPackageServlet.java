/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.injection.jakarta.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import componenttest.app.FATServlet;
import io.openliberty.injection.jakarta.ejb.JakartaSingletonBean;
import io.openliberty.injection.jakarta.ejb.JakartaSingletonResourcesBean;
import io.openliberty.injection.jakarta.ejb.JakartaStatefulPreDestroyBean;
import io.openliberty.injection.jakarta.ejb.JakartaStatelessBean;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

/**
 * Servlet using Jakarta package annotations, except for one incorrect use
 * of javax.annotation.Resource.
 */
@SuppressWarnings("serial")
@WebServlet("/WrongPackageServlet")
public class WrongPackageServlet extends FATServlet {
    @Resource
    UserTransaction userTx;

    @javax.annotation.Resource
    UserTransaction userTx2;

    @EJB
    JakartaSingletonWarBean bean1;

    @EJB
    JakartaStatelessWarBean bean2;

    @EJB
    JakartaStatelessWarPostConstructBean bean3;

    @EJB
    JakartaSingletonBean bean4;

    @EJB
    JakartaSingletonResourcesBean bean5;

    @EJB
    JakartaStatefulPreDestroyBean bean6;

    @EJB
    JakartaStatelessBean bean7;

    /**
     * This test verifies a warning is logged when the javax.annotation.Resource annotation
     * is used with Jakarta EE features and the annotation is ignored. There should be one
     * warning per module. The jakarta.annotation.Resource annotation works as expected.
     */
    public void testWrongPackageCommonAnnotations() throws Exception {
        assertNotNull("jakarta Resource UserTransaction is null", userTx);
        assertNull("javax Resource UserTransaction is not null", userTx2);

        assertNotNull("jakarta EJB Singleton WAR is null", bean1);
        bean1.verifyInjection();
        assertNotNull("jakarta EJB Stateless WAR is null", bean2);
        bean2.verifyInjection();
        assertNotNull("jakarta EJB Stateless PostConstruct WAR is null", bean3);
        bean3.verifyPostConstruct();
        assertNotNull("jakarta EJB Singleton EJB is null", bean4);
        bean4.verifyInjection();
        assertNotNull("jakarta EJB Singleton Resources EJB is null", bean5);
        bean5.verifyInjection();
        assertNotNull("jakarta EJB Stateful PreDestroy EJB is null", bean6);
        bean6.remove();
        assertNotNull("jakarta EJB Stateless EJB is null", bean7);
        bean7.verifyInjection();
    }
}