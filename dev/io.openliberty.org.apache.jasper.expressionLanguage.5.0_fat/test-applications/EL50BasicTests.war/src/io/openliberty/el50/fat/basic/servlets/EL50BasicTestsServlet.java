/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.el50.fat.basic.servlets;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.el.ELProcessor;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.StandardELContext;
import jakarta.el.ValueExpression;
import jakarta.servlet.annotation.WebServlet;


/**
 * Servlet for testing basic changes in EL 5.0
 */
@WebServlet({ "/EL50BasicTests" })
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
     * Classes with changes we don't test or implicitly test TypeConverter(returns null), CompositeELResolver(returns null)
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testGenerics() throws Exception {
        // Test generics with ELProcessor uses ValueExpression.getValue under the covers that uses generics
        // No need to cast this to boolean like previous versions
        assertTrue("The type was expected to be true for ELProcessor but was not", elp.eval("simpleBean.getNumber() == 25"));

        // Test generics with ExpressionFactory
        ExpressionFactory factory = ExpressionFactory.newInstance();
        Integer number = 12;
        Double result = factory.coerceToType(number, java.lang.Double.class);
        assertNotNull(result);
        assertTrue("The type was expected to be coerced to Double for ExpressionFactory but was not: " + result, result.equals(new Double(number)));

        // Test generics with ELContext uses ELResolver.convertToType under the covers
        ELContext elctx = new StandardELContext(ELManager.getExpressionFactory());
        elctx.convertToType(number, java.lang.Double.class);
        assertNotNull(result);
        assertTrue("The type was expected to be converted to Double for ELContext but was not: " + result, result.equals(new Double(number)));

        // Test generics with ValueExpression technically not necessary since it is used in the covers of elprocessor
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
     * Expression Language 5.0 in Jakarta EE10 switched ELResolver getFeatureDescriptors to return a default value of null and deprecate it to remove in EL 6.0.
     * https://github.com/jakartaee/expression-language/issues/167
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testgetFeatureDescriptors_returnsNull() throws Exception {
        ELResolver resolver = new CustomELResolver();
        assertNull("The result was expected to be null for ELResolver getFeatureDescriptors but was not.",resolver.getFeatureDescriptors(new StandardELContext(ELManager.getExpressionFactory()),new Object()));
    }


    public class SimpleBean {

        public int getNumber() {
            return 25;
        }

    }

    /*
    * (non-Javadoc)
    *
    * Used to test getFeatureDescriptors already returning null in super class ELResolver for https://github.com/openliberty/open-liberty/issues/20455
    */
    public class CustomELResolver extends ELResolver{

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
