/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.web;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.appConfig.cdi.beans.ConfigPropertyBean;

@SuppressWarnings("serial")
@WebServlet("/configProperty")
public class ConfigPropertyTestServlet extends AbstractBeanServlet {

    @Inject
    ConfigPropertyBean configBean;

    /** {@inheritDoc} */
    @Override
    public Object getBean() {
        return configBean;
    }

    @Test
    public void testNullKey() throws Exception {
        test("nullKey", "nullKeyValue");
    }

    @Test
    public void testEmptyKey() throws Exception {
        test("emptyKey", "emptyKeyValue");
    }

    @Test
    public void testDefaultKey() throws Exception {
        test("defaultKey", "defaultKeyValue");
    }

    @Test
    public void testDefaultValueNotUsed() throws Exception {
        test("URL_KEY", "http://www.ibm.com");
    }

    @Test
    public void testDefaultValue() throws Exception {
        test("DEFAULT_URL_KEY", "http://www.default.com");
    }

    @Test
    public void testOptionalThatExists() throws Exception {
        test("fromOptionalThatExists", "itExists");
    }

    @Test
    public void testOptionalThatDoesNotExist() throws Exception {
        test("elseFromOptionalThatExists", "passed: should not exist");
    }
}
