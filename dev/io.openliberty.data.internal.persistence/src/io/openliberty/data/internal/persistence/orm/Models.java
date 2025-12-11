/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.orm;

import java.lang.reflect.Type;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Maintains the model types for the managed objects for the orm
 * - MappedSuperclass
 * - Entity
 * - Embeddable
 * - Converter
 *
 * As well as sub-model types of these managed objects for the orm
 * - Attribute
 *
 * Each model extends comparable so that model objects are ordered as they
 * are discovered.
 */
public class Models {
    @Trivial
    enum AccessType {
        FIELD, PROPERTY;
    }

    /**
     * Describes the kind of attributes found under the `attributes` element.
     * Note: the order of this enum is used to order each attribute.
     */
    @Trivial
    enum AttributeKind {
        ID, EMBEDDED_ID, BASIC, VERSION, BASIC$ELEMENT_COLLECTION, EMBEDDED$ELEMENT_COLLECTION, EMBEDDED;

        public String toElementName() {
            if (this.name().contains("$"))
                return this.name().substring(this.name().indexOf('$') + 1).toLowerCase().replace('_', '-');
            else
                return this.name().toLowerCase().replace('_', '-');
        }
    }

    @Trivial
    static class Attribute implements Comparable<Attribute> {
        private final Class<?> type;
        private final Type genericType;
        private final String name;
        private final AccessType access;

        private AttributeKind kind;
        private Attribute collectionId = null;
        private Class<?> collectionType = null;
        private Set<Attribute> overrides = Set.of();

        public Attribute(Class<?> type, Type genericType, String name, AccessType access) {
            this.type = type;
            this.genericType = genericType;
            this.name = name;
            this.access = access;
        }

        public AttributeKind kind() {
            return kind;
        }

        public void setKind(AttributeKind kind) {
            this.kind = kind;
        }

        public Attribute collectionId() {
            return collectionId;
        }

        public void setCollectionId(Attribute collectionId) {
            this.collectionId = collectionId;
        }

        public Class<?> collectionType() {
            return collectionType;
        }

        public void setCollectionType(Class<?> collectionType) {
            this.collectionType = collectionType;
        }

        public Set<Attribute> overrides() {
            return overrides;
        }

        public void setOverrides(Set<Attribute> overrides) {
            this.overrides = overrides;
        }

        public Class<?> type() {
            return type;
        }

        public Type genericType() {
            return genericType;
        }

        public String name() {
            return name;
        }

        public AccessType access() {
            return access;
        }

        public boolean isCollection() {
            return this.kind == AttributeKind.BASIC$ELEMENT_COLLECTION || //
                   this.kind == AttributeKind.EMBEDDED$ELEMENT_COLLECTION;
        }

        public boolean isEmbedded() {
            return this.kind == AttributeKind.EMBEDDED || //
                   this.kind == AttributeKind.EMBEDDED_ID;
        }

        public boolean isId() {
            return this.kind == AttributeKind.EMBEDDED_ID || //
                   this.kind == AttributeKind.ID;
        }

        public boolean isEmbeddedCollection() {
            return this.kind == AttributeKind.EMBEDDED$ELEMENT_COLLECTION;
        }

        public boolean rejectsNull() {
            return this.type().isPrimitive() && //
                   this.kind != AttributeKind.EMBEDDED;
        }

        @Override
        public int compareTo(Attribute o) {
            if (this.kind == o.kind)
                return this.name.compareTo(o.name);

            if (this.kind == null)
                return -1;

            if (o.kind == null)
                return 1;

            return Integer.compare(this.kind.ordinal(), o.kind.ordinal());
        }

        @Override
        public String toString() {
            return "Attribute [type=" + type + //
                   ", genericType=" + genericType + //
                   ", name=" + name + //
                   ", access=" + access + //
                   ", kind=" + kind + //
                   ", collectionId=" + collectionId + //
                   ", collectionType=" + collectionType + //
                   ", overrides=" + overrides + "]";
        }
    }

    @Trivial
    static record MappedSuperclass(Class<?> type, Set<Attribute> attributes)
                    implements Comparable<MappedSuperclass> {

        @Override
        public int compareTo(MappedSuperclass o) {
            return this.type.getName().compareTo(o.type.getName());
        }
    }

    @Trivial
    static record EntityRecord(Class<?> type, String tableName, Set<Attribute> attributes)
                    implements Comparable<EntityRecord> {

        @Override
        public int compareTo(EntityRecord o) {
            return this.type.getName().compareTo(o.type.getName());
        }
    }

    @Trivial
    static record EmbeddableRecord(Class<?> type, Set<Attribute> attributes)
                    implements Comparable<EmbeddableRecord> {

        @Override
        public int compareTo(EmbeddableRecord o) {
            return this.type.getName().compareTo(o.type.getName());
        }
    }

    @Trivial
    static record Converter(Class<?> type) implements Comparable<Converter> {

        @Override
        public int compareTo(Converter o) {
            return this.type.getName().compareTo(o.type.getName());
        }

    }
}
