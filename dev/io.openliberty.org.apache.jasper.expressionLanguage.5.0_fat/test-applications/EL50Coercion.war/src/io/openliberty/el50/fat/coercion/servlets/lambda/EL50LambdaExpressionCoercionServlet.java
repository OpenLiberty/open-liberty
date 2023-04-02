/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.el50.fat.coercion.servlets.lambda;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.util.function.Predicate;

import jakarta.el.ELException;
import jakarta.el.ELProcessor;

/**
 * Servlet for testing LambdaExpression coercion in Expression Language 5.0
 * https://github.com/jakartaee/expression-language/issues/45
 */
@WebServlet({ "/EL50LambdaExpressionCoercion" })
public class EL50LambdaExpressionCoercionServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    ELProcessor elp;

    public EL50LambdaExpressionCoercionServlet() {
        super();

        elp = new ELProcessor();
        elp.defineBean("lambdaBean", new LambdaBean());
    }

    /**
     * 
     * Expression Language 5.0 in Jakarta EE10 Return the result of invoking the LambdaExpression with the parameters (coerced if necessary) that were passed to the Functional Interface method invocation.
     *
     * @throws Exception
     */
    @Test
    public void testLambdaCoercion() throws Exception {
        // Test lambda coercion basic test passing a lambda as a parameter from a defined bean
        String result;
        String expectedResult ="success";
        Object obj = elp.eval("lambdaBean.testLambdaCoercion(param -> true)");
        assertNotNull(obj);
        result = obj.toString();

        assertEquals("The expression did not evaluate to: " + expectedResult + " but was: " + result, expectedResult, result);
    }

    /**
     * 
     * Expression Language 5.0 in Jakarta EE10 Return the result of invoking the LambdaExpression with the parameters (coerced if necessary) that were passed to the Functional Interface method invocation.
     *
     * Should fail since parameter of lambda was coerced to a wrong type then expected (int expected)
     * @throws Exception
     */
    @Test
    public void testLambdaCoercionWrongType() throws Exception {
        // Test lambda with a wrong type by passing a lambda evaluating with a wrong type
        Object result = null;
        boolean exceptionCaught = false;
        try {
            result = elp.eval("lambdaBean.testIntLambdaCoercion(param -> param.equals('should be int not string'))");
        } catch (ELException e) {
            exceptionCaught = true;
        }
        assertEquals("The expression did not throw expected ELException. Got result: "+result, true, exceptionCaught);
    }

    /**
     * 
     * Expression Language 5.0 in Jakarta EE10 Return the result of invoking the LambdaExpression with the parameters (coerced if necessary) that were passed to the Functional Interface method invocation.
     *
     * Should fail since the call was not defined
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testLambdaCoercionMissingLambda() throws Exception {
        // Test lambda coercion by passing a lambda that is not defined
        Object result = null;
        boolean exceptionCaught = false;
        try {
            result = elp.eval("lambdaBean.missingLambda(param -> true)");
        } catch (ELException e) {
            exceptionCaught = true;
        }
        assertEquals("The expression did not throw expected ELException. Got result: "+result, true, exceptionCaught);
    }

    /**
     * 
     * Expression Language 5.0 in Jakarta EE10 Return the result of invoking the LambdaExpression with the parameters (coerced if necessary) that were passed to the Functional Interface method invocation.
     *
     * Should fail since parameter was coerced to a wrong type then expected (lambda expected)
     * @throws Exception
     */
    @Test
    public void testLambdaCoercionNotALambda() throws Exception {
        // Test by not passing a lambda as parameter but a normal string
        Object result = null;
        boolean exceptionCaught = false;
        try {
            result = elp.eval("lambdaBean.testLambdaCoercion('noLambdaPassed')");
        } catch (ELException e) {
            exceptionCaught = true;
        }
        assertEquals("The expression did not throw expected ELException. Got result: "+result, true, exceptionCaught);
    }

    /*
    * (non-Javadoc)
    *
    * Simple bean used to test lambda coercion 
    */
    public class LambdaBean {

        public String testLambdaCoercion(Predicate<String> filter) {
            return "success";
        }

        public String testIntLambdaCoercion(Predicate<Integer> filter) {
            // Expected string to test against
            Integer expectedInteger = 184;
            if(filter.test(expectedInteger)){
                return "success";
            }
            return "failed";
        }

    }

}
