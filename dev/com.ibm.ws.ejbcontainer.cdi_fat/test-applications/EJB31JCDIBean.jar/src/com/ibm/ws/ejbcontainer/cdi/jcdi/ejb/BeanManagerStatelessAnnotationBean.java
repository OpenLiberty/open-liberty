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

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Stateless bean that has a BeanManager injected via the @Resource annotation;
 * that should also be able to lookup a BeanManager in java:comp.
 **/
@Stateless(name = "BeanManagerStatelessAnnotation")
@Local(BeanManagerLocal.class)
@Remote(BeanManagerRemote.class)
public class BeanManagerStatelessAnnotationBean {
    private static final String CLASS_NAME = BeanManagerStatelessAnnotationBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource(name = "EJBName")
    private String ivEJBName = "BeanManagerStatelessAnnotation";

    @Resource(name = "EJBContext")
    private SessionContext ivContext;

    @Resource(name = "BeanManager")
    private BeanManager ivBeanManager;

    /**
     * Verifies that the BeanManager was properly injected per the
     * configuration of the bean, and that the BeanManager may be
     * looked up.
     **/
    public void verifyBeanMangerInjectionAndLookup() {
        svLogger.info("> " + ivEJBName + ".verifyBeanMangerInjectionAndLookup()");

        assertNotNull("BeanManager was not injected", ivBeanManager);

        BeanManager bm = (BeanManager) ivContext.lookup("BeanManager");

        assertNotNull("BeanManager was null from java:comp/env", ivBeanManager);

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
