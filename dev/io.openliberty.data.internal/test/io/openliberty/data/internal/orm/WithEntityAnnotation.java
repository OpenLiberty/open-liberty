/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.data.internal.orm;

import io.openliberty.data.internal.orm.TestConverters.ClassConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * A simple entity with an id
 * and a converter in the superclass
 */
@Entity
@Convert(converter = ClassConverter.class)
public class WithEntityAnnotation extends AnnotatedConverterSuper {

    @Id
    public long id;

}
