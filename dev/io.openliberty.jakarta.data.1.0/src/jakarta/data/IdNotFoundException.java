/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package jakarta.data;

import java.util.function.Supplier;

import jakarta.data.exceptions.DataException;

/**
 * Copied from jakarta.nosql.mapping.IdNotFoundException to investigate
 * how well the NoSQL template pattern works for relational database access.
 * Some spelling errors are corrected after copying.
 */
public class IdNotFoundException extends DataException {
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
