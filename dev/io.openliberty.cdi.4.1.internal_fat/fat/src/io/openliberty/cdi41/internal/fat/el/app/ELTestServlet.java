/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi41.internal.fat.el.app;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.StandardELContext;
import jakarta.el.ValueExpression;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.el.ELAwareBeanManager;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/elTest")
public class ELTestServlet extends FATServlet {

    @Inject
    private ELAwareBeanManager beanManager;

    @Test
    public void testElAwareBeanManager() {
        // Check it was injected
        assertNotNull("elBeanManager", beanManager);

        // Check we can call it
        ELResolver elResolver = beanManager.getELResolver();
        assertNotNull("elResolver", elResolver);

        // Attempt to use the resolver
        ExpressionFactory factory = ExpressionFactory.newInstance();
        StandardELContext context = new StandardELContext(factory);
        context.addELResolver(elResolver);

        ValueExpression exp = factory.createValueExpression(context, "Result: ${testBean.value}", String.class);
        String value = exp.getValue(context);
        assertEquals("Result: hello", value);

        // Use it as a regular BeanManager (e.g. look up a bean)
        Set<Bean<?>> beans = beanManager.getBeans(TestBean.class);
        assertThat(beans, hasSize(1));
        Bean<?> bean = beans.stream().findFirst().get();
        assertEquals("testBean", bean.getName());
    }
}
