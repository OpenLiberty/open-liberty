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
package io.openliberty.el50.fat.coercion.servlets.array;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jakarta.servlet.annotation.WebServlet;
import java.util.Arrays;

import jakarta.el.ELException;
import jakarta.el.ELProcessor;

/**
 * Servlet for testing Array coercion in Expression Language 5.0
 * https://github.com/jakartaee/expression-language/issues/172
 */
@WebServlet({ "/EL50ArrayCoercion" })
public class EL50ArrayCoercionServlet extends FATServlet {
    private static final long serialVersionUID = 1L;

    ELProcessor elp;

    public EL50ArrayCoercionServlet() {
        super();

        elp = new ELProcessor();
        elp.defineBean("arrayBean", new ArrayBean());
    }

    /**
     * 
     * Expression Language 5.0 in Jakarta EE10 If `A` is assignable to an array of type `T`, coerce quietly.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleArrayCoercion() throws Exception {
        // Simple array coercion test
        int[] expectedResult = new int[] {1, 8, 4};
        int[] result = elp.eval("arrayBean.testArrayCoercion([1,8,4].toArray())");
        assertNotNull(result);
        assertArrayEquals("The expression did not evaluate to: " + Arrays.toString(expectedResult) + " but was: " + Arrays.toString(result), expectedResult, result);
    }

    /**
     * 
     * Expression Language 5.0 in Jakarta EE10 whenever an array is expected but not received an error is thrown.
     *
     * @throws Exception
     */
    @Test
    @Mode(TestMode.FULL)
    public void testNoArrayCoercion() throws Exception {
        // Array coercion attempt with an invalid parameter
        int[] result = null;
        boolean exceptionCaught = false;
        try {
            result = elp.eval("arrayBean.testArrayCoercion(\"No array here\")");
        } catch (ELException e) {
            exceptionCaught = true;
        }
        assertEquals("The expression did not throw expected ELException. Got result: " + Arrays.toString(result), true, exceptionCaught);
    }

    /**
     * 
     * Expression Language 5.0 in Jakarta EE10 whenever an array is coerced if the array is null then null is expected.
     *
     * @throws Exception
     */
    @Test
    public void testNullArrayCoercion() throws Exception {
        // Array coercion passing a null value which should return null
        int[] result = elp.eval("arrayBean.testArrayCoercion(null)");
        assertNull("The expression did not evaluate to null but was: " + Arrays.toString(result), result);
    }

    /**
     * 
     * Expression Language 5.0 in Jakarta EE10 whenever an array is coerced it goes element by element coercing them to the expected type.
     *
     * @throws Exception
     */
    @Test
    public void testOtherTypesArrayCoercion() throws Exception {
        // Array coercion where each element should coerce to an int value
        int[] expectedResult = new int[] {0, 1, 8, 10, 4};
        int[] result = elp.eval("arrayBean.testArrayCoercion([null, 1, 8, \"10\", 4].toArray())");
        assertNotNull(result);
        assertArrayEquals("The expression did not evaluate to: " +  Arrays.toString(expectedResult) + " but was: " + Arrays.toString(result), expectedResult, result);
    }

    /**
     * 
     * Expression Language 5.0 in Jakarta EE10 whenever an array is coerced it goes element by element coercing them to the expected type.
     * According to spec while coercing the elements to integer if A is boolean then an error is thrown.
     *
     * @throws Exception
     */
    @Test
    public void testInvalidTypeArrayCoercion() throws Exception {
        // Array coercion where an invalid value is found (boolean false)
        int[] result = null;
        boolean exceptionCaught = false;
        try {
            result = elp.eval("arrayBean.testArrayCoercion([false, 1, 8, \"10\", 4].toArray())");
        } catch (ELException e) {
            exceptionCaught = true;
        }
        assertEquals("The expression did not throw expected ELException. Got result: " + Arrays.toString(result), true, exceptionCaught);
    }

    /*
    * (non-Javadoc)
    *
    * Simple bean used to test array coercion 
    */
    public class ArrayBean {

        public int[] testArrayCoercion(int array[]) {
            return array;
        }

    }

}
