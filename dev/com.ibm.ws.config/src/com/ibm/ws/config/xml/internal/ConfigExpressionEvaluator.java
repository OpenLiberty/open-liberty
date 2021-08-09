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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.osgi.framework.Constants;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 *
 */
abstract class ConfigExpressionEvaluator {

    abstract String getProperty(String argName) throws ConfigEvaluatorException;

    abstract Object getPropertyObject(String argName) throws ConfigEvaluatorException;

    private long subtotal;

    private String resultExpr;

    private long value;

    /**
     * Evaluate a variable expression.
     *
     * @param expr the expression string (for example, "x+0")
     * @param context
     * @return the result, or null if the evaluation fails because the string
     *         cannot be parsed, a referenced property does not exist, or some
     *         other detectable errors occurs
     * @throws ConfigEvaluatorException if getPropertyObject fails
     * @throws NumberFormatException if a property contains a value that cannot
     *             be parsed as a Long, or a literal with a too large number
     * @throws ArithmeticException if a division error occurs
     */
    String evaluateExpression(String expr) throws ConfigEvaluatorException {
        ConfigExpressionScanner scanner = new ConfigExpressionScanner(expr);

        // Expression   = Arithmetic | FilterCall
        // Arithmetic   = Operand [( "+" | "-" | "*" | "/") Operand]*
        // Operand      = VarName | Long | FunctionCall
        // VarName      = <Java identifier plus "." after start>
        // Long         = <[0-9]+ handled by Long.parseLong>
        // FunctionCall = FunctionName "(" VarName ")"
        // FunctionName = "count"
        // FilterCall   = "servicePidOrFilter(" VarName ")"

        // Parse the first operand (or function name).
        if (!parseOperand(expr, scanner, true)) {
            return null;
        }
        if (resultExpr != null) {
            return scanner.end() ? resultExpr : null;
        }

        subtotal = value;

        while (!scanner.end()) {
            // Parse operator.
            ConfigExpressionScanner.NumericOperator op = scanner.scanNumericOperator();
            if (op == null) {
                return null;
            }

            if (!parseOperand(expr, scanner, false) || (resultExpr != null)) {
                return null;
            }

            subtotal = op.evaluate(subtotal, value);

        }

        return String.valueOf(subtotal);
    }

    private boolean parseOperand(String expr, ConfigExpressionScanner scanner, boolean initial) throws ConfigEvaluatorException {
        // Parse the first operand (or function name).
        String name = scanner.scanName();
        if (name == null) {
            Long value = scanner.scanLong();
            if (value == null) {
                return false;
            }
            this.value = value;
            return true;
        } else {
            // Is the name part a function call?
            if (scanner.scan('(')) {
                String argName = scanner.scanFilterArgument();
                if (argName != null && scanner.scan(')')) {
                    if (name.equals("servicePidOrFilter")) {
                        resultExpr = evaluateServicePidOrFilterExpression(getPropertyObject(argName));
                        return resultExpr != null && initial;
                    }
                    if (name.equals("count")) {
                        value = (evaluateCountExpression(getPropertyObject(argName)));
                        return true;
                    }
                } else
                    return false;
            }

            // If not, it must be a property name.
            String value = getProperty(name);
            if (value == null) {
                return false;
            }
            this.value = Long.parseLong(value);
            return true;
        }
    }

    private static final String SERVICE_PID_UNMATCHED_FILTER = "(" + Constants.SERVICE_PID + "=unbound)";

    /**
     * Evaluates the servicePidOrFilter expression function. If the value is
     * null, an unmatchable filter is returned. If the value is a string, a
     * filter matching "service.pid" to that value is returned. If the value
     * is an array or vector of strings, an "or" filter is returned matching
     * "service.pid" to any of those values. Otherwise (including if the vector
     * contains a non-string), null is returned.
     *
     * @param value the object to evaluate
     * @return a filter or null if the value is invalid for the function
     */
    private String evaluateServicePidOrFilterExpression(Object value) {
        if (value == null) {
            return SERVICE_PID_UNMATCHED_FILTER;
        }
        if (value instanceof String) {
            return FilterUtils.createPropertyFilter(Constants.SERVICE_PID, (String) value);
        }
        if (value instanceof String[]) {
            return evaluateServicePidOrFilterExpression(Arrays.asList((String[]) value));
        }
        if (value instanceof Vector) {
            return evaluateServicePidOrFilterExpression((Vector<?>) value);
        }
        return null;
    }

    private String evaluateServicePidOrFilterExpression(List<?> pids) {
        int size = pids.size();
        if (size > 0) {
            StringBuilder b = new StringBuilder();
            if (size != 1) {
                b.append("(|");
            }

            for (Object pid : pids) {
                if (!(pid instanceof String)) {
                    return null;
                }
                b.append(FilterUtils.createPropertyFilter(Constants.SERVICE_PID, (String) pid));
            }

            if (pids.size() != 1) {
                b.append(')');
            }
            return b.toString();
        }

        return SERVICE_PID_UNMATCHED_FILTER;
    }

    /**
     * Evaluates the count expression function. If the value is null, then 0 is
     * returned. If the value is an array, the length is returned. If the value
     * is a vector, the size is returned. Otherwise, 1 is returned.
     *
     * @param value the value to evaluate
     * @return the count of the value
     */
    private int evaluateCountExpression(Object value) {
        if (value == null) {
            return 0;
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value);
        }
        if (value instanceof Vector<?>) {
            return ((Vector<?>) value).size();
        }
        return 1;
    }

}
