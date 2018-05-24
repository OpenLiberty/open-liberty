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

package com.ibm.ws.ejbcontainer.cdi.jcdi.ejb_int;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;

/**
 * Basic Stateless bean that has no interceptors (EJB or CDI).
 **/
@Stateless(name = "NoInterceptorBasicStateless")
@Local(InterceptorLocal.class)
@Remote(InterceptorRemote.class)
public class BasicStatelessBean {
    private static final String CLASS_NAME = BasicStatelessBean.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @Resource(name = "EJBName")
    private String ivEJBName = "NoInterceptorBasicStateless";

    private final List<String> ivPostConstructStack = new ArrayList<String>();

    @PostConstruct
    void initialize() {
        ivPostConstructStack.add(CLASS_NAME + ".initialize");
    }

    /**
     * Verifies that the interceptors were properly called per the
     * configuration of the bean.
     *
     * @param callStack list to be updated by interceptors
     **/
    public void verifyInterceptorCalls(List<String> callStack) {
        svLogger.info("> " + ivEJBName + ".verifyInterceptorCalls()");

        assertEquals("Unexpected PostConstruct interceptor calls : " + ivPostConstructStack,
                     1, ivPostConstructStack.size());

        String stackEntry = ivPostConstructStack.get(0);
        assertEquals("Unexpected PostConstruct interceptor call : " + stackEntry,
                     CLASS_NAME + ".initialize", stackEntry);

        assertEquals("Unexpected interceptor calls : " + callStack,
                     0, callStack.size());

        svLogger.info("< " + ivEJBName + ".verifyInterceptorCalls()");
    }
}
