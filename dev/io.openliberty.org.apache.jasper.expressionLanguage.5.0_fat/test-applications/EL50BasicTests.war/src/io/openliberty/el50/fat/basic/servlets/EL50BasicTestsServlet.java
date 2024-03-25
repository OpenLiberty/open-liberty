/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el50.fat.basic.servlets;

import static componenttest.annotation.SkipForRepeat.EE11_OR_LATER_FEATURES;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jakarta.el.BeanELResolver;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.el.ELProcessor;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.StandardELContext;
import jakarta.el.StaticFieldELResolver;
import jakarta.el.ValueExpression;
import jakarta.servlet.annotation.WebServlet;

/**
 * Servlet for testing basic changes in Expression Language 5.0
 */
@WebServlet({ "/EL50BasicTests" })
@Mode(TestMode.FULL)
public class EL50BasicTestsServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    ELProcessor elp;

    public EL50BasicTestsServlet() {
        super();

        elp = new ELProcessor();
        elp.defineBean("simpleBean", new SimpleBean());
    }

    /**
     *
     * Expression Language 5.0 in Jakarta EE10 added generics to methods in multiple classes https://github.com/jakartaee/expression-language/issues/157
     * Classes with changes we tests are ELProcessor, ExpressionFactory, ELContext, ValueExpression, ELResolver (returns null)
     * Classes with changes we don't test TypeConverter(returns null)
     * Classes with changes we implicitly test CompositeELResolver(returns null) since it is used by StandardELContext internally
     *
     * @throws Exception
     */
    @Test
    public void testGenerics() throws Exception {
        // Test generics with ELProcessor uses ValueExpression.getValue internally that uses generics
        // No need to cast this to boolean like previous versions
        assertTrue("The type was expected to be true for ELProcessor but was not", elp.eval("simpleBean.getNumber() == 25"));

        // Test generics with ExpressionFactory
        ExpressionFactory factory = ExpressionFactory.newInstance();
        Integer number = 12;
        Double result = factory.coerceToType(number, java.lang.Double.class);
        assertNotNull(result);
        assertTrue("The type was expected to be coerced to Double for ExpressionFactory but was not: " + result, result.equals(new Double(number)));

        // Test generics with ELContext uses ELResolver.convertToType internally
        ELContext elctx = new StandardELContext(ELManager.getExpressionFactory());
        result = elctx.convertToType(number, java.lang.Double.class);
        assertNotNull(result);
        assertTrue("The type was expected to be converted to Double for ELContext but was not: " + result, result.equals(new Double(number)));

        // Test generics with ValueExpression technically not necessary since it is used within the internals of ELProcessor
        ValueExpression valueExp = factory.createValueExpression(number, java.lang.Double.class);
        result = valueExp.getValue(elctx);
        assertNotNull(result);
        assertTrue("The type was expected to be Double for ValueExpression but was not: " + result, result.equals(new Double(number)));

        // Test generics with ELResolver used in ELContext implementation but normal elr.convertToType returns null if not implemented
        elctx = new StandardELContext(ELManager.getExpressionFactory());
        ELResolver elr = elctx.getELResolver();
        result = elr.convertToType(elctx, number, java.lang.Double.class);
        assertNull("The result was expected to be null for ELResolver but was not: " + result, result);
    }

    /**
     *
     * Expression Language 5.0 in Jakarta EE10 switched ELResolver getFeatureDescriptors to return a default value of null and deprecated it to be remove in Expression Language
     * 6.0.
     * https://github.com/jakartaee/expression-language/issues/167
     *
     * @throws Exception
     */
    @Test
    @SkipForRepeat(EE11_OR_LATER_FEATURES) // Expression Language 6.0 removed the getFeatureDescriptors method.
    public void testGetFeatureDescriptors_returnsNull() throws Exception {
        ELResolver resolver = new CustomELResolver();
        assertNull("The result was expected to be null for ELResolver getFeatureDescriptors but was not.",
                   resolver.getFeatureDescriptors(new StandardELContext(ELManager.getExpressionFactory()), new Object()));
    }

    /**
     *
     * Expression Language 5.0 in Jakarta EE10 clarified and documented getType must return null if property is read-only
     * and switched implementation of StaticFieldELResolver getType to match this behavior as it was inconsistent before Expression Language 5.0.
     * https://github.com/jakartaee/expression-language/issues/168
     *
     * @throws Exception
     */
    @Test
    public void testGetType_returnsNull() throws Exception {
        ELManager elm = new ELManager();
        StaticFieldELResolver resolver = new StaticFieldELResolver();
        elm.addELResolver(resolver);
        ELContext context = elm.getELContext();
        context.setPropertyResolved(false);
        Object bean = new ELClass(SimpleBean.class);
        Class<?> type = resolver.getType(context, bean, "staticString");
        assertTrue("When calling getType for StaticFieldELResolver, the property was not resolved!", context.isPropertyResolved());
        assertNull("When calling getType for StaticFieldELResolver, the returned value was not null and instead was: " + type, type);

        BeanELResolver beanResolver = new BeanELResolver();
        elm.addELResolver(beanResolver);
        context.setPropertyResolved(false);
        type = beanResolver.getType(context, new SimpleBean(), "simpleProperty");
        assertTrue("When calling getType for BeanELResolver, the property was not resolved!", context.isPropertyResolved());
        assertNull("When calling getType for BeanELResolver, the returned value was not null and instead was: " + type, type);
    }

    /*
     * (non-Javadoc)
     *
     * Used to test getFeatureDescriptors already returning null in super class ELResolver for https://github.com/openliberty/open-liberty/issues/20455
     */
    public class CustomELResolver extends ELResolver {

        @Override
        public Object getValue(ELContext context, Object base, Object property) {
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.el.ELResolver#getType(javax.el.ELContext, java.lang.Object, java.lang.Object)
         */
        @Override
        public Class<?> getType(ELContext context, Object base, Object property) {
            return String.class;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.el.ELResolver#setValue(javax.el.ELContext, java.lang.Object, java.lang.Object, java.lang.Object)
         */
        @Override
        public void setValue(ELContext context, Object base, Object property, Object value) {
            System.out.println("CustomELResolver:setValue: " + property.getClass());
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.el.ELResolver#isReadOnly(javax.el.ELContext, java.lang.Object, java.lang.Object)
         */
        @Override
        public boolean isReadOnly(ELContext context, Object base, Object property) {
            return false;
        }

        /*
         * (non-Javadoc)
         *
         * @see javax.el.ELResolver#getCommonPropertyType(javax.el.ELContext, java.lang.Object)
         */
        @Override
        public Class<?> getCommonPropertyType(ELContext context, Object base) {
            return null;
        }

    }

}
