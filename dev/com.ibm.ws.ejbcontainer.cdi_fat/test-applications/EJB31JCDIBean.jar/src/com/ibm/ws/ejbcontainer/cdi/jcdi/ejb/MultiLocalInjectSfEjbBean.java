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
 * Stateless bean that injects a stateful bean with multiple local interfaces using
 * the @Inject annotation.
 **/
@Stateless(name = "MultiLocalInjectSfEjb")
public class MultiLocalInjectSfEjbBean {
    private static final String CLASS_NAME = MultiLocalInjectSfEjbBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource(name = "EJBName")
    private String ivEJBName = "MultiLocalInjectSfEjb";

    @Inject
    MultiLocalStatefulOne localEjb1;

    @Inject
    MultiLocalStatefulTwo localEjb2;

    @Inject
    MultiLocalStatefulBean localEjb;

    /**
     * Verifies that multiple interfaces of an EJB were injected
     * properly and are functional.
     **/
    public void verifyEJBInjection() {
        svLogger.info("> " + ivEJBName + ".verifyEJBInjection()");

        assertNotNull("localEjb1 was not injected", localEjb1);
        assertNotNull("localEjb2 was not injected", localEjb2);
        assertNotNull("localEjb  was not injected", localEjb);
        assertEquals("Incorrect bean name returned", "MultiLocalStateful", localEjb1.getName());
        assertEquals("Incorrect bean name returned", "MultiLocalStateful", localEjb2.getEjbName());
        assertEquals("Incorrect bean name returned", "MultiLocalStateful", localEjb.getName());

        svLogger.info("< " + ivEJBName + ".verifyEJBInjection()");
    }
}
