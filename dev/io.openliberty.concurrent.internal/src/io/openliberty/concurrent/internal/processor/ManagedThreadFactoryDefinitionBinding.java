/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
    private static final String KEY_VIRTUAL = "virtual";
    private static final String KEY_QUALIFIERS = "qualifiers";

    private static final boolean DEFAULT_VIRTUAL = false;
    private static final String[] DEFAULT_QUALIFIERS = new String[] {};

    private final int eeVersion;

    // Concurrent 3.0 attributes

    private String contextServiceJndiName;
    private boolean XMLContextServiceRef;

    private String description;
    private boolean XMLDescription;

    private Integer priority;
    private boolean XMLPriority;

    // Concurrent 3.1 attributes

    private Boolean virtual;
    private boolean XMLvirtual;

    private String[] qualifiers;
    private boolean XMLqualifers;

    // General attribute

    private Map<String, String> properties;
    private final Set<String> XMLProperties = new HashSet<String>();

    public ManagedThreadFactoryDefinitionBinding(String jndiName, ComponentNameSpaceConfiguration nameSpaceConfig, int eeVersion) {
        super(null, nameSpaceConfig);
        setJndiName(jndiName);
        this.eeVersion = eeVersion;
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
            Tr.entry(this, tc, "merge", toString(annotation, eeVersion), instanceClass, member,
                     (XMLContextServiceRef ? "(xml)" : "     ") + "contextServiceRef: " + contextServiceJndiName + " << " + annotation.context(),
                     (XMLPriority ? "         (xml)" : "              ") + "priority: " + priority + " << " + annotation.priority(),
                     (XMLvirtual ? "          (xml)" : "               ") + "virtual: " + virtual + " << " + (eeVersion >= 11 ? annotation.virtual() : "Unspecified"),
                     (XMLqualifers ? "        (xml)" : "            ") + "qualifiers: " + toString(qualifiers) + " << " + (eeVersion >= 11 ? toString(annotation.qualifiers()) : "Unspecified"));

        if (member != null) {
            // ManagedThreadFactoryDefinition is a class-level annotation only.
            throw new IllegalArgumentException(member.toString());
        }

        contextServiceJndiName = mergeAnnotationValue(contextServiceJndiName, XMLContextServiceRef, annotation.context(), KEY_CONTEXT, "java:comp/DefaultContextService");
        description = mergeAnnotationValue(description, XMLDescription, "", KEY_DESCRIPTION, ""); // ManagedThreadFactoryDefinition has no description attribute
        priority = mergeAnnotationValue(priority, XMLPriority, annotation.priority(), KEY_PRIORITY, Thread.NORM_PRIORITY);

        //Only merge EE 11 annotations when present, otherwise rely on defaults from mergeXML
        if (eeVersion >= 11) {
            virtual = mergeAnnotationBoolean(virtual, XMLvirtual, annotation.virtual(), KEY_VIRTUAL, DEFAULT_VIRTUAL);
            qualifiers = mergeAnnotationValue(qualifiers, XMLqualifers, toQualifierStringArray(annotation.qualifiers()), KEY_QUALIFIERS, DEFAULT_QUALIFIERS);
        }

        properties = mergeAnnotationProperties(properties, XMLProperties, new String[] {}); // ManagedThreadFactoryDefinition has no properties attribute

        if (trace)
            Tr.exit(this, tc, "merge", new String[] {
                                                      (XMLContextServiceRef ? "(xml)" : "     ") + "contextServiceRef= " + contextServiceJndiName,
                                                      (XMLPriority ? "         (xml)" : "              ") + "priority= " + priority,
                                                      (XMLvirtual ? "          (xml)" : "               ") + "virtual= " + virtual,
                                                      (XMLqualifers ? "        (xml)" : "            ") + "qualifiers= " + toString(qualifiers)
            });
    }

    void mergeXML(ManagedThreadFactory mtfd) throws InjectionConfigurationException {
        final boolean trace = TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (trace)
            Tr.entry(this, tc, "mergeXML", mtfd, mtfd.getName(),
                     (XMLContextServiceRef ? "(xml)" : "     ") + "contextServiceRef: " + contextServiceJndiName + " << " + mtfd.getContextServiceRef(),
                     (XMLPriority ? "         (xml)" : "              ") + "priority: " + priority + " << " + mtfd.getPriority(),
                     (XMLvirtual ? "          (xml)" : "               ") + "virtual: " + virtual + " << " + mtfd.isVirtual(),
                     (XMLqualifers ? "        (xml)" : "            ") + "qualifiers: " + toString(qualifiers) + " << " + toString(mtfd.getQualifiers()));

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

        if (mtfd.isSetVirtual()) {
            virtual = mergeXMLValue(virtual, mtfd.isVirtual(), "virtual", KEY_VIRTUAL, null);
            XMLvirtual = true;
        }

        String[] qualifierValues = mtfd.getQualifiers();
        if (qualifierValues == null || qualifierValues.length == 0) {
            // No qualifiers provided via xml
            if (qualifiers == null)
                qualifiers = DEFAULT_QUALIFIERS;
        } else if (qualifierValues.length == 1 && qualifierValues[0].isEmpty()) {
            // Special case <qualifier></qualifier>
            qualifiers = DEFAULT_QUALIFIERS;
            XMLqualifers = true;
        } else {
            // Actual list of qualifiers provided
            qualifiers = mergeXMLValue(qualifiers, qualifierValues, "qualifier", KEY_QUALIFIERS, null);
            XMLqualifers = true;
        }

        List<Property> mxdProps = mtfd.getProperties();
        properties = mergeXMLProperties(properties, XMLProperties, mxdProps);

        if (trace)
            Tr.exit(this, tc, "mergeXML", new String[] {
                                                         (XMLContextServiceRef ? "(xml)" : "     ") + "contextServiceRef= " + contextServiceJndiName,
                                                         (XMLPriority ? "         (xml)" : "              ") + "priority= " + priority,
                                                         (XMLvirtual ? "          (xml)" : "               ") + "virtual= " + virtual,
                                                         (XMLqualifers ? "        (xml)" : "            ") + "qualifiers= " + toString(qualifiers)
            });
    }

    @Override
    public void mergeSaved(InjectionBinding<ManagedThreadFactoryDefinition> injectionBinding) throws InjectionException {
        ManagedThreadFactoryDefinitionBinding managedThreadFactoryBinding = (ManagedThreadFactoryDefinitionBinding) injectionBinding;

        mergeSavedValue(contextServiceJndiName, managedThreadFactoryBinding.contextServiceJndiName, "context-service-ref");
        mergeSavedValue(description, managedThreadFactoryBinding.description, "description");
        mergeSavedValue(priority, managedThreadFactoryBinding.priority, "priority");
        mergeSavedValue(virtual, managedThreadFactoryBinding.virtual, "virtual");
        mergeSavedValue(qualifiers, managedThreadFactoryBinding.qualifiers, "qualifier");
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
        addOrRemoveProperty(props, KEY_VIRTUAL, virtual);
        addOrRemoveProperty(props, KEY_QUALIFIERS, qualifiers);

        setObjects(null, createDefinitionReference(null, jakarta.enterprise.concurrent.ManagedThreadFactory.class.getName(), props));
    }

    @Trivial
    static final String toString(ManagedThreadFactoryDefinition anno, int eeVersion) {
        StringBuilder b = new StringBuilder();
        b.append("ManagedThreadFactoryDefinition@") //
                        .append(Integer.toHexString(anno.hashCode())) //
                        .append("#EE").append(eeVersion) //
                        .append("(name=").append(anno.name()) //
                        .append(", context=").append(anno.context()) //
                        .append(", priority=").append(anno.priority());

        if (eeVersion >= 11) {
            b.append(", virtual=").append(anno.virtual());
            b.append(", qualifiers=").append(Arrays.toString(anno.qualifiers()));
        }

        b.append(")");
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
    private static final String[] toQualifierStringArray(Class<?>[] classList) {
        String[] qualifierNames = new String[classList.length];
        for (int i = 0; i < classList.length; i++) {
            qualifierNames[i] = classList[i].getCanonicalName();
        }
        return qualifierNames;
    }
}
