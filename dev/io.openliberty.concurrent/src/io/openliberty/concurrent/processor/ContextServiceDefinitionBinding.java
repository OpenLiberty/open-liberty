/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.concurrent.processor;

import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.ws.javaee.dd.common.ContextService;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;

import jakarta.enterprise.concurrent.ContextServiceDefinition;

/**
 * Injection binding for ContextServiceDefinition annotation
 * and context-service deployment descriptor element.
 */
public class ContextServiceDefinitionBinding extends InjectionBinding<ContextServiceDefinition> {
    private static final String KEY_CLEARED = "cleared";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_PROPAGATED = "propagated";
    private static final String KEY_UNCHANGED = "unchanged";

    private String[] cleared;
    private boolean XMLcleared;

    private String description;
    private boolean XMLDescription;

    private String[] propagated;
    private boolean XMLpropagated;

    private Map<String, String> properties;
    private final Set<String> XMLProperties = new HashSet<String>();

    private String[] unchanged;
    private boolean XMLunchanged;

    public ContextServiceDefinitionBinding(String jndiName, ComponentNameSpaceConfiguration nameSpaceConfig) {
        super(null, nameSpaceConfig);
        setJndiName(jndiName);
    }

    @Override
    public Class<?> getAnnotationType() {
        return ContextServiceDefinition.class;
    }

    @Override
    protected JNDIEnvironmentRefType getJNDIEnvironmentRefType() {
        return JNDIEnvironmentRefType.ContextService;
    }

    @Override
    public void merge(ContextServiceDefinition annotation, Class<?> instanceClass, Member member) throws InjectionException {
        if (member != null) {
            // ContextServiceDefinition is a class-level annotation only.
            throw new IllegalArgumentException(member.toString());
        }

        cleared = mergeAnnotationValue(cleared, XMLcleared, annotation.cleared(), KEY_CLEARED, new String[] { ContextServiceDefinition.TRANSACTION });
        description = mergeAnnotationValue(description, XMLDescription, "", KEY_DESCRIPTION, ""); // ContextServiceDefinition has no description attribute
        propagated = mergeAnnotationValue(propagated, XMLpropagated, annotation.propagated(), KEY_PROPAGATED, new String[] { ContextServiceDefinition.ALL_REMAINING });
        properties = mergeAnnotationProperties(properties, XMLProperties, new String[] {}); // ContextServiceDefinition has no properties attribute
        unchanged = mergeAnnotationValue(unchanged, XMLunchanged, annotation.unchanged(), KEY_UNCHANGED, new String[0]);
    }

    void mergeXML(ContextService csd) throws InjectionConfigurationException {
        List<Description> descriptionList = csd.getDescriptions();

        String[] clearedValues = csd.getCleared();
        if (clearedValues != null) {
            cleared = mergeXMLValue(cleared, clearedValues, "cleared", KEY_CLEARED, null);
            XMLcleared = true;
        }

        if (description != null) {
            description = mergeXMLValue(description, descriptionList.toString(), "description", KEY_DESCRIPTION, null);
            XMLDescription = true;
        }

        String[] propagatedValues = csd.getPropagated();
        if (propagatedValues != null) {
            propagated = mergeXMLValue(propagated, propagatedValues, "propagated", KEY_PROPAGATED, null);
            XMLpropagated = true;
        }

        List<Property> csdProps = csd.getProperties();
        properties = mergeXMLProperties(properties, XMLProperties, csdProps);

        String[] unchangedValues = csd.getUnchanged();
        if (unchangedValues != null) {
            unchanged = mergeXMLValue(unchanged, unchangedValues, "unchanged", KEY_UNCHANGED, null);
            XMLunchanged = true;
        }
    }

    @Override
    public void mergeSaved(InjectionBinding<ContextServiceDefinition> injectionBinding) throws InjectionException {
        ContextServiceDefinitionBinding contextServiceBinding = (ContextServiceDefinitionBinding) injectionBinding;

        mergeSavedValue(cleared, contextServiceBinding.cleared, "cleared");
        mergeSavedValue(description, contextServiceBinding.description, "description");
        mergeSavedValue(propagated, contextServiceBinding.propagated, "propagated");
        mergeSavedValue(properties, contextServiceBinding.properties, "properties");
        mergeSavedValue(unchanged, contextServiceBinding.unchanged, "unchanged");
    }

    void resolve() throws InjectionException {
        Map<String, Object> props = new HashMap<String, Object>();

        if (properties != null) {
            props.putAll(properties);
        }

        // Insert all remaining attributes.
        addOrRemoveProperty(props, KEY_CLEARED, cleared);
        addOrRemoveProperty(props, KEY_DESCRIPTION, description);
        addOrRemoveProperty(props, KEY_PROPAGATED, propagated);
        addOrRemoveProperty(props, KEY_UNCHANGED, unchanged);

        setObjects(null, createDefinitionReference(null, jakarta.enterprise.concurrent.ContextService.class.getName(), props));
    }
}
