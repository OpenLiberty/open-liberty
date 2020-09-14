/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.config.xml.internal.EvaluationContext.NestedInfo;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public class VariableEvaluator {

    private final ConfigVariableRegistry variableRegistry;
    private final StringUtils stringUtils = new StringUtils();
    private final ConfigEvaluator configEvaluator;

    /**
     * @param variableRegistry
     * @param configEvaluator
     */
    public VariableEvaluator(ConfigVariableRegistry variableRegistry, ConfigEvaluator configEvaluator) {
        this.variableRegistry = variableRegistry;
        this.configEvaluator = configEvaluator;
    }

    @Sensitive
    private String lookupVariableFromRegistry(String variableName) {
        return variableRegistry == null ? null : variableRegistry.lookupVariable(variableName);
    }

    @Sensitive
    String resolveVariables(@Sensitive String str, EvaluationContext context, boolean ignoreWarnings) throws ConfigEvaluatorException {

        // Look for normal variables of the form $(variableName)
        Matcher matcher = XMLConfigConstants.VAR_PATTERN.matcher(str);
        while (matcher.find()) {
            String var = matcher.group(1);

            // Try to resolve the variable normally ( for ${var-Name} resolve var-Name }
            String rep = getProperty(var, context, ignoreWarnings, true);

            // Resolve the original var name as an expression
            if (rep == null) {
                rep = tryEvaluateExpression(var, context, ignoreWarnings);
            }

            if (rep != null) {
                // Recursively resolve variables so that we can find cycles.

                context.push(rep);
                rep = resolveVariables(rep, context, ignoreWarnings);
                context.pop();

                str = str.replace(matcher.group(0), rep);
                matcher.reset(str);
            }
        }
        return str;
    }

    /**
     * Returns the value of the variable as a string, or null if the property
     * does not exist.
     *
     * @param variable the variable name
     */
    @Sensitive
    private String getProperty(String variable, EvaluationContext context, boolean ignoreWarnings, boolean useEnvironment) throws ConfigEvaluatorException {
        return stringUtils.convertToString(getPropertyObject(variable, context, ignoreWarnings, useEnvironment));
    }

    /**
     * Returns the raw value object of the variable, or null if the property
     * does not exist.
     *
     * @param variable the variable name
     */
    @Sensitive
    private Object getPropertyObject(String variable, EvaluationContext context, boolean ignoreWarnings, boolean useEnvironment) throws ConfigEvaluatorException {
        Object realValue = null;

        // checked if we already looked up the value
        if (context.containsValue(variable)) {
            // get it from cache
            realValue = context.getValue(variable);
        } else if (XMLConfigConstants.CFG_SERVICE_PID.equals(variable)) {
            try {
                realValue = configEvaluator.getPid(context.getConfigElement().getConfigID());
                context.putValue(XMLConfigConstants.CFG_SERVICE_PID, realValue);
            } catch (ConfigNotFoundException ex) {
                throw new ConfigEvaluatorException("Could not obtain PID for configID", ex);
            }
        } else {
            // evaluate the variable

            context.push(variable);

            realValue = lookupVariableFromRegistry(variable);

            if (realValue == null) {
                // Try checking the properties. This will pick up already evaluated attributes, including flattened config
                realValue = context.getProperties().get(variable);

                if (realValue == null) {
                    // check if this is an metatype attribute
                    ExtendedAttributeDefinition attributeDef = context.getAttributeDefinition(variable);
                    if (attributeDef != null) {

                        String currentAttribute = context.getAttributeName();
                        // Get the nested info here, then set it later so that evaluating the
                        // metatype attribute here doesn't affect it
                        Set<NestedInfo> nestedInfo = context.getNestedInfo();
                        String flatPrefix = "";
                        try {
                            realValue = configEvaluator.evaluateMetaTypeAttribute(variable, context, attributeDef, flatPrefix, ignoreWarnings);
                        } finally {
                            context.setAttributeName(currentAttribute);
                            context.setNestedInfo(nestedInfo);
                        }

                    } else {
                        // check if this is just an attribute
                        ConfigElement configElement = context.getConfigElement();
                        Object rawValue = configElement.getAttribute(variable);
                        if (rawValue != null) {

                            String currentAttribute = context.getAttributeName();
                            Set<NestedInfo> nestedInfo = context.getNestedInfo();
                            try {
                                realValue = configEvaluator.evaluateSimpleAttribute(variable, rawValue, context, "", ignoreWarnings);
                            } finally {
                                context.setAttributeName(currentAttribute);
                                context.setNestedInfo(nestedInfo);
                            }

                        }
                    }
                }

                if (realValue == null) {
                    // Check if this variable points to an unevaluated flattened config
                    Map<String, ExtendedAttributeDefinition> attributeMap = context.getAttributeMap();
                    if (attributeMap != null) {
                        for (Map.Entry<String, ExtendedAttributeDefinition> entry : attributeMap.entrySet()) {
                            if (!context.isProcessed(entry.getKey()) && entry.getValue().isFlat()) {
                                try {
                                    configEvaluator.evaluateMetaTypeAttribute(entry.getKey(), context, entry.getValue(), "", true);
                                } catch (ConfigEvaluatorException ex) {
                                    // Ignore -- errors should be generated during main line processing
                                }

                            }

                        }
                        // Try again now that everything should be evaluated
                        realValue = context.getProperties().get(variable);
                    }
                }

                // Try to get an environment variable ( env.MYVAR )
                if (realValue == null && useEnvironment && variableRegistry != null) {
                    realValue = variableRegistry.lookupVariableFromAdditionalSources(variable);
                    if (realValue != null)
                        context.addDefinedVariable(variable, realValue);
                }

                // Try to get a default value (<variable name="var" default="defaultValue"/>)
                if (realValue == null) {
                    realValue = lookupDefaultVariable(variable);
                    if (realValue != null)
                        context.addDefinedVariable(variable, realValue);
                }

                if (realValue == null && useEnvironment) {
                    // If the value is null, add it to the context so that we don't try to evaluate it again.
                    // If the value is not null here, it is either:
                    // 1) An environment variable or mangled variable, in which case we added it above, or
                    // 2) this is a variable that points to a configuration attribute,
                    // so we don't want to add it to the variable registry.
                    context.addDefinedVariable(variable, null);
                }
            } else {
                // Only add the variable to the context if this is a user defined variable
                context.addDefinedVariable(variable, realValue);
            }

            context.pop();

            context.putValue(variable, realValue);
        }

        return realValue;
    }

    /**
     * Attempt to evaluate a variable expression.
     *
     * @param expr    the expression string (for example, "x+0")
     * @param context the context for evaluation
     * @return the result, or null if evaluation fails
     */
    @FFDCIgnore({ NumberFormatException.class, ArithmeticException.class, ConfigEvaluatorException.class })
    private String tryEvaluateExpression(String expr, final EvaluationContext context, final boolean ignoreWarnings) {
        try {
            return new ConfigExpressionEvaluator() {

                @Override
                String getProperty(String argName) throws ConfigEvaluatorException {
                    return VariableEvaluator.this.getProperty(argName, context, ignoreWarnings, false);
                }

                @Override
                Object getPropertyObject(String argName) throws ConfigEvaluatorException {
                    return VariableEvaluator.this.getPropertyObject(argName, context, ignoreWarnings, false);
                }
            }.evaluateExpression(expr);

        } catch (NumberFormatException e) {
            // ${0+string}, or a number that exceeds MAX_LONG.
        } catch (ArithmeticException e) {
            // ${x/0}
        } catch (ConfigEvaluatorException e) {
            // If getPropertyObject fails
        }
        return null;
    }

    /**
     * Replaces list variable expressions in raw string values
     */
    @SuppressWarnings("unchecked")
    @Sensitive
    Object processVariableLists(Object rawValue, ExtendedAttributeDefinition attributeDef,
                                EvaluationContext context, boolean ignoreWarnings) throws ConfigEvaluatorException {
        if (attributeDef != null && !attributeDef.resolveVariables())
            return rawValue;

        if (rawValue instanceof List) {
            List<Object> returnList = new ArrayList<Object>();
            List<Object> values = (List<Object>) rawValue;
            for (int i = 0, size = values.size(); i < size; ++i) {
                Object o = values.get(i);
                Object processed = processVariableLists(o, attributeDef, context, ignoreWarnings);
                if (processed instanceof List)
                    returnList.addAll((List<Object>) processed);
                else
                    returnList.add(processed);
            }
            return returnList;
        } else if (rawValue instanceof String) {
            // Look for functions of the form ${list(variableName)} first
            Matcher matcher = XMLConfigConstants.VAR_LIST_PATTERN.matcher((String) rawValue);
            if (matcher.find()) {
                String var = matcher.group(1);
                String rep = getProperty(var, context, ignoreWarnings, true);
                return rep == null ? rawValue : MetaTypeHelper.parseValue(rep);
            } else {
                return rawValue;
            }
        } else {
            return rawValue;
        }

    }

    Object lookupVariableExtension(EvaluationContext context, ExtendedAttributeDefinition attrDef) throws ConfigEvaluatorException {
        if (variableRegistry == null)
            return null;

        String variableName = attrDef.getVariable();
        if (variableName != null) {
            // Check for deferred resolution
            if (attrDef.resolveVariables() == false)
                return "${" + variableName + "}";

            boolean isList = false;
            if (variableName.startsWith("list(") && variableName.endsWith(")")) {
                variableName = variableName.substring(5, variableName.length() - 1);
                isList = true;
            }
            Object variableValue = variableRegistry.lookupVariable(variableName);
            if (variableValue == null && !variableName.equals(attrDef.getID()) && !isList) {
                // Try a metatype attribute
                ExtendedAttributeDefinition targetAttrDef = context.getAttributeDefinition(variableName);
                if (targetAttrDef != null) {
                    variableValue = configEvaluator.evaluateMetaTypeAttribute(variableName, context, targetAttrDef, "", true);
                }
            }

            context.addDefinedVariable(variableName, variableValue);
            if (variableValue == null)
                return null;

            return isList ? MetaTypeHelper.parseValue((String) variableValue) : variableValue;
        }
        return null;
    }

    @Sensitive
    private Object lookupDefaultVariable(String variableName) {
        if (variableRegistry == null)
            return null;

        return variableRegistry.lookupVariableDefaultValue(variableName);
    }

}
