/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejblite.interceptor.v32.mix.ejb;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

public class XMLInjectionInterceptor2 {
    private static final String LOGGER_CLASS_NAME = XMLInjectionInterceptor2.class.getName();
    private final static Logger svLogger = Logger.getLogger(LOGGER_CLASS_NAME);

    // @EJB(beanName="MixedSFInterceptorBean")
    MixedSFLocal ejbLocalRef; // EJB Local ref

    // @Resource(name="env/StringVal")
    public String envEntry;

    @AroundInvoke
    private Object aroundInvoke(InvocationContext inv) throws Exception {
        Method m = inv.getMethod();
        svLogger.info("XMLInjectionInterceptor2.aroundInvoke: " + m.getName());
        if (m.getName().equals("getXMLInterceptorResults2")) {
            assertNotNull("Checking EJB Local Ref", ejbLocalRef);
            ejbLocalRef.setString("XMLInjectionInterceptor2");
            assertEquals("Checking ejb local ref", "XMLInjectionInterceptor2", ejbLocalRef.getString());

            assertNotNull("Checking env entry", envEntry);

            assertEquals("Checking value of env entry", envEntry, "Hello!");
        }

        Object rv = inv.proceed();
        return rv;
    }
}
