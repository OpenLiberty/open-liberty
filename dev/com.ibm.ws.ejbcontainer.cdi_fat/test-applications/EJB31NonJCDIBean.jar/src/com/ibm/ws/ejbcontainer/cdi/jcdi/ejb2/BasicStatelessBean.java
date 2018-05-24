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

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb2;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import com.ibm.ws.ejbcontainer.cdi.jcdi.ejb.BeanManagerLocal;

/**
 * Basic Stateless bean that should be able to lookup a BeanManager.
 **/
@Stateless(name = "BasicStatelessNonJcdi")
@Local(BeanManagerLocal.class)
public class BasicStatelessBean {
    private static final String CLASS_NAME = BasicStatelessBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource(name = "EJBName")
    private String ivEJBName = "BasicStatelessNonJcdi";

    /**
     * Verifies that the BeanManager was properly injected per the
     * configuration of the bean, and that the BeanManager may be
     * looked up.
     **/
    public void verifyBeanMangerInjectionAndLookup() {
        svLogger.info("> " + ivEJBName + ".verifyBeanMangerInjectionAndLookup()");

        try {
            new InitialContext().lookup("java:comp/BeanManager");
            fail("BeanManager lookup in java:comp/BeanManager should have failed");
        } catch (NameNotFoundException ex)//CannotInstantiateObjectException ex )
        {
            Throwable cause = ex.getCause();
            if (cause instanceof UnsatisfiedResolutionException) {
                assertTrue("Incorrect message text : " + cause.getMessage(),
                           cause.getMessage().contains("is not CDI enabled"));
            } else {
                ex.printStackTrace(System.out);
                fail("Unexpected cause exception : " + cause);
            }
        } catch (NamingException ex) {
            ex.printStackTrace(System.out);
            fail("Unexpected NamingException occurred : " + ex);
        }

        svLogger.info("< " + ivEJBName + ".verifyBeanMangerInjectionAndLookup()");
    }
}
