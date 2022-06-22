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

import java.util.List;
import java.util.function.Supplier;

import io.openliberty.data.internal.SortsImpl;

/**
 * Copied from jakarta.nosql.mapping.Sorts to investigate how well the
 * JNoSQL repository-related classes work for relational database access.
 * Unable to copy the of method because it depends on a JNoSQL class, ServiceLoaderProvider.
 */
public interface Sorts {
    Sorts asc(String name);

    Sorts desc(String name);

    Sorts add(Sort sort);

    Sorts remove(Sort sort);

    List<Sort> getSorts();

    static Sorts sorts() {
        // We don't have the JNoSQL ServiceLoaderProvider here.
        return new SortsImpl.Provider().get();
    }

    interface SortsProvider extends Supplier<Sorts> {
    }
}
