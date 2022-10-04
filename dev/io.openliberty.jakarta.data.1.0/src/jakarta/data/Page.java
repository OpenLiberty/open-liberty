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
package jakarta.data;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Copied from jakarta.nosql.mapping.Page to investigate how well the
 * JNoSQL repository-related annotations work for relational database access.
 */
public interface Page<T> extends Supplier<Stream<T>> {
    Pageable getPagination();

    Page<T> next();

    Stream<T> getContent(); // Why do we have this when Stream<T> get() is inherited from Supplier?

    <C extends Collection<T>> C getContent(Supplier<C> collectionFactory);
}
