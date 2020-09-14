/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.validation.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openliberty.microprofile.openapi20.validation.RuntimeExpressionUtils;

/**
 *
 */
public class OpenAPIRuntimeExpressionTest {

    @Test
    public void testRuntimeExpressionsRegular() {
        assertTrue("Test expression $url", RuntimeExpressionUtils.isRuntimeExpression("$url"));
        assertTrue("Test expression $method", RuntimeExpressionUtils.isRuntimeExpression("$method"));
        assertTrue("Test expression $statusCode", RuntimeExpressionUtils.isRuntimeExpression("$statusCode"));
    }

    @Test
    public void testRuntimeExpressionsSimpleInvalid() {
        assertFalse("Test expression with invalid code 1", RuntimeExpressionUtils.isRuntimeExpression("$barred"));
        assertFalse("Test expression with invalid code 2", RuntimeExpressionUtils.isRuntimeExpression(""));
        assertFalse("Test expression with invalid code 3", RuntimeExpressionUtils.isRuntimeExpression(null));
    }

    @Test
    public void testRuntimeExpressionsForRequest() {
        testRuntimeExpressionsX("$request");
    }

    @Test
    public void testRuntimeExpressionsForResponse() {
        testRuntimeExpressionsX("$response");
    }

    @Test
    public void testExtractVariableContents() {
        assertNotNull("Test one variable in url", RuntimeExpressionUtils.extractURLVars("http://abc.com/{$url}"));
        assertNotNull("Test zero variables in url", RuntimeExpressionUtils.extractURLVars("http://abc.com/$url"));
        assertNotNull("Test two variables in url", RuntimeExpressionUtils.extractURLVars("http://abc.com/{$url}/v1/{id}/path"));
        assertNotNull("Test simple case 1", RuntimeExpressionUtils.extractURLVars(""));
        assertNotNull("Test simple case 2", RuntimeExpressionUtils.extractURLVars("{}"));
        assertNull("Test degenerate case 1", RuntimeExpressionUtils.extractURLVars("}"));
        assertNull("Test degenerate case 2", RuntimeExpressionUtils.extractURLVars("{"));
        assertNull("Test invalid case 1", RuntimeExpressionUtils.extractURLVars("x{x{yy}zzz}"));
        assertNull("Test invalid case 2", RuntimeExpressionUtils.extractURLVars("x{x{x}"));
        assertNull("Test invalid case 3", RuntimeExpressionUtils.extractURLVars("x{xx}xx{yy"));
        assertNull("Test invalid case 4", RuntimeExpressionUtils.extractURLVars("yyy}x{xx}xx"));
        assertNull("Test invalid case 5", RuntimeExpressionUtils.extractURLVars("x{xx}xx}yy"));
    }

    public void testRuntimeExpressionsX(String exp) {

        assertTrue("Test expression" + exp + ".header.name0123", RuntimeExpressionUtils.isRuntimeExpression(exp + ".header.name0123"));
        assertFalse("Test expression" + exp + ".header.{invalid}", RuntimeExpressionUtils.isRuntimeExpression(exp + ".header.{invalid}"));

        assertTrue("Test expression" + exp + ".query.name", RuntimeExpressionUtils.isRuntimeExpression(exp + ".query.name"));
        assertFalse("Test invalid expression" + exp + ".query.", RuntimeExpressionUtils.isRuntimeExpression(exp + ".query."));
        assertFalse("Test invalid expression" + exp + ".query", RuntimeExpressionUtils.isRuntimeExpression(exp + ".query"));

        assertTrue("Test expression" + exp + ".path.name", RuntimeExpressionUtils.isRuntimeExpression(exp + ".path.name"));
        assertFalse("Test invalid expression" + exp + ".path.", RuntimeExpressionUtils.isRuntimeExpression(exp + ".path."));
        assertFalse("Test invalid expression" + exp + ".path", RuntimeExpressionUtils.isRuntimeExpression(exp + ".path"));

        assertTrue("Test expression" + exp + ".body#/components/schema/food", RuntimeExpressionUtils.isRuntimeExpression(exp + ".body#/components/schema/food"));
        assertTrue("Test expression" + exp + ".body#/comp~1onents/sch~0ema/food~1 (some tildes)",
                   RuntimeExpressionUtils.isRuntimeExpression(exp + ".body#/comp~1onents/sch~0ema/food~1"));
        assertFalse("Test invalid expression" + exp + ".body# empty 1", RuntimeExpressionUtils.isRuntimeExpression(exp + ".body#/"));
        assertFalse("Test invalid expression" + exp + ".body# empty 2", RuntimeExpressionUtils.isRuntimeExpression(exp + ".body#//"));
        assertFalse("Test invalid expression" + exp + ".body# empty first segment", RuntimeExpressionUtils.isRuntimeExpression(exp + ".body#//components/schema/food"));
        assertFalse("Test invalid expression" + exp + ".body# empty segment", RuntimeExpressionUtils.isRuntimeExpression(exp + ".body#/components//schema/food"));
        assertFalse("Test invalid expression" + exp + ".body# invalid tilde escape 1", RuntimeExpressionUtils.isRuntimeExpression(exp + ".body#/components/s~chema/food"));
        assertFalse("Test invalid expression" + exp + ".body# invalid tilde escape 2", RuntimeExpressionUtils.isRuntimeExpression(exp + ".body#/components/schema/food~"));
    }
}
