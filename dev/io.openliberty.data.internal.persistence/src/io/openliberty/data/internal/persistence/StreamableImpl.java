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
package io.openliberty.data.internal.persistence;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.data.repository.Streamable;

/**
 * Enables Streamable to be used as a return type for repository methods.
 */
public class StreamableImpl<T> implements Streamable<T> {
    private final List<T> results;

    StreamableImpl(List<T> results) {
        this.results = results;
    }

    @Override
    public Iterator<T> iterator() {
        return results.iterator();
    }

    @Override
    public Stream<T> stream() {
        return StreamSupport.stream(results.spliterator(), false);
    }
}
