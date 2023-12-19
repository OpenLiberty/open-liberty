/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.processor;

import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.ManagedExecutor;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;

import jakarta.enterprise.concurrent.ManagedExecutorDefinition;
import jakarta.enterprise.concurrent.ManagedExecutorService;

/**
 * Injection binding for ManagedExecutorDefinition annotation
 * and managed-executor deployment descriptor element.
 */
public class ManagedExecutorDefinitionBinding extends InjectionBinding<ManagedExecutorDefinition> {
    private static final TraceComponent tc = Tr.register(ManagedExecutorDefinitionBinding.class);

    private static final String KEY_CONTEXT = "context";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_HUNG_TASK_THRESHOLD = "hungTaskThreshold";
    private static final String KEY_MAX_ASYNC = "maxAsync";
    private static final String KEY_VIRTUAL = "virtual";
    private static final String KEY_QUALIFIERS = "qualifiers";

    private static final boolean DEFAULT_VIRTUAL = false;
    private static final Class<?>[] DEFAULT_QUALIFIERS = new Class<?>[] {};

    private String contextServiceJndiName;
    private boolean XMLContextServiceRef;

    private String description;
    private boolean XMLDescription;

    private Long hungTaskThreshold;
    private boolean XMLHungTaskThreshold;

    private Integer maxAsync;
    private boolean XMLMaxAsync;

    private boolean virtual;
    private boolean XMLvirtual;

    private Class<?>[] qualifiers;
    private boolean XMLqualifers;

    private Map<String, String> properties;
    private final Set<String> XMLProperties = new HashSet<String>();

    public ManagedExecutorDefinitionBinding(String jndiName, ComponentNameSpaceConfiguration nameSpaceConfig) {
        super(null, nameSpaceConfig);
        setJndiName(jndiName);
    }

    @Override
    public Class<?> getAnnotationType() {
        return ManagedExecutorDefinition.class;
    }

    @Override
    protected JNDIEnvironmentRefType getJNDIEnvironmentRefType() {
        return JNDIEnvironmentRefType.ManagedExecutor;
    }

    @Override
    public void merge(ManagedExecutorDefinition annotation, Class<?> instanceClass, Member member) throws InjectionException {
        final boolean trace = TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (trace)
            Tr.entry(this, tc, "merge", toString(annotation), instanceClass, member,
                     (XMLContextServiceRef ? "(xml)" : "     ") + "contextServiceRef: " + contextServiceJndiName + " << " + annotation.context(),
                     (XMLHungTaskThreshold ? "(xml)" : "     ") + "hungTaskThreshold: " + hungTaskThreshold + " << " + annotation.hungTaskThreshold(),
                     (XMLMaxAsync ? "         (xml)" : "              ") + "maxAsync: " + maxAsync + " << " + annotation.maxAsync(),
                     (XMLvirtual ? "          (xml)" : "               ") + "virtual: " + virtual + " << " + annotation.virtual(),
                     (XMLqualifers ? "        (xml)" : "            ") + "qualifiers: " + toString(qualifiers) + " << " + toString(annotation.qualifiers()));

        if (member != null) {
            // ManagedExecutorDefinition is a class-level annotation only.
            throw new IllegalArgumentException(member.toString());
        }

        contextServiceJndiName = mergeAnnotationValue(contextServiceJndiName, XMLContextServiceRef, annotation.context(), KEY_CONTEXT, "java:comp/DefaultContextService");
        description = mergeAnnotationValue(description, XMLDescription, "", KEY_DESCRIPTION, ""); // ManagedExecutorDefinition has no description attribute
        hungTaskThreshold = mergeAnnotationValue(hungTaskThreshold, XMLHungTaskThreshold, annotation.hungTaskThreshold(), KEY_HUNG_TASK_THRESHOLD, -1L);
        maxAsync = mergeAnnotationValue(maxAsync, XMLMaxAsync, annotation.maxAsync(), KEY_MAX_ASYNC, -1);
        virtual = mergeAnnotationBoolean(virtual, XMLvirtual, annotation.virtual(), KEY_VIRTUAL, DEFAULT_VIRTUAL);
        qualifiers = mergeAnnotationValue(qualifiers, XMLqualifers, annotation.qualifiers(), KEY_QUALIFIERS, DEFAULT_QUALIFIERS);
        properties = mergeAnnotationProperties(properties, XMLProperties, new String[] {}); // ManagedExecutorDefinition has no properties attribute

        if (trace)
            Tr.exit(this, tc, "merge", new String[] {
                                                      (XMLContextServiceRef ? "(xml)" : "     ") + "contextServiceRef= " + contextServiceJndiName,
                                                      (XMLHungTaskThreshold ? "(xml)" : "     ") + "hungTaskThreshold= " + hungTaskThreshold,
                                                      (XMLMaxAsync ? "         (xml)" : "              ") + "maxAsync= " + maxAsync,
                                                      (XMLvirtual ? "          (xml)" : "               ") + "virtual= " + virtual,
                                                      (XMLqualifers ? "        (xml)" : "            ") + "qualifiers= " + toString(qualifiers)
            });
    }

