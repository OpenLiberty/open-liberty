/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.persistence.models;

/**
 * A class that has inner and nested classes, generic classes, and records
 */
public class Outer_Class {
    public class Inner_Class {
    }

    public static class Nested_Class {
    }

    public class Inner_Generic<T> {
    }

    public static class Nested_Generic<K> {
    }

    public record Inner_Record() {
    }

    public static record Nested_Record() {
    }

}
