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

import io.openliberty.data.internal.persistence.models.Outer_Class.Nested_Class;
import io.openliberty.data.internal.persistence.models.Outer_Class.Nested_Generic;
import io.openliberty.data.internal.persistence.models.Outer_Class.Nested_Record;

/**
 * A record that tests special typed components
 */
public record Special(
                _Enum e,
                String[] array,
                Outer_Class oc,
                Outer_Class.Inner_Class ic,
                Nested_Class nc,
                Outer_Class.Inner_Generic<?> ig,
                Nested_Generic<? extends Object> ng,
                Outer_Class.Inner_Record ir,
                Nested_Record nr) {
}
