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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 * A record that tests generic components
 */
public record Generic(
                List<String> single,
                Map<Integer, String> multiple,
                Set<Map<Integer, String>> nested,
                Class<Integer>[] array,
                Set<Class<Integer>[]> embeddedArray,
                Vector<?> unknown,
                Class<? extends Serializable> subtypes,
                Class<? super Integer> supertypes,
                MyMap<Number, String>.MyIterator<Integer> inner) {
}
