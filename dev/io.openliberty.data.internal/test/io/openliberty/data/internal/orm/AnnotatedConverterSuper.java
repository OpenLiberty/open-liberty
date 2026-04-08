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

import io.openliberty.data.internal.orm.TestConverters.FieldConverter;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

/**
 * Mapped superclass with a converter
 */
@MappedSuperclass
public class AnnotatedConverterSuper {

    @Convert(converter = FieldConverter.class)
    public String firstName;

    @Embedded
    public AnnotatedConverterEmbedded emb;

}
