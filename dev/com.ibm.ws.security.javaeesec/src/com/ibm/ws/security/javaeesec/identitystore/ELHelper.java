/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.identitystore;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.el.ELException;
import javax.el.ELProcessor;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStore.ValidationType;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition.LdapSearchScope;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.javaeesec.CDIHelper;

/**
 * Class to help with evaluating EL expressions for identity stores.
 */
public class ELHelper {
    private static final String OBFUSCATED_STRING = "******";
    private static final TraceComponent tc = Tr.register(ELHelper.class);

    private static final ThreadLocal<Map<String, String>> valuesMap = new ThreadLocal<Map<String, String>>() {
        @Override
        protected Map<String, String> initialValue() {
            return new HashMap<String, String>(1);
        }
    };

    /**
     * Evaluate a possible EL expression.
     *
     * @param expression The expression to evaluate.
     * @return The evaluated expression.
     */
    @Trivial
    protected Object evaluateElExpression(String expression) {
        return evaluateElExpression(expression, false);
    }

    /**
     * Evaluate a possible EL expression.
     *
     * @param expression The expression to evaluate.
     * @param mask       Set whether to mask the expression and result. Useful for when passwords might be
     *                       contained in either the expression or the result.
     * @return The evaluated expression.
     */
    @Trivial
    protected Object evaluateElExpression(String expression, boolean mask) {
        final String methodName = "evaluateElExpression";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { (expression == null) ? null : mask ? OBFUSCATED_STRING : expression, mask });
        }

        EvalPrivilegedAction evalPrivilegedAction = new EvalPrivilegedAction(expression, mask);
        Object result = AccessController.doPrivileged(evalPrivilegedAction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, (result == null) ? null : mask ? OBFUSCATED_STRING : result);
        }
        return result;
    }

    /*
     * Class to avoid tracing expression that otherwise is traced when using
     * an anonymous PrivilegedAction.
     */
    class EvalPrivilegedAction implements PrivilegedAction<Object> {

        private final String expression;
        private final boolean mask;

        @Trivial
        public EvalPrivilegedAction(String expression, boolean mask) {
            this.expression = expression;
            this.mask = mask;
        }

        @Trivial
        @Override
        public Object run() {
            ELProcessor elProc = CDIHelper.getELProcessor();
            String[] expressionStrings = checkAndSplitBeforeEvaluation(expression, mask);
            if (!valuesMap.get().isEmpty()) {
                Map<String, String> variables = valuesMap.get();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "EvalPrivilegedAction", "Found threadLocal values to set on the ELProcessor: " + (mask ? OBFUSCATED_STRING : variables));
                }
                for (Map.Entry<String, String> variable : variables.entrySet()) {
                    elProc.setValue(variable.getKey(), variable.getValue());
                }
            }
            if (expressionStrings == null) {
                return elProc.eval(removeBrackets(expression, mask));
            } else {
                return elProc.eval(expressionStrings[0]) + expressionStrings[1];
            }
        }

    }

    /**
     * Return whether the expression is a deferred EL expression.
     *
     * @param expression The expression to evaluate.
     * @return True if the expression is a deferred EL expression.
     */
    @Trivial
    public static boolean isDeferredExpression(String expression) {
        return isDeferredExpression(expression, false);
    }

    /**
     * Return whether the expression is an deferred EL expression.
     *
     * @param expression The expression to evaluate.
     * @param mask       Set whether to mask the expression and result. Useful for when passwords might be
     *                       contained in either the expression or the result.
     * @return True if the expression is a deferred EL expression.
     */
    @Trivial
    static boolean isDeferredExpression(String expression, boolean mask) {
        final String methodName = "isDeferredExpression";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { (expression == null) ? null : mask ? OBFUSCATED_STRING : expression, mask });
        }

        boolean result = false;
        if (expression != null) {
            result = expression.startsWith("#{") && expression.endsWith("}");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    /**
     * Return whether the expression is an immediate EL expression.
     *
     * @param expression The expression to evaluate.
     * @return True if the expression is an immediate EL expression.
     */
    @Trivial
    static boolean isImmediateExpression(String expression) {
        return isImmediateExpression(expression, false);
    }

    /**
     * Return whether the expression is an immediate EL expression.
     *
     * @param expression The expression to evaluate.
     * @param mask       Set whether to mask the expression and result. Useful for when passwords might be
     *                       contained in either the expression or the result.
     * @return True if the expression is an immediate EL expression.
     */
    @Trivial
    static boolean isImmediateExpression(String expression, boolean mask) {
        final String methodName = "isImmediateExpression";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { (expression == null) ? null : mask ? OBFUSCATED_STRING : expression, mask });
        }

        boolean result = false;
        if (expression != null) {
            result = expression.startsWith("${") && expression.endsWith("}");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    /**
     * This method will process a configuration value for an Integer setting in
     * {@link LdapIdentityStoreDefinition} or {@link DatabaseIdentityStoreDefinition}.
     * It will first check to see if there is an EL expression. It there is, it will return
     * the evaluated expression; otherwise, it
     * will return the non-EL value.
     *
     * @param name          The name of the property. Used for error messages.
     * @param expression    The EL expression returned from from the identity store definition.
     * @param value         The non-EL value.
     * @param immediateOnly Return null if the value is a deferred EL expression.
     * @return Either the evaluated EL expression or the non-EL value.
     */
    public Integer processInt(String name, String expression, int value, boolean immediateOnly) {
        Integer result = null;
        boolean immediate = false;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (expression.isEmpty()) {
            /*
             * Direct setting.
             */
            result = value;
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            Object obj = evaluateElExpression(expression);
            if (obj == null) {
                throw new IllegalArgumentException("EL expression '" + expression + "' for '" + name + "' evaluated to null.");
            } else if (obj instanceof Number) {
                result = ((Number) obj).intValue();
                immediate = isImmediateExpression(expression);
            } else {
                throw new IllegalArgumentException("Expected '" + name + "' to evaluate to an integer value.");
            }
        }

        return (immediateOnly && !immediate) ? null : result;
    }

    public Boolean processBoolean(String name, String expression, boolean value, boolean immediateOnly) {
        Boolean result = null;
        boolean immediate = false;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (expression.isEmpty()) {
            /*
             * Direct setting.
             */
            result = value;
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            Object obj = evaluateElExpression(expression);
            if (obj == null) {
                throw new IllegalArgumentException("EL expression '" + expression + "' for '" + name + "' evaluated to null.");
            } else if (obj instanceof Boolean) {
                result = (Boolean) obj;
                immediate = isImmediateExpression(expression);
            } else {
                throw new IllegalArgumentException("Expected '" + name + "' to evaluate to a Boolean value.");
            }
        }

        return (immediateOnly && !immediate) ? null : result;
    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore(Exception.class)
    public <T> T processGeneric(String name, String expression, T value, boolean immediateOnly) {
        T result = null;
        boolean immediate = false;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (expression.isEmpty()) {
            /*
             * Direct setting.
             */
            result = value;
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            Object obj = evaluateElExpression(expression);
            if (obj == null) {
                throw new IllegalArgumentException("EL expression '" + expression + "' for '" + name + "' evaluated to null.");
            } else {
                try {
                    result = (T) obj;
                    immediate = isImmediateExpression(expression);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Expected '" + name + "' to evaluate to a " + value.getClass().getName() + " value.");
                }
            }
        }

        return (immediateOnly && !immediate) ? null : result;
    }

    public String[] processStringArray(String name, String expression, String[] value, boolean immediateOnly) {
        String[] result = null;
        boolean immediate = false;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (expression.isEmpty()) {
            /*
             * Direct setting.
             */
            result = value;
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            Object obj = evaluateElExpression(expression);
            if (obj == null) {
                throw new IllegalArgumentException("EL expression '" + expression + "' for '" + name + "' evaluated to null.");
            } else if (obj instanceof String[]) {
                result = (String[]) obj;
                immediate = isImmediateExpression(expression);
            } else {
                throw new IllegalArgumentException("Expected '" + name + "' to evaluate to a String[] value.");
            }
        }

        return (immediateOnly && !immediate) ? null : result;
    }

    /**
     * This method will process a configuration value for LdapSearchScope setting in
     * {@link LdapIdentityStoreDefinition}. It will first check to see if there is an
     * EL expression. It there is, it will return the evaluated expression; otherwise, it
     * will return the non-EL value.
     *
     * @param name          The name of the property. Used for error messages.
     * @param expression    The EL expression returned from from the identity store definition.
     * @param value         The non-EL value.
     * @param immediateOnly Return null if the value is a deferred EL expression.
     * @return Either the evaluated EL expression or the non-EL value.
     */
    protected LdapSearchScope processLdapSearchScope(String name, String expression, LdapSearchScope value, boolean immediateOnly) {
        LdapSearchScope result;
        boolean immediate = false;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (expression.isEmpty()) {
            /*
             * Direct setting.
             */
            result = value;
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            Object obj = evaluateElExpression(expression);
            if (obj instanceof LdapSearchScope) {
                result = (LdapSearchScope) obj;
                immediate = isImmediateExpression(expression);
            } else if (obj instanceof String) {
                result = LdapSearchScope.valueOf(((String) obj).toUpperCase());
                immediate = isImmediateExpression(expression);
            } else {
                throw new IllegalArgumentException("Expected '" + name + "' to evaluate to an LdapSearchScope type.");
            }
        }

        return (immediateOnly && !immediate) ? null : result;
    }

    /**
     * This method will process a configuration value for any configuration setting in
     * {@link LdapIdentityStoreDefinition} or {@link DatabaseIdentityStoreDefinition} that
     * is a string and whose name is NOT a "*Expression". It will first check to see if it
     * is a EL expression. It it is, it will return the evaluated expression; otherwise, it
     * will return the literal String.
     *
     * @param name          The name of the property. Used for error messages.
     * @param expression    The value returned from from the identity store definition, which can
     *                          either be a literal String or an EL expression.
     * @param immediateOnly Return null if the value is a deferred EL expression.
     * @return The String value.
     */
    @Trivial
    public String processString(String name, String expression, boolean immediateOnly) {
        return processString(name, expression, immediateOnly, false);
    }

    /**
     * This method will process a configuration value for any configuration setting in
     * {@link LdapIdentityStoreDefinition} or {@link DatabaseIdentityStoreDefinition} that
     * is a string and whose name is NOT a "*Expression". It will first check to see if it
     * is a EL expression. It it is, it will return the evaluated expression; otherwise, it
     * will return the literal String.
     *
     * @param name          The name of the property. Used for error messages.
     * @param expression    The value returned from from the identity store definition, which can
     *                          either be a literal String or an EL expression.
     * @param immediateOnly Return null if the value is a deferred EL expression.
     * @param mask          Set whether to mask the expression and result. Useful for when passwords might
     *                          be contained in either the expression or the result.
     * @return The String value.
     */
    @FFDCIgnore(ELException.class)
    @Trivial
    public String processString(String name, String expression, boolean immediateOnly, boolean mask) {

        final String methodName = "processString";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { name, (expression == null) ? null : mask ? OBFUSCATED_STRING : expression, immediateOnly, mask });
        }

        String result;
        boolean immediate = false;

        try {
            Object obj = evaluateElExpression(expression, mask);
            if (obj == null) {
                throw new IllegalArgumentException("EL expression '" + (mask ? OBFUSCATED_STRING : expression) + "' for '" + name + "' evaluated to null.");
            } else if (obj instanceof String) {
                result = (String) obj;
                immediate = isImmediateExpression(expression, mask);
            } else {
                throw new IllegalArgumentException("Expected '" + name + "' to evaluate to a String value.");
            }
        } catch (ELException e) {
            result = expression;
            immediate = true;
        }

        String finalResult = (immediateOnly && !immediate) ? null : result;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, (finalResult == null) ? null : mask ? OBFUSCATED_STRING : finalResult);
        }

        return finalResult;
    }

    /**
     * This method will process a configuration value for an String[] setting in
     * {@link LdapIdentityStoreDefinition} or {@link DatabaseIdentityStoreDefinition}.
     * It will first check to see if there is an EL expression. It there is, it will return
     * the evaluated expression; otherwise, it will return the non-EL value.
     *
     * @param name          The name of the property. Used for error messages.
     * @param expression    The EL expression returned from from the identity store definition.
     * @param immediateOnly Return null if the value is a deferred EL expression.
     * @param mask          Set whether to mask the expression and result. Useful for when passwords might be
     *                          contained in either the expression or the result.
     * @return Either the evaluated EL expression or the non-EL value.
     */
    @FFDCIgnore(ELException.class)
    @Trivial
    public String[] processStringArray(String name, String expression, boolean immediateOnly, boolean mask) {

        final String methodName = "processStringArray";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { name, (expression == null) ? null : mask ? OBFUSCATED_STRING : expression, immediateOnly, mask });
        }

        String[] result;
        boolean immediate = false;

        try {
            Object obj = evaluateElExpression(expression, mask);
            if (obj == null) {
                throw new IllegalArgumentException("EL expression '" + (mask ? OBFUSCATED_STRING : expression) + "' for '" + name + "' evaluated to null.");
            } else if (obj instanceof String[]) {
                result = (String[]) obj;
                immediate = isImmediateExpression(expression, mask);
            } else {
                throw new IllegalArgumentException("Expected '" + name + "' to evaluate to a String value.");
            }
        } catch (ELException e) {
            throw new IllegalArgumentException("Expected '" + name + "' to evaluate to a String[] value.");
        }

        String[] finalResult = (immediateOnly && !immediate) ? null : result;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, (finalResult == null) ? null : mask ? OBFUSCATED_STRING : finalResult);
        }

        return finalResult;
    }

    /**
     * This method will process a configuration value for an String[] setting in
     * {@link LdapIdentityStoreDefinition} or {@link DatabaseIdentityStoreDefinition}.
     * It will first check to see if there is an EL expression. It there is, it will return
     * the evaluated expression; otherwise, it will return the non-EL value.
     *
     * @param name          The name of the property. Used for error messages.
     * @param expression    The EL expression returned from from the identity store definition.
     * @param immediateOnly Return null if the value is a deferred EL expression.
     * @param mask          Set whether to mask the expression and result. Useful for when passwords might be
     *                          contained in either the expression or the result.
     * @return Either the evaluated EL expression or the non-EL value.
     */
    @FFDCIgnore(ELException.class)
    @Trivial
    protected Stream<String> processStringStream(String name, String expression, boolean immediateOnly, boolean mask) {

        final String methodName = "processStringStream";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { name, (expression == null) ? null : mask ? OBFUSCATED_STRING : expression, immediateOnly, mask });
        }

        Stream<String> result;
        boolean immediate = false;

        try {
            Object obj = evaluateElExpression(expression, mask);
            if (obj == null) {
                throw new IllegalArgumentException("EL expression '" + (mask ? OBFUSCATED_STRING : expression) + "' for '" + name + "' evaluated to null.");
            } else if (obj instanceof Stream) {
                result = (Stream<String>) obj;
                immediate = isImmediateExpression(expression, mask);
            } else {
                throw new IllegalArgumentException("Expected '" + name + "' to evaluate to a Stream<String> value.");
            }
        } catch (ELException e) {
            throw new IllegalArgumentException("Expected '" + name + "' to evaluate to a Stream<String> value.");
        }

        Stream<String> finalResult = (immediateOnly && !immediate) ? null : result;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, (finalResult == null) ? null : mask ? OBFUSCATED_STRING : finalResult);
        }

        return finalResult;
    }

    /**
     * Validate and return the {@link ValidationType}s for the {@link IdentityStore} from either
     * the EL expression or the direct useFor setting.
     *
     * @param useForExpression The EL expression returned from from the identity store definition.
     * @param useFor           The non-EL value.
     * @param immediateOnly    Return null if the value is a deferred EL expression.
     *
     * @return The validated useFor types.
     */
    protected Set<ValidationType> processUseFor(String useForExpression, ValidationType[] useFor, boolean immediateOnly) {
        Set<ValidationType> result = null;
        boolean immediate = false;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (useForExpression.isEmpty()) {
            result = EnumSet.copyOf(Arrays.asList(useFor));
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            Object obj = evaluateElExpression(useForExpression);
            if (obj instanceof ValidationType[]) {
                Tr.debug(tc, "processUseFor (validationtype): " + obj);
                ValidationType[] types = (ValidationType[]) obj;
                result = EnumSet.copyOf(Arrays.asList(types));
                immediate = isImmediateExpression(useForExpression);

            } else if (obj instanceof String) {
                Tr.debug(tc, "processUseFor (String): " + (String) obj);
                ValidationType[] types = new ValidationType[2];
                String validation = ((String) obj).toLowerCase();
                if (validation.contains("validate")) {
                    types[0] = ValidationType.VALIDATE;
                }
                if (validation.contains("provide_groups")) {
                    types[1] = ValidationType.PROVIDE_GROUPS;
                }
                result = EnumSet.copyOf(Arrays.asList(types));
                immediate = isImmediateExpression(useForExpression);

            } else {
                Tr.debug(tc, "processUseFor obj was not an instance of string or ValidationType[]");
                throw new IllegalArgumentException("Expected 'useForExpression' to evaluate to an array of ValidationType.");
            }
        }

        if (result == null || result.isEmpty()) {
            Tr.debug(tc, "processUseFor result is empty or null");
            throw new IllegalArgumentException("The identity store must be configured with at least one ValidationType.");
        }
        for (ValidationType v : result) {
            Tr.debug(tc, "processUseFor result: " + v);
        }
        return (immediateOnly && !immediate) ? null : Collections.unmodifiableSet(result);
    }

    /**
     * Remove the brackets from an EL expression.
     *
     * @param expression The expression to remove the brackets from.
     * @return The EL expression without the brackets.
     */
    @Trivial
    static String removeBrackets(String expression) {
        return removeBrackets(expression, false);
    }

    /**
     * Remove the brackets from an EL expression.
     *
     * @param expression The expression to remove the brackets from.
     * @param mask       Set whether to mask the expression and result. Useful for when passwords might
     *                       be contained in either the expression or the result.
     * @return The EL expression without the brackets.
     */
    @Trivial
    static String removeBrackets(String expression, boolean mask) {
        final String methodName = "removeBrackets";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { (expression == null) ? null : mask ? OBFUSCATED_STRING : expression, mask });
        }

        expression = expression.trim();
        if ((expression.startsWith("${") || expression.startsWith("#{")) && expression.endsWith("}")) {
            expression = expression.substring(2, expression.length() - 1);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, (expression == null) ? null : mask ? OBFUSCATED_STRING : expression);
        }
        return expression;
    }

    /**
     * Check if the closing bracket is mid expression and return a split string if it is. If the expression is
     * a single expression and can be evaluated as a whole, return null.
     *
     * @param expression
     * @param mask
     * @return A 2 length String array if the expression needed to be split, otherwise null.
     */
    @Trivial
    static String[] checkAndSplitBeforeEvaluation(String expression, boolean mask) {
        final String methodName = "checkAndSplitBeforeEvaluation";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { (expression == null) ? null : mask ? OBFUSCATED_STRING : expression, mask });
        }
        String[] expressionStrings = null;
        if ((expression.startsWith("${") || expression.startsWith("#{")) && !expression.endsWith("}")) {
            String prep = expression.substring(2, expression.length());
            expressionStrings = prep.split("}", 2);

            if (expressionStrings.length != 2) {
                expressionStrings = null;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName,
                             "Unable to split expression as expected, revert to evaluating whole expression string. Expression: " + (mask ? OBFUSCATED_STRING : expression)
                                             + ", attempted split: " + (mask ? OBFUSCATED_STRING : Arrays.toString(expressionStrings)));
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, (expressionStrings == null) ? null : mask ? OBFUSCATED_STRING : Arrays.toString(expressionStrings));
        }
        return expressionStrings;
    }

    /**
     * Add an expression and value that will be used when doing an EL evaluation. Added as
     * a Threadlocal and will be picked up during EvalPrivilegedAction.
     *
     * @param expression
     * @param value
     */
    @Trivial
    public void addValue(String expression, String value, boolean mask) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addValue", expression + ":" +
                                     (mask ? OBFUSCATED_STRING : value));
        }
        valuesMap.get().put(expression, value);
    }

    /**
     * Removes an expression and value from the ThrealLocal map
     *
     * @param expression
     */
    public void removeValue(String expression) {
        valuesMap.get().remove(expression);
    }
}