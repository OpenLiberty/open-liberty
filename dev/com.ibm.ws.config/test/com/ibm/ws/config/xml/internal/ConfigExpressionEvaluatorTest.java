/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.ibm.websphere.config.ConfigEvaluatorException;

/**
 *
 */
public class ConfigExpressionEvaluatorTest {

    private ConfigExpressionEvaluator getEv() {
        return new ConfigExpressionEvaluator() {

            @Override
            Object getPropertyObject(String argName) throws ConfigEvaluatorException {
                if ("array1".equals(argName))
                    return new String[] { "foo" };
                if ("array2".equals(argName))
                    return new String[] { "foo", "bar" };
                if ("array3".equals(argName))
                    return new String[] { "foo", "bar", "baz" };
                if ("intArray2".equals(argName))
                    return new int[] { 1, 2 };
                if ("string".equals(argName))
                    return "str";
                throw new IllegalArgumentException("unrecognized: " + argName);
            }

            @Override
            String getProperty(String argName) throws ConfigEvaluatorException {
                return null;
            }
        };
    }

    @Test
    public void testSumOfCount2() throws Exception {
        ConfigExpressionEvaluator ev = getEv();
        assertEquals("3", ev.evaluateExpression("count(array1)+count(array2)"));
    }

    @Test
    public void testSumOfCount3() throws Exception {
        ConfigExpressionEvaluator ev = getEv();
        assertEquals("6", ev.evaluateExpression("count(array1)+count(array2)+count(array3)"));
    }

    @Test
    public void testfilterSyntaxError() throws Exception {
        ConfigExpressionEvaluator ev = getEv();
        assertNull(ev.evaluateExpression("servicePidOrFilter(intArray2)"));
    }

    @Test
    public void testTrailingSpae() throws Exception {
        ConfigExpressionEvaluator ev = getEv();
        assertNull(ev.evaluateExpression("servicePidOrFilter(string) "));
    }

}
