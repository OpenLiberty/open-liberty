/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Stateless bean that has a BeanManager injected via the @Inject annotation;
 * that should not be able to lookup a BeanManager in java:comp/env.
 **/
@Stateless(name = "BeanManagerStatelessInject")
@Local(BeanManagerLocal.class)
@Remote(BeanManagerRemote.class)
public class BeanManagerStatelessInjectBean {
    private static final String CLASS_NAME = BeanManagerStatelessInjectBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource(name = "EJBName")
    private String ivEJBName = "BeanManagerStatelessInject";

    @Resource(name = "EJBContext")
    private SessionContext ivContext;

    @Inject
    private BeanManager ivBeanManager;

    private BeanManager ivBeanManagerM;

    @Inject
    protected void setAnotherBeanManager(BeanManager beanmgr) {
        ivBeanManagerM = beanmgr;
    }

    /**
     * Verifies that the BeanManager was properly injected per the
     * configuration of the bean, and that the BeanManager may NOT
     * be looked up in java:comp/env.
     **/
    public void verifyBeanMangerInjectionAndLookup() {
        svLogger.info("> " + ivEJBName + ".verifyBeanMangerInjectionAndLookup()");

        assertNotNull("Field BeanManager was not injected", ivBeanManager);
        assertNotNull("Method BeanManager was not injected", ivBeanManagerM);

        BeanManager bm = null;
        try {
            bm = (BeanManager) ivContext.lookup("suite.r80.base.ejb31misc.jcdi.ejb.BeanManagerStatelessInjectBean/ivBeanManager");
            fail("BeanManager lookup in java:comp/env should have failed");
        } catch (IllegalArgumentException iaex) {
            assertNotNull("Expected IllegalArgumentException did not occur", iaex);
        }

        try {
            bm = (BeanManager) new InitialContext().lookup("java:comp/BeanManager");
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            throw new EJBException("Unexpected Exception : " + ex, ex);
        }

        assertNotNull("null returned on BeanManager lookup", bm);

        svLogger.info("< " + ivEJBName + ".verifyBeanMangerInjectionAndLookup()");
    }
}
