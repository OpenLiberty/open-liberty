/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.web;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
public abstract class AbstractBeanServlet extends FATServlet {

    public abstract Object getBean();

    public void test(String key, String expected) throws Exception {
        Object value = get(key);
        assertEquals(expected, value == null ? "null" : value.toString());
    }

    public Object get(String key) {
        try {
            Method method = getBean().getClass().getMethod("get" + key);
            return method.invoke(getBean());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return e.getCause();
        } catch (Exception e) {
            e.printStackTrace();
            return e;
        }
    }

}
