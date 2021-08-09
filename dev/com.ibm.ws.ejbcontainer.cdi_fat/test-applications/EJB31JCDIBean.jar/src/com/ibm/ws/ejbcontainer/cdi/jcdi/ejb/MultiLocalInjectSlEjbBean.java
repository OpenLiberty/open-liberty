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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;

/**
 * Stateless bean that injects a stateless bean with multiple local interfaces using
 * the @Inject annotation.
 **/
@Stateless(name = "MultiLocalInjectSlEjb")
public class MultiLocalInjectSlEjbBean {
    private static final String CLASS_NAME = MultiLocalInjectSlEjbBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource(name = "EJBName")
    private String ivEJBName = "MultiLocalInjectSlEjb";

    @Inject
    MultiLocalStatelessOne localEjb1;

    @Inject
    MultiLocalStatelessTwo localEjb2;

    /**
     * Verifies that multiple interfaces of an EJB were injected
     * properly and are functional.
     **/
    public void verifyEJBInjection() {
        svLogger.info("> " + ivEJBName + ".verifyEJBInjection()");

        assertNotNull("localEjb1 was not injected", localEjb1);
        assertNotNull("localEjb2 was not injected", localEjb2);
        assertEquals("Incorrect bean name returned", "MultiLocalStateless", localEjb1.getName());
        assertEquals("Incorrect bean name returned", "MultiLocalStateless", localEjb2.getEjbName());

        svLogger.info("< " + ivEJBName + ".verifyEJBInjection()");
    }
}
