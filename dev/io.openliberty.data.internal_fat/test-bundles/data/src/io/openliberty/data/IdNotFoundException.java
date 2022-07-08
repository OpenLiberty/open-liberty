/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.data;

import java.util.function.Supplier;

/**
 * Copied from jakarta.nosql.mapping.IdNotFoundException to investigate
 * how well the NoSQL template pattern works for relational database access.
 * Some spelling errors are corrected after copying.
 */
public class IdNotFoundException extends MappingException {
    public static final Supplier<IdNotFoundException> KEY_NOT_FOUND_EXCEPTION_SUPPLIER = //
                    () -> new IdNotFoundException("To use this resource, you must annotate a field with @Id");

    public IdNotFoundException(String message) {
        super(message);
    }

    public static IdNotFoundException newInstance(Class<?> clazz) {
        String message = "The entity " + clazz + " must have a field annotated with @Id";
        return new IdNotFoundException(message);
    }
}
