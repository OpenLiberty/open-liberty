/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el50.fat.defaultmethods.servlet;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jakarta.el.BeanELResolver;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.el.StandardELContext;
import jakarta.servlet.annotation.WebServlet;

/**
 * Servlet for testing default methods in Expression Language 5.0
 * https://github.com/jakartaee/expression-language/issues/43
 */
@WebServlet({ "/EL50DefaultMethodsServlet" })
public class EL50DefaultMethodsServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    public EL50DefaultMethodsServlet() {
        super();
    }

    @Test
    @Mode(TestMode.FULL)
    public void testRegularValue() throws Exception {
        SomeBean bean = new SomeBean();

        BeanELResolver resolver = new BeanELResolver();
        ELContext context = new StandardELContext(ELManager.getExpressionFactory());

        Object result = resolver.getValue(context, bean, "valueA");
        assertEquals(bean.getValueA(), result);
    }

    @Test
    public void testDefaultValue() {
        SomeBean bean = new SomeBean();

        BeanELResolver resolver = new BeanELResolver();
        ELContext context = new StandardELContext(ELManager.getExpressionFactory());

        Object result = resolver.getValue(context, bean, "valueB");
        assertEquals(bean.getValueB(), result);
    }

    @Test
    @Mode(TestMode.FULL)
    public void testOverriddenValue() {
        SomeBean bean = new SomeBean();

        BeanELResolver resolver = new BeanELResolver();
        ELContext context = new StandardELContext(ELManager.getExpressionFactory());

        Object result = resolver.getValue(context, bean, "valueC");
        assertEquals(bean.getValueC(), result);
    }

    public interface SomeInterface {
        default public String getValueB() {
            return "DefaultB";
        }

        default public String getValueC() {
            return "DefaultC";
        }
    }

    public class SomeBean implements SomeInterface {
        public String getValueA() {
            return "valueA";
        }

        @Override
        public String getValueC() {
            return "valueC";
        }

    }

}
