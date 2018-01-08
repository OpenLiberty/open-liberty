/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config12.converter.priority.web;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.microprofile.config12.converter.priority.beans.ConverterPriorityBean;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@SuppressWarnings("serial")
@WebServlet("/ConverterPriorityServlet")
public class ConverterPriorityServlet extends FATServlet {

    @Inject
    ConverterPriorityBean bean;

    @Test
    @Mode(TestMode.LITE)
    public void noConversionTest() throws Exception {
        bean.noConversionTest();
    }

    @Test
    @Mode(TestMode.LITE)
    public void converterPriorityTest() throws Exception {
        bean.converterPriorityTest();
    }

    @Test
    @Mode(TestMode.LITE)
    public void converterPrioritySPITest() throws Exception {
        bean.converterPrioritySPITest();
    }
}
