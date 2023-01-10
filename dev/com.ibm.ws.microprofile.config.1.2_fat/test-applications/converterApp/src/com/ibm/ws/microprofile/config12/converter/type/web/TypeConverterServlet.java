/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
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
package com.ibm.ws.microprofile.config12.converter.type.web;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/TypeConverterServlet")
public class TypeConverterServlet extends FATServlet {

    @Inject
    TypeConverterBean bean;

    @Test
    public void converterPrioritySPITest() throws Exception {
        bean.lambdaConverterTest();
    }

    @Test
    public void forcedTypeConverterTest() throws Exception {
        bean.forcedTypeConverterTest();
    }

    @Test
    public void listConverterTest() throws Exception {
        bean.listConverterTest();
    }

    @Test
    public void optionalListConverterTest() throws Exception {
        bean.optionalListConverterTest();
    }

    @Test
    public void providerOptionalListConverterTest() throws Exception {
        bean.providerOptionalListConverterTest();
    }

    @Test
    public void setConverterTest() throws Exception {
        bean.setConverterTest();
    }

    @Test
    public void optionalConverterTest() throws Exception {
        bean.optionalConverterTest();
    }

    @Test
    public void defaultOptionalConverterTest() throws Exception {
        bean.defaultOptionalConverterTest();
    }
}
