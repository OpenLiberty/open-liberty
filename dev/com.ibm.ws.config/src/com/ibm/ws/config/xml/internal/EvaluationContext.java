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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.osgi.service.metatype.AttributeDefinition;

import com.ibm.websphere.config.ConfigEvaluatorException;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.internal.ConfigEvaluator.AttributeValueCopy;
import com.ibm.ws.config.xml.internal.ConfigEvaluator.EvaluationResult;
import com.ibm.ws.config.xml.internal.ConfigEvaluator.UnresolvedPidType;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;

class EvaluationContext {

    static final String EMPTY_STRING = "";

    private final EvaluationResult result;
    private final LinkedList<String> lookupStack = new LinkedList<String>();
    private final Map<String, Object> cache = new HashMap<String, Object>();
    private final Set<String> processed = new HashSet<String>();
    private Map<String, Object> variables;
    private Map<String, ExtendedAttributeDefinition> attributeMap;

    // associated with a particular attribute
    private Set<NestedInfo> nested = new HashSet<NestedInfo>();
    private final List<AttributeValueCopy> attributeValueCopies = new ArrayList<AttributeValueCopy>();
    private String attribute;

    private final VariableEvaluator variableEvaluator;

    public EvaluationContext(RegistryEntry registryEntry, VariableEvaluator ve) {
        this.result = new EvaluationResult(registryEntry);
        this.variableEvaluator = ve;
    }

    static class NestedInfo {

        ConfigElement configElement;
        RegistryEntry registryEntry;
        String pid;

    }

    public void evaluateCopiedAttributes() {

        Dictionary<String, Object> properties = getProperties();
        for (AttributeValueCopy copy : attributeValueCopies) {
            Object value = properties.get(copy.getCopiedAttribute());
            if (value != null) {
                setProperty(copy.getAttributeName(), value);
            }
        }

        attributeValueCopies.clear();

    }

    /**
     * @param copy
     */
    public void addAttributeValueCopy(AttributeValueCopy copy) {
        attributeValueCopies.add(copy);
    }

    /**
     * @return
     *
     */
    public Map<String, ExtendedAttributeDefinition> getAttributeMap() {
        return attributeMap;
    }

    /**
     * @param attributeDef
     * @return
     */
    public boolean hasUnresolvedAttribute(AttributeDefinition attributeDef) {
        return result.hasUnresolvedReference(attributeDef);
    }

    /**
     * @param b
     */
    public void setValid(boolean b) {
        result.setValid(b);

    }

    /**
     * @param referencePid
     * @param value
     * @param string
     */
    protected void addUnresolvedReference(UnresolvedPidType ref) {
        result.addUnresolvedReference(ref);
    }

    public EvaluationResult getEvaluationResult() {
        return result;
    }

    protected void setConfigElement(ConfigElement configElement) {
        result.setConfigElement(configElement);
    }

    public ConfigElement getConfigElement() {
        return result.getConfigElement();
    }

    protected void setProperties(Dictionary<String, Object> properties) {
        result.setProperties(properties);
    }

    protected void setProperty(String key, Object value) {
        result.getProperties().put(key, value);
    }

    public Dictionary<String, Object> getProperties() {
        return result.getProperties();
    }

    protected void addProcessed(String attributeName) {
        processed.add(attributeName.toUpperCase(Locale.ROOT));
    }

    public boolean isProcessed(String attributeName) {
        return processed.contains(attributeName.toUpperCase(Locale.ROOT));
    }

    /*
     * Sets the definition of the currently processed attribute.
     */
    protected void setAttributeDefinitionMap(Map<String, ExtendedAttributeDefinition> attributeMap) {
        this.attributeMap = attributeMap;
    }

    public ExtendedAttributeDefinition getAttributeDefinition(String name) {
        return (attributeMap == null) ? null : attributeMap.get(name);
    }

    public void push(@Sensitive String variableName) throws ConfigEvaluatorException {
        if (lookupStack.contains(variableName)) {
            throw new ConfigEvaluatorException("Variable evaluation loop detected: " + lookupStack.subList(lookupStack.indexOf(variableName), lookupStack.size()));
        }
        lookupStack.add(variableName);
    }

    public void pop() {
        lookupStack.removeLast();
    }

    @Trivial
    public void putValue(String variableName, @Sensitive Object value) {
        cache.put(variableName, value);
    }

    @Sensitive
    public Object getValue(String variableName) {
        return cache.get(variableName);
    }

    public boolean containsValue(String variableName) {
        return cache.containsKey(variableName);
    }

    protected void addDefinedVariable(String variableName, @Sensitive Object variableValue) {
        if (variables == null) {
            variables = new HashMap<String, Object>();
            result.setVariables(variables);
        }
        variables.put(variableName, variableValue);
    }

    // below methods are used during evaluation of particular attribute
    // should move to AttributeContext?

    /*
     * Sets the name of the currently processed attribute name.
     */
    protected void setAttributeName(String attribute) {
        this.attribute = attribute;
    }

    public String getAttributeName() {
        return attribute;
    }

    protected boolean addNestedInfo(NestedInfo nestedInfo) throws ConfigEvaluatorException {
        for (NestedInfo info : nested) {
            if (info.configElement.getConfigID().equals(nestedInfo.configElement.getConfigID())) {

                info.configElement.override(nestedInfo.configElement);

                return false;
            }
        }
        nested.add(nestedInfo);
        return true;
    }

    public Set<NestedInfo> getNestedInfo() {
        return nested;
    }

    protected void setNestedInfo(Set<NestedInfo> nested) {
        this.nested = nested;
    }

    /**
     * Tries to resolve variables.
     *
     * @throws ConfigEvaluatorException
     */
    @Trivial
    public String resolveString(String value, boolean ignoreWarnings) throws ConfigEvaluatorException {
        value = variableEvaluator.resolveVariables(value, this, ignoreWarnings);
        return value;
    }
}