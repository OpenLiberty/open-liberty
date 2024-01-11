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
    private static final TraceComponent tc = Tr.register(ContextServiceDefinitionBinding.class);

    private static final String KEY_CLEARED = "cleared";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_PROPAGATED = "propagated";
    private static final String KEY_UNCHANGED = "unchanged";
    private static final String KEY_QUALIFIERS = "qualifiers";

    private static final String[] DEFAULT_CLEARED = new String[] { ContextServiceDefinition.TRANSACTION };
    private static final String[] DEFAULT_PROPAGATED = new String[] { ContextServiceDefinition.ALL_REMAINING };
    private static final String[] DEFAULT_UNCHANGED = new String[] {};
    private static final String[] DEFAULT_QUALIFIERS = new String[] {};

    private final int eeVersion;

    // Concurrent 3.0 attributes

    private String[] cleared;
    private boolean XMLcleared;

    private String description;
    private boolean XMLDescription;

    private String[] propagated;
    private boolean XMLpropagated;

    private String[] unchanged;
    private boolean XMLunchanged;

    // Concurrent 3.1 attributes

    private String[] qualifiers;
    private boolean XMLqualifers;

    // General attribute

    private Map<String, String> properties;
    private final Set<String> XMLProperties = new HashSet<String>();

    public ContextServiceDefinitionBinding(String jndiName, ComponentNameSpaceConfiguration nameSpaceConfig, int eeVersion) {
        super(null, nameSpaceConfig);
        setJndiName(jndiName);
        this.eeVersion = eeVersion;
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
        final boolean trace = TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (trace)
            Tr.entry(this, tc, "merge", toString(annotation, eeVersion), instanceClass, member,
                     (XMLcleared ? "   (xml)" : "        ") + "cleared: " + toString(cleared) + " << " + toString(annotation.cleared()),
                     (XMLpropagated ? "(xml)" : "     ") + "propagated: " + toString(propagated) + " << " + toString(annotation.propagated()),
                     (XMLunchanged ? " (xml)" : "      ") + "unchanged: " + toString(unchanged) + " << " + toString(annotation.unchanged()),
                     (XMLqualifers ? " (xml)" : "     ") + "qualifiers: " + toString(qualifiers) + " << " + (eeVersion >= 11 ? toString(annotation.qualifiers()) : "Unspecified"));

        if (member != null) {
            // ContextServiceDefinition is a class-level annotation only.
            throw new IllegalArgumentException(member.toString());
        }

        cleared = mergeAnnotationValue(cleared == DEFAULT_CLEARED ? null : cleared,
                                       XMLcleared, annotation.cleared(), KEY_CLEARED, DEFAULT_CLEARED);

        description = mergeAnnotationValue(description, XMLDescription, "", KEY_DESCRIPTION, ""); // ContextServiceDefinition has no description attribute

        propagated = mergeAnnotationValue(propagated == DEFAULT_PROPAGATED ? null : propagated,
                                          XMLpropagated, annotation.propagated(), KEY_PROPAGATED, DEFAULT_PROPAGATED);

        unchanged = mergeAnnotationValue(unchanged, XMLunchanged, annotation.unchanged(), KEY_UNCHANGED, DEFAULT_UNCHANGED);

        //Only merge EE 11 annotations when present, otherwise rely on defaults from mergeXML
        if (eeVersion >= 11)
            qualifiers = mergeAnnotationValue(qualifiers, XMLqualifers, toQualifierStringArray(annotation.qualifiers()), KEY_QUALIFIERS, DEFAULT_QUALIFIERS);

        properties = mergeAnnotationProperties(properties, XMLProperties, new String[] {}); // ContextServiceDefinition has no properties attribute

        if (trace)
            Tr.exit(this, tc, "merge", new String[] {
                                                      (XMLcleared ? "   (xml)" : "        ") + "cleared= " + toString(cleared),
                                                      (XMLpropagated ? "(xml)" : "     ") + "propagated= " + toString(propagated),
                                                      (XMLunchanged ? " (xml)" : "      ") + "unchanged= " + toString(unchanged),
                                                      (XMLqualifers ? " (xml)" : "     ") + "qualifiers= " + toString(qualifiers)
            });
    }

    void mergeXML(ContextService csd) throws InjectionConfigurationException {
        final boolean trace = TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled();
        if (trace)
            Tr.entry(this, tc, "mergeXML", csd, csd.getName(),
                     (XMLcleared ? "   (xml)" : "        ") + "cleared: " + toString(cleared) + " << " + toString(csd.getCleared()),
                     (XMLpropagated ? "(xml)" : "     ") + "propagated: " + toString(propagated) + " << " + toString(csd.getPropagated()),
                     (XMLunchanged ? " (xml)" : "      ") + "unchanged: " + toString(unchanged) + " << " + toString(csd.getUnchanged()),
                     (XMLqualifers ? " (xml)" : "     ") + "qualifiers: " + toString(qualifiers) + " << " + toString(csd.getQualifiers()));

        List<Description> descriptionList = csd.getDescriptions();

        String[] clearedValues = csd.getCleared();
        if (clearedValues == null || clearedValues.length == 0) {
            if (cleared == null)
                cleared = DEFAULT_CLEARED;
        } else {
            cleared = mergeXMLValue(cleared, clearedValues, "cleared", KEY_CLEARED, null);
            XMLcleared = true;
        }

        if (description != null) {
            description = mergeXMLValue(description, descriptionList.toString(), "description", KEY_DESCRIPTION, null);
            XMLDescription = true;
        }

        String[] propagatedValues = csd.getPropagated();
        if (propagatedValues == null || propagatedValues.length == 0) {
            if (propagated == null)
                propagated = DEFAULT_PROPAGATED;
        } else {
            propagated = mergeXMLValue(propagated, propagatedValues, "propagated", KEY_PROPAGATED, null);
            XMLpropagated = true;
        }

        String[] unchangedValues = csd.getUnchanged();
        if (unchangedValues != null && unchangedValues.length > 0) {
            unchanged = mergeXMLValue(unchanged, unchangedValues, "unchanged", KEY_UNCHANGED, null);
            XMLunchanged |= true;
        }

        String[] qualifierValues = csd.getQualifiers();
        if (qualifierValues == null || qualifierValues.length == 0) {
            // No qualifiers provided via xml
            if (qualifiers == null)
                qualifiers = DEFAULT_QUALIFIERS;
        } else if (qualifierValues.length == 1 && qualifierValues[0].isEmpty()) {
            // Special case <qualifier></qualifier>
            qualifiers = DEFAULT_QUALIFIERS;
            XMLqualifers = true;
        } else {
            // List of qualifiers provided
            qualifiers = mergeXMLValue(qualifiers, qualifierValues, "qualifier", KEY_QUALIFIERS, null);
            XMLqualifers = true;
        }

        List<Property> csdProps = csd.getProperties();
        properties = mergeXMLProperties(properties, XMLProperties, csdProps);

        if (trace)
            Tr.exit(this, tc, "mergeXML", new String[] {
                                                         (XMLcleared ? "   (xml)" : "        ") + "cleared= " + toString(cleared),
                                                         (XMLpropagated ? "(xml)" : "     ") + "propagated= " + toString(propagated),
                                                         (XMLunchanged ? " (xml)" : "      ") + "unchanged= " + toString(unchanged),
                                                         (XMLqualifers ? " (xml)" : "     ") + "qualifiers= " + toString(qualifiers)
            });
    }

    @Override
    public void mergeSaved(InjectionBinding<ContextServiceDefinition> injectionBinding) throws InjectionException {
        ContextServiceDefinitionBinding contextServiceBinding = (ContextServiceDefinitionBinding) injectionBinding;

        mergeSavedValue(cleared, contextServiceBinding.cleared, "cleared");
        mergeSavedValue(description, contextServiceBinding.description, "description");
        mergeSavedValue(propagated, contextServiceBinding.propagated, "propagated");
        mergeSavedValue(unchanged, contextServiceBinding.unchanged, "unchanged");
        mergeSavedValue(qualifiers, contextServiceBinding.qualifiers, "qualifier");
        mergeSavedValue(properties, contextServiceBinding.properties, "properties");

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
        addOrRemoveProperty(props, KEY_QUALIFIERS, qualifiers);

        setObjects(null, createDefinitionReference(null, jakarta.enterprise.concurrent.ContextService.class.getName(), props));
    }

    @Trivial
    static final String toString(ContextServiceDefinition anno, int eeVersion) {
        StringBuilder b = new StringBuilder();
        b.append("ContextServiceDefinition@") //
                        .append(Integer.toHexString(anno.hashCode())) //
                        .append("#EE").append(eeVersion) //
                        .append("(name=").append(anno.name()) //
                        .append(", cleared=").append(Arrays.toString(anno.cleared())) //
                        .append(", propagated=").append(Arrays.toString(anno.propagated())) //
                        .append(", unchanged=").append(Arrays.toString(anno.unchanged()));

        if (eeVersion >= 11)
            b.append(", qualifiers=").append(Arrays.toString(anno.qualifiers()));

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
