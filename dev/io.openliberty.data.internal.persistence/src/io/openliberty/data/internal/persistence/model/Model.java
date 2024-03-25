/*******************************************************************************
 * Copyright (c) 2023,2024 IBM Corporation and others.
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
package io.openliberty.data.internal.persistence.model;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import jakarta.data.metamodel.Attribute;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.AttributeRecord;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

/**
 * Utility class for the static metamodel.
 */
public class Model {
    private static final TraceComponent tc = Tr.register(Model.class);

    private static final Set<Class<?>> ATTRIBUTE_TYPES = new HashSet<>();
    static {
        ATTRIBUTE_TYPES.add(Attribute.class);
        ATTRIBUTE_TYPES.add(SortableAttribute.class);
        ATTRIBUTE_TYPES.add(String.class);
        ATTRIBUTE_TYPES.add(TextAttribute.class);
    }

    /**
     * Initialize fields of a static metamodel class.
     *
     * @param metamodelClass the metamodel class to initialize.
     * @param attributeNames mapping of lower case attribute name to properly cased/qualified JPQL attribute name.
     */
    public static void initialize(Class<?> metamodelClass, Map<String, String> attributeNames) {
        final boolean trace = TraceComponent.isAnyTracingEnabled();

        for (Field field : metamodelClass.getFields()) {
            int mod = field.getModifiers();
            if (Modifier.isPublic(mod)
                && Modifier.isStatic(mod)
                && !Modifier.isFinal(mod)) {
                Class<?> fieldType = field.getType();
                if (ATTRIBUTE_TYPES.contains(fieldType)) {
                    String fieldName = field.getName();
                    String attrName = attributeNames.get(fieldName.toLowerCase());
                    if (attrName != null)
                        try {
                            Object value = field.get(null);
                            if (value == null) {
                                if (String.class.equals(fieldType))
                                    value = attrName;
                                else if (SortableAttribute.class.equals(fieldType))
                                    value = new SortableAttributeRecord<>(fieldName);
                                else if (TextAttribute.class.equals(fieldType))
                                    value = new TextAttributeRecord<>(fieldName);
                                else
                                    value = new AttributeRecord<>(fieldName);

                                field.set(null, value);

                                if (trace && tc.isDebugEnabled())
                                    Tr.debug(tc, "initialize " + metamodelClass.getSimpleName() + '.' + fieldName + " (" + attrName + "): " + value);
                            }
                        } catch (IllegalAccessException | IllegalArgumentException x) {
                            System.out.println("Unable to initialize the " + fieldName + " field of the " +
                                               metamodelClass.getName() + " StaticMetamodel class. Valid attribute names are: " +
                                               new TreeSet<String>(attributeNames.values()) + "."); // TODO NLS
                        }
                }
            }
        }
    }
}