    void mergeXML(ManagedExecutor mxd) throws InjectionConfigurationException {
        final boolean trace = TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (trace)
            Tr.entry(this, tc, "mergeXML", mxd, mxd.getName(),
                     (XMLContextServiceRef ? "(xml)" : "     ") + "contextServiceRef: " + contextServiceJndiName + " << " + mxd.getContextServiceRef(),
                     (XMLHungTaskThreshold ? "(xml)" : "     ") + "hungTaskThreshold: " + hungTaskThreshold + " << " + mxd.getHungTaskThreshold(),
                     (XMLMaxAsync ? "         (xml)" : "              ") + "maxAsync: " + maxAsync + " << " + mxd.getMaxAsync(),
                     (XMLvirtual ? "          (xml)" : "               ") + "virtual: " + virtual + " << " + mxd.isVirtual(),
                     (XMLqualifers ? "        (xml)" : "            ") + "qualifiers: " + toString(qualifiers) + " << " + toString(mxd.getQualifiers()));

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

        if (mxd.isSetVirtual()) {
            virtual = mergeXMLValue(virtual, mxd.isVirtual(), "virtual", KEY_VIRTUAL, null);
            XMLvirtual = true;
        }

        Class<?>[] qualifierValues = toQualifierClassArray(mxd.getQualifiers());
        if (qualifierValues == null || qualifierValues.length == 0) {
            if (qualifiers == null)
                qualifiers = DEFAULT_QUALIFIERS;
        } else {
            qualifiers = mergeXMLValue(qualifiers, qualifierValues, "qualifier", KEY_QUALIFIERS, null);
            XMLqualifers = true;
        }

        List<Property> mxdProps = mxd.getProperties();
        properties = mergeXMLProperties(properties, XMLProperties, mxdProps);

        if (trace)
            Tr.exit(this, tc, "mergeXML", new String[] {
                                                         (XMLContextServiceRef ? "(xml)" : "     ") + "contextServiceRef= " + contextServiceJndiName,
                                                         (XMLHungTaskThreshold ? "(xml)" : "     ") + "hungTaskThreshold= " + hungTaskThreshold,
                                                         (XMLMaxAsync ? "         (xml)" : "              ") + "maxAsync= " + maxAsync,
                                                         (XMLvirtual ? "          (xml)" : "               ") + "virtual= " + virtual,
                                                         (XMLqualifers ? "        (xml)" : "            ") + "qualifiers= " + toString(qualifiers)
            });
    }

    @Override
    public void mergeSaved(InjectionBinding<ManagedExecutorDefinition> injectionBinding) throws InjectionException {
        ManagedExecutorDefinitionBinding managedExecutorBinding = (ManagedExecutorDefinitionBinding) injectionBinding;

        mergeSavedValue(contextServiceJndiName, managedExecutorBinding.contextServiceJndiName, "context-service-ref");
        mergeSavedValue(description, managedExecutorBinding.description, "description");
        mergeSavedValue(hungTaskThreshold, managedExecutorBinding.hungTaskThreshold, "hung-task-threshold");
        mergeSavedValue(maxAsync, managedExecutorBinding.maxAsync, "max-async");
        mergeSavedValue(virtual, managedExecutorBinding.virtual, "virtual");
        mergeSavedValue(qualifiers, managedExecutorBinding.qualifiers, "qualifier");
        mergeSavedValue(properties, managedExecutorBinding.properties, "properties");
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
        addOrRemoveProperty(props, KEY_VIRTUAL, virtual);
        addOrRemoveProperty(props, KEY_QUALIFIERS, qualifiers);

        setObjects(null, createDefinitionReference(null, ManagedExecutorService.class.getName(), props));
    }

    @Trivial
    static final String toString(ManagedExecutorDefinition anno) {
        StringBuilder b = new StringBuilder();
        b.append("ManagedExecutorDefinition@").append(Integer.toHexString(anno.hashCode())) //
                        .append("(name=").append(anno.name()) //
                        .append(", context=").append(anno.context()) //
                        .append(", hungTaskThreshold=").append(anno.hungTaskThreshold()) //
                        .append(", maxAsync=").append(anno.maxAsync()) //
                        .append(", virtual=").append(anno.virtual()) //
                        .append(", qualifiers=").append(Arrays.toString(anno.qualifiers())) //
                        .append(")");
        return b.toString();
    }

    @Trivial
    private static final <T> String toString(T[] list) {
        if (list == null || list.length == 0)
            return "Unspecified";
        boolean none = true;
        for (int i = 0; none && i < list.length; i++)
            none &= list[i] == null || list[i].toString().isEmpty();
        return none ? "None" : Arrays.toString(list);
    }

    @Trivial
    private static final Class<?>[] toQualifierClassArray(String[] classList) throws IllegalArgumentException {
        Class<?>[] clazzArray = new Class<?>[classList.length];
        for (int i = 0; i < classList.length; i++) {
            try {
                //TODO is there a certain classloader I should be using to load this class? ApplicationClassLoader?
                clazzArray[i] = Class.forName(classList[i]);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKC1205.qualifier.class.not.found", classList[i]), e);
            }
        }

        return clazzArray;
    }
}
