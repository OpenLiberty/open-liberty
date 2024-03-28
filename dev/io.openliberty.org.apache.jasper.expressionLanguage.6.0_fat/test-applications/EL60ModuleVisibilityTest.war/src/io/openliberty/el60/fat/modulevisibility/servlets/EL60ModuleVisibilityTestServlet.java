/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el60.fat.modulevisibility.servlets;

import static org.junit.Assert.assertEquals;

import java.util.TimeZone;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.el.ExpressionFactory;
import jakarta.el.ValueExpression;
import jakarta.servlet.annotation.WebServlet;

/**
 * Servlet for Expression Language 6.0 module visibility.
 *
 * This has been fixed in our Expression Language implementation since Expression Language 3.0.
 *
 * If we were to run this test with the jsp-2.2 feature we would hit the following exception in the logs:
 *
 * javax.el.ELException: java.lang.IllegalAccessException:
 * class javax.el.BeanELResolver cannot access class sun.util.calendar.ZoneInfo (in module java.base)
 * because module java.base does not export sun.util.calendar to unnamed module x
 * at javax.el.BeanELResolver.getValue(BeanELResolver.java:94)
 */
@WebServlet({ "/EL60ModuleVisibilityTest" })
public class EL60ModuleVisibilityTestServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    /**
     * Test the following Expression Language 6.0 issue:
     * https://github.com/jakartaee/expression-language/issues/188
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testEL60ModuleVisibility() throws Exception {
        Integer expectedRawOffset = TimeZone.getDefault().getRawOffset();

        ELManager manager = new ELManager();
        ELContext context = manager.getELContext();
        ExpressionFactory factory = ELManager.getExpressionFactory();

        ValueExpression expression = factory.createValueExpression(TimeZone.getDefault(), TimeZone.class);
        context.getVariableMapper().setVariable("timeZone", expression);

        expression = factory.createValueExpression(context, "${timeZone.rawOffset}", Integer.class);
        Integer result = expression.getValue(context);

        assertEquals("The expression evaluated to: " + result + " but was expected to be: " + expectedRawOffset, result, expectedRawOffset);
    }
}
