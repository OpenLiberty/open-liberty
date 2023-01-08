/*******************************************************************************
 * Copyright (c) 2021,2022 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.javaee.dd.common.Description;
import com.ibm.ws.javaee.dd.common.ManagedThreadFactory;
import com.ibm.ws.javaee.dd.common.Property;
import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionConfigurationException;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.JNDIEnvironmentRefType;

import jakarta.enterprise.concurrent.ManagedThreadFactoryDefinition;

/**
 * Injection binding for ManagedThreadFactoryDefinition annotation
 * and managed-executor deployment descriptor element.
 */
public class ManagedThreadFactoryDefinitionBinding extends InjectionBinding<ManagedThreadFactoryDefinition> {
    private static final TraceComponent tc = Tr.register(ManagedThreadFactoryDefinitionBinding.class);

    private static final String KEY_CONTEXT = "context";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_PRIORITY = "priority";

    private String contextServiceJndiName;
    private boolean XMLContextServiceRef;

    private String description;
    private boolean XMLDescription;

    private Integer priority;
    private boolean XMLPriority;

    private Map<String, String> properties;
    private final Set<String> XMLProperties = new HashSet<String>();

    public ManagedThreadFactoryDefinitionBinding(String jndiName, ComponentNameSpaceConfiguration nameSpaceConfig) {
        super(null, nameSpaceConfig);
        setJndiName(jndiName);
    }

    @Override
    public Class<?> getAnnotationType() {
        return ManagedThreadFactoryDefinition.class;
    }

    @Override
    protected JNDIEnvironmentRefType getJNDIEnvironmentRefType() {
        return JNDIEnvironmentRefType.ManagedThreadFactory;
    }

    @Override
    public void merge(ManagedThreadFactoryDefinition annotation, Class<?> instanceClass, Member member) throws InjectionException {
        final boolean trace = TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (trace)
            Tr.entry(this, tc, "merge", toString(annotation), instanceClass, member,
                     (XMLContextServiceRef ? "(xml)" : "     ") + "contextServiceRef: " + contextServiceJndiName + " << " + annotation.context(),
                     (XMLPriority ? "         (xml)" : "              ") + "priority: " + priority + " << " + annotation.priority());

        if (member != null) {
            // ManagedThreadFactoryDefinition is a class-level annotation only.
            throw new IllegalArgumentException(member.toString());
        }

        contextServiceJndiName = mergeAnnotationValue(contextServiceJndiName, XMLContextServiceRef, annotation.context(), KEY_CONTEXT, "java:comp/DefaultContextService");
        description = mergeAnnotationValue(description, XMLDescription, "", KEY_DESCRIPTION, ""); // ManagedThreadFactoryDefinition has no description attribute
        priority = mergeAnnotationValue(priority, XMLPriority, annotation.priority(), KEY_PRIORITY, Thread.NORM_PRIORITY);
        properties = mergeAnnotationProperties(properties, XMLProperties, new String[] {}); // ManagedThreadFactoryDefinition has no properties attribute

        if (trace)
            Tr.exit(this, tc, "merge", new String[] {
                                                      (XMLContextServiceRef ? "(xml)" : "     ") + "contextServiceRef= " + contextServiceJndiName,
                                                      (XMLPriority ? "         (xml)" : "              ") + "priority= " + priority
            });
    }

    void mergeXML(ManagedThreadFactory mtfd) throws InjectionConfigurationException {
        final boolean trace = TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (trace)
            Tr.entry(this, tc, "mergeXML", mtfd, mtfd.getName(),
                     (XMLContextServiceRef ? "(xml)" : "     ") + "contextServiceRef: " + contextServiceJndiName + " << " + mtfd.getContextServiceRef(),
                     (XMLPriority ? "         (xml)" : "              ") + "priority: " + priority + " << " + mtfd.getPriority());

        List<Description> descriptionList = mtfd.getDescriptions();

        String contextServiceRefValue = mtfd.getContextServiceRef();
        if (contextServiceRefValue != null) {
            contextServiceJndiName = mergeXMLValue(contextServiceJndiName, contextServiceRefValue, "context-service-ref", KEY_CONTEXT, null);
            XMLContextServiceRef = true;
        }

        if (description != null) {
            description = mergeXMLValue(description, descriptionList.toString(), "description", KEY_DESCRIPTION, null);
            XMLDescription = true;
        }

        if (mtfd.isSetPriority()) {
            priority = mergeXMLValue(priority, mtfd.getPriority(), "priority", KEY_PRIORITY, null);
            XMLPriority = true;
        }

        List<Property> mxdProps = mtfd.getProperties();
        properties = mergeXMLProperties(properties, XMLProperties, mxdProps);

        if (trace)
            Tr.exit(this, tc, "mergeXML", new String[] {
                                                         (XMLContextServiceRef ? "(xml)" : "     ") + "contextServiceRef= " + contextServiceJndiName,
                                                         (XMLPriority ? "         (xml)" : "              ") + "priority= " + priority
            });
    }

    @Override
    public void mergeSaved(InjectionBinding<ManagedThreadFactoryDefinition> injectionBinding) throws InjectionException {
        ManagedThreadFactoryDefinitionBinding managedThreadFactoryBinding = (ManagedThreadFactoryDefinitionBinding) injectionBinding;

        mergeSavedValue(contextServiceJndiName, managedThreadFactoryBinding.contextServiceJndiName, "context-service-ref");
        mergeSavedValue(description, managedThreadFactoryBinding.description, "description");
        mergeSavedValue(priority, managedThreadFactoryBinding.priority, "priority");
        mergeSavedValue(properties, managedThreadFactoryBinding.properties, "properties");
    }

    void resolve() throws InjectionException {
        Map<String, Object> props = new HashMap<String, Object>();

        if (properties != null) {
            props.putAll(properties);
        }

        // Insert all remaining attributes.
        addOrRemoveProperty(props, KEY_CONTEXT, contextServiceJndiName);
        addOrRemoveProperty(props, KEY_DESCRIPTION, description);
        addOrRemoveProperty(props, KEY_PRIORITY, priority);

        setObjects(null, createDefinitionReference(null, jakarta.enterprise.concurrent.ManagedThreadFactory.class.getName(), props));
    }

    @Trivial
    static final String toString(ManagedThreadFactoryDefinition anno) {
        StringBuilder b = new StringBuilder();
        b.append("ManagedThreadFactoryDefinition@").append(Integer.toHexString(anno.hashCode())) //
                        .append("(name=").append(anno.name()) //
                        .append(", context=").append(anno.context()) //
                        .append(", priority=").append(anno.priority()) //
                        .append(")");
        return b.toString();
    }
}
