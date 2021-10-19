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

import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.ManagedScheduledExecutor;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;

import jakarta.enterprise.concurrent.ManagedScheduledExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;

/**
 * Injection binding for ManagedScheduledExecutorDefinition annotation
 * and managed-scheduled-executor deployment descriptor element.
 */
public class ManagedScheduledExecutorDefinitionBinding extends InjectionBinding<ManagedScheduledExecutorDefinition> {
    private static final String KEY_CONTEXT = "context";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_HUNG_TASK_THRESHOLD = "hungTaskThreshold";
    private static final String KEY_MAX_ASYNC = "maxAsync";

    private String contextServiceJndiName;
    private boolean XMLContextServiceRef;

    private String description;
    private boolean XMLDescription;

    private Long hungTaskThreshold;
    private boolean XMLHungTaskThreshold;

    private Integer maxAsync;
    private boolean XMLMaxAsync;

    private Map<String, String> properties;
    private final Set<String> XMLProperties = new HashSet<String>();

    public ManagedScheduledExecutorDefinitionBinding(String jndiName, ComponentNameSpaceConfiguration nameSpaceConfig) {
        super(null, nameSpaceConfig);
        setJndiName(jndiName);
    }

    @Override
    public Class<?> getAnnotationType() {
        return ManagedScheduledExecutorDefinition.class;
    }

    @Override
    protected JNDIEnvironmentRefType getJNDIEnvironmentRefType() {
        return JNDIEnvironmentRefType.ManagedScheduledExecutor;
    }

    @Override
    public void merge(ManagedScheduledExecutorDefinition annotation, Class<?> instanceClass, Member member) throws InjectionException {
        if (member != null) {
            // ManagedScheduledExecutorDefinition is a class-level annotation only.
            throw new IllegalArgumentException(member.toString());
        }

        contextServiceJndiName = mergeAnnotationValue(contextServiceJndiName, XMLContextServiceRef, annotation.context().name(), KEY_CONTEXT, "java:comp/DefaultContextService");
        description = mergeAnnotationValue(description, XMLDescription, "", KEY_DESCRIPTION, ""); // ManagedScheduledExecutorDefinition has no description attribute
        hungTaskThreshold = mergeAnnotationValue(hungTaskThreshold, XMLHungTaskThreshold, annotation.hungTaskThreshold(), KEY_HUNG_TASK_THRESHOLD, -1L);
        maxAsync = mergeAnnotationValue(maxAsync, XMLMaxAsync, annotation.maxAsync(), KEY_MAX_ASYNC, -1);
        properties = mergeAnnotationProperties(properties, XMLProperties, new String[] {}); // ManagedScheduledExecutorDefinition has no properties attribute
    }

    void mergeXML(ManagedScheduledExecutor mxd) throws InjectionConfigurationException {
        List<Description> descriptionList = mxd.getDescriptions();

        String contextServiceRefValue = mxd.getContextServiceRef();
        if (contextServiceRefValue != null) {
            contextServiceJndiName = mergeXMLValue(contextServiceJndiName, contextServiceRefValue, "context-service-ref", KEY_CONTEXT, null);
            XMLContextServiceRef = true;
        }

        if (description != null) {
            description = mergeXMLValue(description, descriptionList.toString(), "description", KEY_DESCRIPTION, null);
            XMLDescription = true;
        }

        if (mxd.isSetHungTaskThreshold()) {
            hungTaskThreshold = mergeXMLValue(hungTaskThreshold, mxd.getHungTaskThreshold(), "hung-task-threshold", KEY_HUNG_TASK_THRESHOLD, null);
            XMLHungTaskThreshold = true;
        }

        if (mxd.isSetMaxAsync()) {
            maxAsync = mergeXMLValue(maxAsync, mxd.getMaxAsync(), "max-async", KEY_MAX_ASYNC, null);
            XMLMaxAsync = true;
        }

        List<Property> mxdProps = mxd.getProperties();
        properties = mergeXMLProperties(properties, XMLProperties, mxdProps);
    }

    @Override
    public void mergeSaved(InjectionBinding<ManagedScheduledExecutorDefinition> injectionBinding) throws InjectionException {
        ManagedScheduledExecutorDefinitionBinding managedScheduledExecutorBinding = (ManagedScheduledExecutorDefinitionBinding) injectionBinding;

        mergeSavedValue(contextServiceJndiName, managedScheduledExecutorBinding.contextServiceJndiName, "context-service-ref");
        mergeSavedValue(description, managedScheduledExecutorBinding.description, "description");
        mergeSavedValue(hungTaskThreshold, managedScheduledExecutorBinding.hungTaskThreshold, "hung-task-threshold");
        mergeSavedValue(maxAsync, managedScheduledExecutorBinding.maxAsync, "max-async");
        mergeSavedValue(properties, managedScheduledExecutorBinding.properties, "properties");
    }

    void resolve() throws InjectionException {
        Map<String, Object> props = new HashMap<String, Object>();

        if (properties != null) {
            props.putAll(properties);
        }

        // Insert all remaining attributes.
        addOrRemoveProperty(props, KEY_CONTEXT, contextServiceJndiName);
        addOrRemoveProperty(props, KEY_DESCRIPTION, description);
        addOrRemoveProperty(props, KEY_HUNG_TASK_THRESHOLD, hungTaskThreshold);
        addOrRemoveProperty(props, KEY_MAX_ASYNC, maxAsync);

        setObjects(null, createDefinitionReference(null, ManagedScheduledExecutorService.class.getName(), props));
    }
}
